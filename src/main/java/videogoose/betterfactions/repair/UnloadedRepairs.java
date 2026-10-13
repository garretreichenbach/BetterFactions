package videogoose.betterfactions.repair;

import api.mod.ModSkeleton;
import api.mod.StarLoader;
import com.bulletphysics.linearmath.Transform;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.controller.elements.shipyard.ShipyardElementManager;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.game.common.data.player.inventory.OfflineInventories;
import org.schema.game.common.data.world.EntityUID;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.schine.common.language.Lng;

/**
 * Shipyard repairs with nobody around: the station and the ships at it are unloaded, so the yard's own machine
 * cannot run. The repair is done on paper to the same result as the yard's: after the yard's own deconstruct
 * and build time, the shipyard computer's inventory pays the blueprint's components less the old ship's (which a
 * deconstruction would have given back), the old ship is deleted, and the blueprint is spawned in its
 * place, straight into the database, and put back into its fleet.
 *
 * <p>Nothing is paid until the end, so when the area loads part-way through, the ship simply goes back to the
 * normal loaded repair. The old ship's blocks are the counts it had when it last unloaded ({@link
 * ShipyardRepairs#onUnload}); one never seen loaded since it joined a fleet pays the whole blueprint.</p>
 *
 * <p>Production feeding the yard while it is unloaded is Resources Reorganized's, when it is installed: its
 * factories write into the station's save, and the station is opened through it so they are caught up first.</p>
 */
final class UnloadedRepairs {

	private static final String RR_ID = "videogoose.resourcesreorganized";
	private static final String RR_OFFLINE = "videogoose.resourcesreorganized.logistics.offline.OfflineProcessing";
	private static final long PAY_RETRY_MS = 10000;
	private static boolean rrWarned;

	private UnloadedRepairs() {
	}

	/** One yard's unloaded work this tick. The station is not loaded. */
	static void update(ShipyardRepairs r, GameServerState state, ShipyardRepairs.Yard y, long now) {
		long dt = y.lastOffline == 0 ? 0 : Math.min(now - y.lastOffline, 5000);
		y.lastOffline = now;
		Long yardIndex = r.yardIndexByStation.get(y.stationUid);
		if (yardIndex == null) {
			y.status = Lng.str("Station not loaded, and its shipyard has not been seen yet; repairs start once it loads.");
			return;
		}
		ShipyardRepairs.Entry active = null;
		for (ShipyardRepairs.Entry e : y.queue) {
			if (e.step != ShipyardRepairs.Step.EN_ROUTE && e.step != ShipyardRepairs.Step.OFFLINE) {
				y.status = Lng.str("Station not loaded; %s will finish when it loads.", e.name);
				return; //a loaded repair stopped mid-way by an unload: the yard's own machine finishes it
			}
			if (e.step == ShipyardRepairs.Step.OFFLINE) {
				active = e;
				break;
			}
		}
		if (active == null) {
			for (ShipyardRepairs.Entry e : y.queue) {
				FleetMember m = member(state, e);
				if (m != null && !m.isLoaded() && y.sector.equals(m.getSector())) {
					BlueprintEntry bp = blueprint(e.blueprint);
					if (bp == null) {
						r.drop(state, y, e, Lng.str("Blueprint %s for %s no longer exists.", e.blueprint, e.name));
						return;
					}
					ElementCountMap had = r.counts.get(e.uid);
					e.step(ShipyardRepairs.Step.OFFLINE);
					e.offlineMs = 0;
					e.offlineTotal = ShipyardElementManager.DECONSTRUCTION_CONST_TIME_MS
							+ (long) ((had != null ? had.getTotalAmount() : 0) * ShipyardElementManager.DECONSTRUCTION_MS_PER_BLOCK)
							+ (long) (ShipyardElementManager.CONSTRUCTION_TICK_IN_SECONDS * 1000);
					active = e;
					break;
				}
			}
		}
		if (active == null) {
			y.status = Lng.str("Station not loaded; waiting for ships to arrive.");
			return;
		}
		ShipyardRepairs.Entry e = active;
		Fleet fleet = state.getFleetManager().getByFleetDbId(e.fleetDbId);
		FleetMember m = member(state, e);
		if (fleet == null || m == null || !y.stillOrdered(fleet)) {
			r.drop(state, y, e, fleet == null || m == null ? null : Lng.str("%s was given another order; %s left the repair queue.", fleet.getName(), e.name));
			return;
		}
		if (m.isLoaded()) {
			e.step(ShipyardRepairs.Step.EN_ROUTE); //nothing paid yet: the loaded repair takes it from here
			return;
		}
		e.offlineMs += dt;
		y.status = Lng.str("Station not loaded; repairing on paper.");
		if (e.offlineMs < e.offlineTotal || now < e.nextPayAttempt) {
			return;
		}
		e.nextPayAttempt = now + PAY_RETRY_MS;
		finish(r, state, y, e, fleet, m, yardIndex);
	}

