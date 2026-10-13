package videogoose.betterfactions.diplomacy;

import java.util.List;

/**
 * Run with {@code -ea}; see {@code ./gradlew selfChecks}.
 */
public class SettlementPricing_test {

    private static final int PLAYER = 1;
    private static final int NPC = 2;
    private static final double TREATY = 1000;
    private static final double PEACE = 500;

    public static void main(String[] args) {
        leverageSetsThePrice();
        termsAreValuedFromTheNpcsSide();
        counterTalksDownWhatItGives();
        counterAsksForTheShortfall();
        someThingsAreNotForSale();
        systemsAreWorthTwoTreaties();
        System.out.println("SettlementPricing_test: OK");
    }

    private static void leverageSetsThePrice() {
        //willing and equal: gives up to the peace value
        assert SettlementPricing.need(true, true, 0, PEACE) == -500;
        //they are twice as strong: gives twice as much
        assert SettlementPricing.need(true, true, 1, PEACE) == -1000;
        //unwilling, and they are half as strong: wants paying double
        assert SettlementPricing.need(true, false, -1, PEACE) == 1000;
        //leverage stops counting at the clamp
        assert SettlementPricing.need(true, true, 99, PEACE) == SettlementPricing.need(true, true, SettlementPricing.LEVERAGE_CLAMP, PEACE);
        //anything but peace is not for sale when the archetype says no, and costs nothing when it says yes
        //a beaten NPC asks less to stop, down to nothing, and will give more; weariness never buys an alliance
        assert SettlementPricing.need(true, false, 1, PEACE) < 0 : "losing, it pays for peace instead of asking";
        assert SettlementPricing.need(true, false, 0, PEACE, 0.5) == 250 && SettlementPricing.need(true, false, 0, PEACE, 1) == 0;
        assert SettlementPricing.need(true, true, 0, PEACE, 1) == -1000;
        assert SettlementPricing.need(false, false, 0, PEACE, 1) == Double.POSITIVE_INFINITY;
        assert SettlementPricing.need(false, false, 6, PEACE) == Double.POSITIVE_INFINITY;
        assert SettlementPricing.need(false, true, 6, PEACE) == 0;
    }

    private static void termsAreValuedFromTheNpcsSide() {
        assert SettlementPricing.valueTo(NPC, SettlementTerm.points(NPC, 300), PLAYER, NPC, TREATY) == -300;
        assert SettlementPricing.valueTo(NPC, SettlementTerm.points(PLAYER, 300), PLAYER, NPC, TREATY) == 300;
        //a pact binds both alike, so it is the rule's to accept and never a price: whichever side the
        //term names as giving it, it costs and pays nothing
        assert SettlementPricing.valueTo(NPC, SettlementTerm.signTreaty(PLAYER, TreatyTypeRegistry.TRADE_AGREEMENT), PLAYER, NPC, TREATY) == 0;
        assert SettlementPricing.valueTo(NPC, SettlementTerm.signTreaty(NPC, TreatyTypeRegistry.TRADE_AGREEMENT), PLAYER, NPC, TREATY) == 0;
        assert SettlementPricing.valueTo(NPC, SettlementTerm.signTreaty(PLAYER, TreatyTypeRegistry.TRIBUTARY), PLAYER, NPC, TREATY) > 0;
    }

    private static void counterTalksDownWhatItGives() {
        //asked for 2000 when it will give 500: it talks the points down rather than walking away
        List<SettlementTerm> asked = List.of(SettlementTerm.points(NPC, 2000));
        double need = SettlementPricing.need(true, true, 0, PEACE);
        assert SettlementPricing.net(asked, NPC, PLAYER, NPC, TREATY) < need;
        List<SettlementTerm> back = SettlementPricing.counter(asked, NPC, PLAYER, NPC, need, TREATY);
        assert back != null && back.size() == 1 : back;
        assert back.get(0).giver == NPC && back.get(0).amount == 500 : back;
        assert SettlementPricing.net(back, NPC, PLAYER, NPC, TREATY) >= need;
    }

    private static void counterAsksForTheShortfall() {
        //unwilling: a bare peace comes back with a price on it, rounded
        double need = SettlementPricing.need(true, false, -0.5, PEACE);
        List<SettlementTerm> back = SettlementPricing.counter(List.of(), NPC, PLAYER, NPC, need, TREATY);
        assert back != null && back.size() == 1 : back;
        SettlementTerm t = back.get(0);
        assert t.giver == PLAYER && t.amount >= need && t.amount % SettlementPricing.ROUND_TO == 0 : t;
        //an offer it would already take is not countered
        assert SettlementPricing.counter(List.of(SettlementTerm.points(PLAYER, 5000)), NPC, PLAYER, NPC, need, TREATY) == null;
    }

    private static void someThingsAreNotForSale() {
        SettlementTerm submit = SettlementTerm.signTreaty(NPC, TreatyTypeRegistry.VASSALAGE);
        assert SettlementPricing.valueTo(NPC, submit, PLAYER, NPC, TREATY) == Double.NEGATIVE_INFINITY;
        //dropped from the counter, never paid for
        List<SettlementTerm> back = SettlementPricing.counter(List.of(submit), NPC, PLAYER, NPC,
                SettlementPricing.need(true, true, 0, PEACE), TREATY);
        assert back != null && back.isEmpty() : back;
        assert SettlementPricing.counter(List.of(), NPC, PLAYER, NPC, Double.POSITIVE_INFINITY, TREATY) == null;
        //unless it has lost the war: then submitting is a price like any other, a steep one
        assert SettlementPricing.valueTo(NPC, submit, PLAYER, NPC, TREATY, true) == -TREATY * SettlementPricing.DEMAND_MULTIPLIER;
    }

    private static void systemsAreWorthTwoTreaties() {
        SettlementTerm cede = SettlementTerm.cedeSystem(NPC, new org.schema.common.util.linAlg.Vector3i(3, -1, 8));
        assert SettlementPricing.valueTo(NPC, cede, PLAYER, NPC, TREATY) == -2 * TREATY;
        assert SettlementPricing.valueTo(PLAYER, cede, PLAYER, NPC, TREATY) == 2 * TREATY;
        //priced alone, a system is more than even a beaten NPC gives for peace: taking one takes a
        //surrender (the war's own goals, see DiplomacyManager.judgeAsNPC) or faction points on top
        double need = SettlementPricing.need(true, true, 0, PEACE, 1);
        assert SettlementPricing.net(List.of(cede), NPC, PLAYER, NPC, TREATY, true) < need;
        assert SettlementPricing.surrenders(List.of(cede), NPC, List.of(cede));
        assert !SettlementPricing.surrenders(List.of(cede, SettlementTerm.points(NPC, 100)), NPC, List.of(cede)) : "nothing past the goals";
        assert SettlementPricing.surrenders(List.of(SettlementTerm.points(PLAYER, 100)), NPC, List.of()) : "what it receives is no part of surrender";
        assert cede.problem(PLAYER, NPC) == null;
    }
}
