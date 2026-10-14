package videogoose.betterfactions.battle;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetCommandTypes;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionNewsCategory;
import org.schema.game.common.data.player.faction.FactionNewsPost;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SectorInformation;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.common.data.world.Universe;
import org.schema.game.network.objects.remote.FleetCommand;
import org.schema.game.server.controller.SectorSwitch;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.network.NetUtil;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.Wrecks;
import videogoose.betterfactions.diplomacy.CombatRatings;
import videogoose.betterfactions.fleet.TradeFleetOrders;
import videogoose.betterfactions.mixin.TableAccessor;
import videogoose.betterfactions.net.BFPacket;

import com.bulletphysics.dynamics.RigidBody;

import javax.vecmath.Vector3f;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.ToDoubleFunction;

/**
 * Battles between factions, wherever they happen, and the soft-loading that lets fleets fight with no
 * player nearby.
 *
 * <p>A battle is a sector where one faction's entity took a hit from another's. Loaded fights are
 * found by the hit itself ({@code CheckAttackMixin}). Unloaded fleets never shoot, so a scan looks for
 * an unloaded fleet sharing a sector with something hostile and queues that sector; up to
 * {@link BFConfig#maxSoftBattles()} queued sectors are loaded at a time and kept alive by pinging them,
 * which is how a player keeps a sector alive, minus the 26 neighbours a player also loads.</p>
 *
 * <p>A battle sector no player is watching (see {@link #watched}) is <em>soft-loaded</em>: the mixins skip what only a viewer
 * would notice (rail collisions) or what costs both sides equally (AI ticks). Everything is
 * re-evaluated every tick, so a player arriving mid-fight turns it all back on with no handoff.</p>
 *
 * <p>When a battle goes quiet, every faction in it gets a news post with the losses on each side.</p>
 *
 * <p>A queued sector that waits longer than {@link BFConfig#autoResolveSeconds()} is resolved without
 * loading it ({@link #resolve}): each side's stored combat ratings, stations counted half again, a die
 * roll, and the losing fleets withdraw. Nothing is destroyed, so a build that only looks strong on paper
 * gains a retreat, not a kill.</p>
 *
 * <p>Battles over the soft-load cap (a player left one while every slot was taken) and battles alive at
 * a restart are <em>carried</em>: their tallies wait for the sector to fight again, so a report covers
 * the whole fight. A carried battle nothing resumes is reported after {@link #CARRY_MS}.</p>
 *
 * <p>What each battle costs the server is timed by {@code BattleLoadMixin} (entity and sector updates in
 * a battle sector) and shown to admins in a BATTLES tab of the profiler ({@code ProfilerBattlesTabMixin}),
 * which asks for {@link #adminReport} about once a second while it is open.</p>
 */
public final class BattleTracker {

	/**
	 * A battle ends after this long without a hit. Long enough for a faction to call in reinforcements
	 * and have them counted in the same fight and report, rather than starting a second battle (or,
	 * after a break-off, landing in the sector's hit hold and not being counted at all).
	 */
	static final long QUIET_MS = 2 * 60_000;
	/** How long a loaded sector gets to start a battle before it is released. */
	static final long START_MS = 60_000;
	/** A soft-loaded battle is released after this long however it is going. */
	static final long SOFT_MAX_MS = 15 * 60_000;
	static final long SCAN_MS = 5_000;
	/** How long a sector that never fought, or hit a cap, is left alone; see {@link Ending}. */
	static final long COOLDOWN_MS = 5 * 60_000;
	/** How long a sector is left alone after an encounter there broke off, unless a player is there. */
	static final long DISENGAGE_HOLD_MS = 10 * 60_000;
	/** How often a long battle's fleets are looked at to see whether it is an encounter. */
	static final long ENCOUNTER_CHECK_MS = 5_000;

	/** How long a battle nobody has business in is fought before it breaks off; see {@link BFConfig}. */
	static long encounterMs() {
		return BFConfig.encounterTimeoutMinutes() * 60_000L;
	}

	/** An encounter still trading hits this recently is not broken off yet, up to twice {@link #encounterMs}. */
	static final long ENCOUNTER_GRACE_MS = 60_000;

	/** Whether an encounter has run past its timeout, extended while hits are still landing; see {@link #ENCOUNTER_GRACE_MS}. */
	static boolean timedOut(Battle b, long now, long limit) {
		return now - b.start > limit && (now - b.lastHit > ENCOUNTER_GRACE_MS || now - b.start > 2 * limit);
	}

	/** How long a battle has been fought: first hit to last, so the quiet tail and a carried wait do not count. */
	static long fought(Battle b, long now) {
		return (b.lastHit > 0 ? b.lastHit : now) - b.start;
	}

	/** How long a sector an admin ended a battle in is left alone, unless a player is there. */
	static final long ADMIN_COOLDOWN_MS = 30 * 60_000;
	public static final byte CMD_PAUSE = 0, CMD_END = 1, CMD_TELEPORT = 2, CMD_WATCH = 3;
	/** The server's tick budget: it sleeps out whatever of this a tick leaves over. */
	static final long BUDGET_MS = NetUtil.UPDATE_RATE_SERVER;
	/** Soft battles that have not had a tick in this long show as held: on the spectator's banner and to admins. */
	static final long STARVED_MS = 2_000;
	/** A spectator not in the battle sector this long after asking is given up on. */
	static final long WATCH_ARRIVE_MS = 30_000;
	/** A carried battle that has not fought again in this long is reported as it stands. */
	static final long CARRY_MS = 60 * 60_000;
	/** Stations fight from prepared positions: an auto-resolved station counts this much of its rating. */
	static final double STATION_BONUS = 1.5;
	private static final Random RNG = new Random();
	static final int RECENT = 8;

	private static BattleTracker instance = new BattleTracker();
	/** Smoothed server tick time, from {@code ServerTickMixin}, and the part of it that was not battles. Main thread only. */
	private static double tickMs, otherMs;
	/** Battle update time so far this tick, from {@link #addCost}. */
	private static long tickBattleNanos;
	/**
	 * Milliseconds of tick time battles may still spend: each tick adds what the rest of the server left
	 * under the ceiling and takes off what battles spent. Soft battles run on ticks it is above 0.
	 */
	private static double credit;
	/** Smoothed share of ticks soft battles ran on; new ones only start while it is near 1. */
	private static double runShare = 1;

	/** Server, every tick: how long the tick took. */
	public static void recordTick(long nanos) {
		double battleMs = tickBattleNanos / 1e6;
		tickBattleNanos = 0;
		//one sample counts at most four budgets: a single hitch (a world loading, a save) is not load
		double tick = Math.min(nanos / 1e6, BUDGET_MS * 4.0);
		double other = Math.max(0, tick - battleMs);
		tickMs = tickMs * 0.95 + tick * 0.05;
		otherMs = otherMs * 0.95 + other * 0.05;
		credit = credit(credit, other, battleMs, BFConfig.battleHoldAbovePercent());
	}

	/**
	 * Battles are the lowest priority on the server's thread: they get what the rest of the tick leaves
	 * under {@code capPct} of the budget, and no more. A battle that costs three times what is left runs
	 * one tick in four, slowed down rather than switched off and on. Bounded so a quiet minute is not
	 * banked for one long burst, nor one hitch paid for over seconds.
	 */
	static double credit(double credit, double otherMs, double battleMs, int capPct) {
		double next = credit + BUDGET_MS * capPct / 100.0 - otherMs - battleMs;
		return Math.max(-BUDGET_MS * 4.0, Math.min(BUDGET_MS, next));
	}

	static final class Side {
		final int faction;
		int hitsTaken;
		int lost;
		double lostRating;
		boolean survivors;
		/** Retreated: its fleets were sent home by auto-resolve, or every one of them was ordered away mid-fight; see {@link #noteRetreats}. */
		boolean withdrew;
		/** Came to attack: one of its fleets here was ordered to; see {@link #attacks}. A side that never was defended. */
		boolean attacking;
		/**
		 * The most this side had in the fight at once, counting what it had already lost: so a side that
		 * pours in reinforcements is judged on the share it lost, not the raw count; see {@link #commit}.
		 * 0 until the sector is first looked at.
		 */
		double committedRating;
		int committedShips;

		Side(int faction) {
			this.faction = faction;
		}
	}

	static final class Battle {
		final int sectorId;
		final Vector3i pos;
		/** When the fight began, carried rounds included: what the report's length counts. */
		long start;
		/** When this round loaded; what the quiet and soft-load clocks count from. */
		long opened;
		long lastHit;
		/** Frozen by an admin: no AI anywhere in the sector, no updates at all while nobody is there. */
		boolean paused;
		long pausedAt;
		boolean endedByAdmin;
		boolean autoResolved;
		/** An encounter that ran its time: both sides went back to their orders; see {@link #breaksOff}. */
		boolean disengaged;
		/** When {@link #breaksOff} may next look at the fleets. */
		long nextEncounterCheck;
		/** Each fleet's order when it was first seen here, by fleet id; see {@link #noteRetreats}. */
		final Long2ObjectOpenHashMap<Order> orders = new Long2ObjectOpenHashMap<>();
		/** Fleets {@link #noteRetreats} has seen ordered away. */
		final LongOpenHashSet retreated = new LongOpenHashSet();
		/** When {@link #noteRetreats} may next look at the fleets. */
		long nextRetreatCheck;
		/** When it was carried; see the class comment. */
		long carriedAt;
		/** Loaded by this tracker rather than by a player. */
		boolean soft;
		/** When the server's load froze it, or 0; see {@link #held}. */
		long heldAt;
		boolean observed;
		/** Update time spent in this sector since the last tick, and its smoothed per-tick figure. */
		long costNanos;
		double avgMs;
		int entities;
		final Int2ObjectOpenHashMap<Side> sides = new Int2ObjectOpenHashMap<>();
		/** Given when it is reported; see {@link BattleNames}. */
		String name;
		/**
		 * Every root ship and station seen fighting, by UID, as last seen: the full report's tables.
		 * todo: not saved with the battle, so a fight carried over a restart reports only what it saw after.
		 */
		final Object2ObjectOpenHashMap<String, BattleReports.Unit> units = new Object2ObjectOpenHashMap<>();

		Battle(int sectorId, Vector3i pos, long start) {
			this.sectorId = sectorId;
			this.pos = new Vector3i(pos);
			this.start = start;
			this.opened = start;
		}

		boolean anyLost() {
			for (Side s : sides.values()) {
				if (s.lost > 0) {
					return true;
				}
			}
			return false;
		}

		Side side(int faction) {
			Side s = sides.get(faction);
			if (s == null) {
				s = new Side(faction);
				sides.put(faction, s);
			}
			return s;
		}
	}

