package videogoose.betterfactions.battle;

/** Battle names: templates that need an attacker wait for one, small fights are skirmishes, the seed is stable. Run with -ea. */
public final class BattleNames_test {

	public static void main(String[] args) {
		for (long seed = 0; seed < 20; seed++) {
			String plain = BattleNames.name("Kepler", null, null, 40, seed);
			assert plain.contains("Kepler") && !plain.contains("'s") && !plain.startsWith("Defence") && !plain.contains("  ")
					: "no attacker known: no template naming one: " + plain;
			String small = BattleNames.name("Kepler", "Dovan", "Pirates", 3, seed);
			assert !small.startsWith("Battle") && !small.contains("Engagement") && !small.contains("Offensive") : "a skirmish: " + small;
		}
		assert BattleNames.name("Kepler", "Dovan", "Pirates", 40, 7).equals(BattleNames.name("Kepler", "Dovan", "Pirates", 40, 7)) : "stable";
		boolean named = false;
		for (long seed = 0; seed < 20; seed++) {
			named |= BattleNames.name("Kepler", "Dovan", "Pirates", 40, seed).contains("Dovan");
		}
		assert named : "an attacker gets its name on some battles";
		assert BattleNames.numbered("Battle of Kepler", 0).equals("Battle of Kepler");
		assert BattleNames.numbered("Battle of Kepler", 1).equals("Second Battle of Kepler");
		assert BattleNames.numbered("The Kepler Engagement", 2).equals("The Third Kepler Engagement");
		assert BattleNames.numbered("Battle of Kepler", 10).equals("11th Battle of Kepler");
		assert BattleNames.numbered("Battle of Kepler", 20).equals("21st Battle of Kepler");
		assert BattleNames.numbered("Battle of Kepler", 11).equals("12th Battle of Kepler");
		for (int i = 0; i < 30; i++) {
			assert BattleNames.base(BattleNames.numbered("The Kepler Engagement", i)).equals("The Kepler Engagement") : i;
			assert BattleNames.base(BattleNames.numbered("Battle of Kepler", i)).equals("Battle of Kepler") : i;
		}
		assert BattleNames.base("Battle of Kepler").equals("Battle of Kepler") && BattleNames.base("The Kepler Skirmish").equals("The Kepler Skirmish");

		BattleReports a = new BattleReports();
		for (String n : new String[]{"Battle of Kepler", "Second Battle of Kepler", "Clash at Kepler", "Battle of Vega"}) {
			a.add(new BattleReports.Report(0, new org.schema.common.util.linAlg.Vector3i(), "v", "v\nb", 1, false, 1, Integer.MIN_VALUE,
					java.util.List.of(), java.util.List.of(), n));
		}
		assert BattleNames.numbered("Battle of Kepler", a.countNamed("Battle of Kepler")).equals("Third Battle of Kepler");
		System.out.println("BattleNames_test: OK");
	}
}
