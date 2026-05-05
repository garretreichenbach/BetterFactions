package videogoose.betterfactions.data.diplomacy.war;

import videogoose.betterfactions.data.serializeable.war.WarGoalData;

/**
 * Represents a casus belli (justification for war) between two factions.
 * CBs are generated automatically from diplomatic conditions or fabricated over time.
 * Declaring war with a valid CB avoids the unjustified war opinion penalty.
 */
public class CasusBelli {

    public enum CBType {
        RIVALRY("Rivalry", "Rival faction — you have a standing justification for war.",
            WarGoalData.WarGoalType.HUMILIATE_RIVAL, 0, false, 0.0f),
        BORDER_FRICTION("Border Friction", "Contested territorial claims create justification for war.",
            WarGoalData.WarGoalType.TERRITORY, 0, false, 0.0f),
        ALLIANCE_THREAT("Threat to Ally", "This faction threatens one of your allies.",
            WarGoalData.WarGoalType.SHOW_SUPERIORITY, 0, false, 0.0f),
        SUBJUGATION("Subjugation", "You seek to impose your will on a weaker faction.",
            WarGoalData.WarGoalType.FORCE_DIPLO, 600000, true, -0.1f),
        FABRICATED("Fabricated Claim", "A manufactured justification for war.",
            WarGoalData.WarGoalType.TERRITORY, 300000, true, -0.05f),
        CONTAINMENT("Containment", "This faction declared an unjustified war — contain their aggression.",
            WarGoalData.WarGoalType.CONTAINMENT, 0, false, 0.0f),
        REJECTED_DEMAND("Rejected Demand", "This faction rejected your diplomatic demands.",
            WarGoalData.WarGoalType.FORCE_DIPLO, 0, false, 0.0f),
        GUARANTEE_BROKEN("Broken Guarantee", "This faction attacked a faction you guaranteed independence for.",
            WarGoalData.WarGoalType.SHOW_SUPERIORITY, 0, false, 0.0f);

        public final String displayName;
        public final String description;
        public final WarGoalData.WarGoalType unlockedWarGoal;
        public final long fabricationTimeMs;
        public final boolean requiresFabrication;
        public final float demandCostModifier; // Negative = discount on peace demands

        CBType(String displayName, String description, WarGoalData.WarGoalType unlockedWarGoal,
               long fabricationTimeMs, boolean requiresFabrication, float demandCostModifier) {
            this.displayName = displayName;
            this.description = description;
            this.unlockedWarGoal = unlockedWarGoal;
            this.fabricationTimeMs = fabricationTimeMs;
            this.requiresFabrication = requiresFabrication;
            this.demandCostModifier = demandCostModifier;
        }
    }

    public CBType type;
    public int fromFactionId;
    public int toFactionId;
    public long createdTime;
    public long fabricationStartTime; // 0 if not fabricating, >0 if in progress
    public boolean ready; // True if CB is available to use

    public CasusBelli() {}

    public CasusBelli(CBType type, int fromFactionId, int toFactionId) {
        this.type = type;
        this.fromFactionId = fromFactionId;
        this.toFactionId = toFactionId;
        this.createdTime = System.currentTimeMillis();
        this.fabricationStartTime = 0;
        this.ready = !type.requiresFabrication;
    }

    /**
     * Start fabricating this CB (for types that require it).
     */
    public void startFabrication() {
        if (type.requiresFabrication && !ready) {
            fabricationStartTime = System.currentTimeMillis();
        }
    }

    /**
     * Check and update fabrication progress.
     * @return true if the CB just became ready
     */
    public boolean updateFabrication() {
        if (ready || !type.requiresFabrication || fabricationStartTime == 0) return false;
        if (System.currentTimeMillis() - fabricationStartTime >= type.fabricationTimeMs) {
            ready = true;
            return true;
        }
        return false;
    }

    public float getFabricationProgress() {
        if (ready) return 1.0f;
        if (fabricationStartTime == 0 || type.fabricationTimeMs == 0) return 0.0f;
        long elapsed = System.currentTimeMillis() - fabricationStartTime;
        return Math.min(1.0f, (float) elapsed / type.fabricationTimeMs);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CasusBelli other
            && other.type == type
            && other.fromFactionId == fromFactionId
            && other.toFactionId == toFactionId;
    }

    @Override
    public int hashCode() {
        return type.ordinal() * 31 + fromFactionId * 17 + toFactionId;
    }
}