	private final Int2ObjectOpenHashMap<Battle> battles = new Int2ObjectOpenHashMap<>();
	private final ArrayDeque<Vector3i> pending = new ArrayDeque<>();
	/** Queued sector to when it was queued. */
	private final Object2LongOpenHashMap<Vector3i> queued = new Object2LongOpenHashMap<>();
	private final Object2ObjectOpenHashMap<Vector3i, Battle> carried = new Object2ObjectOpenHashMap<>();
	/** Battles no player is watching, as of the last tick; what the soft-load cap counts. */
	private int unwatched;
	/** Sector to when the scan may queue it again. */
	private final Object2LongOpenHashMap<Vector3i> cooldownUntil = new Object2LongOpenHashMap<>();
	/** Sector to when hits there may start a battle again, unless a player is watching. */
	private final Object2LongOpenHashMap<Vector3i> hitsBlockedUntil = new Object2LongOpenHashMap<>();
	/** Soft-loaded rounds in a row a sector ended with nothing lost; each doubles its scan cooldown. */
	private final Object2IntOpenHashMap<Vector3i> fruitless = new Object2IntOpenHashMap<>();
	/** Admins watching a battle, by name; see {@link #watch}. Main thread only. */
	private final Object2ObjectOpenHashMap<String, Spectator> spectators = new Object2ObjectOpenHashMap<>();
	/** Soft battles have had no tick for {@link #STARVED_MS}: the rest of the server is using all of it. */
	private boolean held;
	private long lastRun;

	private static final class Spectator {
		final Vector3i battle;
		final Vector3i origin;
		final boolean wasInvisible;
		final long since;
		boolean arrived;
		/** What their HUD was last told: 1 soft-loaded, 2 held for load. */
		byte shown;

		Spectator(Vector3i battle, Vector3i origin, boolean wasInvisible, long since) {
			this.battle = new Vector3i(battle);
			this.origin = new Vector3i(origin);
			this.wasInvisible = wasInvisible;
			this.since = since;
		}
	}

	/**
	 * How a battle ended, which decides how long its sector is left alone. After a fight that went quiet,
	 * or one that never started, the scan waits before loading the sector again, or fleets that will not
	 * finish each other would be loaded every scan; a shot fired there while it is still loaded starts a
	 * new battle at once, whoever fires it. The soft-load cap and an over-cap release
	 * hold off both for a while, or the next hit would undo them. An admin's end holds off both longest.
	 */
	enum Ending {
		FOUGHT_OUT(COOLDOWN_MS, 0), NEVER_STARTED(COOLDOWN_MS, 0), CAPPED(COOLDOWN_MS, COOLDOWN_MS),
		OVER_CAP(COOLDOWN_MS, COOLDOWN_MS), ADMIN(ADMIN_COOLDOWN_MS, ADMIN_COOLDOWN_MS),
		/** An encounter broken off: long enough for both sides to be well on their way. */
		DISENGAGED(DISENGAGE_HOLD_MS, DISENGAGE_HOLD_MS);

		final long scanMs;
		final long hitsMs;

		Ending(long scanMs, long hitsMs) {
			this.scanMs = scanMs;
			this.hitsMs = hitsMs;
		}
	}

	/** Why a battle that is over ({@link #isOver}, or its sector gone) ended. */
	static Ending ending(Battle b, long now) {
		if (b.disengaged) {
			return Ending.DISENGAGED;
		}
		if (b.soft && now - b.opened > SOFT_MAX_MS) {
			return Ending.CAPPED;
		}
		return b.lastHit > 0 ? Ending.FOUGHT_OUT : Ending.NEVER_STARTED;
	}
	/** Fleet db id to the sector its flagship was in at the last scan. */
	private Long2ObjectOpenHashMap<Vector3i> fleetSectors = new Long2ObjectOpenHashMap<>();
	/** Per sector a fleet sits in, what the database had there when the fleet arrived. */
	private Object2ObjectOpenHashMap<Vector3i, SectorContents> sectorContents = new Object2ObjectOpenHashMap<>();
	/** Sectors an unloaded fleet has stopped in, as of the last scan; see {@link #stopsHere}. */
	private ObjectOpenHashSet<Vector3i> fleetsStopped = new ObjectOpenHashSet<>();
	/** Rebuilt every tick; what the mixins ask. */
	private final IntOpenHashSet softSectors = new IntOpenHashSet();
	private final IntOpenHashSet pausedSectors = new IntOpenHashSet();
	/** Paused and soft-loaded: nothing in the sector updates. */
	private final IntOpenHashSet frozenSectors = new IntOpenHashSet();
	private final ArrayDeque<String> recent = new ArrayDeque<>();
	private long nextScan;
	private int tick;
	/** Reports sent per sector this run, for the world tests. */
	private final Object2IntOpenHashMap<Vector3i> reports = new Object2IntOpenHashMap<>();
	private final BattleReports archive = new BattleReports();

	/** Server ticks since the world loaded. */
	public static int ticks() {
		return instance.tick;
	}

	/** The battle in this sector, or null. */
	Battle battleAt(Vector3i pos) {
		for (Battle b : battles.values()) {
			if (b.pos.equals(pos)) {
				return b;
			}
		}
		return null;
	}

	int reportsAt(Vector3i pos) {
		return reports.getInt(pos);
	}

	public static BattleTracker get() {
		return instance;
	}

	/** Drops every battle, for a world being loaded. */
	public static void reset() {
		instance = new BattleTracker();
	}

	/** For the mixins: a battle sector no player is watching. Main thread only. */
	public static boolean isSoftLoaded(int sectorId) {
		return instance.softSectors.contains(sectorId);
	}

	/**
	 * For the AI mixin: whether this entity skips its AI this tick. Entities are staggered by id so a
	 * sector's AI load is spread over the ticks rather than all landing on one.
	 */
	public static boolean skipAiTick(int sectorId, int entityId) {
		if (instance.pausedSectors.contains(sectorId)) {
			return true;
		}
		int divisor = BFConfig.softAiTickDivisor();
		//counted in the ticks the entity is updated on, so the two divisors multiply rather than line up
		return divisor > 1 && isSoftLoaded(sectorId) && !runsOn(instance.tick / Math.max(1, BFConfig.softEntityTickDivisor()), entityId, divisor);
	}

	/**
	 * For the entity load mixin: 0 to skip {@code o} this tick, else how many ticks this update stands
	 * for. In a soft-loaded sector a ship is updated one tick in {@code soft_loaded_entity_tick_divisor},
	 * staggered by the root of its dock chain, so a turret always updates with the ship it sits on.
	 */
	public static int entityTickEvery(SimpleTransformableSendableObject<?> o) {
		int every = BFConfig.softEntityTickDivisor();
		if (every <= 1 || !(o instanceof SegmentController sc) || !isSoftLoaded(o.getSectorId())
				|| instance.pausedSectors.contains(o.getSectorId())) {
			return 1;
		}
		return runsOn(instance.tick, sc.railController.getRoot().getId(), every) ? every : 0;
	}

	/** Whether something staggered by {@code id} has its turn on {@code tick}, one in {@code every}. */
	static boolean runsOn(long tick, int id, int every) {
		return Math.floorMod(tick + id, every) == 0;
	}

	/** For the load mixins: a paused battle no player is watching, where nothing updates. */
	public static boolean isFrozen(int sectorId) {
		return instance.frozenSectors.contains(sectorId);
	}

	/** For the load mixin: whether update time in this sector is a battle's. Main thread only. */
	public static boolean isBattle(int sectorId) {
		return instance.battles.containsKey(sectorId);
	}

	public static void addCost(int sectorId, long nanos) {
		Battle b = instance.battles.get(sectorId);
		if (b != null) {
			b.costNanos += nanos;
			tickBattleNanos += nanos;
		}
	}

	/** Most units a battle's full report lists; past it new arrivals are left out, losses still land. */
	static final int MAX_UNITS = 300;

	/**
	 * A ship or station in its own right: not docked to anything, nor waiting to dock, as turrets and
	 * docked ships are for a moment after their sector loads. What every ship and station count counts.
	 */
	public static boolean root(SegmentController sc) {
		return !sc.railController.isDockedOrDirty() && !sc.railController.hasInitialRailRequest();
	}

	/**
	 * The faction {@code sc} fought for. A ship whose faction block is shot off turns unaffiliated before
	 * it dies; it is still a loss for the side it was on when the battle first saw it.
	 */
	static int factionOf(Battle b, SegmentController sc) {
		if (sc.getFactionId() != 0) {
			return sc.getFactionId();
		}
		BattleReports.Unit u = b.units.get(sc.getUniqueIdentifier());
		return u != null ? u.faction() : 0;
	}

	/** Records {@code sc} as it stands now for the full report; once lost it stays lost. */
	static void note(Battle b, SegmentController sc, boolean lost) {
		String uid = sc.getUniqueIdentifier();
		BattleReports.Unit was = b.units.get(uid);
		if (was != null && was.lost() || was == null && !lost && b.units.size() >= MAX_UNITS) {
			return;
		}
		b.units.put(uid, new BattleReports.Unit(sc.getRealName(), factionOf(b, sc), (byte) sc.getType().dbTypeId,
				sc.getMassWithDocks(), lost ? 0 : (float) sc.getHpController().getHpPercent(), CombatRatings.rateWithDocks(sc), lost));
	}

	/** Server: {@code victim} accepted a hit from {@code damager}. Called per hit, so it stays cheap. */
	public void onHit(SegmentController victim, Damager damager) {
		if (damager instanceof Sector || !(victim instanceof Ship || victim instanceof SpaceStation)) {
			return; //stars and black holes are not a side, and mining an asteroid is not a battle
		}
		Battle battle = battles.get(victim.getSectorId());
		int a = battle != null ? factionOf(battle, victim) : victim.getFactionId();
		int b = battle != null && damager instanceof SegmentController d ? factionOf(battle, d) : damager.getFactionId();
		if (a == b || a == 0 || b == 0) {
			return; //unaffiliated ships are nobody's side: shooting one is not a battle
		}
		if (battle == null) {
			Sector sector = GameServerState.instance.getUniverse().getSector(victim.getSectorId());
			if (sector == null) {
				return;
			}
			//a battle that was just released keeps fighting until its sector times out; without this
			//those hits would start it again at once
			if (hitsBlockedUntil.getLong(sector.pos) > System.currentTimeMillis() && !watched(GameServerState.instance, sector.pos)) {
				return;
			}
			battle = open(sector, System.currentTimeMillis());
		}
		battle.lastHit = System.currentTimeMillis();
		battle.side(a).hitsTaken++;
		battle.side(b);
	}

	/** Server: {@code sc} started overheating, which is how a ship dies. */
	public void onOverheat(SegmentController sc) {
		Battle battle = battles.get(sc.getSectorId());
		if (battle == null || !root(sc) || factionOf(battle, sc) == 0) {
			return; //a turret is part of the ship it sits on, not a loss of its own; and neutrals are no side
		}
		Side side = battle.side(factionOf(battle, sc));
		videogoose.betterfactions.civics.Council.server().onShipLost(battle.sides.keySet().toIntArray(), factionOf(battle, sc));
		side.lost++;
		side.lostRating += CombatRatings.rateWithDocks(sc);
		note(battle, sc, true);
	}

