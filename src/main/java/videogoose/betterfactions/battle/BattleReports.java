package videogoose.betterfactions.battle;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
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
	 * One root ship or station in the fight, as last seen: the full report's rows.
	 * @param type {@code EntityType.dbTypeId}
	 * @param hp share of its structure left, 0 to 1
	 */
	public record Unit(String name, int faction, byte type, float mass, float hp, double rating, boolean lost) {
	}

	/**
	 * @param winner the one side left holding the field, or {@link Integer#MIN_VALUE} for none
	 * @param favoured with no winner, the side that lost clearly less and came off better, or
	 *                 {@link Integer#MIN_VALUE}; see {@code BattleTracker.favoured}
	 * @param name     what the battle is called; see {@link BattleNames}
	 */
	public record Report(long time, Vector3i pos, String verdict, String body, long lengthMs, boolean soft, int winner, int favoured,
			List<Line> sides, List<Unit> units, String name) {

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

		public boolean involves(int faction) {
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

	/**
	 * Server: reports named {@code base}, numbered or not; what {@link BattleNames#numbered} counts.
	 * ponytail: only the {@link #KEPT} newest are counted, so a name worn out long ago starts over.
	 */
	synchronized int countNamed(String base) {
		int n = 0;
		for (Report r : all) {
			n += BattleNames.base(r.name).equals(base) ? 1 : 0;
		}
		return n;
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

	/** How long a battle's result keeps moving a faction's power; it fades linearly to nothing over this. */
	static final long MOMENTUM_MS = 24 * 60 * 60 * 1000L;
	/** Power gained per fresh outright win (lost per loss); half for coming off better or worse. */
	static final double MOMENTUM_PER_WIN = 0.05;
	/** Momentum never moves power by more than this share either way. */
	static final double MOMENTUM_CAP = 0.25;

	/**
	 * Server: every faction with recent results, to its momentum: a share of its power to add (or take
	 * away), from the battles of the last {@link #MOMENTUM_MS}. Winning is holding the field, or failing
	 * that coming off clearly better; every other side lost it. Inconclusive battles count for nobody.
	 */
	public synchronized Int2DoubleOpenHashMap momentum(long now) {
		Int2DoubleOpenHashMap out = new Int2DoubleOpenHashMap();
		for (Report r : all) {
			long age = now - r.time;
			if (age >= MOMENTUM_MS) {
				break; //newest first
			}
			int victor = victor(r);
			if (victor == Integer.MIN_VALUE || age < 0) {
				continue;
			}
			double swing = swing(r, age);
			for (Line l : r.sides) {
				out.put(l.faction, out.get(l.faction) + (l.faction == victor ? swing : -swing));
			}
		}
		for (Int2DoubleMap.Entry e : out.int2DoubleEntrySet()) {
			e.setValue(Math.max(-MOMENTUM_CAP, Math.min(MOMENTUM_CAP, e.getDoubleValue())));
		}
		return out;
	}

	/**
	 * Server: 0 to 1, how badly {@code faction}'s recent battles against {@code against} have gone - the
	 * same swings as {@link #momentum}, counting only the battles both were sides in and only the ones
	 * either of them won, over the same cap. Wins back against them make up for losses.
	 */
	public synchronized double weariness(int faction, int against, long now) {
		double net = 0;
		for (Report r : all) {
			long age = now - r.time;
			if (age >= MOMENTUM_MS) {
				break;
			}
			int victor = victor(r);
			if (age < 0 || (victor != faction && victor != against) || !r.involves(faction) || !r.involves(against)) {
				continue;
			}
			net += victor == faction ? swing(r, age) : -swing(r, age);
		}
		return Math.max(0, Math.min(1, -net / MOMENTUM_CAP));
	}

	/**
	 * A war's balance from {@code faction}'s side, 0 to 1: the mean of two shares over the battles against
	 * {@code enemy} since {@code since} - the battles won (weighted like {@link #swing} without the fade)
	 * and the ships destroyed. A share with nothing in it sits out; 0.5 when both are empty.
	 */
	public static double warBalance(List<Report> reports, int faction, int enemy, long since) {
		double ours = 0, theirs = 0;
		int killed = 0, lost = 0;
		for (Report r : reports) {
			if (r.time < since || !r.involves(faction) || !r.involves(enemy)) {
				continue;
			}
			int victor = victor(r);
			double w = r.winner != Integer.MIN_VALUE ? 1 : 0.5;
			ours += victor == faction ? w : 0;
			theirs += victor == enemy ? w : 0;
			killed += r.lostBy(enemy);
			lost += r.lostBy(faction);
		}
		boolean battles = ours + theirs > 0, ships = killed + lost > 0;
		double b = battles ? ours / (ours + theirs) : 0, s = ships ? (double) killed / (killed + lost) : 0;
		return battles && ships ? (b + s) / 2 : battles ? b : ships ? s : 0.5;
	}

	/** Server: {@link #warBalance} over every kept report. */
	public synchronized double warBalance(int faction, int enemy, long since) {
		return warBalance(new ArrayList<>(all), faction, enemy, since);
	}

	/** The side that held the field, else the side that came off better, else {@link Integer#MIN_VALUE}. */
	private static int victor(Report r) {
		return r.winner != Integer.MIN_VALUE ? r.winner : r.favoured;
	}

	/** What a report with a victor is worth to it, {@code age} after it was fought. */
	private static double swing(Report r, long age) {
		return MOMENTUM_PER_WIN * (r.winner != Integer.MIN_VALUE ? 1 : 0.5) * (1 - (double) age / MOMENTUM_MS);
	}

	private static final Object2LongOpenHashMap<String> answeredAt = new Object2LongOpenHashMap<>();

	/**
	 * Server: at most one answer a second per {@code player}; a request is a byte and an answer is a
	 * hundred reports. Other requests pass a prefixed name to be limited on their own.
	 */
	public static synchronized boolean mayAnswer(String player, long now) {
		if (now - answeredAt.getLong(player) < 1000) {
			return false;
		}
		answeredAt.put(player, now);
		return true;
	}

	/** Server: the answer to a client's request. Units are left out: a hundred reports of them is too much to send unasked. */
	public Tag tagFor(int faction) {
		return toTag(of(faction, SENT), false);
	}

	/** Server: one report's units, if {@code faction} was a side in the report fought at {@code pos} at {@code time}; else null. */
	public synchronized Tag detailFor(int faction, Tag request) {
		Tag[] q = request.getStruct();
		long time = (Long) q[0].getValue();
		for (Report r : all) {
			if (r.time == time && r.pos.equals(q[1].getValue()) && r.involves(faction)) {
				return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.LONG, null, time), unitsTag(r.units), FinishTag.INST});
			}
		}
		return null;
	}

	synchronized Tag toTag() {
		return toTag(new ArrayList<>(all), true);
	}

	/** Loads what {@link #toTag()} saved, newest first as it was. */
	synchronized void fromTag(Tag tag) {
		all.clear();
		all.addAll(listFrom(tag));
	}

	static Tag toTag(List<Report> reports, boolean units) {
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
					new Tag(Tag.Type.INT, null, r.favoured), unitsTag(units ? r.units : List.of()),
					new Tag(Tag.Type.STRING, null, r.name), FinishTag.INST});
		}
		t[i] = FinishTag.INST;
		return new Tag(Tag.Type.STRUCT, null, t);
	}

	static Tag unitsTag(List<Unit> units) {
		Tag[] t = new Tag[units.size() + 1];
		int i = 0;
		for (Unit u : units) {
			t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
					new Tag(Tag.Type.STRING, null, u.name), new Tag(Tag.Type.INT, null, u.faction), new Tag(Tag.Type.BYTE, null, u.type),
					new Tag(Tag.Type.FLOAT, null, u.mass), new Tag(Tag.Type.FLOAT, null, u.hp), new Tag(Tag.Type.DOUBLE, null, u.rating),
					new Tag(Tag.Type.BYTE, null, (byte) (u.lost ? 1 : 0)), FinishTag.INST});
		}
		t[i] = FinishTag.INST;
		return new Tag(Tag.Type.STRUCT, null, t);
	}

	static List<Unit> unitsFrom(Tag tag) {
		List<Unit> out = new ArrayList<>();
		Tag[] t = tag.getStruct();
		for (int i = 0; i < t.length - 1; i++) {
			Tag[] u = t[i].getStruct();
			out.add(new Unit(u[0].getString(), u[1].getInt(), u[2].getByte(), u[3].getFloat(), u[4].getFloat(),
					(Double) u[5].getValue(), u[6].getByte() != 0));
		}
		return out;
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
				if (l[0].getInt() == 0) {
					continue; //saved before unaffiliated ships stopped being a side
				}
				sides.add(new Line(l[0].getInt(), l[1].getString(), l[2].getInt(), l[3].getByte() != 0));
			}
			out.add(new Report((Long) e[0].getValue(), (Vector3i) e[1].getValue(), e[2].getString(), e[3].getString(),
					(Long) e[4].getValue(), e[5].getByte() != 0, e[7].getInt(),
					e.length > 9 ? e[8].getInt() : Integer.MIN_VALUE, sides, e.length > 10 ? unitsFrom(e[9]) : List.of(),
					e.length > 11 ? e[10].getString() : "Battle at " + e[1].getValue()));
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

	/** Client: the units of the report last asked for in full, and which report that is (its time). */
	private static volatile List<Unit> detail;
	private static volatile long detailOf = Long.MIN_VALUE;

	public static void detailReceived(Tag tag) {
		Tag[] t = tag.getStruct();
		detail = unitsFrom(t[1]);
		detailOf = (Long) t[0].getValue();
	}

	/** Client: the units of {@code r}, or null until the server answers. */
	public static List<Unit> detail(Report r) {
		return detailOf == r.time ? detail : null;
	}

	/** Client: asks for one report's units. */
	public static void requestDetail(Report r) {
		BFPacket.toServer(BFPacket.Kind.BATTLE_DETAIL_REQUEST, new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.LONG, null, r.time), new Tag(Tag.Type.VECTOR3i, null, new Vector3i(r.pos)), FinishTag.INST}));
	}
}
