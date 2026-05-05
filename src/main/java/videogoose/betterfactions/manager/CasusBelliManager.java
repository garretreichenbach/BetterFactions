package videogoose.betterfactions.manager;

import api.common.GameCommon;
import api.utils.StarRunnable;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacy;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacyEntity;
import videogoose.betterfactions.data.diplomacy.war.CasusBelli;
import videogoose.betterfactions.data.diplomacy.war.CasusBelli.CBType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages casus belli between factions. Tracks available CBs, handles fabrication timers,
 * and generates automatic CBs from diplomatic conditions.
 */
public class CasusBelliManager {

    // Key: fromFactionId, Value: map of toFactionId -> list of CBs
    private static final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, List<CasusBelli>>> cbMap = new ConcurrentHashMap<>();
    private static boolean initialized;

    public static void initialize() {
        if (initialized) return;

        // Periodic check: update fabrication timers and generate automatic CBs
        new StarRunnable() {
            @Override
            public void run() {
                updateFabrications();
                generateAutomaticCBs();
            }
        }.runTimer(BetterFactions.getInstance(), 600); // Every 30 seconds

        initialized = true;
    }

    /**
     * Get all available (ready) CBs that faction has against target.
     */
    public static List<CasusBelli> getAvailableCBs(int fromFactionId, int toFactionId) {
        List<CasusBelli> result = new ArrayList<>();
        var targetMap = cbMap.get(fromFactionId);
        if (targetMap == null) return result;
        var cbs = targetMap.get(toFactionId);
        if (cbs == null) return result;
        for (CasusBelli cb : cbs) {
            if (cb.ready) result.add(cb);
        }
        return result;
    }

    /**
     * Check if faction has any ready CB against target.
     */
    public static boolean hasCB(int fromFactionId, int toFactionId) {
        return !getAvailableCBs(fromFactionId, toFactionId).isEmpty();
    }

    /**
     * Check if faction has a specific CB type against target.
     */
    public static boolean hasCB(int fromFactionId, int toFactionId, CBType type) {
        for (CasusBelli cb : getAvailableCBs(fromFactionId, toFactionId)) {
            if (cb.type == type) return true;
        }
        return false;
    }

