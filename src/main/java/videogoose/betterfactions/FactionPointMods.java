package videogoose.betterfactions;

import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What the mod added to or took from each faction's points on the last income turn, line by line, so the
 * engine's points screen and {@code FactionStrength} can show and count it. Gains are positive, costs
 * negative. Set by the server on each turn and broadcast whole after it.
 * Saved with the world so they hold across a restart until the next turn replaces them.
 */
public final class FactionPointMods {

	public enum Line {FLEET, INSTITUTIONS, SPICE_DEALER, STATIONS, TRIBUTE, DIVIDENDS}

	private final Map<Integer, float[]> byFaction = new ConcurrentHashMap<>();

	public void set(int factionId, Line line, float v) {
		byFaction.computeIfAbsent(factionId, k -> new float[Line.values().length])[line.ordinal()] = v;
	}

	public void add(int factionId, Line line, float v) {
		float[] a = byFaction.computeIfAbsent(factionId, k -> new float[Line.values().length]);
		a[line.ordinal()] += v;
	}

	public float get(int factionId, Line line) {
		float[] a = byFaction.get(factionId);
		return a == null ? 0 : a[line.ordinal()];
	}

	/** Every line summed: what the mod moved the faction's points by last turn. */
	public float net(int factionId) {
		float[] a = byFaction.get(factionId);
		float n = 0;
		if (a != null) {
			for (float v : a) {
				n += v;
			}
		}
		return n;
	}

	/** Zeroes one line for every faction, for a line that is added to through the turn. */
	public void clear(Line line) {
		for (float[] a : byFaction.values()) {
			a[line.ordinal()] = 0;
		}
	}

	public void remove(int factionId) {
		byFaction.remove(factionId);
	}

	public Tag toTag() {
		Tag[] t = new Tag[byFaction.size() + 1];
		int i = 0;
		for (Map.Entry<Integer, float[]> e : byFaction.entrySet()) {
			Tag[] row = new Tag[e.getValue().length + 2];
			row[0] = new Tag(Tag.Type.INT, null, e.getKey());
			for (int j = 0; j < e.getValue().length; j++) {
				row[j + 1] = new Tag(Tag.Type.FLOAT, null, e.getValue()[j]);
			}
			row[row.length - 1] = FinishTag.INST;
			t[i++] = new Tag(Tag.Type.STRUCT, null, row);
		}
		t[i] = FinishTag.INST;
		return new Tag(Tag.Type.STRUCT, null, t);
	}

	public void fromTag(Tag tag) {
		byFaction.clear();
		for (Tag e : tag.getStruct()) {
			if (e.getType() != Tag.Type.STRUCT) {
				continue;
			}
			Tag[] row = e.getStruct();
			float[] a = new float[Line.values().length];
			for (int j = 0; j < a.length && j + 1 < row.length && row[j + 1].getType() == Tag.Type.FLOAT; j++) {
				a[j] = row[j + 1].getFloat();
			}
			byFaction.put(row[0].getInt(), a);
		}
	}
}
