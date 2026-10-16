package videogoose.betterfactions.battle;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.fleet.FleetCommandTypes;

import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;

/**
 * The battle tracker's own logic: reports, when a battle is over, pausing, carried battles, the paper
 * winner and saving across a restart. What needs a running server (hits, sector loading, fleets) is
 * left to playing it. Run with -ea.
 */
public final class BattleTracker_test {

	public static void main(String[] args) {
		assert BattleTracker.pair(10001, -2) == BattleTracker.pair(-2, 10001) : "a raid's two factions key the same either way round";
		assert BattleTracker.pair(1, 2) != BattleTracker.pair(1, 3);
		reports();
		ending();
		pausing();
		load();
		carrying();
		watching();
		passing();
		encounters();
		paperWinner();
		edges();
		retreats();
		restart();
		adjacent();
		System.out.println("BattleTracker_test: OK");
	}

	private static void adjacent() {
		assert BattleTracker.adjacent(new Vector3i(0, 0, 0), new Vector3i(1, -1, 1));
		assert !BattleTracker.adjacent(new Vector3i(0, 0, 0), new Vector3i(2, 0, 0));
		BattleTracker.Battle a = new BattleTracker.Battle(1, new Vector3i(0, 0, 0), 500);
		BattleTracker.Battle b = new BattleTracker.Battle(2, new Vector3i(1, 0, 0), 100);
		a.side(10).lost = 2;
		b.side(10).lost = 1;
		b.side(20).lost = 4;
		b.side(20).survivors = true;
		BattleTracker.merge(a, b);
		assert b.sides.get(10).lost == 3 && b.sides.get(20).lost == 4 && b.sides.get(20).survivors;
		assert b.start == 100;

		BattleTracker t = new BattleTracker();
		BattleTracker.Battle x = t.open(5, new Vector3i(0, 0, 0), 0);
		t.open(3, new Vector3i(1, 0, 0), 0);
		t.open(4, new Vector3i(2, 1, 0), 0); //next to the second, not the first
		t.open(9, new Vector3i(9, 9, 9), 0);
		List<BattleTracker.Battle> c = t.cluster(x);
		assert c.size() == 3 && c.get(0).sectorId == 3 : c.size();
	}

	private static void reports() {
		BattleTracker.Battle b = new BattleTracker.Battle(1, new Vector3i(2, 3, 4), 0);
		b.side(10).lost = 3;
		b.side(20).lost = 1;
		b.side(20).survivors = true;
		String r = BattleTracker.summarize(b, id -> "F" + id, 5 * 60_000);
		assert r.startsWith("F20 defended successfully.") : "no attack order: it defended: " + r;
		assert r.contains("F10: 3 ships lost") && r.contains("F20: 1 ship lost") : r;
		assert r.endsWith("Lasted 5 min, unobserved.") : r;

		b.side(10).survivors = true;
		b.observed = true;
		r = BattleTracker.summarize(b, id -> "F" + id, 30_000);
		assert r.startsWith("Inconclusive; F20 came off better (lost 1 ship to F10's 3).") : "both standing, F20 lost less: " + r;
		assert r.endsWith("Lasted 1 min.") : r;
		b.lastHit = 6 * 60_000;
		r = BattleTracker.summarize(b, id -> "F" + id, 66 * 60_000);
		assert r.endsWith("Lasted 6 min.") : "the hour it waited carried is not fighting: " + r;
		b.lastHit = 0;

		b.side(10).survivors = false;
		b.side(20).survivors = false;
		r = BattleTracker.summarize(b, id -> "F" + id, 30_000);
		assert r.startsWith("Inconclusive; F20 came off better") : "nobody left, F20 still lost less: " + r;

		b.side(20).survivors = true;
		b.endedByAdmin = true;
		r = BattleTracker.summarize(b, id -> "F" + id, 30_000);
		assert r.startsWith("Ended by an admin.") : "an admin stop names no winner: " + r;

		b.endedByAdmin = false;
		b.autoResolved = true;
		b.side(10).withdrew = true;
		r = BattleTracker.summarize(b, id -> "F" + id, 30_000);
		assert r.startsWith("F20 defended successfully.\nSettled without") && r.contains("F10: 3 ships lost, withdrew") : r;

		BattleTracker.Battle line = new BattleTracker.Battle(1, new Vector3i(2, 3, 4), 0);
		line.side(10);
		line.side(20).survivors = true;
		String galaxy = BattleTracker.galaxyLine(line, BattleTracker.summarize(line, id -> "F" + id, 60_000), id -> "F" + id);
		assert galaxy.contains("Battle at") && galaxy.contains("F10") && galaxy.contains("F20") && galaxy.endsWith(": F20 defended successfully.")
				: "where, both sides, and the verdict, on one line: " + galaxy;
		assert !galaxy.contains("\n") : "one line";

		assert BattleTracker.clock(65_000).equals("1:05") : BattleTracker.clock(65_000);
		assert BattleTracker.clock(-5).equals("0:00") : "a clock never runs backwards";
	}

