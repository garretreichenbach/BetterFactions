package videogoose.betterfactions.diplomacy;

import java.util.List;

/**
 * Tuning the opinion engine reads. Every faction has a ledger, so every faction needs settings for it:
 * {@link org.schema.game.server.data.simulation.npc.NPCFactionConfig} supplies an NPC faction's own
 * per-preset values, and {@link DefaultDiplomacySettings} serves player factions.
 * <p>
 * This is the whole surface the engine touches - it was previously reached by typing the field as the
 * full NPC faction config, which is why player factions could not have a ledger at all.
 */
public interface DiplomacySettings {

    /**
     * What a faction's ledger is scored by: its NPC preset's settings, or the defaults for a player
     * faction (and for an NPC faction whose preset is not attached yet).
     */
    static DiplomacySettings of(org.schema.game.common.data.player.faction.Faction f) {
        if (f instanceof org.schema.game.server.data.simulation.npc.NPCFaction npc && npc.getConfig() != null) {
            return DefaultDiplomacySettings.forPreset(npc.getConfig());
        }
        return DefaultDiplomacySettings.get();
    }

    /**
     * How often derived statuses (at war, allied, ...) are recomputed.
     */
    long getDiplomacyStatusCheckDelay();

    /**
     * How often per-turn modifiers are added to the running score.
     */
    long getDiplomacyTurnEffectDelay();

    /**
     * How often per-turn modifiers ramp up or decay.
     */
    long getDiplomacyTurnEffectChangeDelay();

    /**
     * Opinion a freshly created ledger entry starts at.
     */
    int getDiplomacyStartPoints();

    int getDiplomacyMaxPoints();

    int getDiplomacyMinPoints();

    boolean existsAction(DiplomacyAction.DiplActionType action);

    boolean existsStatus(DiplomacyEntity.DiplStatusType status);

    int getDiplomacyUpperLimit(DiplomacyAction.DiplActionType action);

    int getDiplomacyLowerLimit(DiplomacyAction.DiplActionType action);

    int getDiplomacyValue(DiplomacyAction.DiplActionType action);

    int getDiplomacyValue(DiplomacyEntity.DiplStatusType status);

    DiplomacyReaction getDiplomacyActionRequired(DiplomacyAction.DiplActionType action);

    int getDiplomacyExistingActionModifier(DiplomacyAction.DiplActionType action);

    int getDiplomacyNonExistingActionModifier(DiplomacyAction.DiplActionType action);

    /**
     * How long a per-turn modifier may persist before it is dropped outright.
     */
    long getDiplomacyTurnTimeout(DiplomacyAction.DiplActionType action);

    /**
     * How long a static modifier may sit inactive before it is dropped.
     */
    long getDiplomacyStaticTimeout(DiplomacyEntity.DiplStatusType status);

    /**
     * How long a single action stays counted before it lapses.
     */
    long getDiplomacyActionTimeout(DiplomacyAction.DiplActionType action);

    /**
     * Reactions evaluated against a ledger entry. Only NPC factions act on these - a player faction
     * accrues opinion for display but never has its options decided for it.
     */
    List<DiplomacyReaction> getDiplomacyReactions();

    /**
     * The rule deciding whether this faction agrees to a treaty somebody offers it, or null when it
     * has none for that type - in which case it agrees to nothing of that kind.
     *
     * @param treatyId the treaty type being offered, or null for plain peace
     */
    DiplomacyConditionGroup getDiplomacyAcceptance(String treatyId);

    /**
     * What this faction says when it refuses an offer of that kind, or null to let the engine
     * assemble a refusal out of the conditions that were not met.
     */
    String getDiplomacyRefusal(String treatyId);

    /**
     * How much a spare faction point per period counts toward how strong a faction looks.
     *
     * <p>The surplus, not the balance: see {@code FactionStrength.pointSurplus(Faction)} for why the
     * balance is the wrong number.</p>
     */
    double getDiplomacyPowerSurplusWeight();

    /**
     * How much one claimed system counts toward how strong a faction looks.
     */
    double getDiplomacyPowerSystemWeight();

    /**
     * How much one active member counts toward how strong a faction looks.
     */
    double getDiplomacyPowerMemberWeight();
}
