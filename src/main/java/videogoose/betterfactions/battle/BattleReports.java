package videogoose.betterfactions.battle;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import videogoose.betterfactions.net.BFPacket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Every battle report, for the Battle Reports tab under Assets. The faction news only gets the verdict
 * and a pointer here, because a full report does not fit a news row.
 *
 * <p>The server keeps the newest {@link #KEPT} across all factions and saves them with the battles; a
 * client asks for its own faction's and gets the newest {@link #SENT}. Faction names are stored as they
 * were, so a report still reads right after a faction is renamed or disbanded.</p>
 */
public final class BattleReports {

	static final int KEPT = 400;
	static final int SENT = 100;

	/** One side of a report. */
	public record Line(int faction, String name, int lost, boolean withdrew) {
	}

	/**
	 * @param winner the one side left holding the field, or {@link Integer#MIN_VALUE} for none
	 * @param favoured with no winner, the side that lost clearly less and came off better, or
	 *                 {@link Integer#MIN_VALUE}; see {@code BattleTracker.favoured}
	 */
	public record Report(long time, Vector3i pos, String verdict, String body, long lengthMs, boolean soft, int winner, int favoured,
			List<Line> sides) {

		/** Ships {@code faction} lost here; 0 if it was not a side. */
		public int lostBy(int faction) {
			int n = 0;
			for (Line l : sides) {
				n += l.faction == faction ? l.lost : 0;
			}
			return n;
		}

		/** Ships every other side lost. */
		public int lostByOthers(int faction) {
			int n = 0;
			for (Line l : sides) {
				n += l.faction != faction ? l.lost : 0;
			}
			return n;
		}

		/** Everyone but {@code faction}, by name. */
		public String opponents(int faction) {
			StringBuilder out = new StringBuilder();
			for (Line l : sides) {
				if (l.faction != faction) {
					out.append(out.length() > 0 ? ", " : "").append(l.name);
				}
			}
			return out.toString();
		}

		boolean involves(int faction) {
			for (Line l : sides) {
				if (l.faction == faction) {
					return true;
				}
			}
			return false;
		}
	}

	private final ArrayDeque<Report> all = new ArrayDeque<>();

	/** Server: newest first; the oldest past {@link #KEPT} is dropped. */
	synchronized void add(Report r) {
		all.addFirst(r);
		while (all.size() > KEPT) {
			all.removeLast();
		}
	}

	/** Server: the newest {@code max} reports {@code faction} was a side in. */
	synchronized List<Report> of(int faction, int max) {
		List<Report> out = new ArrayList<>();
		for (Report r : all) {
			if (out.size() >= max) {
				break;
			}
			if (r.involves(faction)) {
				out.add(r);
			}
		}
		return out;
	}

	private static final Object2LongOpenHashMap<String> answeredAt = new Object2LongOpenHashMap<>();

	/** Server: at most one answer a second per player; a request is a byte and an answer is a hundred reports. */
	public static synchronized boolean mayAnswer(String player, long now) {
		if (now - answeredAt.getLong(player) < 1000) {
			return false;
		}
		answeredAt.put(player, now);
		return true;
	}

	/** Server: the answer to a client's request. */
	public Tag tagFor(int faction) {
		return toTag(of(faction, SENT));
	}

	synchronized Tag toTag() {
		return toTag(new ArrayList<>(all));
	}

	/** Loads what {@link #toTag()} saved, newest first as it was. */
	synchronized void fromTag(Tag tag) {
		all.clear();
		all.addAll(listFrom(tag));
	}

	static Tag toTag(List<Report> reports) {
		Tag[] t = new Tag[reports.size() + 1];
		int i = 0;
		for (Report r : reports) {
			Tag[] sides = new Tag[r.sides.size() + 1];
			int j = 0;
			for (Line l : r.sides) {
				sides[j++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
						new Tag(Tag.Type.INT, null, l.faction), new Tag(Tag.Type.STRING, null, l.name),
						new Tag(Tag.Type.INT, null, l.lost), new Tag(Tag.Type.BYTE, null, (byte) (l.withdrew ? 1 : 0)), FinishTag.INST});
			}
			sides[j] = FinishTag.INST;
			t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
					new Tag(Tag.Type.LONG, null, r.time), new Tag(Tag.Type.VECTOR3i, null, new Vector3i(r.pos)),
					new Tag(Tag.Type.STRING, null, r.verdict), new Tag(Tag.Type.STRING, null, r.body),
					new Tag(Tag.Type.LONG, null, r.lengthMs), new Tag(Tag.Type.BYTE, null, (byte) (r.soft ? 1 : 0)),
					new Tag(Tag.Type.STRUCT, null, sides), new Tag(Tag.Type.INT, null, r.winner),
					new Tag(Tag.Type.INT, null, r.favoured), FinishTag.INST});
		}
		t[i] = FinishTag.INST;
		return new Tag(Tag.Type.STRUCT, null, t);
	}

	static List<Report> listFrom(Tag tag) {
		List<Report> out = new ArrayList<>();
		Tag[] t = tag.getStruct();
		//reports saved before "came off better" end at the winner: e[8] is their finish tag
		for (int i = 0; i < t.length - 1; i++) {
			Tag[] e = t[i].getStruct();
			Tag[] s = e[6].getStruct();
			List<Line> sides = new ArrayList<>();
			for (int j = 0; j < s.length - 1; j++) {
				Tag[] l = s[j].getStruct();
				sides.add(new Line(l[0].getInt(), l[1].getString(), l[2].getInt(), l[3].getByte() != 0));
			}
			out.add(new Report((Long) e[0].getValue(), (Vector3i) e[1].getValue(), e[2].getString(), e[3].getString(),
					(Long) e[4].getValue(), e[5].getByte() != 0, e[7].getInt(),
					e.length > 9 ? e[8].getInt() : Integer.MIN_VALUE, sides));
		}
		return out;
	}

	/** Client: the last answer, newest first, and a counter that moves when it changes. */
	private static volatile List<Report> received;
	private static volatile int version;
	private static long requestedAt;

	public static void received(Tag tag) {
		received = listFrom(tag);
		version++;
	}

	/** Client: null until the first answer. */
	public static List<Report> received() {
		return received;
	}

	public static int version() {
		return version;
	}

	/**
	 * Client: asks for this faction's reports, at most every 30 seconds unless forced (every 2).
	 * @return whether a request went out
	 */
	public static boolean request(boolean force) {
		long now = System.currentTimeMillis();
		if (now - requestedAt < (force ? 2000 : 30000)) {
			return false;
		}
		requestedAt = now;
		BFPacket.toServer(BFPacket.Kind.BATTLE_REPORTS_REQUEST,
				new Tag(Tag.Type.BYTE, null, (byte) 0));
		return true;
	}
}
