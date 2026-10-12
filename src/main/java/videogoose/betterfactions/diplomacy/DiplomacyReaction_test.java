package videogoose.betterfactions.diplomacy;

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;

/**
 * How often an NPC reaction fires across turns. Run with -ea.
 */
public final class DiplomacyReaction_test {

	public static void main(String[] args) {
		Int2BooleanOpenHashMap fired = new Int2BooleanOpenHashMap();
		int[] sent = {0};

		for (int turn = 0; turn < 10; turn++) {
			DiplomacyEntity.step(fired, 3, true, () -> {
				sent[0]++;
				return true;
			});
		}
		assert sent[0] == 1 : "a condition that holds for ten turns sends one mail, not " + sent[0];

		DiplomacyEntity.step(fired, 3, false, () -> true);
		DiplomacyEntity.step(fired, 3, true, () -> {
			sent[0]++;
			return true;
		});
		assert sent[0] == 2 : "once it lapses and comes true again, it fires again";

		int[] tries = {0};
		for (int turn = 0; turn < 3; turn++) {
			DiplomacyEntity.step(fired, 4, true, () -> {
				tries[0]++;
				return false;
			});
		}
		assert tries[0] == 3 && !fired.containsKey(4) : "one that fails to fire is tried again each turn";

		System.out.println("DiplomacyReaction_test: OK");
	}
}
