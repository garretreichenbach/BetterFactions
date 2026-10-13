package videogoose.betterfactions.diplomacy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * How an NPC faction prices a settlement, in faction points.
 *
 * <p>Every term is worth something to the NPC: what it receives counts for it, what it gives counts
 * against it. Peace itself is worth something too, and how much depends on who is winning: an NPC
 * facing a stronger enemy will give up more to stop the war, and one facing a weaker enemy wants to be
 * paid for stopping. Strength is the archetype's own {@link FactionStrength}, which counts ships.</p>
 *
 * <p>Pure, so the haggling can be checked without a server.</p>
 */
public final class SettlementPricing {

    /** Strength advantage beyond this many doublings changes nothing more. */
    static final double LEVERAGE_CLAMP = 4;
    /** Submitting to anyone is never for sale. */
    static final double NEVER = Double.NEGATIVE_INFINITY;
    /** A vassalage or tributary costs this many ordinary treaties. */
    static final double DEMAND_MULTIPLIER = 5;
    /** A star system costs this many ordinary treaties. */
    static final double SYSTEM_MULTIPLIER = 2;
    /**
     * Weariness at or past which an NPC has lost the war outright and will submit: a vassalage or
     * tributary demanded of it is priced like any other term rather than refused.
     */
    public static final double BEATEN = 0.9;
    /** Counter demands are rounded up to this, so they read like a price rather than a calculation. */
    static final int ROUND_TO = 50;

    private SettlementPricing() {
    }

    /**
     * What one term is worth to {@code npc}, signing between {@code a} and {@code b}.
     */
    public static double valueTo(int npc, SettlementTerm t, int a, int b, double treatyValue) {
        return valueTo(npc, t, a, b, treatyValue, false);
    }

    /**
     * @param beaten whether {@code npc} has lost the war ({@link #BEATEN}); only then is submitting for sale
     */
    public static double valueTo(int npc, SettlementTerm t, int a, int b, double treatyValue, boolean beaten) {
        boolean gives = t.giver == npc;
        switch (t.kind) {
            case TRANSFER_FACTION_POINTS:
                return gives ? -t.amount : t.amount;
            case SIGN_TREATY: {
                TreatyType type = TreatyTypeRegistry.get(t.typeId);
                if (type != null && type.isSymmetric()) {
                    //both sides take on the same thing, so neither is giving it to the other: whether a
                    //pact is wanted is the archetype's rule to say, not something to be paid for
                    return 0;
                }
                boolean demand = type != null && type.getKind() == TreatyType.Kind.DEMAND;
                if (gives && demand && !beaten) {
                    return NEVER;
                }
                double v = treatyValue * (demand ? DEMAND_MULTIPLIER : 1);
                return gives ? -v : v;
            }
            case BREAK_TREATY:
                return gives ? -treatyValue : treatyValue;
            case CEDE_SYSTEM:
                return (gives ? -1 : 1) * treatyValue * SYSTEM_MULTIPLIER;
            default:
                return 0;
        }
    }

    /**
     * Whether everything {@code npc} gives in {@code terms} is among the war's {@code goals}: a beaten NPC
     * signs that whatever it costs, because it is what the war was fought for.
     */
    public static boolean surrenders(List<SettlementTerm> terms, int npc, List<SettlementTerm> goals) {
        for (SettlementTerm t : terms) {
            if (t.giver == npc && !goals.contains(t)) {
                return false;
            }
        }
        return true;
    }

    public static double net(List<SettlementTerm> terms, int npc, int a, int b, double treatyValue) {
        return net(terms, npc, a, b, treatyValue, false);
    }

    public static double net(List<SettlementTerm> terms, int npc, int a, int b, double treatyValue, boolean beaten) {
        double sum = 0;
        for (SettlementTerm t : terms) {
            sum += valueTo(npc, t, a, b, treatyValue, beaten);
        }
        return sum;
    }

