package videogoose.betterfactions.diplomacy;

/**
 * Pins how reputation accrues and fades: one half-life halves it, the side that fires first in an
 * engagement is the only one charged, and it survives a save.
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class Reputation_test {

    private static final long DAY = 86400000L;

    private Reputation_test() {
    }

    public static void main(String[] args) {
        halfLifeHalves();
        defenderIsNotCharged();
        oneEngagementCountsOnce();
        roundTrips();
        System.out.println("Reputation_test: OK");
    }

    private static void halfLifeHalves() {
        Reputation r = new Reputation();
        r.brokeTreaty(1);
        r.brokeTreaty(1);
        assert r.decay(1000, 14) : "fresh infamy should be sent";
        r.decay(1000 + 14 * DAY, 14);
        assert Math.abs(r.infamyOf(1) - 1) < 1e-9 : r.infamyOf(1);
        r.decay(1000 + 200 * DAY, 14);
        assert r.infamyOf(1) == 0 : "faded below the floor is forgotten: " + r.infamyOf(1);
    }

    private static void defenderIsNotCharged() {
        Reputation r = new Reputation();
        assert r.attacked(1, 2, 1000);
        assert !r.attacked(2, 1, 2000) : "shooting back is defence";
        assert r.aggressionOf(2) == 0;
        assert r.aggressionOf(1) == Reputation.PER_ATTACK;
    }

    private static void oneEngagementCountsOnce() {
        Reputation r = new Reputation();
        r.attacked(1, 2, 0);
        r.attacked(1, 2, Reputation.ENGAGEMENT_MS - 1);
        assert r.aggressionOf(1) == Reputation.PER_ATTACK : r.aggressionOf(1);
        //a hit inside the window extends it, so a long fight stays one engagement
        r.attacked(1, 2, 2 * Reputation.ENGAGEMENT_MS - 2);
        assert r.aggressionOf(1) == Reputation.PER_ATTACK : r.aggressionOf(1);
        r.attacked(1, 2, 4 * Reputation.ENGAGEMENT_MS);
        assert r.aggressionOf(1) == 2 * Reputation.PER_ATTACK : r.aggressionOf(1);
    }

    private static void roundTrips() {
        Reputation r = new Reputation();
        r.brokeTreaty(5);
        r.declaredWar(6);
        Reputation back = new Reputation();
        back.fromTag(r.toTag());
        assert back.infamyOf(5) == Reputation.PER_BREAK && back.aggressionOf(5) == 0;
        assert back.aggressionOf(6) == Reputation.PER_WAR && back.infamyOf(6) == 0;
    }
}
