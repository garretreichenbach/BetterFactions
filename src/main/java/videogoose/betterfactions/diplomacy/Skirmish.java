package videogoose.betterfactions.diplomacy;

/**
 * Shooting between two factions with no declared war: they are enemies while the fighting lasts, so
 * the victim's whole faction returns fire, and neutral again once it goes quiet. Only a declared war
 * makes it last. The victim keeps a casus belli to declare one with.
 */
public final class Skirmish {

	/** Quiet this long and it is over: the same silence that ends a battle (BattleTracker.QUIET_MS). */
	public static final long QUIET_MS = 2 * 60_000;

	/** Who fired first. Fire coming back from the other side is not a new attack. */
	public final int aggressor;
	public final int victim;
	private long last;

	public Skirmish(int aggressor, int victim, long now) {
		this.aggressor = aggressor;
		this.victim = victim;
		this.last = now;
	}

	public boolean active(long now) {
		return now - last < QUIET_MS;
	}

	/**
	 * Another hit between the pair keeps it going.
	 *
	 * @return true if {@code by} fired first, false if this is the victim returning fire
	 */
	public boolean hit(int by, long now) {
		last = now;
		return by == aggressor;
	}
}