	private static void ending() {
		long t = 1_000_000;
		BattleTracker.Battle b = new BattleTracker.Battle(1, new Vector3i(), t);
		assert !BattleTracker.isOver(b, t + BattleTracker.START_MS) : "a loaded sector gets a full minute to start shooting";
		assert BattleTracker.isOver(b, t + BattleTracker.START_MS + 1) : "and is released if nobody does";

		b.lastHit = t + 5 * 60_000;
		assert !BattleTracker.isOver(b, b.lastHit + BattleTracker.QUIET_MS) : "a lull while reinforcements come is still the same fight";
		assert BattleTracker.isOver(b, b.lastHit + BattleTracker.QUIET_MS + 1) : "two minutes without a hit ends it";

		b.soft = true;
		assert BattleTracker.isOver(b, t + BattleTracker.SOFT_MAX_MS + 1) : "a soft-loaded fight is capped even mid-fight";
		b.soft = false;
		b.lastHit = t + BattleTracker.SOFT_MAX_MS;
		assert !BattleTracker.isOver(b, t + BattleTracker.SOFT_MAX_MS + 1) : "a watched fight still going has no cap";

		b.paused = true;
		assert !BattleTracker.isOver(b, b.lastHit + 10 * BattleTracker.QUIET_MS) : "a paused battle's quiet clock is stopped";

		//how it ended decides how long its sector is left alone
		BattleTracker.Battle fought = new BattleTracker.Battle(1, new Vector3i(), t);
		fought.lastHit = t + 1000;
		BattleTracker.Ending e = BattleTracker.ending(fought, fought.lastHit + BattleTracker.QUIET_MS + 1);
		assert e == BattleTracker.Ending.FOUGHT_OUT && e.hitsMs == 0
				: "after a fight that went quiet, fleets still there, or arriving, fight again at once";
		assert e.scanMs > 0 : "but fleets that cannot finish each other are not reloaded on every scan";
		BattleTracker.Battle idle = new BattleTracker.Battle(2, new Vector3i(), t);
		e = BattleTracker.ending(idle, t + BattleTracker.START_MS + 1);
		assert e == BattleTracker.Ending.NEVER_STARTED && e.scanMs > 0 : "a sector that never fought is not reloaded on every scan";
		assert e.hitsMs == 0 : "but a shot fired there is still a battle";
		BattleTracker.Battle capped = new BattleTracker.Battle(3, new Vector3i(), t);
		capped.soft = true;
		capped.lastHit = t + BattleTracker.SOFT_MAX_MS;
		e = BattleTracker.ending(capped, t + BattleTracker.SOFT_MAX_MS + 1);
		assert e == BattleTracker.Ending.CAPPED && e.hitsMs > 0 : "the cap holds, or its next hit would undo it";
		assert BattleTracker.Ending.ADMIN.hitsMs > BattleTracker.Ending.CAPPED.hitsMs : "an admin's end holds longest";

		assert BattleTracker.scanBackoff(0) == 1 && BattleTracker.scanBackoff(1) == 1 : "one fruitless round waits the plain cooldown";
		assert BattleTracker.scanBackoff(2) == 2 && BattleTracker.scanBackoff(3) == 4 : "each more doubles it";
		assert BattleTracker.scanBackoff(50) == 8 : "up to 40 minutes";

		//a carried fight picked up again: its report counts the fighting so far, its clocks from now
		BattleTracker tracker = new BattleTracker();
		BattleTracker.Battle first = new BattleTracker.Battle(4, new Vector3i(3, 3, 3), t);
		first.soft = true;
		first.lastHit = t + 1000;
		tracker.carry(first, t + 2000);
		long later = t + 2 * BattleTracker.SOFT_MAX_MS;
		BattleTracker.Battle again = tracker.open(5, new Vector3i(3, 3, 3), later);
		again.soft = true;
		assert BattleTracker.fought(again, later) == 1000 && again.opened == later;
		assert !BattleTracker.isOver(again, later + 1000) : "not capped the moment it reloads for being old";
	}

