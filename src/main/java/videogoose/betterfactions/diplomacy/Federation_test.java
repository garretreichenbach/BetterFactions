package videogoose.betterfactions.diplomacy;

import videogoose.betterfactions.diplomacy.Federation.FederationVote;
import videogoose.betterfactions.diplomacy.Federation.Outcome;
import videogoose.betterfactions.diplomacy.Federation.Threshold;

import java.util.function.IntToDoubleFunction;

/**
 * Pins the things about a bloc that nothing else would catch: that it survives a save, that a vote
 * is counted the way its charter says, and that the ballots stay the members' business.
 *
 * <p>The tally is worth a check rather than an eyeball because it is the only arithmetic in the
 * feature and it is arithmetic nobody sees until it goes wrong - an off-by-one either admits somebody
 * the bloc voted against or refuses somebody it voted for, and both look like a network problem from
 * the outside.</p>
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class Federation_test {

    private static final IntToDoubleFunction EQUAL = id -> 1.0;
    private static final long NOW = 1000L;

    private Federation_test() {
    }

    public static void main(String[] args) {
        aBlocSurvivesASave();
        openVotesSurviveASave();
        theCharterRulesPackAndSanitize();
        aMajorityIsMoreThanHalfOfWhoMayVote();
        theThresholdIsTheCharters();
        powerWeightingLetsTheBigOutvoteTheSmall();
        aVoteSettlesEarlyWhenItCannotCarry();
        anUnansweredVoteLapses();
        theSubjectOfAnExpulsionDoesNotVoteOnIt();
        aCarriedWarBindsWhomTheCharterSays();
        anEmptyBlocIsAGoneBloc();
        aVoteIsTheMembersBusiness();
        System.out.println("Federation_test: OK");
    }

    private static Federation sample() {
        Federation f = new Federation(3, "The Tarsis Concord", 1001, 500L);
        f.members.add(1002);
        f.members.add(1003);
        f.invites.add(1004);
        f.charter = "Mutual defence of the inner rim.";
        f.charterFlags = Federation.CHARTER_VOTE_ADMISSION;
        f.nextVoteId = 7;
        return f;
    }

    private static Federation roundTrip(Federation f) {
        Federation back = new Federation();
        back.fromTag(f.toTag());
        return back;
    }

    private static FederationVote vote(Federation f, FederationVote.Kind kind, int subject) {
        FederationVote v = new FederationVote(f.nextVoteId++, kind, subject, NOW);
        f.votes.add(v);
        return v;
    }

    private static void aBlocSurvivesASave() {
        Federation f = sample();
        f.chairSince = 700L;
        Federation back = roundTrip(f);
        assert back.id == 3 : back.id;
        assert "The Tarsis Concord".equals(back.name) : back.name;
        assert "Mutual defence of the inner rim.".equals(back.charter);
        assert back.leader == 1001 : back.leader;
        assert back.members.size() == 3 && back.members.getInt(0) == 1001 && back.members.getInt(2) == 1003
                : back.members;
        assert back.invites.size() == 1 && back.invites.getInt(0) == 1004 : back.invites;
        assert back.hasCharterFlag(Federation.CHARTER_VOTE_ADMISSION);
        assert !back.hasCharterFlag(Federation.CHARTER_VOTE_EXPULSION);
        assert back.nextVoteId == 7 : back.nextVoteId;
        assert back.founded == 500L && back.chairSince == 700L : back.founded + "/" + back.chairSince;
        assert back.votes.isEmpty() : "a bloc with no vote came back with one";
    }

    /**
     * Ballots are the part of a vote that cannot be recomputed - who answered and how is the whole of
     * it - so a save that drops them silently restarts the vote with everyone's answer lost. A
     * charter vote also carries the charter it would enact.
     */
    private static void openVotesSurviveASave() {
        Federation f = sample();
        FederationVote expel = vote(f, FederationVote.Kind.EXPEL, 1003);
        expel.ballots.put(1001, (byte) 1);
        expel.ballots.put(1002, (byte) 0);
        FederationVote charter = vote(f, FederationVote.Kind.CHARTER, 0);
        charter.value = Federation.withThreshold(0, Threshold.TWO_THIRDS);
        charter.text = "No first strike.";

        Federation back = roundTrip(f);
        assert back.votes.size() == 2 : "an open vote was lost on save";
        FederationVote e = back.vote(expel.id);
        assert e != null && e.kind == FederationVote.Kind.EXPEL && e.subject == 1003;
        assert e.closes == NOW + Federation.VOTE_WINDOW_MS : e.closes;
        assert e.count(true) == 1 && e.count(false) == 1 : e.count(true) + "/" + e.count(false);
        assert e.hasVoted(1001) && e.hasVoted(1002) && !e.hasVoted(1003);
        FederationVote c = back.voteOn(FederationVote.Kind.CHARTER, 12345);
        assert c != null : "a charter vote is found whatever subject is asked for";
        assert c.value == charter.value && "No first strike.".equals(c.text) : "the proposed charter was lost";
    }

    /**
     * The rules ride one long, so a field that bleeds into its neighbour silently rewrites the
     * charter. And the client names the flags, so what it may name has to be clamped.
     */
    private static void theCharterRulesPackAndSanitize() {
        long flags = Federation.CHARTER_VOTE_WAR | Federation.CHARTER_BIND_SUPPORTERS;
        flags = Federation.withThreshold(flags, Threshold.UNANIMOUS);
        flags = Federation.withRotationDays(flags, 30);
        Federation f = new Federation();
        f.charterFlags = flags;
        assert f.threshold() == Threshold.UNANIMOUS : f.threshold();
        assert f.rotationDays() == 30 : f.rotationDays();
        assert f.hasCharterFlag(Federation.CHARTER_VOTE_WAR) && f.hasCharterFlag(Federation.CHARTER_BIND_SUPPORTERS);
        assert !f.hasCharterFlag(Federation.CHARTER_VOTE_ADMISSION) : "the packed fields set a vote flag";

        f.charterFlags = Federation.withThreshold(flags, Threshold.SIMPLE_MAJORITY);
        assert f.rotationDays() == 30 : "changing the threshold changed the chair's term";

        assert Federation.sanitize(flags) == flags : "sanitising a valid charter changed it";
        f.charterFlags = Federation.sanitize(flags | (1L << 40) | 64L | (3L << 8));
        assert (f.charterFlags & (1L << 40)) == 0 && (f.charterFlags & 64L) == 0 : "an unknown rule got through";
        assert f.threshold() == Threshold.UNANIMOUS : "an out-of-range threshold was not clamped";
        f.charterFlags = Federation.sanitize(Federation.withRotationDays(0, 200));
        assert f.rotationDays() == Federation.MAX_ROTATION_DAYS : f.rotationDays();
    }

    private static void aMajorityIsMoreThanHalfOfWhoMayVote() {
        Federation f = new Federation(1, "x", 1);
        FederationVote v = vote(f, FederationVote.Kind.ADMIT, 9);
        v.ballots.put(1, (byte) 1);
        assert f.tally(v, EQUAL, NOW) == Outcome.CARRIED : "a bloc of one needs its own vote and no more";
        f.members.add(2);
        assert f.tally(v, EQUAL, NOW) == Outcome.OPEN : "two members, so one yes is a tie and not a majority";
        f.members.add(3);
        assert f.tally(v, EQUAL, NOW) == Outcome.OPEN;
        v.ballots.put(2, (byte) 1);
        assert f.tally(v, EQUAL, NOW) == Outcome.CARRIED : "three members, so two carries it";
        f.members.add(4);
        assert f.tally(v, EQUAL, NOW) == Outcome.OPEN : "four members, so two is a tie";
    }

    private static void theThresholdIsTheCharters() {
        Federation f = new Federation(1, "x", 1);
        f.members.add(2);
        f.members.add(3);
        FederationVote v = vote(f, FederationVote.Kind.WAR, 9);
        v.ballots.put(1, (byte) 1);
        v.ballots.put(2, (byte) 1);
        f.charterFlags = Federation.withThreshold(0, Threshold.TWO_THIRDS);
        assert f.tally(v, EQUAL, NOW) == Outcome.CARRIED : "two of three is two thirds";
        f.charterFlags = Federation.withThreshold(0, Threshold.UNANIMOUS);
        assert f.tally(v, EQUAL, NOW) == Outcome.OPEN : "unanimity carried without the third member";
        v.ballots.put(3, (byte) 0);
        assert f.tally(v, EQUAL, NOW) == Outcome.LOST : "one no cannot be outvoted under unanimity";
    }

    private static void powerWeightingLetsTheBigOutvoteTheSmall() {
        Federation f = new Federation(1, "x", 1);
        f.members.add(2);
        f.members.add(3);
        IntToDoubleFunction power = id -> id == 1 ? 500 : 100;
        FederationVote v = vote(f, FederationVote.Kind.WAR, 9);
        v.ballots.put(1, (byte) 1);
        assert f.tally(v, EQUAL, NOW) == Outcome.OPEN;
        assert f.tally(v, power, NOW) == Outcome.CARRIED : "500 of 700 is a majority by power";
    }

    /**
     * Once enough have said no that the rest together could not carry it, waiting is only waiting.
     */
    private static void aVoteSettlesEarlyWhenItCannotCarry() {
        Federation f = new Federation(1, "x", 1);
        f.members.add(2);
        f.members.add(3);
        FederationVote v = vote(f, FederationVote.Kind.ADMIT, 9);
        v.ballots.put(2, (byte) 0);
        assert f.tally(v, EQUAL, NOW) == Outcome.OPEN : "one no of three still leaves a majority possible";
        v.ballots.put(3, (byte) 0);
        assert f.tally(v, EQUAL, NOW) == Outcome.LOST : "two no of three cannot carry, but was left open";
    }

    private static void anUnansweredVoteLapses() {
        Federation f = new Federation(1, "x", 1);
        f.members.add(2);
        FederationVote v = vote(f, FederationVote.Kind.ADMIT, 9);
        v.ballots.put(1, (byte) 1);
        assert f.tally(v, EQUAL, v.closes - 1) == Outcome.OPEN;
        assert f.tally(v, EQUAL, v.closes) == Outcome.LOST : "an expired vote did not lapse";
    }

    /**
     * Otherwise a bloc of two could never expel anybody: the subject's own no would always tie it.
     */
    private static void theSubjectOfAnExpulsionDoesNotVoteOnIt() {
        Federation f = new Federation(1, "x", 1);
        f.members.add(2);
        FederationVote v = vote(f, FederationVote.Kind.EXPEL, 2);
        assert !f.mayVote(v, 2) : "the subject may vote on their own expulsion";
        v.ballots.put(1, (byte) 1);
        assert f.tally(v, EQUAL, NOW) == Outcome.CARRIED : "the subject was counted among those who may vote";
    }

    /**
     * The one real decision on the governance pane: whether a member that voted against a war is in it.
     */
    private static void aCarriedWarBindsWhomTheCharterSays() {
        Federation f = new Federation(1, "x", 1);
        f.members.add(2);
        f.members.add(3);
        FederationVote v = vote(f, FederationVote.Kind.WAR, 9);
        v.ballots.put(1, (byte) 1);
        v.ballots.put(2, (byte) 0);
        assert f.binds(v, 1) && f.binds(v, 2) && f.binds(v, 3) : "binding everyone left somebody out";
        f.charterFlags = Federation.CHARTER_BIND_SUPPORTERS;
        assert f.binds(v, 1) : "a supporter was not bound";
        assert !f.binds(v, 2) : "a member who voted against was bound";
        assert !f.binds(v, 3) : "a member who never voted was bound";
        assert !f.binds(v, 9) : "a non-member was bound";
    }

    private static void anEmptyBlocIsAGoneBloc() {
        Federation f = sample();
        assert !f.isDissolved();
        f.members.clear();
        assert f.isDissolved() : "a bloc with nobody in it still claims to exist";
        assert roundTrip(f).isDissolved() : "dissolution did not survive the wire";
    }

    /**
     * The one check here that is about a boundary rather than about arithmetic.
     *
     * <p>A bloc's roster is public - the membership treaties tell everybody anyway, and it decides
     * who shoots at whom. What it is arguing about is not, and because a federation is pushed per
     * recipient there is exactly one place that decision is made. A regression here would not throw
     * or look wrong on screen; it would quietly hand every client the ballots.</p>
     */
    private static void aVoteIsTheMembersBusiness() {
        Federation f = sample();
        FederationVote v = vote(f, FederationVote.Kind.ADMIT, 1004);
        v.ballots.put(1001, (byte) 1);
        v.ballots.put(1002, (byte) 0);

        Federation member = f.copyFor(1002);
        assert member.votes.size() == 1 : "a member cannot see the vote they are meant to be casting";
        assert member.votes.get(0).count(true) == 1 && member.votes.get(0).count(false) == 1;

        //the faction being voted about is the most tempting person to leak to, and gets nothing
        assert f.copyFor(1004).votes.isEmpty() : "the faction under discussion was handed the ballots";

        Federation outsider = f.copyFor(9999);
        assert outsider.votes.isEmpty() : "a faction outside the bloc was handed its ballots";
        assert outsider.members.size() == 3 : "redaction also took the roster, which is public";
        assert "The Tarsis Concord".equals(outsider.name) && outsider.leader == 1001
                : "redaction also took the name or the leader, which are public";

        assert f.copyFor(0).votes.isEmpty() : "a player in no faction was handed the ballots";

        //a copy, not a view: what goes on the wire must not drift as the live bloc changes
        member.name = "changed";
        member.members.clear();
        assert "The Tarsis Concord".equals(f.name) && f.members.size() == 3
                : "copyFor handed out a view of the live bloc";
    }
}
