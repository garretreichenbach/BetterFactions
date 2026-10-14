package videogoose.betterfactions.civics;

import static videogoose.betterfactions.civics.Senate.Outcome.CARRIED;
import static videogoose.betterfactions.civics.Senate.Outcome.LOST;
import static videogoose.betterfactions.civics.Senate.Outcome.OPEN;

/**
 * Pins how a Senate vote is counted, and the rising institution upkeep it is paid for with.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class Senate_test {

    private Senate_test() {
    }

    public static void main(String[] args) {
        assert Senate.tally(1, 0, 1, false) == CARRIED : "a faction of one decides at once";
        assert Senate.tally(1, 0, 4, false) == OPEN;
        assert Senate.tally(3, 0, 4, false) == CARRIED : "more than half of all senators";
        assert Senate.tally(2, 0, 4, false) == OPEN : "half is not more than half";
        assert Senate.tally(1, 2, 4, false) == LOST : "two against of four: a majority is out of reach";
        assert Senate.tally(2, 1, 10, true) == CARRIED : "at the close, the ballots cast decide";
        assert Senate.tally(1, 1, 10, true) == LOST : "a tie at the close fails";

        assert FactionCivics.institutionUpkeep(0, 10) == 0;
        assert FactionCivics.institutionUpkeep(1, 10) == 10;
        assert FactionCivics.institutionUpkeep(3, 10) == 60 : "10 + 20 + 30";

        FactionCivics c = new FactionCivics();
        c.institutions.add(CivicRegistry.SENATE);
        assert FactionCivics.fromTag(c.toTag()).institutions.equals(c.institutions);
        assert c.conflict(0, CivicRegistry.SENATE) != null : "not a civic";

        assert Civics.seatMask(0) == 0 : "every rank sits";
        assert Civics.seatMask(1 << 2) == ((1 << 2) | (1 << 9)) : "the founder always sits";
        assert Civics.seatMask(-1) == (1 << 10) - 1 : "rank bits only";
        FactionCivics r = new FactionCivics();
        r.senateSeats = Civics.seatMask(1 << 7);
        r.senateHours = 12;
        FactionCivics rb = FactionCivics.fromTag(r.toTag());
        assert rb.seats(9) && rb.seats(7) && !rb.seats(0) : "seats survive a save";
        assert rb.voteHours(24, 72) == 12 && new FactionCivics().voteHours(24, 72) == 24;
        assert !rb.senateVeto;
        r.senateVeto = true;
        assert FactionCivics.fromTag(r.toTag()).senateVeto : "the veto survives a save";
        r.senateName = FactionCivics.cleanSenateName("  Grand Assembly  ");
        assert FactionCivics.fromTag(r.toTag()).senateName.equals("Grand Assembly") : "the name survives a save, trimmed";
        assert FactionCivics.cleanSenateName("x".repeat(50)).length() == FactionCivics.MAX_SENATE_NAME;
        rb.senateHours = 500;
        assert rb.voteHours(24, 72) == 72 : "capped by the server";

        Senate.Vote v = new Senate.Vote();
        v.proposer = "Vael";
        v.reason = "They raided our miners.";
        v.ballots.put("vael", (byte) 1);
        v.holdUntil = 12345L;
        Senate.Vote back = Senate.Vote.fromTag(v.toTag(true));
        assert back.reason.equals(v.reason) && back.count(true) == 1 : "reason and ballots survive a save";
        assert back.held() && back.holdUntil == 12345L : "a vote waiting on the leader still waits after a restart";
        System.out.println("Senate_test: OK");
    }
}
