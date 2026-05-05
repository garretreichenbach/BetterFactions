package videogoose.betterfactions.manager;

import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.persistent.faction.BetterFactionStore;
import videogoose.betterfactions.data.persistent.faction.FactionRank;
import videogoose.betterfactions.data.persistent.federation.FactionMessage;
import videogoose.betterfactions.mixin.BetterFactionAccessor;
import videogoose.betterfactions.mixin.BetterMemberAccessor;
import videogoose.betterfactions.utils.PermissionUtils;

import java.util.ArrayList;

/**
 * Manages BetterFactions data on top of the game's native Faction system.
 * Uses mixin accessors for runtime access and BetterFactionStore for persistence.
 */
public class FactionManager {

    /**
     * Cast a Faction to its BetterFactions accessor interface.
     */
    public static BetterFactionAccessor getBetterFaction(Faction faction) {
        return (BetterFactionAccessor) faction;
    }

    /**
     * Get the BetterFactions accessor for a faction by ID.
     */
    public static BetterFactionAccessor getBetterFaction(int factionId) {
        if (GameCommon.getGameState() == null) return null;
        Faction faction = GameCommon.getGameState().getFactionManager().getFaction(factionId);
        return faction != null ? (BetterFactionAccessor) faction : null;
    }

    public static boolean inFaction(PlayerState playerState) {
        return playerState != null && playerState.getFactionId() > 0;
    }

    public static Faction getFaction(PlayerState playerState) {
        if (playerState == null || playerState.getFactionId() <= 0) return null;
        if (GameCommon.getGameState() == null) return null;
        return GameCommon.getGameState().getFactionManager().getFaction(playerState.getFactionId());
    }

    /**
     * Get the FactionPermission (member data) for a player by name.
     */
    public static FactionPermission getPlayerMember(String playerName) {
        PlayerState player = GameCommon.getPlayerFromName(playerName);
        if (player == null) return null;
        Faction faction = getFaction(player);
        if (faction == null) return null;
        return faction.getMembersUID().get(playerName);
    }

    /**
     * Check if a member has a BetterFactions permission via their custom rank.
     */
    public static boolean hasPermission(FactionPermission member, String... permissions) {
        return PermissionUtils.hasPermission(member, permissions);
    }

    /**
     * Add a message to a faction's inbox.
     */
    public static void addMessage(int factionId, FactionMessage message) {
        BetterFactionAccessor accessor = getBetterFaction(factionId);
        if (accessor == null) return;
        accessor.getInbox().add(message);
        saveStore(factionId);
    }

    /**
     * Remove a message from a faction's inbox.
     */
    public static void removeMessage(int factionId, FactionMessage message) {
        BetterFactionAccessor accessor = getBetterFaction(factionId);
        if (accessor == null) return;
        accessor.getInbox().removeIf(m -> m.date == message.date && m.fromId == message.fromId);
        saveStore(factionId);
    }

    /**
     * Load all BetterFactionStore objects from persistence and inject into Faction mixin fields.
     */
    public static void loadStores() {
        for (Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), BetterFactionStore.class)) {
            if (obj instanceof BetterFactionStore store) {
                Faction faction = GameCommon.getGameState().getFactionManager().getFaction(store.factionId);
                if (faction == null) continue;
                BetterFactionAccessor accessor = (BetterFactionAccessor) faction;
                accessor.setFederationId(store.federationId);
                accessor.setFactionLogo(store.factionLogo);
                accessor.setInbox(store.inbox);
                accessor.setRanks(store.ranks);

                // Assign custom ranks to members
                for (FactionPermission member : faction.getMembersUID().values()) {
                    BetterMemberAccessor memberAccessor = (BetterMemberAccessor) member;
                    if (memberAccessor.getCustomRank() == null && !store.ranks.isEmpty()) {
                        memberAccessor.setCustomRank(store.ranks.get(store.ranks.size() - 1)); // Lowest rank
                    }
                }
            }
        }
    }

    /**
     * Save a faction's BetterFactions data to persistence.
     */
    public static void saveStore(int factionId) {
        if (!NetworkSyncManager.onServer()) return;
        Faction faction = GameCommon.getGameState().getFactionManager().getFaction(factionId);
        if (faction == null) return;
        BetterFactionAccessor accessor = (BetterFactionAccessor) faction;

        // Remove old store
        for (Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), BetterFactionStore.class)) {
            if (obj instanceof BetterFactionStore store && store.factionId == factionId) {
                PersistentObjectUtil.removeObject(BetterFactions.getInstance().getSkeleton(), obj);
                break;
            }
        }

        // Create new store from current mixin state
        BetterFactionStore store = new BetterFactionStore();
        store.factionId = factionId;
        store.federationId = accessor.getFederationId();
        store.factionLogo = accessor.getFactionLogo();
        store.inbox = accessor.getInbox();
        store.ranks = accessor.getRanks();

        PersistentObjectUtil.addObject(BetterFactions.getInstance().getSkeleton(), store);
        PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());
    }

    /**
     * Save all faction stores.
     */
    public static void saveAllStores() {
        if (GameCommon.getGameState() == null) return;
        for (Faction faction : GameCommon.getGameState().getFactionManager().getFactionCollection()) {
            saveStore(faction.getIdFaction());
        }
    }

    /**
     * Ensure a faction has BetterFactions data initialized.
     * Creates a store if one doesn't exist.
     */
    public static void ensureInitialized(Faction faction) {
        if (faction == null) return;
        BetterFactionAccessor accessor = (BetterFactionAccessor) faction;
        if (accessor.getRanks() == null || accessor.getRanks().isEmpty()) {
            accessor.setRanks(new ArrayList<>());
            accessor.getRanks().add(FactionRank.getDefaultRank());
        }
        if (accessor.getInbox() == null) accessor.setInbox(new ArrayList<>());
        if (accessor.getFactionLogo() == null) accessor.setFactionLogo("");
    }
}
