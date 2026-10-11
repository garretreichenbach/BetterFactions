package videogoose.betterfactions;

import api.event.network.ClientDisconnectListener;
import api.event.entity.SegmentControllerChangeFactionListener;
import api.event.faction.FactionDeleteListener;
import api.event.faction.FactionDiplomacyActionListener;
import api.event.gui.FactionPanelCreateListener;
import api.event.faction.FactionRelationQueryListener;
import api.event.faction.NPCExecuteDiplomacyReactionListener;
import api.event.EventResult;
import api.event.lifecycle.ServerInitializeListener;
import api.event.lifecycle.ServerUpdateListener;
import api.event.player.PlayerJoinWorldListener;
import api.event.world.WorldSaveListener;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.world.Universe;
import org.schema.game.server.data.GameServerState;
import videogoose.betterfactions.assets.FactionAssetIndex;
import videogoose.betterfactions.commands.DiplomacyCommands;
import videogoose.betterfactions.diplomacy.DefaultDiplomacySettings;
import videogoose.betterfactions.diplomacy.DiplomacyAction;
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
		PacketUtil.registerPacket(this, BFPacket.class);
		registerEvents();
		DiplomacyCommands.register(this);
	}

	@Override
	public void onDisable() {
		if (GameServerState.instance != null) {
			BFStore.save(DiplomacyManager.server());
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
		}, this);

		//vanilla's own NPC ledger still runs; this keeps it from acting, so the mod's is the only one
		//that declares wars or offers peace
		NPCExecuteDiplomacyReactionListener.TYPE.register((entity, reaction, isServer) -> EventResult.CANCEL, this);

		ServerInitializeListener.TYPE.register((controller, serverState, isServer) -> {
			DiplomacyManager.resetServer();
			DefaultDiplomacySettings.clearPresets();
			assetIndex = new FactionAssetIndex(serverState);
			BFStore.load(DiplomacyManager.server());
		}, this);

		WorldSaveListener.TYPE.register(new WorldSaveListener() {
			@Override
			public void onPreWorldSave(boolean terminate, boolean clear, Universe universe, boolean isServer) {
				if (isServer) {
					BFStore.save(DiplomacyManager.server());
				}
			}
		}, this);

		ServerUpdateListener.TYPE.register((serverState, timer) -> DiplomacyManager.server().updateServer(timer.currentTime), this);

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

		FactionPanelCreateListener.TYPE.register(FactionPanelTabs::addTabs, this);

		ClientDisconnectListener.TYPE.register(state -> DiplomacyManager.resetClient(), this);
	}

	@Override
	public void logInfo(String message) {
		super.logInfo("[BetterFactions] " + message);
	}
}
