package videogoose.betterfactions.commands;

import api.chat.ChatCommand;
import api.chat.CommandArg;
import api.chat.CommandContext;
import api.mod.StarLoader;
import com.bulletphysics.linearmath.Transform;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetCommandTypes;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.game.server.data.blueprintnw.BlueprintType;
import videogoose.betterfactions.BetterFactions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Admin tools for testing battles: a fleet of any faction, built from any catalog blueprint, put straight
 * into an unloaded sector the way NPC fleets are, so the battle tracker finds it like any other.
 */
public final class BattleCommands {

	private BattleCommands() {
	}

	/** Most ships one command spawns: past this a typo becomes a server that will not load the sector. */
	static final int MAX_SHIPS = 50;
	/** Room left between ships, in metres, on top of the blueprint's own size. */
	static final float GAP = 40;

	private record Command(String name, String usage, String description, Consumer<CommandContext> run, CommandArg... args) implements ChatCommand {
		@Override
		public String getName() {
			return name;
		}

		//the parser enforces these: a command declaring none refuses any argument
		@Override
		public CommandArg[] getArgs() {
			return args;
		}

		@Override
		public String getDescription() {
			return description + " Usage: /" + name + " " + usage;
		}

		@Override
		public void execute(CommandContext ctx) {
			try {
				run.accept(ctx);
			} catch (RuntimeException e) {
				ctx.reply("Usage: /" + name + " " + usage + " (" + e.getMessage() + ")");
			}
		}
	}

	public static void register(BetterFactions mod) {
		StarLoader.registerCommand(mod, new Command("battle_spawn_fleet", "<factionId> <count> <x> <y> <z> <blueprint name>",
				"Spawns a fleet of one catalog blueprint for a faction in an unloaded sector, on sentry. The fleet is yours to"
						+ " command from the fleet menu; its ships are the faction's.", BattleCommands::spawnFleet,
				CommandArg.integer("factionId", "10001"), CommandArg.integer("count", "3"), CommandArg.integer("x", "2"), CommandArg.integer("y", "2"), CommandArg.integer("z", "2"), CommandArg.greedy("blueprint", "Isanth Type-Zero")));
		StarLoader.registerCommand(mod, new Command("battle_blueprints", "[filter]",
				"Lists catalog ship blueprints, heaviest first, for /battle_spawn_fleet.", BattleCommands::listBlueprints,
				CommandArg.greedy("filter", "isanth").optional()));
	}

	private static void listBlueprints(CommandContext ctx) {
		String filter = String.join(" ", ctx.rawArgs()).trim().toLowerCase(Locale.ENGLISH);
		List<BlueprintEntry> ships = new ArrayList<>();
		for (BlueprintEntry e : BluePrintController.active.readBluePrints()) {
			if (e.getType() == BlueprintType.SHIP && e.getName().toLowerCase(Locale.ENGLISH).contains(filter)) {
				ships.add(e);
			}
		}
		ships.sort(Comparator.comparingDouble(BlueprintEntry::getMass).reversed());
		if (ships.isEmpty()) {
			ctx.reply("No ship blueprints" + (filter.isEmpty() ? "" : " matching '" + filter + "'"));
			return;
		}
		for (BlueprintEntry e : ships.subList(0, Math.min(30, ships.size()))) {
			ctx.reply(Math.round(e.getMass()) + " mass  " + e.getName());
		}
		if (ships.size() > 30) {
			ctx.reply((ships.size() - 30) + " more; narrow it with a filter");
		}
	}

