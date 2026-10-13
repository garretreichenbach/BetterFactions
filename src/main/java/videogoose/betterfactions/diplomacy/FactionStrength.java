package videogoose.betterfactions.diplomacy;

import org.schema.game.common.data.player.faction.Faction;

/**
 * How big a faction is, on an arbitrary but shared scale.
 *
 * <p>Three quantities that do not overlap: how many people it can field, how much it holds, and
 * whether it can afford what it holds. The weights are a judgement, so they are config rather than
 * constants - an archetype that counts crew and one that counts territory are most of what makes two
 * NPC factions feel different - and {@link DefaultDiplomacySettings} carries the neutral set that
 * anything without an opinion of its own uses.</p>
 *
 * <p>It used to be a private method on {@link DiplomacyEntity}, which was fine while the only reader
 * was the acceptance rule that needed it. The moment a player can see the number on screen it has to
 * be the same number the rules use, or the panel is quietly describing a different game.</p>
 *
 * <p>The fourth term is military: the sum of {@link CombatRatings} over every ship and station the
 * faction owns, taken while each was last loaded.</p>
 */
public final class FactionStrength {

    /**
     * Systems this faction holds. A player faction's sector list rides the faction blob to clients and
     * has one entry per claimed system; an NPC faction's systems come from the server's NPC simulation,
     * synced with the combat totals.
     */
    public static int claimedSystems(Faction f) {
        if (f.isNPC()) {
            return videogoose.betterfactions.DiplomacyManager.of(f.isOnServer()).getCombatRatings().npcSystemsOf(f.getIdFaction());
        }
        return f.lastSystemSectors.size();
    }

    /**
     * Members, weighted by whether anyone is actually playing them, so forty alts do not make an
     * empire. No premium for being online: this is "how big are they", not "who is awake".
     */
    public static float activeMembers(Faction f) {
        float total = 0;
        for (org.schema.game.common.data.player.faction.FactionPermission p : f.getMembersUID().values()) {
            total += p.isActiveMember() ? 1.0f : 0.25f;
        }
        return total;
    }

    /**
     * Faction points earned per period minus those owed on territory: what the faction has spare.
     * The balance is the wrong number - it grows for as long as a faction stays solvent, and
     * territory drains it, so it rates hoarders above empires.
     */
    public static float pointSurplus(Faction f) {
        return (f.lastPointsFromOnline + f.lastPointsFromOffline)
                - (f.lastPointsSpendOnBaseRate + f.lastPointsSpendOnCenterDistance
                + f.lastPointsSpendOnDistanceToHome + org.schema.game.common.data.player.faction.config.FactionPointSpendingConfig.BASIC_FLAT_COST);
    }

    private FactionStrength() {
    }

    /**
     * As the given archetype sizes a faction up. The sum of the four terms below, scaled by {@link #momentum}, which
     * is the only place the formula is written down - the panel draws the same four numbers rather than a
     * second version of them, because a breakdown that does not add up to the total on screen beside
     * it is worse than no breakdown.
     */
    public static double of(Faction f, DiplomacySettings weights) {
        return (incomeTerm(f, weights) + territoryTerm(f, weights) + peopleTerm(f, weights) + militaryTerm(f, weights))
                * (1 + momentum(f));
    }

    /**
     * Share of its power a faction is up (or down) on its recent battles: a run of wins makes it look
     * and count as stronger for a while, a run of losses weaker. Scales the whole number rather than
     * adding a term, so the shares the panel draws still add up.
     *
     * <p>{@link #weariness} is the losing half of it on a 0 to 1 scale: how much a beaten faction wants out.</p>
     */
    public static double weariness(Faction f) {
        return -Math.min(0, momentum(f)) / videogoose.betterfactions.battle.BattleReports.MOMENTUM_CAP;
    }

    public static double momentum(Faction f) {
        return f == null ? 0 : videogoose.betterfactions.DiplomacyManager.of(f.isOnServer()).getCombatRatings().momentumOf(f.getIdFaction());
    }

    /**
     * What it can field. Works on clients too: the server sends the per-faction totals.
     *
     * <p>Square-rooted: ship ratings span from a few hundred to hundreds of thousands, and no linear
     * weight fits both ends - one that lets a fighter count lets a capital ship drown out territory and
     * people entirely. Four times the fleet reads as twice the force.</p>
     */
    public static double militaryTerm(Faction f, DiplomacySettings weights) {
        return f == null ? 0 : Math.sqrt(combatRating(f)) * weights.getDiplomacyPowerMilitaryWeight();
    }

    public static double combatRating(Faction f) {
        return videogoose.betterfactions.DiplomacyManager.of(f.isOnServer()).getCombatRatings().totalOf(f.getIdFaction());
    }

    public static double militaryTerm(Faction f) {
        return militaryTerm(f, DefaultDiplomacySettings.get());
    }

    /**
     * What its point income is worth. Surplus rather than stock, and never negative: a faction
     * spending more than it earns is in trouble, not in debt to the scale.
     */
    public static double incomeTerm(Faction f, DiplomacySettings weights) {
        return f == null ? 0 : Math.max(0, FactionStrength.pointSurplus(f)) * weights.getDiplomacyPowerSurplusWeight();
    }

    /**
     * What it holds.
     */
    public static double territoryTerm(Faction f, DiplomacySettings weights) {
        return f == null ? 0 : FactionStrength.claimedSystems(f) * weights.getDiplomacyPowerSystemWeight();
    }

    /**
     * Who it can field, discounted for how long they have been away.
     */
    public static double peopleTerm(Faction f, DiplomacySettings weights) {
        return f == null ? 0 : FactionStrength.activeMembers(f) * weights.getDiplomacyPowerMemberWeight();
    }

    public static double incomeTerm(Faction f) {
        return incomeTerm(f, DefaultDiplomacySettings.get());
    }

    public static double territoryTerm(Faction f) {
        return territoryTerm(f, DefaultDiplomacySettings.get());
    }

    public static double peopleTerm(Faction f) {
        return peopleTerm(f, DefaultDiplomacySettings.get());
    }

    /**
     * On the neutral weights - what a faction is worth to nobody in particular, and so the only
     * version of the number it makes sense to show a player.
     */
    public static double of(Faction f) {
        return of(f, DefaultDiplomacySettings.get());
    }

    /**
     * What a lone player outside any faction is worth, which is the unit the weights are calibrated
     * against: one active member.
     */
    public static double ofLonePlayer(DiplomacySettings weights) {
        return weights.getDiplomacyPowerMemberWeight();
    }
}