	private static void finish(ShipyardRepairs r, GameServerState state, ShipyardRepairs.Yard y, ShipyardRepairs.Entry e,
	                           Fleet fleet, FleetMember m, long yardIndex) {
		BlueprintEntry bp = blueprint(e.blueprint);
		if (bp == null) {
			r.drop(state, y, e, Lng.str("Blueprint %s for %s no longer exists.", e.blueprint, e.name));
			return;
		}
		ElementCountMap pull = new ElementCountMap();
		ElementCountMap refund = new ElementCountMap();
		ElementCountMap had = r.counts.get(e.uid);
		cost(RepairComponents.components(bp.getElementCountMapWithChildren()), had == null ? null : RepairComponents.components(had), pull, refund);
		if (!ShipyardRepairs.free) {
			if (!pay(r, y, e, pull, refund, yardIndex)) {
				return;
			}
		}
		swap(r, state, y, e, fleet, m, bp);
	}

	/** Takes the repair's cost out of the unloaded station's shipyard. @return false to try again later */
	private static boolean pay(ShipyardRepairs r, ShipyardRepairs.Yard y, ShipyardRepairs.Entry e, ElementCountMap pull, ElementCountMap refund, long yardIndex) {
		OfflineInventories.OfflineEntity station = open(y.stationUid);
		if (station == null) {
			return false; //loaded or being saved this instant: try again shortly
		}
		Inventory inv = station.getInventory(yardIndex);
		if (inv == null) {
			r.yardIndexByStation.remove(y.stationUid);
			y.status = Lng.str("The shipyard computer is gone from the station.");
			return false;
		}
		String missing = missing(inv, pull);
		if (!missing.isEmpty()) {
			y.status = Lng.str("Station not loaded; repair of %s is waiting for components in the shipyard: %s", e.name, missing);
			return false;
		}
		try {
			inv.decreaseBatch(pull, new IntOpenHashSet());
			for (short t : ElementKeyMap.typeList()) {
				if (refund.get(t) > 0) {
					inv.incExistingOrNextFreeSlotWithoutException(t, refund.get(t));
				}
			}
			return station.save(); //false: loaded while we worked, nothing was written, and the loaded repair takes over
		} catch (Exception ex) {
			System.err.println("[BetterFactions] unloaded repair of " + e.name + " could not pay: " + ex);
			return false;
		}
	}

	/** The old ship out, the blueprint in its place and its fleet. */
	private static void swap(ShipyardRepairs r, GameServerState state, ShipyardRepairs.Yard y, ShipyardRepairs.Entry e,
	                         Fleet fleet, FleetMember m, BlueprintEntry bp) {
		DatabaseEntry old;
		try {
			old = state.getDatabaseIndex().getTableManager().getEntityTable().getById(m.entityDbId);
		} catch (Exception ex) {
			old = null;
		}
		if (old == null) {
			r.drop(state, y, e, Lng.str("%s could not be found to repair.", e.name));
			return;
		}
		long newId = spawn(state, bp, e, old, y.faction);
		if (newId < 0) {
			r.drop(state, y, e, Lng.str("The repair of %s failed to build the new ship; the components are spent. Tell an admin.", e.name));
			return;
		}
		for (EntityUID docked : state.getDatabaseIndex().getTableManager().getEntityTable().loadByDockedEntity(old.dbId)) {
			if (docked.id != old.dbId) {
				state.destroyEntity(docked.id);
			}
		}
		state.destroyEntity(old.dbId); //also takes it out of its fleet

		String newUid = e.uid;
		try {
			DatabaseEntry made = state.getDatabaseIndex().getTableManager().getEntityTable().getById(newId);
			newUid = made != null ? made.uid : e.uid;
		} catch (Exception ignored) {
		}
		r.counts.remove(e.uid);
		FactionDesigns.server().moved(y.faction, e.uid, newUid);
		ShipyardRepairs.addToFleet(state, fleet, newId, e.fleetIndex);
		r.drop(state, y, e, Lng.str("%s repaired at %s.", e.name, y.stationName));
	}

