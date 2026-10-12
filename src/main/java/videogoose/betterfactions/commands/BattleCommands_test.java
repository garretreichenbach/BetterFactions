package videogoose.betterfactions.commands;

/** Where test fleet ships are put: a centred cube, no two closer than the spacing. Run with -ea. */
public final class BattleCommands_test {

	public static void main(String[] args) {
		for (int count : new int[]{1, 2, 7, 50}) {
			float spacing = 300;
			float[][] at = new float[count][];
			for (int i = 0; i < count; i++) {
				at[i] = BattleCommands.slot(i, count, spacing);
			}
			for (int i = 0; i < count; i++) {
				for (int j = i + 1; j < count; j++) {
					double d = Math.sqrt(Math.pow(at[i][0] - at[j][0], 2) + Math.pow(at[i][1] - at[j][1], 2) + Math.pow(at[i][2] - at[j][2], 2));
					assert d >= spacing - 0.01 : count + " ships: " + i + " and " + j + " are " + d + " apart";
				}
			}
			float half = (float) Math.ceil(Math.cbrt(count)) * spacing / 2;
			for (float[] p : at) {
				assert Math.abs(p[0]) <= half && Math.abs(p[1]) <= half && Math.abs(p[2]) <= half : "centred on the sector";
			}
		}
		assert BattleCommands.slot(0, 1, 300)[0] == 0 && BattleCommands.slot(0, 1, 300)[2] == 0 : "one ship sits in the middle";
		assert BattleCommands.slot(26, 27, 300)[0] == 300 : "27 ships make a 3x3x3 cube";
		System.out.println("BattleCommands_test: OK");
	}
}
