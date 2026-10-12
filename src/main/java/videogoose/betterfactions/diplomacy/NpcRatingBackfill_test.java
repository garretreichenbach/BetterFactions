package videogoose.betterfactions.diplomacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The NPC rating backfill's rule: each recorded entity gets its blueprint's rating, but only if it has
 * none. Walking the engine's NPC systems needs a running server. Run with -ea.
 */
public final class NpcRatingBackfill_test {

	public static void main(String[] args) {
		CombatRatings ratings = new CombatRatings();
		ratings.put(11, -5, 77); //loaded at some point: has its real rating
		ratings.putIfUnrated(12, -5, 40); //spawned after the spawn hook: rated at spawn

		Map<String, List<Long>> spawned = new HashMap<>();
		spawned.put("Pirate Frigate", List.of(10L, 11L, 12L));
		spawned.put("Pirate Station", List.of(20L));
		spawned.put("Deleted Design", List.of(30L));
		spawned.put("Never Spawned", List.of(Long.MIN_VALUE)); //the engine's id before a record spawns

		Map<String, Double> blueprints = Map.of("Pirate Frigate", 50.0, "Pirate Station", 300.0);
		int n = NpcRatingBackfill.backfill(-5, spawned, name -> blueprints.getOrDefault(name, Double.NaN), ratings);

		assert n == 2 : "only the two that had nothing: " + n;
		assert ratings.ratingOf(10) == 50 && ratings.ratingOf(20) == 300 : "never loaded: rated from the blueprint";
		assert ratings.ratingOf(11) == 77 : "a loaded rating is kept";
		assert ratings.ratingOf(12) == 40 : "a spawn-time rating is kept";
		assert !ratings.isRated(30) : "a blueprint that is gone rates nothing, so a later run can still fill it in";
		assert !ratings.isRated(Long.MIN_VALUE) : "a record with no entity is skipped";

		ratings.recomputeTotals();
		assert ratings.totalOf(-5) == 50 + 77 + 40 + 300 : "and they count toward the faction: " + ratings.totalOf(-5);

		assert NpcRatingBackfill.backfill(-5, spawned, name -> blueprints.getOrDefault(name, Double.NaN), ratings) == 0
				: "a second run changes nothing";

		ratings.put(10, -5, 65);
		assert ratings.ratingOf(10) == 65 : "the first real rating still replaces a backfilled one";

		System.out.println("NpcRatingBackfill_test: OK");
	}
}