	/**
	 * What a repair costs the yard, in components ({@link RepairComponents}, the same as a loaded repair): the
	 * blueprint's less the old ship's, which a deconstruction gives back. Where the old ship had more of one than
	 * the blueprint, the rest is refunded.
	 */
	static void cost(ElementCountMap blueprint, ElementCountMap old, ElementCountMap pull, ElementCountMap refund) {
		for (short t : ElementKeyMap.typeList()) {
			int n = blueprint.get(t) - (old != null ? old.get(t) : 0);
			if (n > 0) {
				pull.inc(t, n);
			} else if (n < 0) {
				refund.inc(t, -n);
			}
		}
	}

	private static String missing(Inventory inv, ElementCountMap pull) {
		StringBuilder b = new StringBuilder();
		int kinds = 0;
		for (short t : ElementKeyMap.typeList()) {
			int lack = pull.get(t) - inv.getOverallQuantity(t);
			if (lack > 0 && kinds++ < 5) {
				b.append(b.length() == 0 ? "" : ", ").append(lack).append(' ').append(ElementKeyMap.getInfo(t).getName());
			}
		}
		return kinds > 5 ? b + Lng.str(" and %s more", kinds - 5) : b.toString();
	}

	/** The rebuilt ship, in the database where the old one was. @return its id, or -1 */
	private static long spawn(GameServerState state, BlueprintEntry bp, ShipyardRepairs.Entry e, DatabaseEntry old, int faction) {
		Transform t = new Transform();
		t.setIdentity();
		if (old.pos != null) {
			t.origin.set(old.pos);
		}
		try {
			SegmentControllerOutline<?> o = BluePrintController.active.loadBluePrint(state, "BFR" + System.currentTimeMillis(), t, -1, faction, bp,
					new Vector3i(old.sectorPos), null, "<system>", PlayerState.buffer, true, null, new ChildStats(false));
			o.spawnSectorId = new Vector3i(old.sectorPos);
			o.realName = e.name;
			return o.spawnInDatabase(new Vector3i(old.sectorPos), state, 0, new ObjectArrayList<>(), new ChildStats(false), false);
		} catch (Exception ex) {
			System.err.println("[BetterFactions] unloaded repair could not spawn " + e.blueprint + ": " + ex);
			ex.printStackTrace();
			return -1;
		}
	}

	static BlueprintEntry blueprint(String name) {
		try {
			return BluePrintController.active.getBlueprint(name);
		} catch (Exception ex) {
			return null;
		}
	}

	private static FleetMember member(GameServerState state, ShipyardRepairs.Entry e) {
		Fleet fleet = state.getFleetManager().getByFleetDbId(e.fleetDbId);
		if (fleet != null) {
			for (FleetMember m : fleet.getMembers()) {
				if (m.UID.equals(e.uid)) {
					return m;
				}
			}
		}
		return null;
	}

	/**
	 * The station's save, opened through Resources Reorganized when it is installed so its factories are caught up
	 * first; otherwise straight from the engine. Null while it is loaded or being saved.
	 */
	private static OfflineInventories.OfflineEntity open(String uid) {
		ModSkeleton rr = StarLoader.getModFromModId(RR_ID);
		if (rr != null && rr.isEnabled()) {
			try {
				Class<?> c = Class.forName(RR_OFFLINE, true, rr.getClassLoader());
				return (OfflineInventories.OfflineEntity) c.getMethod("open", String.class).invoke(null, uid);
			} catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
				if (!rrWarned) {
					rrWarned = true;
					System.err.println("[BetterFactions] Resources Reorganized is installed but its offline processing could not be used: " + ex);
				}
			}
		}
		return OfflineInventories.open(uid);
	}
}