	private static void pausing() {
		long t = 1_000_000;
		BattleTracker.Battle b = new BattleTracker.Battle(1, new Vector3i(), t);
		b.lastHit = t + 10_000;
		BattleTracker.togglePause(b, t + 20_000);
		assert b.paused;
		BattleTracker.togglePause(b, t + 20_000 + 600_000);
		assert !b.paused;
		assert b.start == t + 600_000 && b.lastHit == t + 10_000 + 600_000 : "resuming gives back the ten minutes it was held";
		assert !BattleTracker.isOver(b, t + 620_000 + 30_000) : "so it is not over the moment it resumes";

		BattleTracker.Battle quiet = new BattleTracker.Battle(2, new Vector3i(), t);
		BattleTracker.togglePause(quiet, t + 5_000);
		BattleTracker.togglePause(quiet, t + 65_000);
		assert quiet.lastHit == 0 : "a battle never hit stays never hit";
	}

	private static void load() {
		//every entity gets exactly one turn in each run of `every` ticks, spread rather than all on one tick
		for (int id = -3; id < 5; id++) {
			int turns = 0;
			for (long t = 0; t < 6; t++) {
				turns += BattleTracker.runsOn(t, id, 3) ? 1 : 0;
			}
			assert turns == 2 : "id " + id + " ran " + turns + " of 6 ticks";
		}
		assert BattleTracker.runsOn(0, 0, 2) != BattleTracker.runsOn(0, 1, 2) : "neighbouring ids take turns";
		double budget = BattleTracker.BUDGET_MS;
		assert BattleTracker.spare(budget * 0.5, 60) && !BattleTracker.spare(budget * 0.7, 60) : "new battles only under the start line";
		assert BattleTracker.spare(budget * 5, 0) : "0 starts them whatever the load";
		//the rest of the server takes half the budget and the battles would take 1.2 budgets a tick: they get
		//the 40% left under a 90% ceiling, a tick in three, every few ticks rather than in long on/off stretches
		double credit = 0, spent = 0;
		int ran = 0, skipped = 0, longestSkip = 0;
		for (int i = 0; i < 3000; i++) {
			boolean run = credit > 0;
			double battleMs = run ? budget * 1.2 : 0;
			credit = BattleTracker.credit(credit, budget * 0.5, battleMs, 90);
			spent += budget * 0.5 + battleMs;
			ran += run ? 1 : 0;
			skipped = run ? 0 : skipped + 1;
			longestSkip = Math.max(longestSkip, skipped);
		}
		assert spent / 3000 <= budget * 0.92 : "the tick stays under the ceiling: " + spent / 3000;
		assert ran > 900 && ran < 1100 : "battles get what is left, about a tick in three: " + ran;
		assert longestSkip <= 3 : "and never wait long for one: " + longestSkip;
		assert BattleTracker.credit(0, budget * 0.1, 0, 90) <= budget : "a quiet server does not bank a burst";
		assert BattleTracker.credit(0, budget * 100, 0, 90) >= -budget * 4 : "nor does one hitch starve battles for long";

		long t = 1_000_000;
		BattleTracker.Battle b = new BattleTracker.Battle(1, new Vector3i(), t);
		b.soft = true;
		b.lastHit = t + 1000;
		b.heldAt = t + 2000;
		assert !BattleTracker.isOver(b, t + 10 * BattleTracker.SOFT_MAX_MS) : "a held battle neither goes quiet nor runs out";
		BattleTracker.shiftClocks(b, 600_000);
		assert b.start == t + 600_000 && b.opened == t + 600_000 && b.lastHit == t + 601_000 : "released, it gets the held time back";
	}

