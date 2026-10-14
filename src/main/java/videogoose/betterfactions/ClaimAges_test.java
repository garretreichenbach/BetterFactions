package videogoose.betterfactions;

import org.schema.common.util.linAlg.Vector3i;

/**
 * Pins the mining bonus ramp: a fresh claim gets none of it, it climbs evenly, and claim times survive a tag round trip.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class ClaimAges_test {

    private ClaimAges_test() {
    }

    public static void main(String[] args) {
        assert ClaimAges.ramp(0, 100) == 0 : "fresh claim";
        assert ClaimAges.ramp(50, 100) == 0.5f : "halfway";
        assert ClaimAges.ramp(500, 100) == 1 : "capped";
        assert ClaimAges.ramp(-5, 100) == 0 : "clock skew clamps";

        ClaimAges a = new ClaimAges();
        Vector3i sys = new Vector3i(1, -2, 3);
        a.claimed(sys, 1000);
        ClaimAges back = new ClaimAges();
        back.fromTag(a.toTag());
        assert back.maturity(new Vector3i(9, 9, 9), 1000) == 1 : "unknown system counts as long held";
        back.unclaimed(sys);
        assert back.maturity(sys, 1000) == 1 : "unclaimed forgets";
        System.out.println("ClaimAges_test ok");
    }
}
