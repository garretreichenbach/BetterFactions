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
		if (GameServerState.instance != null) {
			BFStore.save(DiplomacyManager.server());
			BFStore.save(BattleTracker.get());
		}
	}

	private void registerEvents() {
		//the one hook that makes a treaty mean something to combat: a pair's treaties outrank its
		//relation byte, and pairs with no treaty that says anything about hostility fall through
		FactionRelationQueryListener.TYPE.register((a, b, current, isServer) -> DiplomacyManager.of(isServer).derivedRelation(a, b), this);

		FactionDeleteListener.TYPE.register((faction, isServer) -> DiplomacyManager.of(isServer).onFactionDeleted(faction), this);

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
		}, this);

		WorldSaveListener.TYPE.register(new WorldSaveListener() {
			@Override
			public void onPreWorldSave(boolean terminate, boolean clear, Universe universe, boolean isServer) {
				if (isServer) {
					BFStore.save(DiplomacyManager.server());
					BFStore.save(BattleTracker.get());
				}
			}
		}, this);

		ServerUpdateListener.TYPE.register((serverState, timer) -> {
			DiplomacyManager.server().updateServer(timer.currentTime);
			BattleTracker.get().update(serverState, timer.currentTime);
			BattleWorldTests.tick();
		}, this);

		//overheating is how a ship dies; the battle it was in counts it as a loss
		SegmentControllerOverheatListener.TYPE.register((sc, damager, isServer) -> {
			if (isServer) {
				BattleTracker.get().onOverheat(sc);
			}
			return EventResult.CONTINUE;
		}, this);

		//removed for good, not unloaded: the sweep would drop its rating within a minute, this drops it now
		ServerSendableRemoveListener.TYPE.register(new ServerSendableRemoveListener() {
			@Override
			public void onPostServerSendableRemove(GameServerState serverState, Sendable s, boolean isServer) {
				if (s instanceof SegmentController sc && sc.isMarkedForPermanentDelete()) {
					DiplomacyManager.server().onEntityDeleted(sc);
				}
			}
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

		//a trade agreement's whole effect: better prices at the partner's shops, server side only
		ShopPriceListener.TYPE.register((player, shop, type, price, playerBuys) -> {
			int adjusted = DiplomacyManager.server().shopPrice(shop.getFactionId(), player.getFactionId(), price, playerBuys);
			if (adjusted != price) {
				player.sendServerMessagePlayerInfo(Lng.astr("Trade agreement: %s credits per unit instead of %s.", StringTools.formatSeperated(adjusted), StringTools.formatSeperated(price)));
			}
			return adjusted;
		}, this);

		FactionPanelCreateListener.TYPE.register(FactionPanelTabs::addTabs, this);

		ClientDisconnectListener.TYPE.register(state -> DiplomacyManager.resetClient(), this);
	}

	@Override
	public void logInfo(String message) {
		super.logInfo("[BetterFactions] " + message);
	}
}
