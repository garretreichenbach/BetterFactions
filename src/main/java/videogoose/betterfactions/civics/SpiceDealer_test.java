package videogoose.betterfactions.civics;

import org.schema.common.util.linAlg.Vector3i;

import java.util.List;
import java.util.Map;

/**
 * Pins Spice Dealer upkeep: faces only, a percent of that system's own upkeep, stacking per dealer
 * faction but not per dealer system, and never against the dealer itself.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class SpiceDealer_test {

    private SpiceDealer_test() {
    }

    public static void main(String[] args) {
        List<Vector3i> mine = List.of(new Vector3i(0, 0, 0), new Vector3i(5, 5, 5));
        float[] upkeep = {100, 40};

        Map<Vector3i, Integer> faceOnly = SpiceDealer.dealerMap(Map.of(2, List.of(new Vector3i(1, 0, 0), new Vector3i(1, 1, 0))));
        assert SpiceDealer.extra(mine, upkeep, faceOnly, 1, 25) == 25 : "one dealer, two of its systems touching, diagonal ignored";

        Map<Vector3i, Integer> two = SpiceDealer.dealerMap(Map.of(2, List.of(new Vector3i(1, 0, 0)), 3, List.of(new Vector3i(0, -1, 0), new Vector3i(5, 5, 6))));
        assert SpiceDealer.extra(mine, upkeep, two, 1, 25) == 50 + 10 : "two dealers stack; second system pays its own share";

        assert SpiceDealer.extra(mine, upkeep, two, 2, 25) == 25 + 10 : "a dealer does not charge itself";
        assert SpiceDealer.extra(mine, upkeep, two, 1, 0) == 0;
        System.out.println("SpiceDealer_test: OK");
    }
}