    /**
     * Add a CB. If an identical CB already exists, it's not duplicated.
     */
    public static void addCB(CasusBelli cb) {
        cbMap.computeIfAbsent(cb.fromFactionId, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(cb.toFactionId, k -> new ArrayList<>());
        List<CasusBelli> cbs = cbMap.get(cb.fromFactionId).get(cb.toFactionId);
        if (!cbs.contains(cb)) {
            cbs.add(cb);
            BetterFactions.getInstance().logInfo("CB added: " + cb.type.displayName
                + " for faction " + cb.fromFactionId + " against " + cb.toFactionId);
        }
    }

    /**
     * Remove a specific CB (e.g., after war is declared using it).
     */
    public static void removeCB(int fromFactionId, int toFactionId, CBType type) {
        var targetMap = cbMap.get(fromFactionId);
        if (targetMap == null) return;
        var cbs = targetMap.get(toFactionId);
        if (cbs == null) return;
        cbs.removeIf(cb -> cb.type == type);
    }

    /**
     * Remove all CBs between two factions (e.g., after peace).
     */
    public static void clearCBs(int fromFactionId, int toFactionId) {
        var targetMap = cbMap.get(fromFactionId);
        if (targetMap != null) targetMap.remove(toFactionId);
    }

    /**
     * Called when a faction declares an unjustified war. Grants CONTAINMENT CB
     * to all factions with opinion below the configured threshold toward the aggressor.
     */
    public static void onUnjustifiedWar(int aggressorFactionId, int victimFactionId) {
        int opinionThreshold = ConfigManager.containmentOpinionThreshold.getValue();
        var factionManager = GameCommon.getGameState().getFactionManager();

        for (Faction faction : factionManager.getFactionCollection()) {
            if (faction.getIdFaction() == aggressorFactionId || faction.getIdFaction() == victimFactionId) continue;
            if (!faction.isPlayerFaction() && !faction.isNPC()) continue;

            FactionDiplomacy diplomacy = FactionDiplomacyManager.getDiplomacy(faction.getIdFaction());
            FactionDiplomacyEntity entity = diplomacy.entities.get((long) aggressorFactionId);
            if (entity != null && entity.getPoints() < opinionThreshold) {
                addCB(new CasusBelli(CBType.CONTAINMENT, faction.getIdFaction(), aggressorFactionId));
            }
        }
    }

    /**
     * Called when a demand is rejected. Grants REJECTED_DEMAND CB to the demanding faction.
     */
    public static void onDemandRejected(int demandingFactionId, int rejectingFactionId) {
        addCB(new CasusBelli(CBType.REJECTED_DEMAND, demandingFactionId, rejectingFactionId));
    }

    /**
     * Start fabricating a CB of the given type.
     */
    public static void startFabrication(int fromFactionId, int toFactionId, CBType type) {
        CasusBelli cb = new CasusBelli(type, fromFactionId, toFactionId);
        addCB(cb);
        cb.startFabrication();
    }

    private static void updateFabrications() {
        for (var targetMap : cbMap.values()) {
            for (var cbs : targetMap.values()) {
                for (CasusBelli cb : cbs) {
                    if (cb.updateFabrication()) {
                        BetterFactions.getInstance().logInfo("CB fabrication complete: " + cb.type.displayName
                            + " for faction " + cb.fromFactionId + " against " + cb.toFactionId);
                    }
                }
            }
        }
    }

    /**
     * Generate automatic CBs from diplomatic conditions.
     * Called periodically by the timer.
     */
    private static void generateAutomaticCBs() {
        var factionManager = GameCommon.getGameState().getFactionManager();
        if (factionManager == null) return;

        for (Faction faction : factionManager.getFactionCollection()) {
            if (!faction.isPlayerFaction() && !faction.isNPC()) continue;
            int fid = faction.getIdFaction();
            FactionDiplomacy diplomacy = FactionDiplomacyManager.getDiplomacy(fid);

            for (var entry : diplomacy.entities.entrySet()) {
                long targetId = entry.getKey();
                if (targetId <= 0 || targetId >= Integer.MAX_VALUE) continue; // Skip players
                int tid = (int) targetId;
                FactionDiplomacyEntity entity = entry.getValue();

                // RIVALRY CB: if rival status exists
                if (entity.existsStatusModifier(FactionDiplomacyEntity.DiploStatusType.RIVAL)) {
                    if (!hasCB(fid, tid, CBType.RIVALRY)) {
                        addCB(new CasusBelli(CBType.RIVALRY, fid, tid));
                    }
                } else {
                    removeCB(fid, tid, CBType.RIVALRY);
                }

                // BORDER_FRICTION CB: if contested claims exist
                if (entity.existsStatusModifier(FactionDiplomacyEntity.DiploStatusType.CONTESTED_CLAIMS)) {
                    if (!hasCB(fid, tid, CBType.BORDER_FRICTION)) {
                        addCB(new CasusBelli(CBType.BORDER_FRICTION, fid, tid));
                    }
                } else {
                    removeCB(fid, tid, CBType.BORDER_FRICTION);
                }

                // ALLIANCE_THREAT CB: if target is at war with one of our allies
                if (entity.existsStatusModifier(FactionDiplomacyEntity.DiploStatusType.IN_WAR_WITH_FRIENDS)) {
                    if (!hasCB(fid, tid, CBType.ALLIANCE_THREAT)) {
                        addCB(new CasusBelli(CBType.ALLIANCE_THREAT, fid, tid));
                    }
                } else {
                    removeCB(fid, tid, CBType.ALLIANCE_THREAT);
                }
            }
        }
    }
}
