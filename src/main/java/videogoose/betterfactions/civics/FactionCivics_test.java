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
    private static final int RUSH = 3000;

    private FactionCivics_test() {
    }

    public static void main(String[] args) {
        slotsFillInOrder();
        noCivicTwiceAndExclusionsBothWays();
        holyLeaderNeedsReligious();
        oneDoctrineInItsOwnSlot();
        aSwapCostsAndWaits();
        theCooldownCanBeBoughtOff();
        survivesATag();
        modifiersListEverythingHeld();
        System.out.println("FactionCivics_test: OK");
    }

    private static boolean ok(FactionCivics c, int slot, String id, float points, long now) {
        return c.problem(slot, id, points, now, COST, 0, 7 * DAY) == null;
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

    private static void holyLeaderNeedsReligious() {
        FactionCivics c = new FactionCivics();
        assert c.conflict(0, CivicRegistry.HOLY_LEADER) != null : "not without Religious";
        c.set(0, CivicRegistry.RELIGIOUS, 0);
        assert c.conflict(1, CivicRegistry.HOLY_LEADER) == null : "Religious held in another slot";
        assert c.conflict(0, CivicRegistry.HOLY_LEADER) != null : "not by replacing Religious itself";
        assert c.conflict(1, CivicRegistry.INDUSTRIALIST) != null : "Religious excludes Industrialist";
        c.set(1, CivicRegistry.HOLY_LEADER, 0);
        assert c.conflict(0, CivicRegistry.RAIDERS) != null : "Religious cannot be swapped out under Holy Leader";
        assert c.conflict(1, CivicRegistry.RAIDERS) == null : "Holy Leader itself can go";
    }

    private static void oneDoctrineInItsOwnSlot() {
        int D = FactionCivics.DOCTRINE;
        FactionCivics c = new FactionCivics();
        assert c.conflict(0, CivicRegistry.ELITE_CREWS) != null : "a doctrine is not a civic";
        assert c.conflict(D, CivicRegistry.RAIDERS) != null : "a civic is not a doctrine";
        assert ok(c, D, CivicRegistry.ELITE_CREWS, 0, 0) : "the first doctrine is free, civics empty or not";
        c.set(D, CivicRegistry.ELITE_CREWS, 0);
        assert CivicRegistry.ELITE_CREWS.equals(c.doctrine) && c.civics.isEmpty() && c.lastSwap == 0;
        assert c.conflict(D, CivicRegistry.ELITE_CREWS) != null : "already held";

        long now = 100 * DAY;
        assert !ok(c, D, CivicRegistry.HORDE_TACTICS, COST - 1, now) : "replacing it is a paid swap";
        c.set(D, CivicRegistry.HORDE_TACTICS, now);
        assert CivicRegistry.HORDE_TACTICS.equals(c.doctrine) && c.lastSwap == now : "one at a time, and it starts the cooldown";
        assert c.cost(0, now + DAY, COST, RUSH, 7 * DAY) == 0 : "filling a civic slot stays free mid cooldown";

        FactionCivics back = FactionCivics.fromTag(c.toTag());
        assert CivicRegistry.HORDE_TACTICS.equals(back.doctrine);
        assert FactionCivics.fromTag(new FactionCivics().toTag()).doctrine == null;
    }

    private static void aSwapCostsAndWaits() {
        FactionCivics c = new FactionCivics();
        c.set(0, CivicRegistry.RAIDERS, 0);
        long now = 100 * DAY;
        assert !ok(c, 0, CivicRegistry.EXPANSIONISTS, COST - 1, now) : "cannot afford";
        assert ok(c, 0, CivicRegistry.EXPANSIONISTS, COST, now) : "first swap has no cooldown";
        c.set(0, CivicRegistry.EXPANSIONISTS, now);
        assert c.lastSwap == now;
        assert !ok(c, 0, CivicRegistry.RAIDERS, 1e9f, now + 6 * DAY) : "cooldown";
        assert ok(c, 0, CivicRegistry.RAIDERS, 1e9f, now + 7 * DAY) : "cooldown over";
    }

    private static void theCooldownCanBeBoughtOff() {
        FactionCivics c = new FactionCivics();
        c.set(0, CivicRegistry.RAIDERS, 0);
        long now = 100 * DAY;
        c.set(0, CivicRegistry.EXPANSIONISTS, now);
        long soon = now + DAY;
        assert c.cost(0, soon, COST, RUSH, 7 * DAY) == COST + RUSH;
        assert c.cost(0, now + 7 * DAY, COST, RUSH, 7 * DAY) == COST : "no rush once the cooldown is over";
        assert c.cost(1, soon, COST, RUSH, 7 * DAY) == 0 : "filling is free even mid cooldown";
        assert c.problem(0, CivicRegistry.RAIDERS, COST + RUSH - 1, soon, COST, RUSH, 7 * DAY) != null : "cannot afford the rush";
        assert c.problem(0, CivicRegistry.RAIDERS, COST + RUSH, soon, COST, RUSH, 7 * DAY) == null : "rushed";
        assert c.problem(0, CivicRegistry.RAIDERS, 1e9f, soon, COST, 0, 7 * DAY) != null : "rush off";
    }

    private static void modifiersListEverythingHeld() {
        FactionCivics c = new FactionCivics();
        assert c.modifiers(null).isEmpty();
        c.set(0, CivicRegistry.RAIDERS, 0);
        c.set(FactionCivics.DOCTRINE, CivicRegistry.ELITE_CREWS, 0);
        c.institutions.add(CivicRegistry.SENATE);
        String m = c.modifiers("Ore");
        assert m.startsWith("Raiders: ") && m.contains("\n\nElite Crews: ") && m.contains("\n\nSenate: ") : m;
        assert !m.contains("Ore") : "no resource shown without a civic that picks one";
        c.set(1, CivicRegistry.RESOURCE_SPECIALIST, (short) 77, 0);
        assert c.modifiers("Ore").contains("Resource: Ore.") : c.modifiers("Ore");
    }

    private static void survivesATag() {
        FactionCivics c = new FactionCivics();
        c.set(0, CivicRegistry.RAIDERS, 0);
        c.set(1, "nomod:gone", (short) 99, 0);
        assert c.resource == 0 : "only a civic that chooses a resource keeps one";
        c.set(2, CivicRegistry.RESOURCE_SPECIALIST, (short) 77, 0);
        c.slotsEarned = 4;
        c.lastSwap = 12345;
        FactionCivics back = FactionCivics.fromTag(c.toTag());
        assert back.civics.equals(c.civics) && back.slotsEarned == 4 && back.lastSwap == 12345 : back.civics;
        assert back.resource == 77;

        FactionCivics empty = FactionCivics.fromTag(new FactionCivics().toTag());
        assert empty.civics.isEmpty() && empty.slotsEarned == FactionCivics.STARTING_SLOTS;
    }
}
