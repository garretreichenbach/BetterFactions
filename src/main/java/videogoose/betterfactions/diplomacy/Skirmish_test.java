package videogoose.betterfactions.diplomacy;

/** Skirmishes: last while hits keep coming, end on quiet, and returning fire is not a fresh attack. Run with -ea. */
public final class Skirmish_test {

	public static void main(String[] args) {
		Skirmish s = new Skirmish(7, 3, 0);
		assert s.active(Skirmish.QUIET_MS - 1) : "still fighting";
		assert !s.active(Skirmish.QUIET_MS) : "quiet long enough: over";
		assert !s.hit(3, 60_000) : "the victim returning fire";
		assert s.active(60_000 + Skirmish.QUIET_MS - 1) : "a hit keeps it going";
		assert s.hit(7, 90_000) : "the aggressor pressing on";
		System.out.println("Skirmish_test passed");
	}
}
