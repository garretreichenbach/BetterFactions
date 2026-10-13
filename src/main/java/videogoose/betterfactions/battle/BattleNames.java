package videogoose.betterfactions.battle;

import videogoose.betterfactions.diplomacy.TreatyNames;

import java.util.ArrayList;
import java.util.List;

/**
 * Names a battle, the way {@link TreatyNames} names a treaty: a handful of templates, filled in from
 * where it was fought and who started it, one picked by a seed so the name does not change.
 *
 * <p>{@code {place}} is the star system, or the sector when the system has no name. {@code {attacker}}
 * and {@code {defender}} are only there when territory (the system's owner defends) or, failing that, fleet orders said who came to attack whom; a template naming
 * one is skipped without it. Small fights are skirmishes, not battles.</p>
 *
 * <p>Pure, so a self-check covers it without a galaxy.</p>
 */
public final class BattleNames {

	/** At most this many ships committed in all and it is a skirmish. */
	static final int SKIRMISH_SHIPS = 6;

	private static final String[] BATTLE = {
			"Battle of {place}", "The {place} Engagement", "Clash at {place}", "The {place} Incident",
			"{attacker}'s Assault on {place}", "Defence of {place}", "The {attacker} Offensive at {place}"};
	private static final String[] SKIRMISH = {
			"Skirmish at {place}", "The {place} Skirmish", "Clash at {place}",
			"The {attacker} Raid on {place}", "{defender}'s Stand at {place}"};

	private static final String[] ORDINALS = {"Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth"};

	private BattleNames() {
	}

	/**
	 * {@code base} after {@code prior} battles of the same name: "Battle of Kepler", then "Second Battle of
	 * Kepler", "The Second Kepler Engagement", and from the eleventh on "11th Battle of Kepler".
	 */
	public static String numbered(String base, int prior) {
		if (prior <= 0) {
			return base;
		}
		String ord = prior <= ORDINALS.length ? ORDINALS[prior - 1] : ordinal(prior + 1);
		String out = base.startsWith("The ") ? "The " + ord + " " + base.substring(4) : ord + " " + base;
		String s = TreatyNames.sanitize(out);
		return s != null ? s : base;
	}

	/** What {@link #numbered} was given: {@code name} without its ordinal. */
	public static String base(String name) {
		String rest = name.startsWith("The ") ? name.substring(4) : name;
		int space = rest.indexOf(' ');
		if (space > 0 && isOrdinal(rest.substring(0, space))) {
			rest = rest.substring(space + 1);
			return name.startsWith("The ") ? "The " + rest : rest;
		}
		return name;
	}

	private static boolean isOrdinal(String word) {
		for (String o : ORDINALS) {
			if (o.equals(word)) {
				return true;
			}
		}
		return word.matches("\\d+(st|nd|rd|th)");
	}

	static String ordinal(int n) {
		int tens = n % 100;
		String suffix = tens >= 11 && tens <= 13 ? "th" : switch (n % 10) {
			case 1 -> "st";
			case 2 -> "nd";
			case 3 -> "rd";
			default -> "th";
		};
		return n + suffix;
	}

	/**
	 * @param place    the system's name, or the sector; never null
	 * @param attacker the side that came to attack, or null
	 * @param defender a side it attacked, or null
	 * @param ships    ships committed by every side together
	 * @param seed     stable for the battle (when it began)
	 */
	public static String name(String place, String attacker, String defender, int ships, long seed) {
		List<String> usable = new ArrayList<>();
		for (String t : ships <= SKIRMISH_SHIPS ? SKIRMISH : BATTLE) {
			if (!(t.contains("{attacker}") && attacker == null) && !(t.contains("{defender}") && defender == null)
					&& !(t.startsWith("Defence") && attacker == null)) {
				usable.add(t);
			}
		}
		String out = usable.get(Math.floorMod(seed, usable.size()))
				.replace("{place}", place)
				.replace("{attacker}", attacker != null ? attacker : "")
				.replace("{defender}", defender != null ? defender : "");
		String s = TreatyNames.sanitize(out);
		return s != null ? s : "Battle of " + place;
	}
}