    /**
     * The least the terms must be worth to the NPC for it to sign. Negative is how much it will give.
     *
     * @param peace        whether this ends a war (a bare peace offer); only then does leverage count
     * @param ruleAccepts  whether its archetype's acceptance rule takes the agreement on its own
     * @param theirLead    how much stronger the other side is, in doublings
     *                     ({@link DiplomacyEntity#getRelativeStrength()})
     * @param peaceValue   the archetype's price of peace between equals
     */
    public static double need(boolean peace, boolean ruleAccepts, double theirLead, double peaceValue) {
        return need(peace, ruleAccepts, theirLead, peaceValue, 0);
    }

    /**
     * @param weariness 0 to 1: how badly its recent battles against this side have gone
     *                  ({@link DiplomacyEntity#getWeariness()}). A beaten NPC asks less to stop, down to nothing, and
     *                  gives up to twice as much. On top of {@code theirLead}, which already counts the
     *                  losses as lost strength: this is the will to keep fighting, not the means.
     */
    public static double need(boolean peace, boolean ruleAccepts, double theirLead, double peaceValue, double weariness) {
        if (!peace) {
            //only peace is bought: an alliance the archetype will not make is not for sale
            return ruleAccepts ? 0 : Double.POSITIVE_INFINITY;
        }
        double lead = Math.clamp(theirLead, -LEVERAGE_CLAMP, LEVERAGE_CLAMP);
        double w = Math.clamp(weariness, 0, 1);
        //willing: it concedes more the stronger they are; unwilling: it wants paying, less the stronger they are
        if (ruleAccepts) {
            return -peaceValue * Math.pow(2, lead) * (1 + w);
        }
        //and once they are the stronger side it stops asking and starts conceding: the winner never pays
        return peaceValue * (Math.pow(2, -lead) * (1 - w) - Math.max(0, lead));
    }

    /**
     * The terms the NPC would sign instead, or null if there are none or they are what was offered.
     * Talks down or drops what it gives, costliest first, then asks the other side for any shortfall.
     */
    public static List<SettlementTerm> counter(List<SettlementTerm> offered, int npc, int a, int b,
                                               double need, double treatyValue) {
        return counter(offered, npc, a, b, need, treatyValue, false);
    }

    public static List<SettlementTerm> counter(List<SettlementTerm> offered, int npc, int a, int b,
                                               double need, double treatyValue, boolean beaten) {
        if (Double.isInfinite(need)) {
            return null;
        }
        int other = npc == a ? b : a;
        List<SettlementTerm> terms = new ArrayList<>(offered);
        double net = net(terms, npc, a, b, treatyValue, beaten);
        List<SettlementTerm> given = new ArrayList<>();
        for (SettlementTerm t : terms) {
            if (t.giver == npc) {
                given.add(t);
            }
        }
        given.sort(Comparator.comparingDouble(t -> valueTo(npc, t, a, b, treatyValue, beaten)));
        for (SettlementTerm t : given) {
            double deficit = need - net;
            if (deficit <= 0) {
                break;
            }
            int at = terms.indexOf(t);
            if (t.kind == SettlementTerm.Kind.TRANSFER_FACTION_POINTS && t.amount > deficit) {
                float left = (float) (Math.floor((t.amount - deficit) / ROUND_TO) * ROUND_TO);
                net += t.amount - left;
                if (left > 0) {
                    terms.set(at, SettlementTerm.points(npc, left));
                } else {
                    terms.remove(at);
                }
            } else {
                net -= valueTo(npc, t, a, b, treatyValue, beaten);
                terms.remove(at);
            }
        }
        double deficit = need - net;
        if (deficit > 0) {
            float ask = (float) (Math.ceil(deficit / ROUND_TO) * ROUND_TO);
            int at = -1;
            for (int i = 0; i < terms.size(); i++) {
                SettlementTerm t = terms.get(i);
                if (t.kind == SettlementTerm.Kind.TRANSFER_FACTION_POINTS && t.giver == other) {
                    at = i;
                }
            }
            if (at >= 0) {
                terms.set(at, SettlementTerm.points(other, terms.get(at).amount + ask));
            } else {
                terms.add(SettlementTerm.points(other, ask));
            }
        }
        if (terms.size() > DiplomacyProposal.MAX_TERMS || terms.equals(offered)) {
            return null;
        }
        return terms;
    }
}
