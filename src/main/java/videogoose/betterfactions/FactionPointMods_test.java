package videogoose.betterfactions;

import static videogoose.betterfactions.FactionPointMods.Line.*;

/**
 * Pins the mod's point lines: net sums them, a cleared line zeroes for everyone, and the lines survive the
 * tag the client is sent.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class FactionPointMods_test {

    private FactionPointMods_test() {
    }

    public static void main(String[] args) {
        FactionPointMods m = new FactionPointMods();
        m.set(1, FLEET, -5);
        m.set(1, STATIONS, 8);
        m.add(1, TRIBUTE, -2);
        m.add(2, TRIBUTE, 2);
        assert m.net(1) == 1 : "8 - 5 - 2";
        assert m.net(3) == 0 : "unknown faction";

        FactionPointMods back = new FactionPointMods();
        back.fromTag(m.toTag());
        assert back.get(1, STATIONS) == 8 && back.get(2, TRIBUTE) == 2 && back.net(1) == 1;

        m.clear(TRIBUTE);
        assert m.get(1, TRIBUTE) == 0 && m.get(2, TRIBUTE) == 0 && m.get(1, FLEET) == -5;
        System.out.println("FactionPointMods_test: OK");
    }
}