	/** Server tick. */
	public void update(GameServerState state, long now) {
		tick++;
		Universe universe = state.getUniverse();
		if (now >= nextScan && (BFConfig.maxSoftBattles() > 0 || BFConfig.autoResolveSeconds() > 0)) {
			nextScan = now + SCAN_MS;
			scanUnloadedFleets(state, universe, now);
			autoResolveOverdue(state, now);
			expireCarried(state, now);
		}
		boolean running = credit > 0 || BFConfig.battleHoldAbovePercent() <= 0;
		if (running) {
			lastRun = now;
		}
		runShare = runShare * 0.98 + (running ? 0.02 : 0);
		boolean wasHeld = held;
		held = now - lastRun > STARVED_MS;
		if (held != wasHeld) {
			System.err.println("[BetterFactions] server " + fmt(otherMs) + " ms/tick without battles: soft-loaded battles " + (held ? "held" : "running again"));
		}
		//only while the ones running get nearly every tick; queued battles that wait too long are settled on paper instead
		while (runShare > 0.9 && spare(otherMs, BFConfig.battleStartBelowPercent()) && unwatched < BFConfig.maxSoftBattles() && !pending.isEmpty()) {
			if (startSoft(universe, pending.poll(), now)) {
				unwatched++;
			}
		}

		updateSpectators(state, now);

		softSectors.clear();
		pausedSectors.clear();
		frozenSectors.clear();
		List<Battle> ended = null;
		List<Battle> overCap = null;
		unwatched = 0;
		for (Battle b : battles.values()) {
			Sector sector = universe.getSector(b.sectorId);
			//an encounter is asked first: past the soft-load cap it breaks off rather than being capped
			noteRetreats(state, sector, b, now);
			if (sector == null || breaksOff(state, b, now) || isOver(b, now) || foughtOut(b, sector)) {
				if (ended == null) {
					ended = new ArrayList<>();
				}
				ended.add(b);
				continue;
			}
			b.avgMs = b.avgMs * 0.9 + b.costNanos / 1e6 * 0.1;
			b.costNanos = 0;
			b.entities = sector.getEntities().size();
			scuttle(sector);
			//kept alive whoever loaded it: a player leaving mid-fight hands the fight to us
			sector.ping();
			boolean watched = watched(state, b.pos);
			boolean heldNow = !running && !watched;
			if (heldNow && b.heldAt == 0) {
				b.heldAt = now;
			} else if (!heldNow && b.heldAt > 0) {
				shiftClocks(b, now - b.heldAt);
				b.heldAt = 0;
			}
			if (watched) {
				b.observed = true;
			} else {
				softSectors.add(b.sectorId);
				//a paused or held battle costs nothing, so it does not take a slot
				if (!b.paused && !heldNow && ++unwatched > BFConfig.maxSoftBattles()) {
					if (overCap == null) {
						overCap = new ArrayList<>();
					}
					overCap.add(b);
				}
			}
			if (b.paused || heldNow) {
				pausedSectors.add(b.sectorId);
				//skipped for a tick or two, a spectator's client just carries on drawing; held for longer, its
				//ships are stopped so it does not draw a fight that is not happening
				if (watched || spectated(b.pos) && (b.paused || held)) {
					holdStill(sector);
				} else {
					frozenSectors.add(b.sectorId);
				}
			}
		}
		if (ended != null) {
			for (Battle b : ended) {
				end(state, b, now, ending(b, now));
			}
		}
		if (overCap != null) {
			for (Battle b : overCap) {
				//stops pinging it; the sector goes quiet and its fleets are found again once it unloads
				battles.remove(b.sectorId);
				softSectors.remove(b.sectorId);
				holdOff(b.pos, Ending.OVER_CAP, now, 0);
				carry(b, now);
				unwatched--;
			}
		}
	}

	/**
	 * Over on its own clock: a minute without a hit (or, never hit, a minute since it opened), or a
	 * soft-loaded one past {@link #SOFT_MAX_MS}. A paused battle's quiet clock is stopped.
	 */
	static boolean isOver(Battle b, long now) {
		if (b.heldAt > 0) {
			return false; //frozen for load: its clocks are stopped
		}
		return !b.paused && (b.lastHit > 0 ? now - b.lastHit > QUIET_MS : now - b.opened > START_MS)
				|| (b.soft && now - b.opened > SOFT_MAX_MS);
	}

	/**
	 * Over without waiting out {@link #QUIET_MS}: it has fought and fewer than two of its sides have ships
	 * left in the sector. Sides are whoever traded hits, not whoever is formally at war: an NPC hostile to
	 * a player, or a player shooting a neutral, fights without a war, and asking diplomacy ended those the
	 * tick they opened. Allies left standing wait out the quiet minute. Paused and held battles wait.
	 */
	private static boolean foughtOut(Battle b, Sector sector) {
		if (b.lastHit == 0 || b.paused || b.heldAt > 0) {
			return false;
		}
		IntOpenHashSet here = new IntOpenHashSet();
		for (SimpleTransformableSendableObject<?> o : sector.getEntities()) {
			if (o instanceof SegmentController sc && root(sc) && !sc.isCoreOverheating()) {
				here.add(sc.getFactionId());
			}
		}
		return sidesLeft(b, here) < 2;
	}

	/** How many of the battle's sides have a faction among {@code here}. */
	static int sidesLeft(Battle b, IntSet here) {
		int n = 0;
		for (int faction : b.sides.keySet()) {
			n += here.contains(faction) ? 1 : 0;
		}
		return n;
	}

	/** Whether the server has time to spare for a new soft-loaded battle. {@code startPct} 0 always does. */
	static boolean spare(double otherMs, int startPct) {
		return startPct <= 0 || otherMs < BUDGET_MS * startPct / 100.0;
	}

	/** Moves a battle's clocks on by time it spent frozen, so the freeze costs it nothing. */
	static void shiftClocks(Battle b, long by) {
		b.start += by;
		b.opened += by;
		if (b.lastHit > 0) {
			b.lastHit += by;
		}
	}

	/**
	 * Whether a battle is an encounter that has run its time: fleets on their way somewhere else that met,
	 * fought for {@link #encounterMs} (or the soft-load cap, if soft-loaded and sooner) and now go back to
	 * their orders, rather than circling each other until one side is gone. A battle any fleet has
	 * business in (on sentry or defending there, or sent to attack it) is fought out as before. Marks it
	 * {@link Battle#disengaged}.
	 */
	private boolean breaksOff(GameServerState state, Battle b, long now) {
		long limit = encounterMs();
		if (limit <= 0 || b.paused || b.heldAt > 0 || b.lastHit == 0 || now < b.nextEncounterCheck) {
			return false;
		}
		if (!timedOut(b, now, limit) && !(b.soft && now - b.opened > SOFT_MAX_MS)) {
			return false;
		}
		b.nextEncounterCheck = now + ENCOUNTER_CHECK_MS;
		List<Vector3i> targets = new ArrayList<>();
		for (Fleet f : fleetsAt(state, b.pos)) {
			targets.add(f.getCurrentMoveTarget());
		}
		b.disengaged = encounter(targets, b.pos);
		return b.disengaged;
	}

	/** A fleet's order as far as retreating goes: what it is doing and where it is bound (null: nowhere). */
	record Order(FleetCommandTypes type, Vector3i target) {
		static Order of(Fleet f) {
			FleetCommand c = f.getCurrentCommand();
			Vector3i t = f.getCurrentMoveTarget();
			return new Order(c != null ? c.getCommandType() : null, t != null ? new Vector3i(t) : null);
		}
	}

	/** Whether a fleet under {@code o} is attacking {@code pos}: ordered to attack, or moving there on purpose. */
	static boolean attacks(Order o, Vector3i pos) {
		return o.type == FleetCommandTypes.FLEET_ATTACK || o.type == FleetCommandTypes.MOVE_FLEET && pos.equals(o.target);
	}

	/**
	 * Marks a side {@link Side#withdrew} once every one of its fleets in the battle has been ordered
	 * away: a fleet's order changed since it was first seen here, and now points elsewhere. Pulling one
	 * damaged squadron out while the rest fight on is not a retreat. Asked every second rather than at
	 * the end, because a retreating fleet is gone by then and its side would simply read as beaten. A
	 * fleet that arrived already bound elsewhere (an encounter) keeps that order and is not retreating;
	 * a patrol moving on to its next waypoint is not either. Also updates what each side has committed.
	 */
	private static void noteRetreats(GameServerState state, Sector sector, Battle b, long now) {
		if (b.paused || now < b.nextRetreatCheck) {
			return;
		}
		b.nextRetreatCheck = now + 1000;
		IntOpenHashSet staying = new IntOpenHashSet();
		IntOpenHashSet leaving = new IntOpenHashSet();
		for (Fleet f : state.getFleetManager().fleetCache.values()) {
			FleetMember flag = f.getFlagShip();
			if (flag == null) {
				continue; //destroyed or empty: neither stayed nor left
			}
			Order first = b.orders.get(f.dbid);
			if (first == null && b.pos.equals(flag.getSector()) && b.sides.containsKey(f.getFactionId())) {
				b.orders.put(f.dbid, first = Order.of(f));
			}
			if (first == null) {
				continue;
			}
			if (attacks(first, b.pos) || b.pos.equals(flag.getSector()) && attacks(Order.of(f), b.pos)) {
				b.side(f.getFactionId()).attacking = true;
			}
			//once ordered away it stays away: arriving home later clears its move target
			if (b.retreated.contains(f.dbid) || retreats(first, Order.of(f), b.pos)) {
				b.retreated.add(f.dbid);
				leaving.add(f.getFactionId());
			} else {
				staying.add(f.getFactionId());
			}
		}
		withdraw(b, staying, leaving);
		if (sector != null) {
			commit(b, sector);
		}
	}

	/** Sides with a fleet ordered away and none left fighting have withdrawn. */
	static void withdraw(Battle b, IntSet staying, IntSet leaving) {
		for (int faction : leaving) {
			if (!staying.contains(faction)) {
				b.side(faction).withdrew = true;
			}
		}
	}

	/** Adds up what each side has in the sector now; see {@link Side#committedRating}. */
	private static void commit(Battle b, Sector sector) {
		Int2DoubleOpenHashMap rating = new Int2DoubleOpenHashMap();
		Int2DoubleOpenHashMap ships = new Int2DoubleOpenHashMap();
		for (SimpleTransformableSendableObject<?> o : sector.getEntities()) {
			if (o instanceof SegmentController sc && root(sc) && !sc.isCoreOverheating()
					&& b.sides.containsKey(sc.getFactionId())) {
				rating.put(sc.getFactionId(), rating.get(sc.getFactionId()) + CombatRatings.rateWithDocks(sc));
				ships.put(sc.getFactionId(), ships.get(sc.getFactionId()) + 1);
				note(b, sc, false);
			}
		}
		for (Side s : b.sides.values()) {
			commit(s, rating.get(s.faction), (int) ships.get(s.faction));
		}
	}

	/** What is here now plus what was already lost is what the side has put in; reinforcements raise it, leaving does not lower it. */
	static void commit(Side s, double ratingHere, int shipsHere) {
		s.committedRating = Math.max(s.committedRating, ratingHere + s.lostRating);
		s.committedShips = Math.max(s.committedShips, shipsHere + s.lost);
	}

