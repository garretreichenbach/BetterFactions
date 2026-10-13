package videogoose.betterfactions.repair;

import org.schema.game.common.data.player.catalog.CatalogPermission;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Which catalog blueprint each of a faction's ships is built to, so a shipyard knows what to repair it back
 * into. Keyed on the ship's UID; a ship nobody assigned falls back to {@link #guess}, from its name.
 */
public final class FactionDesigns {

	private static final FactionDesigns SERVER = new FactionDesigns();
	/** Client: the own faction's assignments, as last sent. */
	private static volatile Map<String, String> client = Map.of();
	private static volatile int clientVersion;

	/** faction id -> ship UID -> blueprint name */
	private final Map<Integer, Map<String, String>> byFaction = new HashMap<>();

	public static FactionDesigns server() {
		return SERVER;
	}

	public synchronized String assigned(int faction, String shipUid) {
		Map<String, String> m = byFaction.get(faction);
		return m == null ? null : m.get(shipUid);
	}

	/** @param blueprint null or empty clears it back to the guess */
	public synchronized void assign(int faction, String shipUid, String blueprint) {
		Map<String, String> m = byFaction.computeIfAbsent(faction, k -> new HashMap<>());
		if (blueprint == null || blueprint.isEmpty()) {
			m.remove(shipUid);
		} else {
			m.put(shipUid, blueprint);
		}
	}

	/** A repair rebuilds the ship as a new entity: its assignment moves with it. */
	public synchronized void moved(int faction, String oldUid, String newUid) {
		Map<String, String> m = byFaction.get(faction);
		if (m != null && m.containsKey(oldUid)) {
			m.put(newUid, m.remove(oldUid));
		}
	}

	public synchronized void clear() {
		byFaction.clear();
	}

	public synchronized void onFactionDeleted(int faction) {
		byFaction.remove(faction);
	}

	/**
	 * The blueprint this ship repairs to: assigned, else guessed - either way only from the blueprints the ordering
	 * player's catalog permissions give them (own, public, faction-shared), the same list the client picks from.
	 * An assignment they can't see (another member's private one) falls back to the guess. Null if none.
	 */
	public String blueprintFor(Faction f, String shipUid, String shipName, Collection<CatalogPermission> available) {
		java.util.List<String> names = available.stream().map(CatalogPermission::getUid).toList();
		String a = assigned(f.getIdFaction(), shipUid);
		return a != null && names.contains(a) ? a : guess(shipName, names);
	}

	/**
	 * The blueprint a ship was most likely bought as. Catalog spawns are named after the blueprint, often with a
	 * suffix ("Falcon_17", "Falcon Mk2 3"), so the longest blueprint the name starts with wins; failing that, the
	 * one sharing the longest start of at least four characters.
	 *
	 * @return null when nothing is close
	 */
	public static String guess(String shipName, Collection<String> blueprints) {
		String ship = norm(shipName);
		String best = null;
		int bestLen = 0;
		boolean bestIsPrefix = false;
		for (String bp : blueprints) {
			String b = norm(bp);
			if (b.isEmpty()) {
				continue;
			}
			boolean prefix = ship.startsWith(b);
			int len = prefix ? b.length() : common(ship, b);
			if (!prefix && len < 4) {
				continue;
			}
			if (prefix && !bestIsPrefix || prefix == bestIsPrefix && len > bestLen) {
				best = bp;
				bestLen = len;
				bestIsPrefix = prefix;
			}
		}
		return best;
	}

	private static String norm(String s) {
		return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private static int common(String a, String b) {
		int n = Math.min(a.length(), b.length());
		int i = 0;
		while (i < n && a.charAt(i) == b.charAt(i)) {
			i++;
		}
		return i;
	}

	// ---- client

	public static Map<String, String> client() {
		return client;
	}

	public static int clientVersion() {
		return clientVersion;
	}

	public static void received(Tag tag) {
		client = fromPairs(tag);
		clientVersion++;
	}

	// ---- tags

	/** Server: one faction's assignments, for its members. */
	public synchronized Tag tagFor(int faction) {
		return pairs(byFaction.getOrDefault(faction, Map.of()));
	}

	public synchronized Tag toTag() {
		Tag[] t = new Tag[byFaction.size() + 1];
		int i = 0;
		for (Map.Entry<Integer, Map<String, String>> e : byFaction.entrySet()) {
			t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.INT, null, e.getKey()), pairs(e.getValue()), FinishTag.INST});
		}
		t[i] = FinishTag.INST;
		return new Tag(Tag.Type.STRUCT, null, t);
	}

	public synchronized void fromTag(Tag tag) {
		byFaction.clear();
		Tag[] t = tag.getStruct();
		for (int i = 0; i < t.length && t[i].getType() == Tag.Type.STRUCT; i++) {
			Tag[] e = t[i].getStruct();
			byFaction.put(e[0].getInt(), new HashMap<>(fromPairs(e[1])));
		}
	}

	private static Tag pairs(Map<String, String> m) {
		Tag[] t = new Tag[m.size() * 2 + 1];
		int i = 0;
		for (Map.Entry<String, String> e : m.entrySet()) {
			t[i++] = new Tag(Tag.Type.STRING, null, e.getKey());
			t[i++] = new Tag(Tag.Type.STRING, null, e.getValue());
		}
		t[i] = FinishTag.INST;
		return new Tag(Tag.Type.STRUCT, null, t);
	}

	private static Map<String, String> fromPairs(Tag tag) {
		Map<String, String> m = new HashMap<>();
		Tag[] t = tag.getStruct();
		for (int i = 0; i + 1 < t.length && t[i].getType() == Tag.Type.STRING; i += 2) {
			m.put(t[i].getString(), t[i + 1].getString());
		}
		return m;
	}
}