	private static void carrying() {
		long t = 1_000_000;
		BattleTracker tracker = new BattleTracker();
		Vector3i pos = new Vector3i(4, 5, 6);
		BattleTracker.Battle left = new BattleTracker.Battle(7, pos, t);
		left.lastHit = t + 1_000;
		left.observed = true;
		left.side(10).lost = 4;
		left.side(20).hitsTaken = 12;
		tracker.carry(left, t + 2_000);

		BattleTracker.Battle resumed = tracker.open(99, new Vector3i(pos), t + 600_000);
		assert tracker.carried().isEmpty() : "picked up, so no longer carried";
		assert resumed.sectorId == 99 : "the sector's id now, not when it was carried";
		assert resumed.side(10).lost == 4 && resumed.side(20).hitsTaken == 12 : "one report covers the whole fight";
		assert BattleTracker.fought(resumed, t + 600_000) == 1_000 : "its age is the fighting so far, not the wait while carried";
		assert resumed.lastHit == t + 600_000 : "a fresh quiet window, or it would end the tick it reloads";
		assert resumed.observed : "seen once is seen";
		assert tracker.battles().get(99) == resumed;

		BattleTracker.Battle fresh = tracker.open(100, new Vector3i(1, 1, 1), t);
		assert fresh.sides.isEmpty() && fresh.lastHit == 0 : "nothing carried there, nothing picked up";
	}

	private static void passing() {
		Vector3i here = new Vector3i(1, 2, 3);
		assert BattleTracker.stopsHere(null, here, FleetCommandTypes.SENTRY) : "no move target: it is here";
		assert BattleTracker.stopsHere(new Vector3i(here), here, FleetCommandTypes.FLEET_ATTACK) : "arrived at what it was sent to attack";
		assert !BattleTracker.stopsHere(new Vector3i(9, 9, 9), here, FleetCommandTypes.FLEET_ATTACK) : "on its way somewhere else";
		assert !BattleTracker.stopsHere(new Vector3i(9, 9, 9), here, FleetCommandTypes.MOVE_FLEET) : "passing through";
		assert BattleTracker.stopsHere(new Vector3i(9, 9, 9), here, FleetCommandTypes.PATROL_FLEET) : "a patrol engages along its route";
	}

	private static void encounters() {
		Vector3i here = new Vector3i(1, 2, 3);
		Vector3i away = new Vector3i(9, 9, 9);
		assert BattleTracker.encounter(java.util.List.of(away, new Vector3i(-5, 0, 0)), here) : "both bound elsewhere: they met";
		assert !BattleTracker.encounter(java.util.Arrays.asList(away, null), here) : "one is on sentry here: it is that fleet's fight";
		assert !BattleTracker.encounter(java.util.List.of(away, new Vector3i(here)), here) : "one was sent to attack here";
		assert !BattleTracker.encounter(java.util.List.of(), here) : "no fleets, no encounter (a station's fight)";
		BattleTracker.Battle t = new BattleTracker.Battle(1, here, 0);
		long limit = 20 * 60_000;
		t.lastHit = limit - 5 * 60_000;
		assert !BattleTracker.timedOut(t, limit, limit) : "not yet past the timeout";
		assert BattleTracker.timedOut(t, limit + 1, limit) : "past it, last hit long ago";
		t.lastHit = limit;
		assert !BattleTracker.timedOut(t, limit + 30_000, limit) : "a recent hit extends it";
		t.lastHit = 2 * limit;
		assert BattleTracker.timedOut(t, 2 * limit + 1, limit) : "but never past twice the timeout";

		BattleTracker.Battle b = new BattleTracker.Battle(1, here, 0);
		b.side(10).survivors = true;
		b.side(20).survivors = true;
		b.side(20).lost = 2;
		b.disengaged = true;
		assert BattleTracker.ending(b, 1) == BattleTracker.Ending.DISENGAGED;
		assert BattleTracker.Ending.DISENGAGED.scanMs > 0 && BattleTracker.Ending.DISENGAGED.hitsMs > 0
				: "the sector is left alone while they leave, or the next shot reopens it";
		String r = BattleTracker.summarize(b, id -> "F" + id, 20 * 60_000);
		assert r.startsWith("Broke off; F10 came off better (lost 0 ships to F20's 2).") && r.contains("F20: 2 ships lost") : r;
		b.side(10).lost = 2;
		r = BattleTracker.summarize(b, id -> "F" + id, 20 * 60_000);
		assert r.startsWith("The fleets broke off and went on their way.") : "even losses: " + r;
	}