	/** Whether a fleet whose order was {@code first} when it joined the fight at {@code pos} has been ordered away. */
	static boolean retreats(Order first, Order now, Vector3i pos) {
		if (now.target == null || now.target.equals(pos) || now.equals(first)) {
			return false;
		}
		return !(now.type == FleetCommandTypes.PATROL_FLEET && first.type == FleetCommandTypes.PATROL_FLEET);
	}

	/**
	 * Fleets with these move targets have no business at {@code pos}: there are some, and every one is
	 * bound elsewhere. A null target is a fleet with nowhere to go (sentry, defend, idle), which stays.
	 */
	static boolean encounter(List<Vector3i> moveTargets, Vector3i pos) {
		if (moveTargets.isEmpty()) {
			return false;
		}
		for (Vector3i t : moveTargets) {
			if (t == null || t.equals(pos)) {
				return false;
			}
		}
		return true;
	}

	private static List<Fleet> fleetsAt(GameServerState state, Vector3i pos) {
		List<Fleet> out = new ArrayList<>();
		for (Fleet f : state.getFleetManager().fleetCache.values()) {
			FleetMember flag = f.getFlagShip();
			if (flag != null && pos.equals(flag.getSector())) {
				out.add(f);
			}
		}
		return out;
	}

	/** Sends every fleet at {@code pos} its current order again, so ships pulled into the fight go back to it. */
	private static void resumeOrders(GameServerState state, Vector3i pos) {
		for (Fleet f : fleetsAt(state, pos)) {
			FleetCommand c = f.getCurrentCommand();
			if (c != null) {
				f.getFleetManager().executeCommand(new FleetCommand(c.getCommandType(), f, c.getArgs()));
			}
		}
	}

	/** Pause or resume. Resuming moves the clocks on by the time held, so the pause costs it nothing. */
	static void togglePause(Battle b, long now) {
		if (b.paused) {
			shiftClocks(b, now - b.pausedAt);
		} else {
			b.pausedAt = now;
		}
		b.paused = !b.paused;
	}

	private Battle open(Sector sector, long now) {
		return open(sector.getId(), sector.pos, now);
	}

	/** A new battle in a loaded sector, picking up any carried tallies for it. */
	Battle open(int sectorId, Vector3i pos, long now) {
		Battle b = new Battle(sectorId, pos, now);
		Battle c = carried.remove(pos);
		if (c != null) {
			b.sides.putAll(c.sides);
			b.observed = c.observed;
			b.start = c.lastHit > 0 ? now - fought(c, now) : c.start; //the wait while carried is not fighting
			b.lastHit = c.lastHit > 0 ? now : 0; //a fresh quiet window: it has only just reloaded
			b.paused = c.paused;
			b.pausedAt = now;
		}
		battles.put(b.sectorId, b);
		return b;
	}

	void carry(Battle b, long now) {
		if (b.lastHit == 0 && !b.paused) {
			return; //nothing happened in it to carry; an admin's pause is kept
		}
		b.carriedAt = now;
		carried.put(b.pos, b);
	}

	private void expireCarried(GameServerState state, long now) {
		Iterator<Battle> it = carried.values().iterator();
		while (it.hasNext()) {
			Battle b = it.next();
			if (now - b.carriedAt > CARRY_MS) {
				it.remove();
				if (b.lastHit > 0) {
					report(state, null, b, now);
				}
			}
		}
	}

	/** Resolves every queued sector that has waited past {@link BFConfig#autoResolveSeconds()}. */
	private void autoResolveOverdue(GameServerState state, long now) {
		int after = BFConfig.autoResolveSeconds();
		if (after <= 0) {
			return;
		}
		Iterator<Vector3i> it = pending.iterator();
		while (it.hasNext()) {
			Vector3i pos = it.next();
			if (now - queued.getLong(pos) > after * 1000L) {
				it.remove();
				queued.removeLong(pos);
				resolve(state, pos, now);
			}
		}
	}

	/**
	 * Settles an unloaded sector on paper. Strength is the sum of each faction's stored ratings for what
	 * it has there (fleet members and anything else the database places in the sector), stations times
	 * {@link #STATION_BONUS}. todo: an entity never seen loaded has no rating and counts nothing;
	 * rate from its blueprint if NPC spawns start losing every paper battle.
	 */
	private void resolve(GameServerState state, Vector3i pos, long now) {
		if (state.getUniverse().isSectorLoaded(pos)) {
			return; //loaded since it was queued: if they fight, the hits will say so
		}
		FactionManager fm = state.getFactionManager();
		CombatRatings ratings = DiplomacyManager.server().getCombatRatings();
		Int2DoubleOpenHashMap strength = new Int2DoubleOpenHashMap();
		LongOpenHashSet counted = new LongOpenHashSet();
		List<Fleet> fleets = new ArrayList<>();
		for (Fleet f : state.getFleetManager().fleetCache.values()) {
			FleetMember flag = f.getFlagShip();
			if (flag == null || flag.isLoaded() || !pos.equals(flag.getSector())) {
				continue;
			}
			fleets.add(f);
			for (FleetMember m : f.getMembers()) {
				if (pos.equals(m.getSector()) && counted.add(m.entityDbId)) {
					strength.put(f.getFactionId(), strength.get(f.getFactionId()) + ratings.ratingOf(m.entityDbId));
				}
			}
		}
		try {
			for (DatabaseEntry d : state.getDatabaseIndex().getTableManager().getEntityTable().getBySector(pos, 200)) {
				boolean station = d.getEntityType() == SimpleTransformableSendableObject.EntityType.SPACE_STATION;
				//faction 0 here is derelicts and abandoned hulls parked in the sector, not a side in the fight
				if (d.faction != 0 && (station || d.getEntityType() == SimpleTransformableSendableObject.EntityType.SHIP) && counted.add(d.dbId)) {
					strength.put(d.faction, strength.get(d.faction) + ratings.ratingOf(d.dbId) * (station ? STATION_BONUS : 1));
				}
			}
		} catch (Exception e) {
			System.err.println("[BetterFactions] auto-resolve could not read sector " + pos + ": " + e);
		}
		cooldownUntil.put(pos, now + COOLDOWN_MS);
		//a raider and its prey here are enemies though their factions need not be at war
		LongOpenHashSet raids = new LongOpenHashSet();
		for (Fleet a : fleets) {
			for (Fleet b : fleets) {
				if (TradeFleetOrders.get().raiding(a.dbid, b.dbid)) {
					raids.add(pair(a.getFactionId(), b.getFactionId()));
				}
			}
		}
		BiPredicate<Integer, Integer> enemies = (x, y) -> fm.isEnemy(x, y) || raids.contains(pair(x, y));
		int winner = pickWinner(strength, enemies, RNG);
		if (winner == Integer.MIN_VALUE) {
			return; //nobody there can fight, or nobody there is at war
		}
		Battle b = carried.remove(pos);
		if (b == null) {
			b = new Battle(-1, pos, now);
		}
		b.autoResolved = true;
		b.lastHit = now;
		for (int faction : strength.keySet()) {
			Side s = b.side(faction);
			s.survivors = faction == winner || !enemies.test(faction, winner);
		}
		for (Fleet f : fleets) {
			if (attacks(Order.of(f), pos)) {
				b.side(f.getFactionId()).attacking = true;
			}
			if (enemies.test(f.getFactionId(), winner)) {
				retreat(f, pos, fm);
				b.side(f.getFactionId()).withdrew = true;
			}
		}
		report(state, null, b, now);
	}

	/** Two factions as one unordered key. */
	static long pair(int a, int b) {
		return ((long) Math.min(a, b) << 32) | (Math.max(a, b) & 0xffffffffL);
	}

	/**
	 * The faction with the highest strength after a roll of 0.75 to 1.25, among those at war with
	 * someone else present. {@link Integer#MIN_VALUE} when there is no such faction with any strength.
	 */
	static int pickWinner(Int2DoubleMap strength, BiPredicate<Integer, Integer> enemies, Random rng) {
		int winner = Integer.MIN_VALUE;
		double best = 0;
		int[] ids = strength.keySet().toIntArray();
		for (int a : ids) {
			boolean atWar = false;
			for (int b : ids) {
				atWar |= a != b && enemies.test(a, b);
			}
			double roll = strength.get(a) * (0.75 + 0.5 * rng.nextDouble());
			if (atWar && roll > best) {
				best = roll;
				winner = a;
			}
		}
		return winner;
	}

	private void end(GameServerState state, Battle b, long now, Ending how) {
		battles.remove(b.sectorId);
		if (b.lastHit == 0 && b.soft) {
			System.err.println("[BetterFactions] no fight started in soft-loaded sector " + b.pos + "; released");
		}
		Sector sector = state.getUniverse().getSector(b.sectorId);
		rateAll(sector);
		int rounds = b.soft && !b.anyLost() ? fruitless.getInt(b.pos) + 1 : 0;
		if (rounds > 0) {
			fruitless.put(new Vector3i(b.pos), rounds);
		} else {
			fruitless.removeInt(b.pos);
		}
		holdOff(b.pos, how, now, rounds);
		if (how == Ending.DISENGAGED) {
			resumeOrders(state, b.pos);
		}
		if (b.lastHit > 0 && how == Ending.FOUGHT_OUT && !b.anyLost()) {
			carry(b, now); //a round nobody lost anything in: the next round, or the hour running out, reports it
		} else if (b.lastHit > 0) {
			report(state, sector, b, now);
		}
	}

	/**
	 * Rates everything in a battle sector that is about to unload. The military sweep only rates what is
	 * loaded when it runs, and a fleet that has not loaded since ratings began would otherwise count as
	 * nothing until it happened to be loaded at a sweep.
	 */
	private static void rateAll(Sector sector) {
		if (sector == null) {
			return;
		}
		CombatRatings ratings = DiplomacyManager.server().getCombatRatings();
		for (SimpleTransformableSendableObject<?> o : sector.getEntities()) {
			if (o instanceof SegmentController sc && (sc instanceof Ship || sc instanceof SpaceStation)
					&& sc.getFactionId() != 0 && sc.getDbId() > 0 && sc.isFullyLoaded()) {
				ratings.put(sc.getDbId(), sc.getFactionId(), CombatRatings.rate(sc));
			}
		}
	}

	/**
	 * Server, from the admin profiler tab; the caller has checked admin rights. Pause toggles; a paused
	 * battle's clocks stop, so resuming gives it back the time it had. Teleport moves whatever the admin
	 * is flying, as {@code /change_sector} does.
	 */
	public void adminCommand(GameServerState state, PlayerState admin, byte cmd, int sectorId) {
		if (cmd == CMD_WATCH && spectators.containsKey(admin.getName())) {
			stopWatching(state, admin, true);
			return;
		}
		Battle b = battles.get(sectorId);
		if (b == null) {
			admin.sendServerMessagePlayerError(Lng.astr("That battle is already over."));
			return;
		}
		long now = System.currentTimeMillis();
		List<Battle> all = cluster(b);
		switch (cmd) {
			case CMD_PAUSE -> {
				boolean pause = !b.paused;
				for (Battle c : all) {
					if (c.paused != pause) {
						togglePause(c, now);
					}
				}
			}
			case CMD_END -> {
				//the ones that fought last, so the others fold into them and one report covers it all
				all.sort((x, y) -> Long.compare(x.lastHit, y.lastHit));
				for (Battle c : all) {
					endByAdmin(state, c, now);
				}
			}
			case CMD_TELEPORT -> {
				SimpleTransformableSendableObject<?> what = admin.getFirstControlledTransformableWOExc();
				if (what instanceof SegmentController sc) {
					what = sc.railController.getRoot();
				}
				if (what == null) {
					admin.sendServerMessagePlayerError(Lng.astr("Nothing to teleport."));
					return;
				}
				state.getController().queueSectorSwitch(what, new Vector3i(b.pos), SectorSwitch.TRANS_JUMP, true, true, true);
			}
			case CMD_WATCH -> watch(state, admin, b, now);
			default -> {
			}
		}
	}

