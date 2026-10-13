package videogoose.betterfactions.civics;

import org.schema.common.util.linAlg.Vector3i;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Neighbourhood Spice Dealer: a system face-adjacent to a dealer's system costs its owner
 * {@code percent} more upkeep, once per distinct dealer faction it touches.
 */
public final class SpiceDealer {

    private static final int[][] FACES = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private SpiceDealer() {
    }

    /**
     * @param systems  the paying faction's systems
     * @param upkeep   each system's own upkeep, same order
     * @param dealers  system position to the dealer faction owning it
     * @return the extra upkeep owed
     */
    public static float extra(List<Vector3i> systems, float[] upkeep, Map<Vector3i, Integer> dealers, int self, int percent) {
        if (dealers.isEmpty() || percent <= 0) {
            return 0;
        }
        float total = 0;
        Set<Integer> touching = new HashSet<>();
        Vector3i n = new Vector3i();
        for (int i = 0; i < systems.size(); i++) {
            Vector3i s = systems.get(i);
            touching.clear();
            for (int[] f : FACES) {
                n.set(s.x + f[0], s.y + f[1], s.z + f[2]);
                Integer owner = dealers.get(n);
                if (owner != null && owner != self) {
                    touching.add(owner);
                }
            }
            total += upkeep[i] * percent / 100f * touching.size();
        }
        return total;
    }

    /** Builds {@link #extra}'s dealer map from each dealer faction's systems. */
    public static Map<Vector3i, Integer> dealerMap(Map<Integer, List<Vector3i>> systemsByDealer) {
        Map<Vector3i, Integer> m = new HashMap<>();
        systemsByDealer.forEach((faction, systems) -> systems.forEach(s -> m.put(new Vector3i(s), faction)));
        return m;
    }
}