	/** Who came off better when nobody was driven off. */
	private static void edges() {
		BattleTracker.Battle b = new BattleTracker.Battle(1, new Vector3i(), 0);
		b.side(10).survivors = true;
		b.side(20).survivors = true;
		assert BattleTracker.favoured(b) == Integer.MIN_VALUE : "nothing happened: even";
		//fought out by who is left of its sides, not by diplomacy: two sides still there fight on, war or not
		assert BattleTracker.sidesLeft(b, new IntOpenHashSet(new int[]{10, 20, 0})) == 2 : "both sides still here";
		assert BattleTracker.sidesLeft(b, new IntOpenHashSet(new int[]{10, 30})) == 1 : "a bystander is not a side";

		b.side(10).hitsTaken = 3;
		b.side(20).hitsTaken = 12;
		assert BattleTracker.favoured(b) == 10 : "no losses, so hits decide";
		assert BattleTracker.summarize(b, id -> "F" + id, 60_000).startsWith("Inconclusive; F10 came off better (took 3 hits to F20's 12).");

		b.side(10).lost = 1;
		assert BattleTracker.favoured(b) == 20 : "a lost ship outweighs any number of hits";

		b.side(20).lost = 1;
		b.side(10).lostRating = 100;
		b.side(20).lostRating = 900;
		assert BattleTracker.favoured(b) == 10 : "one ship each, but F20's was worth more";
		assert BattleTracker.summarize(b, id -> "F" + id, 60_000).contains("(lost 100 combat rating to F20's 900)");

		b.side(10).lostRating = 700;
		assert BattleTracker.favoured(b) == Integer.MIN_VALUE : "700 to 900 is not clearly less";

		b.side(30).lostRating = 1000;
		b.side(10).lostRating = 100;
		assert BattleTracker.favoured(b) == 10 : "three sides: against the runner-up";
		b.side(20).lostRating = 120;
		assert BattleTracker.favoured(b) == Integer.MIN_VALUE : "two sides about as well off: even";

		b.side(20).survivors = false;
		b.side(30).survivors = false;
		assert BattleTracker.favoured(b) == Integer.MIN_VALUE : "F10 held the field: that is a win, not an edge";
		b.side(20).lostRating = 900;
		b.endedByAdmin = true;
		b.side(10).survivors = false;
		assert BattleTracker.favoured(b) == Integer.MIN_VALUE : "an admin stop judges nobody";

		BattleTracker.Battle r = new BattleTracker.Battle(1, new Vector3i(5, 0, 0), 0);
		r.side(10).survivors = true;
		r.side(20).survivors = true;
		r.side(20).lost = 0;
		r.side(10).lost = 2;
		r.side(20).withdrew = true;
		assert BattleTracker.holder(r) == 10 : "a side ordered away retreated, even with ships still in the sector";
		assert BattleTracker.favoured(r) == Integer.MIN_VALUE : "held, so no edge";
		assert BattleTracker.summarize(r, id -> "F" + id, 60_000).startsWith("F10 defended successfully.\n");
		r.side(10).attacking = true;
		assert BattleTracker.summarize(r, id -> "F" + id, 60_000).startsWith("F10 attacked successfully.\n") : "its fleets came to attack";
		r.side(10).attacking = false;
		r.side(20).attacking = false;
		BattleTracker.defendedBy(r, 20, (x, y) -> true);
		assert r.side(10).attacking && !r.side(20).attacking : "an enemy of the system's owner attacked it there";
		r.side(10).attacking = false;
		r.side(20).attacking = true;
		BattleTracker.defendedBy(r, 30, (x, y) -> true);
		assert r.side(20).attacking && !r.side(10).attacking : "owner not in the fight: fleet orders stand";
		r.side(20).attacking = false;
		assert BattleTracker.attacks(new BattleTracker.Order(FleetCommandTypes.FLEET_ATTACK, null), r.pos);
		assert BattleTracker.attacks(new BattleTracker.Order(FleetCommandTypes.MOVE_FLEET, new Vector3i(r.pos)), r.pos);
		assert !BattleTracker.attacks(new BattleTracker.Order(FleetCommandTypes.MOVE_FLEET, new Vector3i(9, 9, 9)), r.pos) : "passing through";
		assert !BattleTracker.attacks(new BattleTracker.Order(FleetCommandTypes.SENTRY, null), r.pos);
		r.disengaged = true;
		assert BattleTracker.holder(r) == 10 : "the encounter running out after does not undo the retreat";
		r.side(10).withdrew = true;
		assert BattleTracker.holder(r) == Integer.MIN_VALUE && BattleTracker.favoured(r) == 20 : "both pulled out: judged on losses";

		BattleTracker.Battle w = new BattleTracker.Battle(1, new Vector3i(), 0);
		w.side(10);
		w.side(20);
		BattleTracker.withdraw(w, new IntOpenHashSet(new int[]{10}), new IntOpenHashSet(new int[]{10, 20}));
		assert !w.side(10).withdrew : "one squadron pulled out while another fights on is not a retreat";
		assert w.side(20).withdrew : "every fleet ordered away is";

		//reinforcements: judged on the share of what each side brought, not the raw count
		BattleTracker.Battle c = new BattleTracker.Battle(1, new Vector3i(), 0);
		c.side(10).survivors = true;
		c.side(20).survivors = true;
		assert !BattleTracker.shares(c) : "nothing looked at yet: raw losses";
		c.side(10).lost = 5;
		c.side(10).lostRating = 500;
		c.side(20).lost = 4;
		c.side(20).lostRating = 400;
		assert BattleTracker.favoured(c) == Integer.MIN_VALUE : "raw, 5 to 4 is even";
		BattleTracker.commit(c.side(10), 2000, 20);
		BattleTracker.commit(c.side(20), 600, 6);
		assert c.side(10).committedShips == 25 && c.side(10).committedRating == 2500 : "what is here plus what was lost";
		BattleTracker.commit(c.side(10), 100, 1);
		assert c.side(10).committedShips == 25 && c.side(10).committedRating == 2500 : "ships leaving do not lower it";
		assert BattleTracker.shares(c) && BattleTracker.favoured(c) == 10 : "5 of 25 is a better day than 4 of 10";
		String s = BattleTracker.summarize(c, id -> "F" + id, 60_000);
		assert s.startsWith("Inconclusive; F10 came off better (lost 5 of 25 ships to F20's 4 of 10).") : s;
		c.side(20).lost = 2;
		c.side(20).lostRating = 450;
		s = BattleTracker.summarize(c, id -> "F" + id, 60_000);
		assert s.contains("(lost 20% of its combat rating to F20's 45%)") : "level on ships (a fifth each), rating decides: " + s;
	}