	private boolean startSoft(Universe universe, Vector3i pos, long now) {
		queued.removeLong(pos);
		if (!fleetsStopped.contains(pos)) {
			//queued a while ago, or saved before a restart, and the fleet has moved on since
			System.err.println("[BetterFactions] no fleet left at queued sector " + pos + "; dropped");
			return false;
		}
		try {
			//loads the sector and its entities, and nothing around it
			Sector sector = universe.getSector(pos);
			Battle b = battles.get(sector.getId());
			if (b == null) {
				b = open(sector, now);
			}
			b.soft = true;
			System.err.println("[BetterFactions] soft-loading battle sector " + pos);
			return true;
		} catch (Exception e) {
			System.err.println("[BetterFactions] could not load battle sector " + pos + ": " + e);
			cooldownUntil.put(pos, now + COOLDOWN_MS);
			return false;
		}
	}

	/**
	 * Queues every unloaded sector where an unloaded fleet shares space with a faction it is at war
	 * with, and something there would actually shoot: a fleet on an order that fights, or a station
	 * (its turrets fire on their own). An idle or moving fleet does not fight when loaded either, so two
	 * of them passing each other is not a battle. The database is asked once per sector a fleet moves into, and
	 * the answer kept while it stays: what sits still in an unloaded sector cannot change.
	 */
	private void scanUnloadedFleets(GameServerState state, Universe universe, long now) {
		FactionManager fm = state.getFactionManager();
		Object2ObjectOpenHashMap<Vector3i, IntOpenHashSet> factions = new Object2ObjectOpenHashMap<>();
		ObjectOpenHashSet<Vector3i> armed = new ObjectOpenHashSet<>();
		ObjectOpenHashSet<Vector3i> raided = new ObjectOpenHashSet<>();
		ObjectOpenHashSet<Vector3i> moved = new ObjectOpenHashSet<>();
		Long2ObjectOpenHashMap<Vector3i> seen = new Long2ObjectOpenHashMap<>();
		for (Fleet f : state.getFleetManager().fleetCache.values()) {
			FleetMember flag = f.getFlagShip();
			if (flag == null || flag.isLoaded() || universe.isSectorLoaded(flag.getSector())) {
				continue; //loaded fights are found by their hits
			}
			Vector3i pos = new Vector3i(flag.getSector());
			seen.put(f.dbid, pos);
			if (!pos.equals(fleetSectors.get(f.dbid))) {
				moved.add(pos);
			}
			if (!stopsHere(f.getCurrentMoveTarget(), pos, f.getCurrentCommand() != null ? f.getCurrentCommand().getCommandType() : null)) {
				continue; //passing through: gone before the sector could load
			}
			factions.computeIfAbsent(pos, k -> new IntOpenHashSet()).add(f.getFactionId());
			if (fights(f)) {
				armed.add(pos);
				//a raider on its prey fights it with no war between them (RaidHostilityMixin)
				Fleet prey = state.getFleetManager().getByFleetDbId(TradeFleetOrders.get().preyOf(f.dbid));
				if (prey != null && prey.getFlagShip() != null && pos.equals(prey.getFlagShip().getSector())) {
					raided.add(pos);
				}
			}
		}
		fleetSectors = seen;
		fleetsStopped = new ObjectOpenHashSet<>(factions.keySet());

		Object2ObjectOpenHashMap<Vector3i, SectorContents> contents = new Object2ObjectOpenHashMap<>();
		for (Object2ObjectOpenHashMap.Entry<Vector3i, IntOpenHashSet> e : factions.object2ObjectEntrySet()) {
			Vector3i pos = e.getKey();
			//kept while a fleet sits there: an idle fleet ordered to fight later must still see its enemy
			SectorContents known = moved.contains(pos) ? null : sectorContents.get(pos);
			if (queued.containsKey(pos) || cooldownUntil.getLong(pos) > now) {
				if (known != null) {
					contents.put(pos, known);
				}
				continue;
			}
			if (known == null) {
				known = readSector(state, pos);
				if (known == null) {
					continue; //the database could not answer; asked again next scan
				}
			}
			contents.put(pos, known);
			IntOpenHashSet here = e.getValue();
			here.addAll(known.factions);
			if ((armed.contains(pos) || known.station) && anyEnemies(here, fm)
					|| armed.contains(pos) && (known.undiscovered || raided.contains(pos))) {
				enqueue(pos, now);
			} else if (armed.contains(pos)) {
				explainSkipped(pos, here, known, fm);
			}
		}
		sectorContents = contents;
		cooldownUntil.object2LongEntrySet().removeIf(c -> c.getLongValue() <= now);
		hitsBlockedUntil.object2LongEntrySet().removeIf(c -> c.getLongValue() <= now);
	}

	/** What was last logged about each armed fleet's sector that is not a battle, so it is said once. */
	private final Object2ObjectOpenHashMap<Vector3i, String> explained = new Object2ObjectOpenHashMap<>();

	/**
	 * Logs, once per sector and per answer, why a fleet under fighting orders is not starting a battle
	 * there: who the scan saw in the sector, and that none of them are at war with each other.
	 */
	private void explainSkipped(Vector3i pos, IntSet here, SectorContents known, FactionManager fm) {
		StringBuilder who = new StringBuilder();
		for (int f : here) {
			Faction faction = fm.getFaction(f);
			who.append(who.length() > 0 ? ", " : "").append(faction != null ? faction.getName() : "faction " + f).append(" (").append(f).append(')');
		}
		String why = "factions there: " + who + (known.station ? ", a station among them" : ", no station") + "; none at war with another";
		if (!why.equals(explained.put(new Vector3i(pos), why))) {
			System.err.println("[BetterFactions] armed fleet at " + pos + " is not a battle: " + why);
		}
	}

	/**
	 * A station sector no one has loaded yet: the galaxy marks it as one (the map shows an "Undiscovered
	 * Station"), but the station itself, usually a pirate one, is only generated when the sector first
	 * loads, so the database has nothing there to be at war with. A fleet under fighting orders in one is
	 * queued anyway: loading it makes the station, and whoever fires starts the battle; if nobody does it
	 * is released like any other sector that never fought. A sector is generated once it has a row in
	 * SECTORS.
	 */
	private static boolean undiscoveredStation(GameServerState state, Vector3i pos) {
		try {
			if (state.getUniverse().getStellarSystemFromSecPos(pos).getSectorType(pos) != SectorInformation.SectorType.SPACE_STATION) {
				return false;
			}
			Connection c = ((TableAccessor) state.getDatabaseIndex().getTableManager().getSectorTable()).betterFactions$connection();
			try (Statement s = c.createStatement();
				 ResultSet r = s.executeQuery("SELECT COUNT(*) FROM SECTORS WHERE X = " + pos.x + " AND Y = " + pos.y + " AND Z = " + pos.z + ";")) {
				return r.next() && r.getInt(1) == 0;
			}
		} catch (Exception e) {
			System.err.println("[BetterFactions] battle scan could not tell whether " + pos + " holds an undiscovered station: " + e);
			return false;
		}
	}

	/** What the database places in an unloaded sector that a fleet shares: whose ships and stations, and whether any is a station. */
	private record SectorContents(IntOpenHashSet factions, boolean station, boolean undiscovered) {
	}

	private static SectorContents readSector(GameServerState state, Vector3i pos) {
		IntOpenHashSet factions = new IntOpenHashSet();
		boolean station = false;
		try {
			for (DatabaseEntry d : state.getDatabaseIndex().getTableManager().getEntityTable().getBySector(pos, 200)) {
				if (d.getEntityType() == SimpleTransformableSendableObject.EntityType.SPACE_STATION) {
					factions.add(d.faction);
					station = true;
				} else if (d.getEntityType() == SimpleTransformableSendableObject.EntityType.SHIP) {
					factions.add(d.faction);
				}
			}
		} catch (Exception ex) {
			System.err.println("[BetterFactions] battle scan could not read sector " + pos + ": " + ex);
			return null;
		}
		return new SectorContents(factions, station, !station && undiscoveredStation(state, pos));
	}

	/**
	 * Whether a fleet counts as being in the sector it is passing: it has stopped there (no move target,
	 * or this one), or it is on patrol, which engages what it meets along its route. A fleet passing
	 * through otherwise is gone before its sector could load, and a battle opened for it is empty.
	 */
	static boolean stopsHere(Vector3i moveTarget, Vector3i pos, FleetCommandTypes order) {
		return moveTarget == null || moveTarget.equals(pos) || order == FleetCommandTypes.PATROL_FLEET;
	}

	/** Orders under which a fleet's ships look for targets in their own sector. */
	private static boolean fights(Fleet f) {
		FleetCommand c = f.getCurrentCommand();
		if (c == null) {
			return false;
		}
		return switch (c.getCommandType()) {
			case SENTRY, SENTRY_FORMATION, FLEET_ATTACK, FLEET_DEFEND, PATROL_FLEET, RECON_FLEET -> true;
			default -> false;
		};
	}

	/**
	 * Sends a losing fleet off: to near its faction's home sector, or with no home, three sectors
	 * straight away from the battle. Not {@code Fleet.moveToNearHB}: that moves the faction's own
	 * home-sector vector in place, and sends homeless factions to the origin.
	 */
	private static void retreat(Fleet f, Vector3i from, FactionManager fm) {
		Faction faction = fm.getFaction(f.getFactionId());
		Vector3i to;
		if (faction != null && faction.getHomebaseUID() != null && !faction.getHomebaseUID().isEmpty()) {
			to = new Vector3i(faction.getHomeSector());
			to.add(RNG.nextInt(5) - 2, RNG.nextInt(5) - 2, RNG.nextInt(5) - 2);
		} else {
			to = new Vector3i(from);
			to.add(RNG.nextBoolean() ? 3 : -3, RNG.nextInt(7) - 3, RNG.nextInt(7) - 3);
		}
		f.getFleetManager().executeCommand(new FleetCommand(FleetCommandTypes.MOVE_FLEET, f, to));
	}

