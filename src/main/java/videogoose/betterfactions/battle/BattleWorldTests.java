package videogoose.betterfactions.battle;

import com.google.gson.JsonObject;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.ai.AIConfiguationElements;
import org.schema.game.common.controller.ai.Types;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetCommandTypes;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.network.objects.remote.FleetCommand;
import org.schema.game.server.ai.program.common.TargetProgram;
import org.schema.schine.ai.AiEntityStateInterface;
import org.schema.schine.ai.stateMachines.State;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.test.world.WorldTestExtension;
import org.schema.game.server.test.world.WorldTestSpec;
import videogoose.betterfactions.diplomacy.CombatRatings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Teaches the engine's in-world tests ({@code /run_tests betterfactions/*}) about battles. The tests
 * themselves are {@code tests/*.json} in the mod jar.
 *
 * <p>Setup key {@code "battle"}, on any entity or docked child, entries without {@code "at"}. Each runs
 * when setup is done, or once {@code "atTick"} server ticks have passed and, with {@code "afterHits": n},
 * the battle in that entity's sector has taken {@code n} hits. A turret takes anywhere from seconds to over
 * a minute to find its first target, so anything that needs a battle under way waits on hits, not time.
 * An action still waiting after {@link #GIVE_UP_TICKS} is dropped with its test.</p>
 * <ul>
 *   <li>{@code { "ai": "Turret", "aimAt": "Any" }}: turns that ship's AI on ({@code "Ship"} or
 *       {@code "Turret"}). A docked turret has its host's faction: the engine's test framework spawns
 *       docked children in their parent's faction. With {@code "debug": true} the ship's AI is logged
 *       ({@code [BetterFactions][WorldTest]} in the server log) whenever its state, target or faction
 *       changes, for a test whose ship never fires.</li>
 *   <li>{@code { "fleetOrder": "SENTRY", "fleet": "guards" }}: a {@code FleetCommandTypes} order to one of
 *       the test's fleets. Works while the fleet is unloaded. With {@code "target": "depot"} the order is
 *       given that entity's sector, for orders that take one ({@code FLEET_ATTACK}, {@code FLEET_DEFEND},
 *       {@code MOVE_FLEET}).</li>
 *   <li>{@code { "command": "pause" }} or {@code "end"}: the admin profiler's button, on the battle in that
 *       entity's sector.</li>
 * </ul>
 *
 * <p>Expect key {@code "battle"}, naming an entity whose sector is checked, with a {@code "measure"}:
 * {@code active}, {@code soft}, {@code paused}, {@code loaded} and {@code queued} (each 1 or 0),
 * {@code hits} (hits taken, all sides), {@code lost} (ships lost, all sides), {@code pausedFor} (seconds),
 * {@code reported} (news reports for that sector this run), and, for the entity itself, {@code gate} (its
 * reactor gate, 0 to 1) and {@code rate} (its combat rating now).</p>
 */
public final class BattleWorldTests implements WorldTestExtension {

	static final String KEY = "battle";

	/** Longer than any test runs: an action whose condition never came is dropped after this. */
	static final int GIVE_UP_TICKS = 30_000;

	private record Pending(int atTick, int giveUp, BooleanSupplier when, Runnable action) {
	}

	/** Main thread only: setup applies and the server tick drains. */
	private static final List<Pending> pending = new ArrayList<>();
	/** Ships whose AI is logged, to the last line logged for each. */
	private static final Map<Ship, String> traced = new HashMap<>();

	@Override
	public String setupKey() {
		return KEY;
	}

	@Override
	public String expectKey() {
		return KEY;
	}

	@Override
	public void apply(SegmentController ship, long index, WorldTestSpec.Extra entry, Scene scene) {
		JsonObject json = entry.json();
		Runnable action;
		if (json.has("ai")) {
			String type = json.get("ai").getAsString();
			String aimAt = json.has("aimAt") ? json.get("aimAt").getAsString() : "Any";
			boolean debug = json.has("debug") && json.get("debug").getAsBoolean();
			action = () -> {
				enableAi(ship, type, aimAt);
				if (debug && ship instanceof Ship s) {
					traced.put(s, "");
				}
			};
		} else if (json.has("fleetOrder")) {
			FleetCommandTypes order = FleetCommandTypes.valueOf(json.get("fleetOrder").getAsString());
			String id = json.get("fleet").getAsString();
			Fleet fleet = scene.fleets().get(id);
			if (fleet == null) {
				throw new IllegalArgumentException("battle setup: no fleet '" + id + "'");
			}
			if (json.has("target")) {
				String targetId = json.get("target").getAsString();
				SegmentController target = scene.entities().get(targetId);
				if (target == null) {
					throw new IllegalArgumentException("battle setup: no entity '" + targetId + "'");
				}
				action = () -> fleet.getFleetManager().executeCommand(new FleetCommand(order, fleet, new Vector3i(sectorOf(target))));
			} else {
				action = () -> fleet.getFleetManager().executeCommand(new FleetCommand(order, fleet));
			}
		} else if (json.has("command")) {
			String command = json.get("command").getAsString();
			action = () -> command(ship, command);
		} else {
			throw new IllegalArgumentException("battle setup needs \"ai\", \"fleetOrder\" or \"command\": " + json);
		}
		int now = BattleTracker.ticks();
		int atTick = now + (json.has("atTick") ? json.get("atTick").getAsInt() : 0);
		BooleanSupplier when = () -> true;
		if (json.has("afterHits")) {
			int hits = json.get("afterHits").getAsInt();
			when = () -> hitsAt(ship) >= hits;
		}
		if (atTick <= now && !json.has("afterHits")) {
			action.run();
		} else {
			pending.add(new Pending(atTick, now + GIVE_UP_TICKS, when, action));
		}
	}

	/** Server tick: runs the setup actions that are due, and logs traced AI that changed. */
	public static void tick() {
		trace();
		Iterator<Pending> it = pending.iterator();
		while (it.hasNext()) {
			Pending p = it.next();
			if (BattleTracker.ticks() >= p.giveUp) {
				it.remove();
			} else if (BattleTracker.ticks() >= p.atTick && p.when.getAsBoolean()) {
				it.remove();
				try {
					p.action.run();
				} catch (Exception e) {
					System.err.println("[BetterFactions] world test action failed: " + e);
				}
			}
		}
	}

	private static void trace() {
		if (traced.isEmpty()) {
			return;
		}
		GameServerState state = GameServerState.instance;
		Iterator<Map.Entry<Ship, String>> it = traced.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Ship, String> e = it.next();
			Ship ship = e.getKey();
			if (!state.getLocalAndRemoteObjectContainer().getLocalObjects().containsKey(ship.getId())) {
				it.remove(); //its test ended
				continue;
			}
			AiEntityStateInterface ai = ship.getAiConfiguration().getAiEntityState();
			State st = ai == null ? null : ai.getStateCurrent();
			Object target = ai != null && ai.getCurrentProgram() instanceof TargetProgram<?> tp ? tp.getTarget() : null;
			String targetDesc = "none";
			if (target instanceof SimpleTransformableSendableObject<?> t) {
				targetDesc = t + " faction " + t.getFactionId() + (state.getFactionManager().isEnemy(ship.getFactionId(), t.getFactionId()) ? " (enemy)" : " (not enemy)");
			} else if (target != null) {
				targetDesc = target.toString();
			}
			String line = "faction " + ship.getFactionId() + " root faction " + ship.railController.getRoot().getFactionId()
					+ " sector " + sectorOf(ship) + " active " + (ai != null && ai.isActive())
					+ " state " + (st == null ? null : st.getClass().getSimpleName()) + " target " + targetDesc;
			if (!line.equals(e.getValue())) {
				e.setValue(line);
				System.err.println("[BetterFactions][WorldTest] " + ship.getName() + ": " + line);
			}
		}
	}

	/** Hits taken in the battle where {@code ship} is, all sides; 0 with no battle. */
	private static int hitsAt(SegmentController ship) {
		Vector3i pos = sectorOf(ship);
		BattleTracker.Battle b = pos == null ? null : BattleTracker.get().battleAt(pos);
		return b == null ? 0 : b.sides.values().stream().mapToInt(s -> s.hitsTaken).sum();
	}

	private static void enableAi(SegmentController ship, String type, String aimAt) {
		if (!(ship instanceof Ship s)) {
			throw new IllegalArgumentException("battle setup: \"ai\" needs a ship, not " + ship);
		}
		var ai = s.getAiConfiguration();
		((AIConfiguationElements) ai.get(Types.TYPE)).setCurrentState(type, true);
		((AIConfiguationElements) ai.get(Types.AIM_AT_PRIORITY)).setCurrentState(aimAt, true);
		((AIConfiguationElements) ai.get(Types.AIM_AT)).setCurrentState(aimAt.split(",")[0].trim(), true);
		((AIConfiguationElements) ai.get(Types.ACTIVE)).setCurrentState(true, true);
		ai.applyServerSettings();
	}

	private static void command(SegmentController in, String command) {
		GameServerState state = GameServerState.instance;
		BattleTracker tracker = BattleTracker.get();
		BattleTracker.Battle b = tracker.battleAt(sectorOf(in));
		if (b == null) {
			System.err.println("[BetterFactions] world test: no battle to " + command + " at " + sectorOf(in));
			return;
		}
		switch (command) {
			case "pause" -> BattleTracker.togglePause(b, System.currentTimeMillis());
			case "end" -> tracker.endByAdmin(state, b, System.currentTimeMillis());
			default -> throw new IllegalArgumentException("battle setup: unknown command '" + command + "'");
		}
	}

	@Override
	public double measure(SegmentController ship, long index, WorldTestSpec.Expect expect, Scene scene) {
		JsonObject json = expect.json();
		String id = json.get(KEY).getAsString();
		SegmentController entity = scene.entities().get(id);
		if (entity == null) {
			throw new IllegalArgumentException("battle expect: no entity '" + id + "'");
		}
		String measure = json.get("measure").getAsString();
		switch (measure) {
			case "gate":
				return CombatRatings.reactorGate(entity);
			case "rate":
				return CombatRatings.rate(entity);
			default:
				break;
		}
		Vector3i pos = sectorOf(entity);
		if (pos == null) {
			return Double.NaN;
		}
		BattleTracker tracker = BattleTracker.get();
		BattleTracker.Battle b = tracker.battleAt(pos);
		return switch (measure) {
			case "active" -> b != null ? 1 : 0;
			case "soft" -> b != null && BattleTracker.isSoftLoaded(b.sectorId) ? 1 : 0;
			case "paused" -> b != null && b.paused ? 1 : 0;
			case "pausedFor" -> b != null && b.paused ? (System.currentTimeMillis() - b.pausedAt) / 1000.0 : 0;
			case "hits" -> hitsAt(entity);
			case "lost" -> b == null ? 0 : b.sides.values().stream().mapToInt(s -> s.lost).sum();
			case "loaded" -> GameServerState.instance.getUniverse().isSectorLoaded(pos) ? 1 : 0;
			case "queued" -> tracker.queued().containsKey(pos) ? 1 : 0;
			case "reported" -> tracker.reportsAt(pos);
			default -> throw new IllegalArgumentException("unknown battle measure '" + measure + "'");
		};
	}

	/** Where an entity is now: its loaded sector, else its database row. */
	static Vector3i sectorOf(SegmentController c) {
		GameServerState state = GameServerState.instance;
		Sector loaded = state.getLocalAndRemoteObjectContainer().getLocalObjects().containsKey(c.getId()) ? state.getUniverse().getSector(c.getSectorId()) : null;
		if (loaded != null) {
			return loaded.pos;
		}
		try {
			List<DatabaseEntry> rows = state.getDatabaseIndex().getTableManager().getEntityTable().getByUIDExact(DatabaseEntry.removePrefixWOException(c.getUniqueIdentifier()), 1);
			return rows.isEmpty() ? null : rows.get(0).sectorPos;
		} catch (Exception e) {
			return null;
		}
	}
}