	private static void retreats() {
		Vector3i here = new Vector3i(5, 0, 0);
		Vector3i home = new Vector3i(40, 0, 0);
		BattleTracker.Order sentry = new BattleTracker.Order(FleetCommandTypes.SENTRY, null);
		BattleTracker.Order going = new BattleTracker.Order(FleetCommandTypes.MOVE_FLEET, home);
		assert BattleTracker.retreats(sentry, going, here) : "on sentry, then sent off";
		assert !BattleTracker.retreats(sentry, sentry, here) : "still on sentry";
		assert !BattleTracker.retreats(sentry, new BattleTracker.Order(FleetCommandTypes.FLEET_ATTACK, new Vector3i(here)), here)
				: "a new order to fight here is not leaving";
		assert !BattleTracker.retreats(going, new BattleTracker.Order(FleetCommandTypes.MOVE_FLEET, new Vector3i(home)), here)
				: "arrived bound elsewhere and still is: an encounter, not a retreat";
		assert BattleTracker.retreats(going, new BattleTracker.Order(FleetCommandTypes.MOVE_FLEET, new Vector3i(1, 2, 3)), here)
				: "passing through, then re-ordered somewhere else";
		assert !BattleTracker.retreats(new BattleTracker.Order(FleetCommandTypes.PATROL_FLEET, here),
				new BattleTracker.Order(FleetCommandTypes.PATROL_FLEET, home), here) : "a patrol on to its next waypoint";
	}

	private static void watching() {
		Vector3i battle = new Vector3i(10, 0, 0);
		assert BattleTracker.watched(battle, false, List.of(new Vector3i(10, 0, 0))) : "a player in the sector";
		assert BattleTracker.watched(battle, false, List.of(new Vector3i(9, 1, -1))) : "a player next door keeps it loaded and sees in";
		assert !BattleTracker.watched(battle, false, List.of(new Vector3i(8, 0, 0))) : "two sectors off is not watching";
		assert !BattleTracker.watched(battle, false, List.of()) : "nobody online";
		assert !BattleTracker.watched(battle, true, List.of(new Vector3i(9, 0, 0))) : "a sector players let go is not watched from next door";
		assert BattleTracker.watched(battle, true, List.of(new Vector3i(10, 0, 0))) : "but is by a player inside it";
	}

