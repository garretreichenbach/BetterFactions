package videogoose.betterfactions.fleet;

/**
 * Pins which deliveries an escort covers and a raid may hit.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class TradeFleetOrders_test {

    private TradeFleetOrders_test() {
    }

    public static void main(String[] args) {
        assert TradeFleetOrders.escortable(5, 5, 9) && TradeFleetOrders.escortable(5, 9, 5) : "ours buying or selling";
        assert !TradeFleetOrders.escortable(5, 8, 9) && !TradeFleetOrders.escortable(0, 0, 9) : "not others', and not for no faction";

        java.util.function.BiPredicate<Integer, Integer> friends = (a, b) -> a == 5 && b == 6;
        assert TradeFleetOrders.raidable(5, 8, 9, -2, friends, java.util.Set.of()) : "strangers carried by the Trading Guild";
        assert !TradeFleetOrders.raidable(5, 5, 9, -2, friends, java.util.Set.of()) : "not our own trade";
        assert !TradeFleetOrders.raidable(5, -2, 9, -2, friends, java.util.Set.of()) : "not the Guild's own trade";
        assert !TradeFleetOrders.raidable(5, 6, 9, 9, friends, java.util.Set.of()) : "not a friend's";
        assert !TradeFleetOrders.raidable(5, 8, 9, 6, friends, java.util.Set.of()) : "not a convoy a friend flies";
        assert TradeFleetOrders.raidable(5, 0, 9, 9, friends, java.util.Set.of()) : "no faction on one end is no friend";
        //raid routes: sectors as typed, and which systems they cover
        java.util.List<org.schema.common.util.linAlg.Vector3i> route = TradeFleetOrders.parseSectors("10, 20, 111; 30 4 -5;7,8");
        assert route.size() == 2 && route.get(0).equals(new org.schema.common.util.linAlg.Vector3i(10, 20, 111))
                && route.get(1).equals(new org.schema.common.util.linAlg.Vector3i(30, 4, -5)) : "groups of three; a short tail is ignored";
        assert TradeFleetOrders.parseSectors("").isEmpty() && TradeFleetOrders.parseSectors(null).isEmpty();
        assert TradeFleetOrders.inRoute(route, new org.schema.common.util.linAlg.Vector3i(0, 16, 100)) : "same system as 10, 20, 111";
        assert TradeFleetOrders.inRoute(route, new org.schema.common.util.linAlg.Vector3i(16, 0, -16)) : "same system as 30, 4, -5";
        assert !TradeFleetOrders.inRoute(route, new org.schema.common.util.linAlg.Vector3i(10, 20, 128)) : "the next system over is not";
        //named targets: only theirs, friends or not, no war needed; never our own
        java.util.Set<Integer> named = java.util.Set.of(6, 8);
        assert TradeFleetOrders.raidable(5, 8, 9, -2, friends, named) : "a target's purchase";
        assert TradeFleetOrders.raidable(5, 6, 9, 9, friends, named) : "a friend, named, is fair game";
        assert !TradeFleetOrders.raidable(5, 7, 9, -2, friends, named) : "nobody named on it";
        assert !TradeFleetOrders.raidable(5, 8, 5, -2, friends, named) : "never our own";
        assert TradeFleetOrders.ints(TradeFleetOrders.ints(named)).equals(named) : "targets survive a save";
        System.out.println("TradeFleetOrders_test: OK");
    }
}
