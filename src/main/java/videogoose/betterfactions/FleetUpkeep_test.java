package videogoose.betterfactions;

import java.util.List;

/**
 * Pins fleet upkeep by mass: an unknown mass is free, and known masses survive a tag round trip.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class FleetUpkeep_test {

    private FleetUpkeep_test() {
    }

    public static void main(String[] args) {
        FleetUpkeep u = new FleetUpkeep();
        u.sawMass("big", 50_000);
        u.sawMass("small", 10_000);
        u.sawMass("ghost", 0);
        List<String> fleet = List.of("big", "small", "unseen", "ghost");
        assert u.upkeep(fleet, 10_000) == 6 : "big + small; unseen and massless are free";
        assert u.upkeep(fleet, 0) == 0 : "off";

        FleetUpkeep back = new FleetUpkeep();
        back.fromTag(u.toTag());
        assert back.upkeep(fleet, 10_000) == 6;

        u.sawThrustMassRatio("big", 2f);
        back = new FleetUpkeep();
        back.fromTag(u.toTag());
        assert back.thrustMassRatioOf("big") == 2f && back.thrustMassRatioOf("small") == 0 : "ratio survives, unseen stays 0";
        assert FleetUpkeep.speedFactor(new float[]{1f, 3f, 0f}, 1f, 0.25f, 4f) == 2f : "average of the known, unknown left out";
        assert FleetUpkeep.speedFactor(new float[]{0f}, 1f, 0.25f, 4f) == 1f : "none known: vanilla";
        assert FleetUpkeep.speedFactor(new float[]{0.01f}, 1f, 0.25f, 4f) == 0.25f : "clamped";
        assert FleetUpkeep.stationIncome(10_000, 2, 10_000) == 2 : "base at min mass";
        assert Math.abs(FleetUpkeep.stationIncome(1_000_000, 2, 10_000) - 6) < 1e-4 : "+base per tenfold";
        assert FleetUpkeep.stationIncome(9_999, 2, 10_000) == 0 : "too small";
        assert FleetUpkeep.stationIncome(0, 2, 0) == 0 : "unseen";
        assert FleetUpkeep.stationIncome(50_000, 0, 10_000) == 0 : "off";
        System.out.println("FleetUpkeep_test: OK");
    }
}