	private static void paperWinner() {
		Int2DoubleOpenHashMap strength = new Int2DoubleOpenHashMap();
		strength.put(1, 1000);
		strength.put(2, 10);
		strength.put(3, 5000); //neutral to both: watches
		BiPredicate<Integer, Integer> war = (x, y) -> x + y == 3;
		Random rng = new Random(7);
		for (int i = 0; i < 50; i++) {
			assert BattleTracker.pickWinner(strength, war, rng) == 1 : "a 100x edge never loses the roll";
		}
		strength.put(2, 900);
		int ones = 0;
		for (int i = 0; i < 1000; i++) {
			ones += BattleTracker.pickWinner(strength, war, rng) == 1 ? 1 : 0;
		}
		assert ones > 500 && ones < 1000 : "a close fight is a coin weighted to the stronger: " + ones;
		assert BattleTracker.pickWinner(strength, (x, y) -> false, rng) == Integer.MIN_VALUE : "nobody at war, no battle";

		Int2DoubleOpenHashMap unrated = new Int2DoubleOpenHashMap();
		unrated.put(1, 0);
		unrated.put(2, 0);
		assert BattleTracker.pickWinner(unrated, war, rng) == Integer.MIN_VALUE : "nothing rated on either side, nothing to settle";
	}

	private static void restart() {
		BattleTracker before = new BattleTracker();
		long now = System.currentTimeMillis();
		BattleTracker.Battle saved = new BattleTracker.Battle(5, new Vector3i(9, 8, 7), now - 120_000);
		saved.lastHit = now;
		saved.side(10).lost = 2;
		saved.side(10).lostRating = 33.5;
		saved.side(20).hitsTaken = 40;
		saved.units.put("ENTITY_SHIP_A", new BattleReports.Unit("A", 10, (byte) 5, 100, 0.5f, 7, false));
		before.carry(saved, now);

		//paused 10 min into a fight that has been held for an hour since: it has run 10 min, not 70
		BattleTracker.Battle held = new BattleTracker.Battle(6, new Vector3i(1, 2, 3), now - 70 * 60_000);
		held.paused = true;
		held.pausedAt = now - 60 * 60_000;
		before.carry(held, now);

		BattleTracker after = new BattleTracker();
		after.fromTag(before.toTag());
		BattleTracker.Battle back = after.carried().get(new Vector3i(9, 8, 7));
		assert back != null : "the battle survives the restart";
		assert back.side(10).lost == 2 && back.side(10).lostRating == 33.5 && back.side(20).hitsTaken == 40 : "tallies survive";
		assert back.lastHit > 0 : "it had hits, so it is reported when it ends";
		assert Math.abs(System.currentTimeMillis() - back.start - 120_000) < 5_000 : "age is a duration, so downtime does not count";
		assert after.queued().containsKey(new Vector3i(9, 8, 7)) : "its sector is queued so it fights on";
		assert back.units.get("ENTITY_SHIP_A").name().equals("A") : "a quiet round's units survive the restart";
		assert after.open(11, new Vector3i(9, 8, 7), now).units.containsKey("ENTITY_SHIP_A") : "and the next round picks them up";

		BattleTracker.Battle heldBack = after.carried().get(new Vector3i(1, 2, 3));
		assert heldBack.paused : "a pause survives a restart";
		assert Math.abs(System.currentTimeMillis() - heldBack.start - 10 * 60_000) < 5_000 : "time paused is not time fought";
		assert heldBack.lastHit == 0 : "never hit stays never hit";

		//the queue survives too, without doubling up a sector that is also a carried battle
		BattleTracker again = new BattleTracker();
		again.fromTag(after.toTag());
		assert again.queued().size() == 2 : again.queued();

		//a battle nobody fired in is neither carried nor saved: after a restart it would load a sector
		//for nothing, an empty battle
		BattleTracker quiet = new BattleTracker();
		quiet.carry(new BattleTracker.Battle(9, new Vector3i(5, 5, 5), now), now);
		assert quiet.carried().isEmpty() : "nothing happened, nothing to carry";
		quiet.open(9, new Vector3i(5, 5, 5), now);
		BattleTracker reloaded = new BattleTracker();
		reloaded.fromTag(quiet.toTag());
		assert reloaded.queued().isEmpty() && reloaded.carried().isEmpty() : "an unfired battle is not saved";
	}
}
