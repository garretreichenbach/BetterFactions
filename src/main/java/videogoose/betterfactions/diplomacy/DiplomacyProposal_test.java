package videogoose.betterfactions.diplomacy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.player.faction.FactionRelation.RType;

/**
 * Pins what a proposal carries across the wire and through a save.
 *
 * <p>The wire half is a trust boundary: the server decides whether a request may set a faction's
 * foreign policy by looking at the sender the network layer stamps on, not at the initiator name
 * the sending client wrote. A round trip that lost the sender, or let a deserialize set
 * {@code serverAuthored}, would hand every client the permission bypass the engine's own proposals
 * use.</p>
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class DiplomacyProposal_test {

    private DiplomacyProposal_test() {
    }

    public static void main(String[] args) throws IOException {
        offerCodeIsDirected();
        legacyRelationsMapToTreaties();
        roundTripsThroughTheWire();
        wireCannotClaimServerAuthority();
        wireCannotInventAnAction();
        roundTripsThroughTag();
        legacyTagLoads();
        responseKeepsTheOffersDirection();
        queriesAreNeitherOffersNorResponses();
        queryResultRoundTripsThroughTheWire();
        counterKeepsItsName();
        termsSurviveWireAndTag();
        counterKeepsWhoPays();
        termsAreCheckedOnTheirOwn();
        System.out.println("DiplomacyProposal_test: OK");
    }

    /**
     * A query asks without offering. If it read as a response it would be matched against a staged
     * offer and silently dropped; if it read as an ordinary proposal it would put a treaty on the
     * table that nobody meant to offer, and move opinion doing it.
     */
    private static void queriesAreNeitherOffersNorResponses() {
        DiplomacyProposal q = DiplomacyProposal.query("Asker", 11, 22, TreatyTypeRegistry.ALLIANCE);
        assert q.action == DiplomacyProposal.Action.QUERY : q.action;
        assert q.isQuery();
        assert !q.isResponse();
        assert q.a == 11 && q.b == 22;

        DiplomacyProposal yes = DiplomacyProposal.queryResult(q, true, "ignored");
        assert yes.isQuery() && !yes.isResponse();
        assert yes.accepted;
        //an accepted answer carries no reasons, so the panel has nothing to render beside "yes"
        assert "".equals(yes.getMessage()) : yes.getMessage();
        //the answer keeps the question's direction, which is how the asking panel tells its own
        //answer from every other client's on the shared buffer
        assert yes.a == q.a && yes.b == q.b;
        assert TreatyTypeRegistry.ALLIANCE.equals(yes.typeId) : yes.typeId;

        DiplomacyProposal no = DiplomacyProposal.queryResult(q, false, "opinion too low");
        assert !no.accepted;
        assert "opinion too low".equals(no.getMessage()) : no.getMessage();
    }

    private static void queryResultRoundTripsThroughTheWire() throws IOException {
        DiplomacyProposal q = DiplomacyProposal.query("Asker", 3, 4, null);
        assert q.typeId == null : "a query about white peace names no treaty";

        DiplomacyProposal answered = DiplomacyProposal.queryResult(q, false, "you are at war with our ally");
        DiplomacyProposal back = wire(answered, 9);
        assert back.action == DiplomacyProposal.Action.QUERY_RESULT : back.action;
        assert !back.accepted;
        assert back.typeId == null : back.typeId;
        assert "you are at war with our ally".equals(back.getMessage()) : back.getMessage();
        //queryResult marks itself server-authored; a deserialize must still not grant that
        assert !back.serverAuthored : "a deserialize granted server authority";

        DiplomacyProposal acceptedBack = wire(DiplomacyProposal.queryResult(q, true, null), 9);
        assert acceptedBack.accepted : "the verdict did not survive the wire";
    }

    /**
     * Who asked is part of what an offer is, so unlike the relation key this one must not collapse
     * the pair - otherwise an incoming offer and an outgoing one overwrite each other.
     */
    private static void offerCodeIsDirected() {
        assert DiplomacyProposal.getOfferCode(10, 20) != DiplomacyProposal.getOfferCode(20, 10);
        assert FactionRelation.getCode(10, 20) == FactionRelation.getCode(20, 10);
    }

    private static void legacyRelationsMapToTreaties() {
        assert TreatyTypeRegistry.WAR.equals(DiplomacyProposal.treatyIdOf(RType.ENEMY.code));
        assert TreatyTypeRegistry.ALLIANCE.equals(DiplomacyProposal.treatyIdOf(RType.FRIEND.code));
        //peace is the absence of an agreement, not one of its own
        assert DiplomacyProposal.treatyIdOf(RType.NEUTRAL.code) == null;
    }

    private static void roundTripsThroughTheWire() throws IOException {
        DiplomacyProposal p = new DiplomacyProposal();
        p.set("Someone", 7, 9, TreatyTypeRegistry.TRADE_AGREEMENT, RType.NEUTRAL.code,
                "ore for parts", DiplomacyProposal.Action.PROPOSE);

        DiplomacyProposal back = wire(p, 42);
        assert back.a == 7 && back.b == 9 : back.a + "->" + back.b;
        assert TreatyTypeRegistry.TRADE_AGREEMENT.equals(back.typeId) : back.typeId;
        assert back.action == DiplomacyProposal.Action.PROPOSE : back.action;
        assert "ore for parts".equals(back.getMessage()) : back.getMessage();
        assert "Someone".equals(back.getInitiator()) : back.getInitiator();
        //the one field on the proposal the sender does not choose
        assert back.sender == 42 : back.sender;

        //a bare relation change carries no treaty, and must not come back as the empty string
        DiplomacyProposal peace = new DiplomacyProposal();
        peace.set("Someone", 1, 2, RType.NEUTRAL.code, "", false);
        assert peace.typeId == null;
        assert wire(peace, 1).typeId == null;
    }

    /**
     * The flag that skips the permission gate exists only for proposals the engine built.
     */
    private static void wireCannotClaimServerAuthority() throws IOException {
        DiplomacyProposal p = new DiplomacyProposal();
        p.set("Someone", 1, 2, RType.ENEMY.code, "war", false);
        p.serverAuthored = true;
        assert !wire(p, 3).serverAuthored : "a deserialize granted server authority";
    }

    private static void wireCannotInventAnAction() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        DiplomacyProposal p = new DiplomacyProposal();
        p.set("Someone", 1, 2, RType.ENEMY.code, "war", false);
        p.serialize(out, true);
        byte[] raw = bytes.toByteArray();
        //the action byte sits after the version, the two faction ids, the relation and the type id
        int actionAt = 1 + 4 + 4 + 1 + 2 + TreatyTypeRegistry.WAR.length();
        assert raw[actionAt] == DiplomacyProposal.Action.PROPOSE.ordinal() : raw[actionAt];
        raw[actionAt] = 99;
        try {
            new DiplomacyProposal().deserialize(new DataInputStream(new ByteArrayInputStream(raw)), 1, true);
            assert false : "an unknown action must be refused, not clamped to a real one";
        } catch (IOException expected) {
            // ok
        }
    }

    private static void roundTripsThroughTag() {
        DiplomacyProposal p = new DiplomacyProposal();
        p.set("Someone", 3, 4, TreatyTypeRegistry.DEFENSIVE_PACT, RType.FRIEND.code,
                "call us", DiplomacyProposal.Action.PROPOSE);

        DiplomacyProposal back = new DiplomacyProposal();
        back.fromTagStructure(p.toTagStructure());
        assert back.a == 3 && back.b == 4;
        assert TreatyTypeRegistry.DEFENSIVE_PACT.equals(back.typeId) : back.typeId;
        assert back.action == DiplomacyProposal.Action.PROPOSE : back.action;
        assert "call us".equals(back.getMessage());
    }

    /**
     * A save written before proposals named a treaty type still loads, as a plain offer.
     */
    private static void legacyTagLoads() {
        org.schema.schine.resource.tag.Tag legacy = new org.schema.schine.resource.tag.Tag(
                org.schema.schine.resource.tag.Tag.Type.STRUCT, null,
                new org.schema.schine.resource.tag.Tag[]{
                        new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.INT, null, 5),
                        new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.INT, null, 6),
                        new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.BYTE, null, RType.FRIEND.code),
                        new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.STRING, null, "be friends"),
                        new org.schema.schine.resource.tag.Tag(org.schema.schine.resource.tag.Tag.Type.STRING, null, "Someone"),
                        org.schema.schine.resource.tag.FinishTag.INST});

        DiplomacyProposal back = new DiplomacyProposal();
        back.fromTagStructure(legacy);
        assert back.a == 5 && back.b == 6;
        assert back.action == DiplomacyProposal.Action.PROPOSE : back.action;
        //an alliance offer is an alliance offer whether or not the file knew the word
        assert TreatyTypeRegistry.ALLIANCE.equals(back.typeId) : back.typeId;
    }

    /**
     * The answer is addressed to the pair the offer was, not to the responder - a response keyed the
     * other way round would never find the offer it answers.
     */
    private static void responseKeepsTheOffersDirection() {
        DiplomacyProposal offer = new DiplomacyProposal();
        offer.set("Proposer", 11, 22, RType.FRIEND.code, "ally?", false);

        DiplomacyProposal yes = DiplomacyProposal.respondTo(offer, "Target", true);
        assert yes.getCode() == offer.getCode() : yes.getCode() + " vs " + offer.getCode();
        assert yes.action == DiplomacyProposal.Action.ACCEPT;
        assert yes.isResponse();
        assert "Target".equals(yes.getInitiator());
        assert !yes.serverAuthored : "a response is only trusted when the engine marks it";

        DiplomacyProposal no = DiplomacyProposal.respondTo(offer, "Target", false);
        assert no.action == DiplomacyProposal.Action.DECLINE;
        assert no.isResponse();
        assert !offer.isResponse();
    }

    /**
     * A counter-offer is a proposal of its own, so the name it asks for has to come with it.
     */
    private static void counterKeepsItsName() {
        DiplomacyProposal counter = new DiplomacyProposal();
        counter.set("Target", 11, 22, RType.FRIEND.code, "how about this", false);
        counter.name = "The Better Accord";
        DiplomacyProposal back = counter.asCounterProposal();
        assert "The Better Accord".equals(back.name) : back.name;
        assert back.a == 22 && back.b == 11 : "the counter is asked the other way round";
    }

    private static DiplomacyProposal settlement() {
        DiplomacyProposal p = new DiplomacyProposal();
        p.set("Someone", 3, 4, null, RType.NEUTRAL.code, "enough", DiplomacyProposal.Action.PROPOSE);
        p.terms.add(SettlementTerm.points(4, 500));
        p.terms.add(SettlementTerm.signTreaty(4, TreatyTypeRegistry.ALLIANCE));
        p.terms.add(SettlementTerm.breakTreaty(3, TreatyTypeRegistry.DEFENSIVE_PACT, 9));
        return p;
    }

    /**
     * Terms are what a peace is worth; one dropped in transit signs a different deal than was agreed.
     */
    private static void termsSurviveWireAndTag() throws IOException {
        DiplomacyProposal p = settlement();
        assert wire(p, 1).terms.equals(p.terms) : wire(p, 1).terms;
        DiplomacyProposal back = new DiplomacyProposal();
        back.fromTagStructure(p.toTagStructure());
        assert back.terms.equals(p.terms) : back.terms;
        //a call to arms names who it is against; losing that would turn it into war on the caller
        p.against = 77;
        assert wire(p, 1).against == 77;
        back.fromTagStructure(p.toTagStructure());
        assert back.against == 77 : back.against;
        //and a query's answer echoes them, so a stale preview is not read as this one's
        DiplomacyProposal q = DiplomacyProposal.query("Asker", 3, 4, null, p.terms);
        assert DiplomacyProposal.queryResult(q, false, "no").terms.equals(p.terms);
    }

    /**
     * The pair flips on a counter; who pays must not. Givers are faction ids for exactly this reason.
     */
    private static void counterKeepsWhoPays() {
        DiplomacyProposal p = settlement();
        DiplomacyProposal c = DiplomacyProposal.counterTo(p, "Other", null, 0L, "", "", p.terms).asCounterProposal();
        assert c.a == 4 && c.b == 3;
        assert c.terms.equals(p.terms) : c.terms;
        assert c.terms.get(0).receiver(c.a, c.b) == 3 : "the payer's points went to the wrong side";
    }

    private static void termsAreCheckedOnTheirOwn() {
        assert SettlementTerm.problem(settlement().terms, 3, 4) == null : SettlementTerm.problem(settlement().terms, 3, 4);
        assert SettlementTerm.points(7, 10).problem(3, 4) != null : "a third faction was made to pay";
        assert SettlementTerm.points(3, 0).problem(3, 4) != null;
        assert SettlementTerm.points(3, Float.NaN).problem(3, 4) != null;
        assert SettlementTerm.points(3, -50).problem(3, 4) != null : "a negative payment is a demand in disguise";
        assert SettlementTerm.signTreaty(3, TreatyTypeRegistry.WAR).problem(3, 4) != null : "war is not agreed";
        assert SettlementTerm.signTreaty(3, TreatyTypeRegistry.FEDERATION).problem(3, 4) != null;
        assert SettlementTerm.signTreaty(3, "nobody:nothing").problem(3, 4) != null;
        assert SettlementTerm.breakTreaty(3, TreatyTypeRegistry.ALLIANCE, 3).problem(3, 4) != null;
        assert SettlementTerm.breakTreaty(3, TreatyTypeRegistry.ALLIANCE, 0).breakWith(3, 4) == 4;
        assert new SettlementTerm(null, 3, "", 0, 0).problem(3, 4) != null : "an unknown kind slipped through";
    }

    private static DiplomacyProposal wire(DiplomacyProposal p, int senderStateId) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        p.serialize(out, true);
        DiplomacyProposal back = new DiplomacyProposal();
        back.deserialize(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())), senderStateId, false);
        return back;
    }
}