	private static void spawnFleet(CommandContext ctx) {
		String[] raw = ctx.rawArgs();
		if (raw.length < 6) {
			throw new IllegalArgumentException("not enough arguments");
		}
		int factionId = Integer.parseInt(raw[0]);
		int count = Integer.parseInt(raw[1]);
		Vector3i sector = new Vector3i(Integer.parseInt(raw[2]), Integer.parseInt(raw[3]), Integer.parseInt(raw[4]));
		String name = String.join(" ", java.util.Arrays.copyOfRange(raw, 5, raw.length)).trim();

		GameServerState state = GameServerState.instance;
		Faction faction = state.getFactionManager().getFaction(factionId);
		if (faction == null) {
			ctx.reply("[ERROR] faction ID not found: " + factionId);
			return;
		}
		if (count < 1 || count > MAX_SHIPS) {
			ctx.reply("[ERROR] count must be 1 to " + MAX_SHIPS);
			return;
		}
		if (state.getUniverse().isSectorLoaded(sector)) {
			//a database spawn only shows up when the sector next loads, which a loaded one will not do
			ctx.reply("[ERROR] sector " + sector + " is loaded; pick one nobody is near");
			return;
		}
		List<BlueprintEntry> all = BluePrintController.active.readBluePrints();
		BlueprintEntry bp = null;
		for (BlueprintEntry e : all) {
			if (e.getName().equalsIgnoreCase(name)) {
				bp = e;
				break;
			}
		}
		if (bp == null || bp.getType() != BlueprintType.SHIP) {
			ctx.reply("[ERROR] no ship blueprint named '" + name + "'. /battle_blueprints lists them");
			return;
		}

		String owner = ctx.getSender() != null ? ctx.getSender().getName() : "BF_TEST";
		String fleetName = faction.getName() + " test fleet " + (System.currentTimeMillis() % 100000);
		state.getFleetManager().requestCreateFleet(fleetName, owner);
		Fleet fleet = null;
		for (Fleet f : state.getFleetManager().getAvailableFleets(owner)) {
			if (f.getName().equals(fleetName)) {
				fleet = f;
				break;
			}
		}
		if (fleet == null) {
			ctx.reply("[ERROR] the fleet could not be created");
			return;
		}

		float spacing = bp.getBb().maxSize() + GAP;
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			long id = spawnInDatabase(state, bp, all, factionId, sector, slot(i, count, spacing),
					"BFTEST_" + factionId + "_" + System.nanoTime(), faction.getName() + " " + bp.getName() + " " + (i + 1));
			if (id > 0) {
				state.getFleetManager().requestShipAdd(fleet, id);
				spawned++;
			}
		}
		if (spawned == 0) {
			state.getFleetManager().requestFleetRemove(fleet);
			ctx.reply("[ERROR] no ship could be spawned; see the server log");
			return;
		}
		//on sentry it fights whatever comes, and a fleet with orders stays cached while its owner is away
		fleet.sendFleetCommand(FleetCommandTypes.SENTRY);
		ctx.reply("[SUCCESS] " + fleetName + ": " + spawned + " x " + bp.getName() + " (" + Math.round(bp.getMass() * spawned)
				+ " mass) for " + faction.getName() + " at " + sector + ", on sentry. Command it from your fleet menu.");
	}

	/**
	 * Where ship {@code i} of {@code count} sits in the sector: a cube grid centred on the middle, so
	 * nothing spawns inside anything else, and a big fleet stays inside its sector rather than spilling a
	 * flat sheet of ships over the edge.
	 */
	static float[] slot(int i, int count, float spacing) {
		int side = (int) Math.ceil(Math.cbrt(count) - 1e-9);
		float offset = (side - 1) * spacing / 2f;
		return new float[]{(i % side) * spacing - offset, (i / side % side) * spacing - offset, (i / (side * side)) * spacing - offset};
	}

	/** As the NPC fleet manager does it: straight into the database, loaded when its sector next is. */
	private static long spawnInDatabase(GameServerState state, BlueprintEntry bp, List<BlueprintEntry> all, int factionId,
										Vector3i sector, float[] at, String uid, String realName) {
		try {
			Transform t = new Transform();
			t.setIdentity();
			t.origin.set(at[0], at[1], at[2]);
			SegmentControllerOutline<?> outline = BluePrintController.active.loadBluePrint(state, bp.getName(), uid, t, -1, factionId,
					all, sector, null, "<system>", ByteBuffer.allocate(10 * 1024), true, null, new ChildStats(true));
			outline.spawnSectorId = new Vector3i(sector);
			outline.realName = realName;
			return outline.spawnInDatabase(sector, state, 0, new ObjectArrayList<>(), new ChildStats(false), true);
		} catch (Exception e) {
			System.err.println("[BetterFactions] test fleet ship " + uid + " did not spawn: " + e);
			return -1;
		}
	}
}
