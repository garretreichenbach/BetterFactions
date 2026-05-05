package videogoose.betterfactions.manager;

import api.common.GameCommon;
import api.mod.config.PersistentObjectUtil;
import api.utils.StarRunnable;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import videogoose.betterfactions.data.serializeable.war.WarData;
import videogoose.betterfactions.data.serializeable.war.WarParticipantData;

import java.util.ArrayList;

/**
 * Manages active wars, war exhaustion, and forced peace.
 */
public class WarManager {

    private static boolean initialized;

    public static void initialize() {
        if (initialized) return;

        // Periodic: update war exhaustion if enabled
        new StarRunnable() {
            @Override
            public void run() {
                if (ConfigManager.warExhaustionEnabled.getValue()) {
                    updateWarExhaustion();
                }
            }
        }.runTimer(BetterFactions.getInstance(), 1200); // Every 60 seconds

        initialized = true;
    }

    public static ArrayList<WarData> getWarsInvolvedIn(Faction faction) {
        ArrayList<WarData> wars = new ArrayList<>();
        for (Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), WarData.class)) {
            if (obj instanceof WarData warData && warData.isInvolved(faction)) {
                wars.add(warData);
            }
        }
        return wars;
    }

    public static ArrayList<WarData> getAllWars() {
        ArrayList<WarData> wars = new ArrayList<>();
        for (Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), WarData.class)) {
            if (obj instanceof WarData warData) wars.add(warData);
        }
        return wars;
    }

    public static boolean isOpposingSides(Faction from, Faction to, WarData warData) {
        return warData.isOpposingSides(from.getIdFaction(), to.getIdFaction());
    }

    /**
     * Update war exhaustion for all active wars.
     * Exhaustion increases based on time at war.
     * When a faction hits max exhaustion, they can be forced to accept status-quo peace.
     */
    private static void updateWarExhaustion() {
        int exhaustionPerDay = ConfigManager.warExhaustionPerDay.getValue();
        int maxExhaustion = ConfigManager.warExhaustionMaxForStatusQuo.getValue();
        // Each tick is ~60 seconds, so scale exhaustion accordingly
        float exhaustionPerTick = exhaustionPerDay / (24.0f * 60.0f); // Per-minute fraction

        for (WarData war : getAllWars()) {
            boolean anyForced = false;
            for (WarParticipantData p : war.attackers.values()) {
                p.exhaustion = Math.min(maxExhaustion, p.exhaustion + exhaustionPerTick);
                if (p.exhaustion >= maxExhaustion) anyForced = true;
            }
            for (WarParticipantData p : war.defenders.values()) {
                p.exhaustion = Math.min(maxExhaustion, p.exhaustion + exhaustionPerTick);
                if (p.exhaustion >= maxExhaustion) anyForced = true;
            }

            if (anyForced) {
                forceStatusQuoPeace(war);
            }
        }
    }

    /**
     * Force a status-quo peace when war exhaustion maxes out.
     * Sets all participants to NEUTRAL and fires peace acceptance actions.
     */
    private static void forceStatusQuoPeace(WarData war) {
        var factionManager = GameCommon.getGameState().getFactionManager();
        BetterFactions.getInstance().logInfo("War exhaustion forced status-quo peace for: " + war.getName());

        // Set all opposing pairs to neutral
        for (WarParticipantData attacker : war.attackers.values()) {
            for (WarParticipantData defender : war.defenders.values()) {
                factionManager.setRelationServer(attacker.factionId, defender.factionId, FactionRelation.RType.NEUTRAL.code);
                FactionDiplomacyManager.forceDiplomacyAction(attacker.factionId, defender.factionId, FactionDiplomacyAction.DiploActionType.ACCEPT_PEACE_OFFER);
            }
        }

        // Remove war from persistence
        for (Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), WarData.class)) {
            if (obj instanceof WarData persisted && persisted.equals(war)) {
                PersistentObjectUtil.removeObject(BetterFactions.getInstance().getSkeleton(), obj);
                break;
            }
        }
        PersistentObjectUtil.save(BetterFactions.getInstance().getSkeleton());
    }
}
