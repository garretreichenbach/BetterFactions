package videogoose.betterfactions;

import api.event.network.ClientDisconnectListener;
import api.event.entity.SegmentControllerChangeFactionListener;
import api.event.entity.SegmentControllerOverheatListener;
import api.event.faction.FactionDeleteListener;
import api.event.faction.FactionDiplomacyActionListener;
import api.event.gui.FactionPanelCreateListener;
import api.event.faction.FactionRelationQueryListener;
import api.event.faction.NPCExecuteDiplomacyReactionListener;
import api.event.EventResult;
import api.event.lifecycle.ServerInitializeListener;
import api.event.lifecycle.ServerUpdateListener;
import api.event.player.PlayerJoinWorldListener;
import api.event.player.BuyTradeListener;
import api.event.player.SellTradeListener;
import api.event.player.ShopPriceListener;
import org.schema.common.util.StringTools;
import org.schema.schine.common.language.Lng;
import api.event.world.ServerSendableRemoveListener;
import api.event.world.WorldSaveListener;
import org.schema.game.common.controller.SegmentController;
import org.schema.schine.network.objects.Sendable;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import org.schema.game.client.view.mainmenu.GuidesRegistry;
import org.schema.game.common.data.world.Universe;
import org.schema.game.server.data.GameServerState;
import videogoose.betterfactions.assets.FactionAssetIndex;
import videogoose.betterfactions.battle.BattleTracker;
import videogoose.betterfactions.battle.BattleWorldTests;
import org.schema.game.server.test.TestRegistry;
import org.schema.game.server.test.world.WorldTests;
import videogoose.betterfactions.commands.BattleCommands;
import videogoose.betterfactions.commands.DiplomacyCommands;
import videogoose.betterfactions.diplomacy.DefaultDiplomacySettings;
import videogoose.betterfactions.diplomacy.DiplomacyAction;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;
import videogoose.betterfactions.gui.FactionPanelTabs;
import videogoose.betterfactions.net.BFPacket;
import videogoose.betterfactions.repair.FactionDesigns;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.CivicPayouts;
import org.schema.game.common.data.player.PlayerState;
import org.schema.common.util.linAlg.Vector3i;
import videogoose.betterfactions.civics.Council;
import videogoose.betterfactions.civics.Senate;
import videogoose.betterfactions.fleet.FleetIntercepts;
import videogoose.betterfactions.repair.ShipyardRepairs;

/**
 * Treaties, opinion, federations and emblems for every faction, player and NPC alike.
 *
 * <p>The engine is reached only through events: the relation query hook is what makes a signed
 * treaty change who shoots at whom, and everything else is the mod's own state, synced over
 * {@link BFPacket} and saved by {@link BFStore}.</p>
 */
public final class BetterFactions extends StarMod {

	private static BetterFactions instance;
	private FactionAssetIndex assetIndex;

	public BetterFactions() {
		instance = this;
	}

	public static BetterFactions getInstance() {
		return instance;
	}

	public FactionAssetIndex getAssetIndex() {
		return assetIndex;
	}

	@Override
	public void onEnable() {
		BFConfig.initialize(this);
		Perms.define();
		PacketUtil.registerPacket(this, BFPacket.class);
		registerEvents();
		//the battle system's in-world tests: /run_tests betterfactions/*
		WorldTests.addExtension(new BattleWorldTests());
		DiplomacyCommands.register(this);
		BattleCommands.register(this);
	}

	@Override
	public void onRegisterGuides(GuidesRegistry.ModGuideRegistrar registrar) {
		//same section key and title as the built-in factions guide, which replaces it
		registrar.registerFromResource("07-multiplayer", "Multiplayer", "Factions", "guides/factions.md", this);
		registrar.registerFromResource("betterfactions", "BetterFactions", "Treaties", "guides/treaties.md", this);
		registrar.registerFromResource("betterfactions", "BetterFactions", "Opinion and NPC Factions", "guides/opinion.md", this);
		registrar.registerFromResource("betterfactions", "BetterFactions", "Federations", "guides/federations.md", this);
		registrar.registerFromResource("betterfactions", "BetterFactions", "Server Administration", "guides/server-admins.md", this);
	}

