package videogoose.betterfactions.civics;

/**
 * Pins the pick rules the server enforces: slots fill in order, no civic twice, exclusions hold both
 * ways, a swap costs points and waits out its cooldown, and the record survives a tag round trip.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class FactionCivics_test {

    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final int COST = 1000;

    private FactionCivics_test() {
    }

    public static void main(String[] args) {
        slotsFillInOrder();
        noCivicTwiceAndExclusionsBothWays();
        aSwapCostsAndWaits();
        survivesATag();
        System.out.println("FactionCivics_test: OK");
    }

    private static boolean ok(FactionCivics c, int slot, String id, float points, long now) {
        return c.problem(slot, id, points, now, COST, 7 * DAY) == null;
    }

    private static void slotsFillInOrder() {
        FactionCivics c = new FactionCivics();
        assert !ok(c, 1, CivicRegistry.RAIDERS, 0, 0) : "slot 1 before slot 0";
        assert !ok(c, -1, CivicRegistry.RAIDERS, 0, 0);
        assert ok(c, 0, CivicRegistry.RAIDERS, 0, 0) : "a first pick is free";
        c.set(0, CivicRegistry.RAIDERS, 0);
        c.set(1, CivicRegistry.OATHKEEPERS, 0);
        c.set(2, CivicRegistry.WARRIOR_CODE, 0);
        assert c.lastSwap == 0 : "filling is not swapping";
        assert !ok(c, 3, CivicRegistry.EXPANSIONISTS, 1e9f, 0) : "only the earned slots";
        assert !ok(c, 0, "nomod:gone", 1e9f, 0) : "unknown civic";
    }

    private static void noCivicTwiceAndExclusionsBothWays() {
        FactionCivics c = new FactionCivics();
        c.set(0, CivicRegistry.ISOLATIONISTS, 0);
        assert c.conflict(1, CivicRegistry.ISOLATIONISTS) != null : "twice";
        assert c.conflict(1, CivicRegistry.DIPLOMATIC_CORPS) != null : "isolationists names diplomatic corps";
        assert c.conflict(0, CivicRegistry.DIPLOMATIC_CORPS) == null : "replacing the excluder itself is fine";

        FactionCivics d = new FactionCivics();
        d.set(0, CivicRegistry.DIPLOMATIC_CORPS, 0);
        assert d.conflict(1, CivicRegistry.ISOLATIONISTS) != null : "the side that names nothing is still excluded";
    }

    private static void aSwapCostsAndWaits() {
        FactionCivics c = new FactionCivics();
        c.set(0, CivicRegistry.RAIDERS, 0);
        long now = 100 * DAY;
        assert !ok(c, 0, CivicRegistry.HOLY_LEADER, COST - 1, now) : "cannot afford";
        assert ok(c, 0, CivicRegistry.HOLY_LEADER, COST, now) : "first swap has no cooldown";
        c.set(0, CivicRegistry.HOLY_LEADER, now);
        assert c.lastSwap == now;
        assert !ok(c, 0, CivicRegistry.RAIDERS, 1e9f, now + 6 * DAY) : "cooldown";
        assert ok(c, 0, CivicRegistry.RAIDERS, 1e9f, now + 7 * DAY) : "cooldown over";
    }

    private static void survivesATag() {
        FactionCivics c = new FactionCivics();
        c.set(0, CivicRegistry.RAIDERS, 0);
        c.set(1, "nomod:gone", 0);
        c.slotsEarned = 4;
        c.lastSwap = 12345;
        FactionCivics back = FactionCivics.fromTag(c.toTag());
        assert back.civics.equals(c.civics) && back.slotsEarned == 4 && back.lastSwap == 12345 : back.civics;

        FactionCivics empty = FactionCivics.fromTag(new FactionCivics().toTag());
        assert empty.civics.isEmpty() && empty.slotsEarned == FactionCivics.STARTING_SLOTS;
    }
}