	/** Unaffiliated (0) is at war with no one here: its ships are not a side. */
	private static boolean anyEnemies(IntSet factions, FactionManager fm) {
		int[] ids = factions.toIntArray();
		for (int i = 0; i < ids.length; i++) {
			for (int j = i + 1; j < ids.length; j++) {
				if (ids[i] != 0 && ids[j] != 0 && fm.isEnemy(ids[i], ids[j])) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Whether a player is watching {@code pos}: in it, or in a neighbouring sector, as long as players
	 * still keep it loaded. A player keeps the 26 sectors around them loaded and sees into them, so a
	 * fight next door is watched. A sector in {@code Universe.notKeptLoaded} is one players have let go,
	 * and only a player inside it counts.
	 */
	private static boolean watched(GameServerState state, Vector3i pos) {
		boolean letGo = state.getUniverse().notKeptLoaded.contains(pos);
		List<Vector3i> players = new ArrayList<>();
		for (PlayerState p : state.getPlayerStatesByName().values()) {
			if (!instance.spectators.containsKey(p.getName())) {
				players.add(p.getCurrentSector()); //a spectator watches without making it a watched battle
			}
		}
		return watched(pos, letGo, players);
	}

	static boolean watched(Vector3i pos, boolean letGo, List<Vector3i> players) {
		for (Vector3i at : players) {
			if (at.equals(pos) || !letGo && Sector.isNeighbor(at, pos)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Takes an admin on foot to a battle as an invisible spectator, without making it a watched battle:
	 * it stays soft-loaded, the AI ignores them, and their sector's neighbours are not loaded around
	 * them ({@code SpectatorPingMixin}). Once there, their client switches to the free-roam camera.
	 * Asking again, or the battle ending, brings them back where they were; leaving on their own just
	 * ends it. Only on foot: getting a player out of their ship is up to their client.
	 */
	private void watch(GameServerState state, PlayerState admin, Battle b, long now) {
		if (admin.getAssingedPlayerCharacter() == null || admin.getFirstControlledTransformableWOExc() != admin.getAssingedPlayerCharacter()) {
			admin.sendServerMessagePlayerError(Lng.astr("Leave your ship first: you watch a battle on foot."));
			return;
		}
		spectators.put(admin.getName(), new Spectator(b.pos, admin.getCurrentSector(), admin.isInvisibilityMode(), now));
		admin.setInvisibilityMode(true);
		state.getController().queueSectorSwitch(admin.getAssingedPlayerCharacter(), new Vector3i(b.pos), SectorSwitch.TRANS_JUMP, true, true, true);
	}

	private void stopWatching(GameServerState state, PlayerState p, boolean goBack) {
		Spectator s = spectators.remove(p.getName());
		if (s == null) {
			return;
		}
		p.setInvisibilityMode(s.wasInvisible);
		if (s.arrived) {
			BFPacket.toPlayer(p, BFPacket.Kind.BATTLE_WATCH, new Tag(Tag.Type.BYTE, null, (byte) 0));
		}
		if (goBack && p.getAssingedPlayerCharacter() != null) {
			state.getController().queueSectorSwitch(p.getAssingedPlayerCharacter(), new Vector3i(s.origin), SectorSwitch.TRANS_JUMP, true, true, true);
		}
	}

	private void updateSpectators(GameServerState state, long now) {
		if (spectators.isEmpty()) {
			return;
		}
		for (PlayerState p : new ArrayList<>(state.getPlayerStatesByName().values())) {
			Spectator s = spectators.get(p.getName());
			if (s == null) {
				continue;
			}
			boolean there = p.getCurrentSector().equals(s.battle);
			if (!s.arrived) {
				if (there) {
					s.arrived = true;
				} else if (now - s.since > WATCH_ARRIVE_MS) {
					stopWatching(state, p, false);
				}
			} else if (!there) {
				stopWatching(state, p, false); //left on their own
			} else if (battleAt(s.battle) == null) {
				stopWatching(state, p, true);
				p.sendServerMessagePlayerInfo(Lng.astr("The battle is over; you are back where you were."));
				continue;
			}
			if (s.arrived) {
				byte show = held ? (byte) 2 : (byte) 1;
				if (show != s.shown) {
					s.shown = show;
					BFPacket.toPlayer(p, BFPacket.Kind.BATTLE_WATCH, new Tag(Tag.Type.BYTE, null, show));
				}
			}
		}
	}

	/**
	 * A spectator has arrived at {@code pos}. A held or paused battle there is not frozen outright: with
	 * no updates the spectator's client carries on alone, flying ships on their last velocity and
	 * drawing their last shots, a fight that is not happening. It is held still instead.
	 */
	private boolean spectated(Vector3i pos) {
		for (Spectator s : spectators.values()) {
			if (s.arrived && s.battle.equals(pos)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Every AI-flown ship in a held or paused battle someone can see, stopped where it is and out of
	 * its fight, every tick. With its AI off a ship would fly on along the last course the AI set, and
	 * clients would keep shooting: they run the AI's firing themselves, for show, whenever its synced
	 * target position is set ({@code ShipAIEntity.updateAIClient}), and nothing would clear it. Turrets
	 * included, their aim; their parent moves them. Ships players fly are theirs.
	 */
	/**
	 * In a soft-loaded battle, a ship that started overheating is wrecked now ({@link Wrecks}) rather than
	 * when its timer runs out, so the sector stops simulating it as a combatant. With wrecks off, it is
	 * removed instead, and its turrets with it rather than undocked to drift. The loss was counted when it
	 * started ({@link #onOverheat}), and {@code destroy} still tells its faction, as the timer would.
	 */
	private static void scuttle(Sector sector) {
		//a copy: destroy() takes the entity out of the sector's set mid-iteration
		for (SimpleTransformableSendableObject<?> o : new ArrayList<>(sector.getEntities())) {
			if (o instanceof SegmentController sc && sc.isCoreOverheating() && !sc.railController.isDocked()
					&& !sc.isMarkedForDeleteVolatile() && (!(sc instanceof Ship ship) || ship.getAttachedPlayers().isEmpty())
					&& !Wrecks.get().isWreck(sc.getUniqueIdentifier())) {
				if (Wrecks.get().tryWreck(sc, System.currentTimeMillis())) {
					continue;
				}
				sc.railController.destroyDockedRecursive();
				sc.destroy();
			}
		}
	}

	private static void holdStill(Sector sector) {
		for (SimpleTransformableSendableObject<?> o : sector.getEntities()) {
			if (o instanceof Ship ship && ship.getAttachedPlayers().isEmpty()) {
				ship.getNetworkObject().targetPosition.set(0, 0, 0);
				ship.getNetworkObject().orientationDir.set(0, 0, 0, 0);
				if (ship.railController.isDocked()) {
					continue;
				}
				ship.getNetworkObject().moveDir.set(0, 0, 0);
				if (ship.getPhysicsDataContainer().getObject() instanceof RigidBody body) {
					body.setLinearVelocity(new Vector3f());
					body.setAngularVelocity(new Vector3f());
				}
			}
		}
	}

	/** For the ping mixin: a sector where every player present is a spectator. Main thread only. */
	public static boolean onlySpectatorsAt(Vector3i pos) {
		if (instance.spectators.isEmpty()) {
			return false;
		}
		boolean any = false;
		for (PlayerState p : GameServerState.instance.getPlayerStatesByName().values()) {
			if (p.getCurrentSector().equals(pos)) {
				Spectator s = instance.spectators.get(p.getName());
				if (s == null || !s.arrived) {
					return false;
				}
				any = true;
			}
		}
		return any;
	}

	void endByAdmin(GameServerState state, Battle b, long now) {
		b.endedByAdmin = true;
		end(state, b, now, Ending.ADMIN);
	}

	/** @param fruitless soft-loaded rounds in a row with nothing lost; the scan waits 5, 10, 20, then 40 minutes */
	static long scanBackoff(int fruitless) {
		return 1L << Math.max(0, Math.min(3, fruitless - 1));
	}

	private void holdOff(Vector3i pos, Ending how, long now, int fruitless) {
		if (how.scanMs > 0) {
			cooldownUntil.put(new Vector3i(pos), now + how.scanMs * scanBackoff(fruitless));
		}
		if (how.hitsMs > 0) {
			hitsBlockedUntil.put(new Vector3i(pos), now + how.hitsMs);
		}
	}

	private void report(GameServerState state, Sector sector, Battle b, long now) {
		if (sector != null) {
			for (SimpleTransformableSendableObject<?> o : sector.getEntities()) {
				if (o instanceof SegmentController sc && root(sc) && !sc.isCoreOverheating()) {
					Side s = b.sides.get(sc.getFactionId());
					if (s != null) {
						s.survivors = true;
						note(b, sc, false);
					}
				}
			}
		}
		//a fight spilling over into the next sector is one battle: the last sector of it to end reports it all
		for (Battle n : battles.values()) {
			if (n != b && n.lastHit > 0 && adjacent(n.pos, b.pos)) {
				merge(b, n);
				return;
			}
		}
		reports.put(new Vector3i(b.pos), reports.getInt(b.pos) + 1);
		FactionManager fm = state.getFactionManager();
		IntFunction<String> name = id -> {
			Faction f = fm.getFaction(id);
			return f != null ? f.getName() : id == 0 ? "Unaffiliated" : "Faction " + id;
		};
		int owner = systemOwner(state, b.pos);
		defendedBy(b, owner, fm::isEnemy);
		b.name = nameOf(state, b, name, owner);
		String body = summarize(b, name, now);
		recent.addFirst(b.pos + ": " + body.substring(0, body.indexOf('\n')) + " (" + Math.max(1, fought(b, now) / 60_000) + " min"
				+ (b.soft ? ", soft-loaded" : "") + ")");
		while (recent.size() > RECENT) {
			recent.removeLast();
		}
		List<BattleReports.Line> lines = new ArrayList<>();
		for (Side s : b.sides.values()) {
			lines.add(new BattleReports.Line(s.faction, name.apply(s.faction), s.lost, s.withdrew));
		}
		archive.add(new BattleReports.Report(now, new Vector3i(b.pos), body.substring(0, body.indexOf('\n')), body,
				fought(b, now), b.soft, holder(b), favoured(b), lines, new ArrayList<>(b.units.values()), b.name));
		String title = b.name;
		String news = newsBody(b, body, name);
		for (Side s : b.sides.values()) {
			if (fm.getFaction(s.faction) == null) {
				continue;
			}
			FactionNewsPost post = new FactionNewsPost();
			post.set(s.faction, "Battle Report", now, title, news, 0);
			post.setCategory(FactionNewsCategory.WAR);
			fm.addNewsPostServer(post);
		}
		//and one line in the galaxy news everyone reads, under the first side that is a real faction
		for (Side s : b.sides.values()) {
			if (fm.getFaction(s.faction) != null) {
				fm.getNpcFactionNews().post(s.faction, galaxyLine(b, body, name), FactionNewsCategory.WAR);
				break;
			}
		}
	}

	/** The same sector or one of the 26 around it. */
	static boolean adjacent(Vector3i a, Vector3i b) {
		return Math.abs(a.x - b.x) <= 1 && Math.abs(a.y - b.y) <= 1 && Math.abs(a.z - b.z) <= 1;
	}

	/**
	 * Folds {@code from}'s tallies into {@code into}, the battle next door it was part of.
	 * todo: committed strength is summed, so a ship that crossed between the sectors counts twice.
	 */
	static void merge(Battle from, Battle into) {
		for (Side f : from.sides.values()) {
			Side s = into.side(f.faction);
			s.hitsTaken += f.hitsTaken;
			s.lost += f.lost;
			s.lostRating += f.lostRating;
			s.survivors |= f.survivors;
			s.withdrew |= f.withdrew;
			s.attacking |= f.attacking;
			s.committedRating += f.committedRating;
			s.committedShips += f.committedShips;
		}
		for (var e : from.units.entrySet()) {
			BattleReports.Unit was = into.units.get(e.getKey());
			if (was == null ? into.units.size() < MAX_UNITS : !was.lost()) {
				into.units.put(e.getKey(), e.getValue());
			}
		}
		into.start = Math.min(into.start, from.start);
		into.observed |= from.observed;
	}

	/** Server: the faction that owns the system {@code pos} is in, or 0. */
	private static int systemOwner(GameServerState state, Vector3i pos) {
		try {
			return state.getUniverse().getStellarSystemFromSecPos(pos).getOwnerFaction();
		} catch (Exception e) {
			System.err.println("[BetterFactions] could not tell who owns the system of " + pos + ": " + e);
			return 0;
		}
	}

	/**
	 * Territory decides who attacked before fleet orders do: when {@code owner} fought here it defended,
	 * and its enemies attacked. Without the owner in the fight, the order-based flags stand.
	 */
	static void defendedBy(Battle b, int owner, BiPredicate<Integer, Integer> enemies) {
		if (owner == 0 || !b.sides.containsKey(owner)) {
			return;
		}
		for (Side s : b.sides.values()) {
			if (s.faction == owner) {
				s.attacking = false;
			} else if (enemies.test(s.faction, owner)) {
				s.attacking = true;
			}
		}
	}

	/** The battle's name, after its system and who attacked whom: territory first, then fleet orders. */
	private String nameOf(GameServerState state, Battle b, IntFunction<String> name, int owner) {
		String system = DiplomacyManager.systemName(state, b.pos);
		Side attacker = null;
		Side defender = null;
		int ships = 0;
		for (Side s : b.sides.values()) {
			ships += s.committedShips;
			if (s.attacking && attacker == null) {
				attacker = s;
			}
		}
		if (attacker != null && owner != 0 && b.sides.containsKey(owner) && state.getFactionManager().isEnemy(owner, attacker.faction)) {
			defender = b.sides.get(owner);
		}
		for (Side s : b.sides.values()) {
			if (defender == null && attacker != null && !s.attacking && state.getFactionManager().isEnemy(s.faction, attacker.faction)) {
				defender = s;
				break;
			}
		}
		String base = BattleNames.name(system != null ? system : "Sector " + b.pos.toStringPure(),
				attacker != null ? name.apply(attacker.faction) : null, defender != null ? name.apply(defender.faction) : null,
				ships, b.start);
		return BattleNames.numbered(base, archive.countNamed(base));
	}

	/** The faction news post: the verdict and the losses; the full report is under Assets. */
	static String newsBody(Battle b, String body, IntFunction<String> name) {
		StringBuilder losses = new StringBuilder();
		for (Side s : b.sides.values()) {
			losses.append(losses.length() == 0 ? "" : ", ").append(name.apply(s.faction)).append(' ').append(s.lost);
		}
		return body.substring(0, body.indexOf('\n')) + "\nShips lost: " + losses + ".";
	}

	/** The galaxy news line: where, who, and the report's verdict. */
	static String galaxyLine(Battle b, String body, IntFunction<String> name) {
		StringBuilder sides = new StringBuilder();
		for (Side s : b.sides.values()) {
			sides.append(sides.length() == 0 ? "" : " and ").append(name.apply(s.faction));
		}
		return (b.name != null ? b.name : "Battle at " + b.pos) + " between " + sides + ": " + body.substring(0, body.indexOf('\n'));
	}

	/**
	 * The one side that held the field, or {@link Integer#MIN_VALUE}: the only side with ships left that
	 * did not retreat. Fleets breaking off together are nobody's win, but a side that stayed while the
	 * other was ordered away held its ground, even if the encounter ran out after.
	 */
	static int holder(Battle b) {
		if (b.endedByAdmin) {
			return Integer.MIN_VALUE;
		}
		int holder = Integer.MIN_VALUE;
		int standing = 0;
		boolean retreat = false;
		for (Side s : b.sides.values()) {
			retreat |= s.withdrew;
			if (s.survivors && !s.withdrew) {
				standing++;
				holder = s.faction;
			}
		}
		return standing == 1 && (!b.disengaged || retreat) ? holder : Integer.MIN_VALUE;
	}

	/**
	 * The side that came off better in a battle nobody won outright (broke off, or more than one side
	 * or none left standing), or {@link Integer#MIN_VALUE} when it was even, held, or ended by an admin.
	 * Most encounters end with both sides still there, so "inconclusive" alone would say nothing about
	 * one side losing four ships to the other's one.
	 */
	static int favoured(Battle b) {
		if (b.endedByAdmin || holder(b) != Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		Side[] e = edge(b);
		return e != null ? e[0].faction : Integer.MIN_VALUE;
	}

	/** Under this share of the runner-up's losses is clearly less; anything closer reads as even. */
	static final double CLEAR_EDGE = 2.0 / 3.0;

	/**
	 * {least hurt side, runner-up}, or null when nobody lost clearly less. Judged on the first tally
	 * anyone has: combat rating lost (a lost frigate is not a lost titan), then ships, then hits taken
	 * for a fight where nothing went down. Only that one tally decides, so a side cannot win on hits
	 * what it lost on ships. Losses are a share of what each side committed when every side's is known
	 * ({@link #shares}): five of twenty-five ships is a better day than four of ten.
	 */
	static Side[] edge(Battle b) {
		if (b.sides.size() < 2) {
			return null;
		}
		List<ToDoubleFunction<Side>> tallies = shares(b)
				? List.of(s -> s.lostRating / s.committedRating, s -> (double) s.lost / s.committedShips, s -> s.hitsTaken)
				: List.of(s -> s.lostRating, s -> s.lost, s -> s.hitsTaken);
		for (ToDoubleFunction<Side> t : tallies) {
			Side best = null;
			Side next = null;
			for (Side s : b.sides.values()) {
				if (best == null || t.applyAsDouble(s) < t.applyAsDouble(best)) {
					next = best;
					best = s;
				} else if (next == null || t.applyAsDouble(s) < t.applyAsDouble(next)) {
					next = s;
				}
			}
			if (t.applyAsDouble(next) > 0) {
				return t.applyAsDouble(best) < t.applyAsDouble(next) * CLEAR_EDGE ? new Side[]{best, next} : null;
			}
		}
		return null;
	}

	/**
	 * Whether every side's commitment is known, so losses can be weighed against it. Not for a battle
	 * the tracker never looked at (auto-resolved, or ended before its first check) or a tally from
	 * before a restart; those are judged on raw losses as before.
	 */
	static boolean shares(Battle b) {
		for (Side s : b.sides.values()) {
			if (s.committedShips == 0 || s.committedRating <= 0) {
				return false;
			}
		}
		return true;
	}

	/** Why {@link #edge} chose: ships where they tell it, else rating, else hits. */
	static String tally(Side[] e, IntFunction<String> name, boolean shares) {
		String other = name.apply(e[1].faction);
		other += other.endsWith("s") ? "'" : "'s";
		if (shares ? (double) e[0].lost / e[0].committedShips < (double) e[1].lost / e[1].committedShips : e[0].lost < e[1].lost) {
			return shares
					? "lost " + e[0].lost + " of " + e[0].committedShips + (e[0].committedShips == 1 ? " ship" : " ships") + " to " + other
							+ " " + e[1].lost + " of " + e[1].committedShips
					: "lost " + e[0].lost + (e[0].lost == 1 ? " ship" : " ships") + " to " + other + " " + e[1].lost;
		}
		if (e[1].lostRating > 0) {
			return shares
					? "lost " + Math.round(100 * e[0].lostRating / e[0].committedRating) + "% of its combat rating to " + other + " "
							+ Math.round(100 * e[1].lostRating / e[1].committedRating) + "%"
					: "lost " + Math.round(e[0].lostRating) + " combat rating to " + other + " " + Math.round(e[1].lostRating);
		}
		return "took " + e[0].hitsTaken + (e[0].hitsTaken == 1 ? " hit" : " hits") + " to " + other + " " + e[1].hitsTaken;
	}

	/**
	 * The report body. The winner is the one side with ships left in the sector that did not retreat
	 * ({@link #holder}); with more than one, or none, whoever lost clearly less came off better
	 * ({@link #favoured}), else it was inconclusive.
	 * todo: "ships left" counts anything of theirs still there, not what fought. Track participants
	 * if a parked station starts deciding battles.
	 */
	static String summarize(Battle b, IntFunction<String> name, long now) {
		StringBuilder out = new StringBuilder();
		int winner = holder(b);
		Side[] edge = favoured(b) != Integer.MIN_VALUE ? edge(b) : null;
		String better = edge == null ? "" : name.apply(edge[0].faction) + " came off better (" + tally(edge, name, shares(b)) + ").";
		out.append(b.endedByAdmin ? "Ended by an admin."
				: winner != Integer.MIN_VALUE ? name.apply(winner) + (b.sides.get(winner).attacking ? " attacked successfully." : " defended successfully.")
				: b.disengaged ? edge != null ? "Broke off; " + better : "The fleets broke off and went on their way."
				: edge != null ? "Inconclusive; " + better : "The battle was inconclusive.").append('\n');
		if (b.autoResolved) {
			out.append("Settled without the sector loading; the losing fleets withdrew.\n");
		}
		for (Side s : b.sides.values()) {
			out.append(name.apply(s.faction)).append(": ").append(s.lost).append(s.lost == 1 ? " ship" : " ships").append(" lost")
					.append(s.withdrew ? ", withdrew" : "").append('\n');
		}
		out.append("Lasted ").append(Math.max(1, fought(b, now) / 60_000)).append(" min");
		if (!b.observed) {
			out.append(", unobserved");
		}
		return out.append('.').toString();
	}

	/**
	 * Server: what the admin profiler tab shows, as {header text, one row per battle}. Rows are
	 * {sector id, sector, state, ms/tick, entities, age, sides, paused}, formatted here so the client
	 * only displays them.
	 */
	public Tag adminTag(GameServerState state, long now) {
		FactionManager fm = state.getFactionManager();
		List<Tag> t = new ArrayList<>();
		t.add(new Tag(Tag.Type.STRING, null, adminReport(state, now)));
		IntOpenHashSet shown = new IntOpenHashSet();
		for (Battle first : battles.values()) {
			if (shown.contains(first.sectorId)) {
				continue;
			}
			List<Battle> all = cluster(first);
			Battle b = all.get(0);
			IntOpenHashSet factions = new IntOpenHashSet();
			boolean paused = false, held = false, soft = true;
			double ms = 0;
			int entities = 0;
			long start = Long.MAX_VALUE, lastHit = 0;
			for (Battle c : all) {
				shown.add(c.sectorId);
				factions.addAll(c.sides.keySet());
				paused |= c.paused;
				held |= c.heldAt > 0;
				soft &= softSectors.contains(c.sectorId);
				ms += c.avgMs;
				entities += c.entities;
				start = Math.min(start, c.start);
				lastHit = Math.max(lastHit, c.lastHit);
			}
			StringBuilder sides = new StringBuilder();
			for (int faction : factions) {
				Faction f = fm.getFaction(faction);
				sides.append(!sides.isEmpty() ? "  vs  " : "").append(f != null ? f.getName() : "Faction " + faction);
			}
			String status = (paused ? "PAUSED " : held ? "HELD " : "") + (soft ? "SOFT" : "WATCHED");
			String age = clock(now - start) + (lastHit > 0 ? ", hit " + clock(now - lastHit) + " ago" : ", no hits");
			String pos = b.pos + (all.size() > 1 ? " +" + (all.size() - 1) : "");
			t.add(new Tag(Tag.Type.STRUCT, null, new Tag[]{
					new Tag(Tag.Type.INT, null, b.sectorId), new Tag(Tag.Type.STRING, null, pos),
					new Tag(Tag.Type.STRING, null, status), new Tag(Tag.Type.STRING, null, fmt(ms)),
					new Tag(Tag.Type.INT, null, entities), new Tag(Tag.Type.STRING, null, age),
					new Tag(Tag.Type.STRING, null, sides.toString()), new Tag(Tag.Type.BYTE, null, (byte) (b.paused ? 1 : 0)),
					FinishTag.INST}));
		}
		t.add(FinishTag.INST);
		return new Tag(Tag.Type.STRUCT, null, t.toArray(new Tag[0]));
	}

	/**
	 * {@code b} and every battle joined to it through adjacent sectors: what the profiler shows as one
	 * battle. Lowest sector id first, so a row keeps its key while that sector fights.
	 */
	List<Battle> cluster(Battle b) {
		List<Battle> out = new ArrayList<>();
		out.add(b);
		for (int i = 0; i < out.size(); i++) {
			for (Battle n : battles.values()) {
				if (!out.contains(n) && adjacent(n.pos, out.get(i).pos)) {
					out.add(n);
				}
			}
		}
		out.sort((x, y) -> Integer.compare(x.sectorId, y.sectorId));
		return out;
	}

	/**
	 * Server: the battles {@code faction} and {@code enemy} are both fighting right now, for anyone in
	 * {@code faction}: what the war's BATTLES dialog lists above its reports, adjacent sectors as one
	 * (see {@link #cluster}). Rows are {sector, sides, age, ships {@code faction} lost, ships everyone else
	 * lost, other sectors in it}; nothing about server load.
	 */
	public Tag liveTag(GameServerState state, int faction, int enemy, long now) {
		FactionManager fm = state.getFactionManager();
		List<Tag> rows = new ArrayList<>();
		IntOpenHashSet shown = new IntOpenHashSet();
		for (Battle first : battles.values()) {
			if (shown.contains(first.sectorId)) {
				continue;
			}
			List<Battle> all = cluster(first);
			Int2ObjectOpenHashMap<int[]> lost = new Int2ObjectOpenHashMap<>();
			long start = Long.MAX_VALUE;
			for (Battle c : all) {
				shown.add(c.sectorId);
				start = Math.min(start, c.start);
				for (Side s : c.sides.values()) {
					lost.computeIfAbsent(s.faction, k -> new int[1])[0] += s.lost;
				}
			}
			if (!lost.containsKey(faction) || !lost.containsKey(enemy)) {
				continue;
			}
			StringBuilder sides = new StringBuilder();
			int ours = 0, theirs = 0;
			for (var e : lost.int2ObjectEntrySet()) {
				Faction f = fm.getFaction(e.getIntKey());
				sides.append(!sides.isEmpty() ? "  vs  " : "").append(f != null ? f.getName() : "Faction " + e.getIntKey());
				if (e.getIntKey() == faction) {
					ours += e.getValue()[0];
				} else {
					theirs += e.getValue()[0];
				}
			}
			rows.add(new Tag(Tag.Type.STRUCT, null, new Tag[]{
					new Tag(Tag.Type.VECTOR3i, null, new Vector3i(all.get(0).pos)), new Tag(Tag.Type.STRING, null, sides.toString()),
					new Tag(Tag.Type.STRING, null, clock(now - start)), new Tag(Tag.Type.INT, null, ours),
					new Tag(Tag.Type.INT, null, theirs), new Tag(Tag.Type.INT, null, all.size() - 1), FinishTag.INST}));
		}
		rows.add(FinishTag.INST);
		return new Tag(Tag.Type.STRUCT, null, rows.toArray(new Tag[0]));
	}

	/** The header above the battle list: totals, the queue and what ended lately. */
	String adminReport(GameServerState state, long now) {
		int soft = 0;
		double load = 0;
		for (Battle b : battles.values()) {
			soft += b.soft ? 1 : 0;
			load += b.avgMs;
		}
		StringBuilder out = new StringBuilder();
		out.append("SERVER  ").append(fmt(tickMs)).append(" of ").append(BUDGET_MS).append(" ms tick, ").append(fmt(otherMs))
				.append(" without battles: soft battles run ").append(Math.round(runShare * 100)).append("% of ticks")
				.append(held ? ", HELD" : runShare > 0.9 && spare(otherMs, BFConfig.battleStartBelowPercent()) ? ", room for more"
						: ", queued battles wait").append('\n');
		out.append("BATTLES  ").append(battles.size()).append(" active, ").append(soft).append(" of ").append(BFConfig.maxSoftBattles())
				.append(" soft-load slots used, ").append(pending.size()).append(" queued, ").append(carried.size()).append(" carried\n");
		out.append("LOAD  ").append(fmt(load)).append(" ms/tick of a ").append(fmt(tickMs)).append(" ms average tick");
		if (tickMs > 0) {
			out.append(" (").append(Math.round(load / tickMs * 100)).append("%)");
		}
		out.append("  |  AI runs 1 tick in ").append(BFConfig.softAiTickDivisor()).append(" when soft-loaded\n");
		if (!pending.isEmpty()) {
			out.append("QUEUED  ");
			for (Vector3i p : pending) {
				out.append(p).append("  ");
			}
			out.append('\n');
		}
		if (!recent.isEmpty()) {
			out.append("RECENT\n");
			for (String r : recent) {
				out.append(r).append('\n');
			}
		}
		return out.toString();
	}

	private static String fmt(double ms) {
		return String.format("%.2f", ms);
	}

	static String clock(long ms) {
		long s = Math.max(0, ms / 1000);
		return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
	}

	/**
	 * What survives a restart: every battle, active or carried, as {pos, ms it has run, had hits,
	 * observed, paused, sides}, and the queue. Ages are saved as durations, so downtime does not count.
	 */
	public Tag toTag() {
		long now = System.currentTimeMillis();
		List<Tag> rows = new ArrayList<>();
		List<Battle> all = new ArrayList<>(battles.values());
		all.addAll(carried.values());
		for (Battle b : all) {
			if (b.lastHit == 0 && !b.paused) {
				continue; //nobody fired: nothing to fight on after a restart
			}
			Tag[] sides = new Tag[b.sides.size() + 1];
			int i = 0;
			for (Side s : b.sides.values()) {
				sides[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
						new Tag(Tag.Type.INT, null, s.faction), new Tag(Tag.Type.INT, null, s.hitsTaken),
						new Tag(Tag.Type.INT, null, s.lost), new Tag(Tag.Type.DOUBLE, null, s.lostRating),
						new Tag(Tag.Type.DOUBLE, null, s.committedRating), new Tag(Tag.Type.INT, null, s.committedShips),
						new Tag(Tag.Type.BYTE, null, (byte) (s.attacking ? 1 : 0)), FinishTag.INST});
			}
			sides[i] = FinishTag.INST;
			long ran = b.paused ? b.pausedAt - b.start : fought(b, now);
			rows.add(new Tag(Tag.Type.STRUCT, null, new Tag[]{
					new Tag(Tag.Type.VECTOR3i, null, new Vector3i(b.pos)), new Tag(Tag.Type.LONG, null, ran),
					new Tag(Tag.Type.BYTE, null, (byte) (b.lastHit > 0 ? 1 : 0)), new Tag(Tag.Type.BYTE, null, (byte) (b.observed ? 1 : 0)),
					new Tag(Tag.Type.BYTE, null, (byte) (b.paused ? 1 : 0)), new Tag(Tag.Type.STRUCT, null, sides), FinishTag.INST}));
		}
		rows.add(FinishTag.INST);
		Tag[] queue = new Tag[pending.size() + 1];
		int i = 0;
		for (Vector3i p : pending) {
			queue[i++] = new Tag(Tag.Type.VECTOR3i, null, new Vector3i(p));
		}
		queue[i] = FinishTag.INST;
		return new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.STRUCT, null, rows.toArray(new Tag[0])), new Tag(Tag.Type.STRUCT, null, queue), archive.toTag(), FinishTag.INST});
	}

	/**
	 * Loads what {@link #toTag} saved. Every battle comes back carried and its sector is queued, so it
	 * fights on when its sector loads, with its tallies.
	 */
	public void fromTag(Tag tag) {
		long now = System.currentTimeMillis();
		Tag[] t = tag.getStruct();
		Tag[] rows = t[0].getStruct();
		for (int r = 0; r < rows.length - 1; r++) {
			Tag[] e = rows[r].getStruct();
			Battle b = new Battle(-1, (Vector3i) e[0].getValue(), now - (Long) e[1].getValue());
			b.lastHit = e[2].getByte() != 0 ? now : 0;
			b.observed = e[3].getByte() != 0;
			b.paused = e[4].getByte() != 0;
			b.pausedAt = now;
			Tag[] sides = e[5].getStruct();
			for (int i = 0; i < sides.length - 1; i++) {
				Tag[] s = sides[i].getStruct();
				if (s[0].getInt() == 0) {
					continue; //saved before unaffiliated ships stopped being a side
				}
				Side side = b.side(s[0].getInt());
				side.hitsTaken = s[1].getInt();
				side.lost = s[2].getInt();
				side.lostRating = (Double) s[3].getValue();
				if (s.length > 6) { //saved before commitments were tracked: 0, judged on raw losses
					side.committedRating = (Double) s[4].getValue();
					side.committedShips = s[5].getInt();
				}
				side.attacking = s.length > 7 && s[6].getByte() != 0;
			}
			carry(b, now);
			enqueue(b.pos, now);
		}
		Tag[] queue = t[1].getStruct();
		for (int i = 0; i < queue.length - 1; i++) {
			enqueue((Vector3i) queue[i].getValue(), now);
		}
		if (t.length > 3) { //saved before reports were archived: none to load
			archive.fromTag(t[2]);
		}
	}

	private void enqueue(Vector3i pos, long now) {
		if (!queued.containsKey(pos)) {
			System.err.println("[BetterFactions] battle queued at " + pos);
			pending.add(new Vector3i(pos));
			queued.put(new Vector3i(pos), now);
		}
	}

	public BattleReports archive() {
		return archive;
	}

	Object2ObjectOpenHashMap<Vector3i, Battle> carried() {
		return carried;
	}

	Object2LongOpenHashMap<Vector3i> queued() {
		return queued;
	}

	Int2ObjectMap<Battle> battles() {
		return battles;
	}
}