	/**
	 * {@code /run_tests @videogoose.betterfactions}: the self-checks here, plus the world tests in
	 * {@code tests/*.json}, which the engine finds in the jar on its own.
	 */
	@Override
	public void onRegisterTests(TestRegistry.ModTestRegistrar registrar) {
		registrar.register(InGameChecks.class);
	}

	@Override
	public void onDisable() {
		//drops types other mods registered with us, so a re-enable does not stack duplicates
		TreatyTypeRegistry.unregisterAll();
		CivicRegistry.unregisterAll();
		if (GameServerState.instance != null) {
			BFStore.save(DiplomacyManager.server());
			BFStore.save(BattleTracker.get());
			BFStore.save(FactionDesigns.server());
			BFStore.save(Civics.server());
			BFStore.save(Senate.server());
			BFStore.save(Council.server());
			BFStore.save(videogoose.betterfactions.civics.Corporations.server());
			BFStore.save(ShipyardRepairs.get());
			BFStore.save(FleetIntercepts.get());
			BFStore.save(Wrecks.get());
			BFStore.save(FleetUpkeep.get());
			BFStore.save(DiplomacyManager.server().getPointMods());
		}
	}

	private void registerEvents() {
		//the one hook that makes a treaty mean something to combat: a pair's treaties outrank its
		//relation byte, and pairs with no treaty that says anything about hostility fall through
		FactionRelationQueryListener.TYPE.register((a, b, current, isServer) -> DiplomacyManager.of(isServer).derivedRelation(a, b), this);

		FactionDeleteListener.TYPE.register((faction, isServer) -> DiplomacyManager.of(isServer).onFactionDeleted(faction), this);

		//a player claims only free space or its own: the engine leaves that to the client's claim button,
		//and a ceded system keeps the loser's station in it - nothing else would stop that station taking
		//the system straight back. Server-made changes (sender 0: treaties, revokes, admins) pass.
		api.event.faction.SystemClaimListener.TYPE.register((system, change, sectorPos, sector, faction, isServer) -> {
			if (isServer && change.sender != 0 && faction != null && system.getOwnerFaction() != 0
					&& system.getOwnerFaction() != faction.getIdFaction()) {
				return EventResult.CANCEL;
			}
			//a system newly ours counts toward a Council agenda to claim systems
			int was = system.getOwnerFaction();
			int now = faction != null ? faction.getIdFaction() : 0;
			if (isServer && now != 0 && was != now) {
				Council.server().onClaim(now);
				CivicPayouts.server().fire(CivicPayouts.CivicEvent.of(CivicPayouts.Event.SYSTEM_CLAIMED, now, was));
			}
			if (isServer && was != 0 && was != now) {
				CivicPayouts.server().fire(CivicPayouts.CivicEvent.of(CivicPayouts.Event.SYSTEM_LOST, was, now));
			}
			return EventResult.CONTINUE;
		}, this);

		//every faction keeps a ledger now, not only NPC ones, so the engine's opinion events feed it
		FactionDiplomacyActionListener.TYPE.register((type, toFaction, otherDbId) -> {
			DiplomacyAction.DiplActionType mine = DiplomacyAction.DiplActionType.fromVanilla(type);
			if (mine != null) {
				DiplomacyManager.server().diplomacyAction(mine, toFaction, otherDbId);
			}
			if (mine == DiplomacyAction.DiplActionType.ATTACK) {
				DiplomacyManager.server().onAttacked(toFaction, otherDbId);
			}
		}, this);

		//vanilla's own NPC ledger still runs; this keeps it from acting, so the mod's is the only one
		//that declares wars or offers peace
		NPCExecuteDiplomacyReactionListener.TYPE.register((entity, reaction, isServer) -> EventResult.CANCEL, this);

		ServerInitializeListener.TYPE.register((controller, serverState, isServer) -> {
			DiplomacyManager.resetServer();
			BattleTracker.reset();
			DefaultDiplomacySettings.clearPresets();
			assetIndex = new FactionAssetIndex(serverState);
			BFStore.load(DiplomacyManager.server());
			BFStore.load(BattleTracker.get());
			FactionDesigns.server().clear();
			BFStore.load(FactionDesigns.server());
			Civics.server().clear();
			Civics.server().loadData();
			BFStore.load(Civics.server());
			Senate.server().clear();
			BFStore.load(Senate.server());
			Council.server().clear();
			BFStore.load(Council.server());
			videogoose.betterfactions.civics.Corporations.server().clear();
			BFStore.load(videogoose.betterfactions.civics.Corporations.server());
			ShipyardRepairs.reset();
			BFStore.load(ShipyardRepairs.get());
			FleetIntercepts.reset();
			BFStore.load(FleetIntercepts.get());
			Wrecks.reset();
			BFStore.load(Wrecks.get());
			FleetUpkeep.reset();
			BFStore.load(FleetUpkeep.get());
			BFStore.load(DiplomacyManager.server().getPointMods());
		}, this);

		WorldSaveListener.TYPE.register(new WorldSaveListener() {
			@Override
			public void onPreWorldSave(boolean terminate, boolean clear, Universe universe, boolean isServer) {
				if (isServer) {
					BFStore.save(DiplomacyManager.server());
					BFStore.save(BattleTracker.get());
					BFStore.save(FactionDesigns.server());
					BFStore.save(Civics.server());
					BFStore.save(Senate.server());
			BFStore.save(Council.server());
			BFStore.save(videogoose.betterfactions.civics.Corporations.server());
					BFStore.save(ShipyardRepairs.get());
					BFStore.save(FleetIntercepts.get());
					BFStore.save(Wrecks.get());
					BFStore.save(FleetUpkeep.get());
					BFStore.save(DiplomacyManager.server().getPointMods());
				}
			}
		}, this);

		ServerUpdateListener.TYPE.register((serverState, timer) -> {
			DiplomacyManager.server().updateServer(timer.currentTime);
			BattleTracker.get().update(serverState, timer.currentTime);
			BattleWorldTests.tick();
			ShipyardRepairs.get().update(serverState, timer.currentTime);
			FleetIntercepts.get().update(serverState, timer.currentTime);
		}, this);

		//overheating is how a ship dies; the battle it was in counts it as a loss
		SegmentControllerOverheatListener.TYPE.register((sc, damager, isServer) -> {
			if (isServer) {
				BattleTracker.get().onOverheat(sc);
				int us = damager != null ? damager.getFactionId() : 0;
				//ponytail: any root entity counts, however small; a minimum mass if one-block ships get farmed past the cap
				if (us != 0 && us != sc.getFactionId() && BattleTracker.root(sc)) {
					CivicPayouts.server().fire(new CivicPayouts.CivicEvent(CivicPayouts.Event.SHIP_DESTROYED, us, sc.getFactionId(),
							null, null, sc.getSector(new Vector3i()), null));
				}
			}
			return EventResult.CONTINUE;
		}, this);

		//removed for good, not unloaded: the sweep would drop its rating within a minute, this drops it now
		ServerSendableRemoveListener.TYPE.register(new ServerSendableRemoveListener() {
			@Override
			public void onPostServerSendableRemove(GameServerState serverState, Sendable s, boolean isServer) {
				if (s instanceof SegmentController sc && sc.isMarkedForPermanentDelete()) {
					DiplomacyManager.server().onEntityDeleted(sc);
					ShipyardRepairs.get().onDeleted(sc);
				}
			}
		}, this);

		//a kill pays the killer's faction, a death costs the victim's; FAIR_KILL needs to know who joined when
		api.event.player.PlayerDeathListener.TYPE.register((victim, damager, isServer) -> {
			if (!isServer || damager == null || !(damager.getOwnerState() instanceof PlayerState killer) || killer == victim) {
				return;
			}
			int theirs = victim.getFactionId();
			int ours = damager.getFactionId();
			if (ours == theirs) {
				return;
			}
			if (ours != 0) {
				Council.server().onPlayerKilled(ours, theirs);
				CivicPayouts.server().fire(new CivicPayouts.CivicEvent(CivicPayouts.Event.PLAYER_KILLED, ours, theirs, killer.getName(), victim.getName(), null, null));
			}
			if (theirs != 0) {
				CivicPayouts.server().fire(new CivicPayouts.CivicEvent(CivicPayouts.Event.PLAYER_LOST, theirs, ours, victim.getName(), null, null, null));
			}
		}, this);
		api.event.player.PlayerJoinFactionListener.TYPE.register((ctx, isServer) -> {
			if (isServer && ctx.getPlayer() != null) {
				CivicPayouts.server().onJoinedFaction(ctx.getPlayer().getName(), System.currentTimeMillis());
			}
			return EventResult.CONTINUE;
		}, this);

		PlayerJoinWorldListener.TYPE.register((client, player, isServer) -> {
			if (isServer) {
				DiplomacyManager.server().onPlayerJoined(player);
			}
		}, this);

		SegmentControllerChangeFactionListener.TYPE.register((context, isServer) -> {
			if (isServer && assetIndex != null) {
				assetIndex.invalidate(context.getOldFaction());
				assetIndex.invalidate(context.getNewFaction());
			}
			return EventResult.CONTINUE;
		}, this);

		//better prices at a trade partner's shops, worse at one that put tariffs on you; server side only
		ShopPriceListener.TYPE.register((player, shop, type, price, playerBuys) -> {
			//ponytail: priced, not completed - a trade that then falls through still counts against the
			//trader; hook the buy itself if that turns out to matter
			DiplomacyManager.server().tradedBetween(shop.getFactionId(), player.getFactionId());
			int adjusted = DiplomacyManager.server().shopPrice(shop.getFactionId(), player.getFactionId(), price, playerBuys);
			if (adjusted != price) {
				//a trade agreement's discount, tariffs, or both
				player.sendServerMessagePlayerInfo(Lng.astr("Diplomatic terms: %s credits per unit instead of %s.", StringTools.formatSeperated(adjusted), StringTools.formatSeperated(price)));
			}
			return adjusted;
		}, this);

		//a trade embargo stops trade routes between the two as well as their shops (ShopEmbargoMixin), and
		//a route that does run is held against whoever traded with an embargoed faction
		//and a corporation running the selling station takes its cut, as from a shop sale
		BuyTradeListener.TYPE.register((order, buyer, seller, active, isServer) -> {
			EventResult r = tradeRoute(isServer, buyer.getFactionId(), seller.getFactionId());
			if (isServer && !r.isCanceled()) {
				videogoose.betterfactions.civics.Corporations.server().onTradeOrder(seller, order.getBuyPrice());
			}
			return r;
		}, this);
		SellTradeListener.TYPE.register((order, seller, buyer, active, isServer) -> {
			EventResult r = tradeRoute(isServer, seller.getFactionId(), buyer.getFactionId());
			if (isServer && !r.isCanceled()) {
				videogoose.betterfactions.civics.Corporations.server().onTradeOrder(seller, order.getSellPrice());
			}
			return r;
		}, this);

		FactionPanelCreateListener.TYPE.register(FactionPanelTabs::addTabs, this);

		//a fleet ship's blocks as it unloads, and a station's shipyard: what an unloaded repair needs
		api.event.world.sector.SegmentControllerUnloadListener.TYPE.register((controller, isServer) -> {
			if (isServer) {
				ShipyardRepairs.get().onUnload(controller);
			}
		}, this);

		ClientDisconnectListener.TYPE.register(state -> {
			DiplomacyManager.resetClient();
			Civics.client().clear();
		}, this);
	}

	@Override
	public void logInfo(String message) {
		super.logInfo("[BetterFactions] " + message);
	}

	/** Whether a trade route between the two may run; one that does counts for opinion pressure. */
	private static EventResult tradeRoute(boolean isServer, int a, int b) {
		DiplomacyManager dm = DiplomacyManager.of(isServer);
		if (dm.refusesTrade(a, b) || dm.refusesTrade(b, a)) {
			return EventResult.CANCEL;
		}
		dm.tradedBetween(a, b);
		return EventResult.CONTINUE;
	}
}
