package videogoose.betterfactions.diplomacy;

import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

/**
 * Pins the treaty model: the registry, the tag round trip, and the relation a set of treaties
 * derives.
 *
 * <p>The derivation is the load-bearing part. Roughly nineteen files across combat, AI, trade and
 * the HUD ask {@code FactionManager.getRelation} for one of three values, and none of them were
 * changed to understand treaties - instead treaties collapse into that same answer. If the
 * collapsing rule is wrong, turrets shoot the wrong ships.</p>
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class FactionTreaty_test {

    private FactionTreaty_test() {
    }

    public static void main(String[] args) {
        vanillaTypesAreRegistered();
        idsAreNamespaced();
        duplicateRegistrationIsRefused();
        unregisterAllKeepsVanilla();
        roundTripsThroughTag();
        aTreatyFromBeforeNamesLoads();
        unknownTypeIsDropped();
        directionSurvivesTheRoundTrip();
        pairCodeIsOrderIndependent();
        derivesStrongestRelation();
        hostilityBeatsFriendship();
        nonImplyingTreatiesDeferToTheRelationTable();
        expiryIsHonoured();
        System.out.println("FactionTreaty_test: OK");
    }

    private static void vanillaTypesAreRegistered() {
        assert TreatyTypeRegistry.exists(TreatyTypeRegistry.WAR);
        assert TreatyTypeRegistry.exists(TreatyTypeRegistry.ALLIANCE);
        assert TreatyTypeRegistry.exists(TreatyTypeRegistry.NON_AGGRESSION);
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.WAR).getImplies() == FactionRelation.RType.ENEMY;
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.ALLIANCE).getImplies() == FactionRelation.RType.FRIEND;
        // war is declared, not negotiated
        assert !TreatyTypeRegistry.get(TreatyTypeRegistry.WAR).needsAcceptance();
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.ALLIANCE).needsAcceptance();
        // an overlord is not a vassal
        assert !TreatyTypeRegistry.get(TreatyTypeRegistry.VASSALAGE).isSymmetric();
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.ALLIANCE).isSymmetric();
        // a trade agreement changes what is permitted, not who shoots
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.TRADE_AGREEMENT).getImplies() == null;
    }

    private static void idsAreNamespaced() {
        assert "starmade:war".equals(TreatyTypeRegistry.WAR) : TreatyTypeRegistry.WAR;
        try {
            TreatyTypeRegistry.register("", new TreatyType.Builder("x"));
            assert false : "empty namespace must be refused";
        } catch (IllegalArgumentException expected) {
            // ok
        }
        try {
            TreatyTypeRegistry.register("mod:sub", new TreatyType.Builder("x"));
            assert false : "namespace containing ':' must be refused";
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    private static void duplicateRegistrationIsRefused() {
        try {
            TreatyTypeRegistry.register(TreatyTypeRegistry.VANILLA, new TreatyType.Builder("alliance"));
            assert false : "re-registering an existing id must be refused";
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    /**
     * Mod teardown drops modded types, but the engine's own must survive it.
     */
    private static void unregisterAllKeepsVanilla() {
        TreatyTypeRegistry.register("selfcheckmod", new TreatyType.Builder("tribute").implies(FactionRelation.RType.NEUTRAL));
        assert TreatyTypeRegistry.exists("selfcheckmod:tribute");
        TreatyTypeRegistry.unregisterAll();
        assert !TreatyTypeRegistry.exists("selfcheckmod:tribute") : "mod type survived teardown";
        assert TreatyTypeRegistry.exists(TreatyTypeRegistry.ALLIANCE) : "vanilla was not re-seeded";
    }

    private static void roundTripsThroughTag() {
        FactionTreaty t = new FactionTreaty(10, 20, TreatyTypeRegistry.NON_AGGRESSION, 1234L);
        t.expiresAt = 9999L;
        t.clause = "no fighting over the belt";
        t.name = "The Xanthe Accord";

        FactionTreaty back = new FactionTreaty();
        assert back.fromTag(t.toTag());
        assert back.from == 10 : back.from;
        assert back.to == 20 : back.to;
        assert back.typeId.equals(TreatyTypeRegistry.NON_AGGRESSION) : back.typeId;
        assert back.signedAt == 1234L : back.signedAt;
        assert back.expiresAt == 9999L : back.expiresAt;
        assert "no fighting over the belt".equals(back.clause) : back.clause;
        assert "The Xanthe Accord".equals(back.name) : back.name;
        assert "The Xanthe Accord".equals(back.getDisplayName()) : back.getDisplayName();
    }

    /**
     * Names were appended after treaties already existed, so a save written before them stops one
     * slot short. It has to load as a treaty that simply has no name of its own, not fail to load.
     */
    private static void aTreatyFromBeforeNamesLoads() {
        FactionTreaty t = new FactionTreaty(1, 2, TreatyTypeRegistry.ALLIANCE, 5L);
        Tag full = t.toTag();
        Tag[] subs = full.getStruct();
        Tag[] shorter = new Tag[subs.length - 1];
        System.arraycopy(subs, 0, shorter, 0, shorter.length - 1);
        shorter[shorter.length - 1] = FinishTag.INST;

        FactionTreaty back = new FactionTreaty();
        assert back.fromTag(new Tag(Tag.Type.STRUCT, null, shorter));
        assert back.typeId.equals(TreatyTypeRegistry.ALLIANCE) : back.typeId;
        assert "".equals(back.name) : back.name;
        //and with no name of its own it still has something to render
        assert "Alliance".equals(back.getDisplayName()) : back.getDisplayName();
    }

    /**
     * A treaty whose type nothing registers is dropped rather than loaded half-understood.
     */
    private static void unknownTypeIsDropped() {
        FactionTreaty t = new FactionTreaty(1, 2, "gonemod:pact", 0L);
        FactionTreaty back = new FactionTreaty();
        assert !back.fromTag(t.toTag()) : "unknown type must be refused";

        FactionTreatySet set = new FactionTreatySet();
        set.fromTag(setTagOf(t));
        assert set.isEmpty() : set.size();
    }

    /**
     * The point of storing direction in the record: the pair key is order-independent, so an
     * asymmetric treaty would otherwise lose which side is the overlord.
     */
    private static void directionSurvivesTheRoundTrip() {
        FactionTreaty t = new FactionTreaty(99, 7, TreatyTypeRegistry.VASSALAGE, 0L);
        FactionTreaty back = new FactionTreaty();
        assert back.fromTag(t.toTag());
        assert back.from == 99 && back.to == 7 : back.from + "->" + back.to;
        assert back.other(99) == 7 : back.other(99);
        assert back.other(7) == 99 : back.other(7);
        assert back.involves(99) && back.involves(7) && !back.involves(5);
    }

    private static void pairCodeIsOrderIndependent() {
        assert FactionRelation.getCode(10, 20) == FactionRelation.getCode(20, 10);
    }

    private static void derivesStrongestRelation() {
        FactionTreatySet set = new FactionTreatySet();
        assert set.getDerivedRelation() == null : "an empty set must not force a relation";

        set.add(new FactionTreaty(1, 2, TreatyTypeRegistry.NON_AGGRESSION, 0L));
        assert set.getDerivedRelation() == FactionRelation.RType.NEUTRAL : set.getDerivedRelation();

        set.add(new FactionTreaty(1, 2, TreatyTypeRegistry.ALLIANCE, 0L));
        assert set.getDerivedRelation() == FactionRelation.RType.FRIEND : set.getDerivedRelation();

        // and the cache keeps up when a treaty goes away
        set.removeByType(TreatyTypeRegistry.ALLIANCE);
        assert set.getDerivedRelation() == FactionRelation.RType.NEUTRAL : set.getDerivedRelation();
    }

    /**
     * A contested pair must resolve hostile. Resolving it the friendly way would make turrets hold
     * fire during a war, which is the expensive direction to be wrong in.
     */
    private static void hostilityBeatsFriendship() {
        FactionTreatySet set = new FactionTreatySet();
        set.add(new FactionTreaty(1, 2, TreatyTypeRegistry.ALLIANCE, 0L));
        set.add(new FactionTreaty(2, 1, TreatyTypeRegistry.WAR, 0L));
        assert set.getDerivedRelation() == FactionRelation.RType.ENEMY : set.getDerivedRelation();
    }

    /**
     * Trade and vision say nothing about shooting, so the relation table still decides.
     */
    private static void nonImplyingTreatiesDeferToTheRelationTable() {
        FactionTreatySet set = new FactionTreatySet();
        set.add(new FactionTreaty(1, 2, TreatyTypeRegistry.TRADE_AGREEMENT, 0L));
        set.add(new FactionTreaty(1, 2, TreatyTypeRegistry.INTEL_SHARING, 0L));
        assert set.getDerivedRelation() == null : set.getDerivedRelation();
        assert set.size() == 2 : set.size();
        assert set.has(TreatyTypeRegistry.TRADE_AGREEMENT);
    }

    private static void expiryIsHonoured() {
        FactionTreatySet set = new FactionTreatySet();
        FactionTreaty timed = new FactionTreaty(1, 2, TreatyTypeRegistry.NON_AGGRESSION, 0L);
        timed.expiresAt = 100L;
        set.add(timed);
        FactionTreaty permanent = new FactionTreaty(1, 2, TreatyTypeRegistry.ALLIANCE, 0L);
        set.add(permanent);

        assert !set.removeExpired(50L) : "nothing should lapse before its term";
        assert set.size() == 2 : set.size();

        assert set.removeExpired(150L);
        assert set.size() == 1 : set.size();
        assert set.has(TreatyTypeRegistry.ALLIANCE);
        assert !permanent.isExpired(Long.MAX_VALUE - 1) : "a treaty with no term never lapses";
    }

    private static org.schema.schine.resource.tag.Tag setTagOf(FactionTreaty... ts) {
        org.schema.schine.resource.tag.Tag[] t = new org.schema.schine.resource.tag.Tag[ts.length + 2];
        t[0] = new org.schema.schine.resource.tag.Tag(
                org.schema.schine.resource.tag.Tag.Type.BYTE, null, (byte) 0);
        for (int i = 0; i < ts.length; i++) {
            t[i + 1] = ts[i].toTag();
        }
        t[t.length - 1] = org.schema.schine.resource.tag.FinishTag.INST;
        return new org.schema.schine.resource.tag.Tag(
                org.schema.schine.resource.tag.Tag.Type.STRUCT, null, t);
    }
}
