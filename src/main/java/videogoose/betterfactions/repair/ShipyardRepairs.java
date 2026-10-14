package videogoose.betterfactions.repair;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.rails.RailRelation;
import org.schema.game.common.controller.rails.RailRelationVars;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.controller.elements.ShipyardManagerContainerInterface;
import org.schema.game.common.controller.elements.shipyard.ShipyardCollectionManager;
import org.schema.game.common.controller.elements.shipyard.ShipyardCollectionManager.ShipyardCommandType;
import org.schema.game.common.controller.elements.shipyard.orders.states.DesignLoaded;
import org.schema.game.common.controller.elements.shipyard.orders.states.NormalShipLoaded;
import org.schema.game.common.controller.elements.shipyard.orders.states.ShipyardState;
import org.schema.game.common.controller.elements.shipyard.orders.states.WaitingForShipyardOrder;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.meta.VirtualBlueprintMetaItem;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetCommandTypes;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.world.SegmentData;
import org.schema.game.network.objects.remote.FleetCommand;
import org.schema.game.server.ai.ShipAIEntity;
import org.schema.game.server.ai.program.common.states.AIDocking;
import org.schema.schine.ai.stateMachines.FSMException;
import org.schema.schine.ai.stateMachines.Transition;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.fleet.FleetLabels;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.*;

/**
 * Server: fleets ordered to repair at one of their faction's shipyards. The fleet flies to the yard's sector,
 * each ship lines up in front of the yard, and one at a time flies in, is rebuilt from its blueprint by the
 * yard's own repair order, and flies out the far end.
 *
 * <p>The repair is the engine's: it deconstructs the ship and builds a new one from a design in the yard's
 * inventory. So a ship comes back as a new entity, and this puts it back into its fleet where the old one was.
 * The design is made from the ship's blueprint (see {@link FactionDesigns}) the first time it is needed, while
 * the yard is empty, and left in the yard for the next ship built to it.</p>
 *
 * <p>The flying is {@code AIDocking} with its steering taken over - see {@code ShipyardDockingMixin}, which
 * reads the {@link Approach} each ship is given here.</p>
 *
 * <p>The queue is saved with the world; see {@link #fromTag} for what a ship mid-way through it does on a load.</p>
 */
public final class ShipyardRepairs {

	private static ShipyardRepairs instance = new ShipyardRepairs();
	/** Admin debugging (/shipyard_repairs_free): repairs cost nothing and refund nothing. Not saved. */
	public static volatile boolean free;

	/** Prefix of the designs this makes, so a yard's own hand-made designs are never mistaken for one. */
	static final String DESIGN_PREFIX = "Repair: ";
	/** A station loaded this long with no working shipyard on it is not going to grow one. */
	private static final long NO_YARD_GIVE_UP_MS = 60000;
	/** A repaired ship gets this long to clear the yard before the next one is let in regardless. */
	private static final long LEAVE_TIMEOUT_MS = 45000;

	public enum Step {
		EN_ROUTE(Lng.str("En route")), HOLDING(Lng.str("Waiting for its turn")), DESIGN(Lng.str("Preparing design")),
		DOCKING(Lng.str("Docking")), REPAIRING(Lng.str("Repairing")), LEAVING(Lng.str("Leaving")),
		/** Station and ship unloaded: see {@link UnloadedRepairs}. Last, so saved ordinals stay valid. */
		OFFLINE(Lng.str("Repairing (unloaded)"));

		final String label;

		Step(String label) {
			this.label = label;
		}
	}

	/** DOCK: in onto the anchor; EXIT: out past the far end; PARK: a fleet's flagship waiting off to the side. */
	public enum Phase {DOCK, EXIT, PARK}

	/**
	 * Where one ship is being flown, read every AI update by the docking mixin. Positions are worked out in the
	 * yard's block space, along the yard's stacking axis through its anchor, so they follow the station around.
	 */
	public static final class Approach {
		/** Within this of the yard entrance the yard takes the ship over from its AI and leads it in. */
		public static final float CAPTURE = 60;
		/** Lead speed cap in m/s, and the acceleration it eases on and off it with, in m/s². */
		static final float LEAD_SPEED = 20, LEAD_ACCEL = 4;
		/** Turn rate cap in rad/s while being lined up, and its angular acceleration. */
		static final float TURN_RATE = 0.5f, TURN_ACCEL = 0.5f;
		/** How far beside the yard fleets park, and how much further out each next fleet does, in metres. */
		static final float PARK_DISTANCE = 400, PARK_SPACING = 500;

		public final SegmentController yard;
		final ShipyardCollectionManager scm;
		final long anchor;
		final int axis;
		/** +1: ships come in off the yard's max end along the axis, -1: off its min end. They leave the other way. */
		final int end;
		volatile Phase phase;
		volatile boolean arrived;
		/** Last distance the ship had to go on its current leg, for the queue view. */
		volatile float lastDist = -1;
		/** The yard has it (within {@link #CAPTURE} of the entrance, or leaving): flown by {@link #lead}, not its AI. */
		volatile boolean captured;
		/** Lined up at the entrance, turned as it will sit docked: going straight in. */
		volatile boolean staged;
		boolean attached;
		/** What is in its way while it is held, for the queue view; null while moving. */
		volatile String blockedBy;
		private long lastLead;

		Approach(SegmentController yard, ShipyardCollectionManager scm, long anchor, int axis, int end, Phase phase) {
			this.yard = yard;
			this.scm = scm;
			this.anchor = anchor;
			this.axis = axis;
			this.end = end;
			this.phase = phase;
		}

		public Phase phase() {
			return phase;
		}

		public void measured(float dist) {
			lastDist = dist;
		}

		/** Out past the far end of the yard: the next ship may come in. */
		public void arrived() {
			arrived = true;
		}

		/** Which leg the ship is on: its AI flying it to the entrance, lining up, going in, or leaving. */
		public int leg() {
			return phase == Phase.EXIT ? 3 : !captured ? 0 : staged ? 2 : 1;
		}

		/** @return true once the yard flies the ship: leaving, or come within {@link #CAPTURE} of the entrance */
		public boolean leading(float distToEntrance) {
			if (phase == Phase.PARK) {
				return false; //its own AI holds it at the spot
			}
			if (phase == Phase.EXIT || distToEntrance < CAPTURE) {
				captured = true;
			}
			return captured;
		}

		/** The world point on the yard's axis through the anchor, clear of the end ships come in by (or leave by). */
		public Vector3f entrance(Ship ship) {
			return phase == Phase.PARK ? parkingSpot() : gate(ship, phase != Phase.EXIT);
		}

		/** Which fleet's spot this is, when parking: each fleet waits further out than the one before. */
		int slot;

		/**
		 * Beside the yard, off its side across the axis, so neither the way in nor the way out runs through the fleet.
		 * ponytail: fixed distances; size them from the fleets' formations if big fleets still crowd the yard.
		 */
		private Vector3f parkingSpot() {
			Vector3i min = new Vector3i(), max = new Vector3i();
			scm.getYardBounds(min, max);
			int side = (axis + 1) % 3;
			Vector3f p = new Vector3f();
			ElementCollection.getPosFromIndex(anchor, p);
			setCoord(p, side, max.getCoord(side) + PARK_DISTANCE + slot * PARK_SPACING);
			toWorld(p);
			return p;
		}

		private Vector3f gate(Ship ship, boolean in) {
			Vector3i min = new Vector3i(), max = new Vector3i();
			scm.getYardBounds(min, max);
			float margin = 2 * halfSize(ship) + 8; //clear of the yard whichever way the ship is turned about its core
			Vector3f p = new Vector3f();
			ElementCollection.getPosFromIndex(anchor, p);
			setCoord(p, axis, in == end > 0 ? max.getCoord(axis) + margin : min.getCoord(axis) - margin);
			toWorld(p);
			return p;
		}

		/** Where the ship sits once docked: core on the anchor, turned as the anchor faces (as RailRelation places it). */
		private Transform dockedTransform() {
			SegmentPiece p = yard.getSegmentBuffer().getPointUnsave(anchor);
			if (p == null) {
				return null;
			}
			Transform t = new Transform(yard.getWorldTransform());
			t.mul(RailRelation.getTrans(p, new Transform(), false, new RailRelationVars()));
			return t;
		}

		/**
		 * Flies the ship by setting its velocities, as a tractor beam does, so it neither needs nor fights its own
		 * thrusters: onto the yard axis at the entrance while turning it to how it will sit docked, then straight in,
		 * attaching once its core is on the anchor; or, leaving, straight out past the far end. Every move eases on
		 * and off, and by the time it attaches there is nothing left to snap.
		 *
		 * @return how far the ship still has to go on this leg
		 */
		public float lead(Ship ship, RigidBody body) {
			long now = System.nanoTime();
			float dt = lastLead == 0 ? 0.03f : Math.min(0.1f, (now - lastLead) / 1.0e9f);
			lastLead = now;
			Transform at = ship.getWorldTransform();
			Vector3f target;
			Quat4f facing = new Quat4f();
			if (phase == Phase.EXIT) {
				target = gate(ship, false);
				at.getRotation(facing); //keeps the heading it undocked with
			} else {
				Transform docked = dockedTransform();
				if (docked == null) {
					return lastDist; //anchor's chunk not loaded yet
				}
				docked.getRotation(facing);
				target = staged ? new Vector3f(docked.origin) : gate(ship, true);
			}
			Vector3f d = new Vector3f();
			d.sub(target, at.origin);
			float dist = d.length();
			body.activate();
			float angle = turn(at, facing, body, dt);
			if (phase == Phase.EXIT) {
				if (dist < 2) {
					arrived();
				}
			} else if (!staged) {
				staged = dist < 1.5f && angle < 0.05f;
			} else if (dist < 0.3f && angle < 0.02f) {
				body.setLinearVelocity(new Vector3f());
				body.setAngularVelocity(new Vector3f());
				attach(ship);
				return dist;
			}
			Vector3f want = new Vector3f();
			blockedBy = blocker(ship, target);
			if (blockedBy == null && dist > 0.001f) {
				want.scale(Math.min(LEAD_SPEED, (float) Math.sqrt(2 * LEAD_ACCEL * dist)) / dist, d);
			}
			body.setLinearVelocity(towards(body.getLinearVelocity(new Vector3f()), want, 2 * LEAD_ACCEL * dt));
			return dist;
		}

		/** Turns the body towards {@code facing}, easing on and off. @return the angle still to turn, in radians */
		private static float turn(Transform at, Quat4f facing, RigidBody body, float dt) {
			Quat4f now = new Quat4f();
			at.getRotation(now);
			Quat4f err = new Quat4f();
			err.mulInverse(facing, now); //world-frame rotation from where it faces to where it should
			if (err.w < 0) {
				err.negate(); //the short way round
			}
			float angle = 2 * (float) Math.acos(Math.min(1, err.w));
			Vector3f want = new Vector3f(err.x, err.y, err.z);
			float s = want.length();
			if (s > 1.0e-6f) {
				want.scale(Math.min(TURN_RATE, (float) Math.sqrt(2 * TURN_ACCEL * angle)) / s);
			} else {
				want.set(0, 0, 0);
			}
			body.setAngularVelocity(towards(body.getAngularVelocity(new Vector3f()), want, 2 * TURN_ACCEL * dt));
			return angle;
		}

		/**
		 * The first entity, other than the yard and anything docked to it or to the ship, that sits in the corridor between
		 * the ship and where it is being led: the ship holds until it clears rather than being flown into it.
		 * ponytail: bounding spheres, so a big entity just off the axis can hold it too; swept boxes if that bites.
		 */
		private String blocker(Ship ship, Vector3f target) {
			Vector3f from = ship.getWorldTransform().origin;
			float r = halfSize(ship) + 5;
			Vector3f ahead = new Vector3f(), to = new Vector3f();
			ahead.sub(target, from);
			for (Object o : ship.getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
				if (o instanceof SegmentController c && c.getSectorId() == ship.getSectorId() && !c.isMarkedForPermanentDelete()) {
					SegmentController root = c.railController.getRoot();
					to.sub(c.getWorldTransform().origin, from);
					if (root != yard && root != ship && to.dot(ahead) > 0 //behind it is not in its way
							&& distanceToSegment(c.getWorldTransform().origin, from, target) < r + halfSize(c)) {
						return c.getRealName();
					}
				}
			}
			return null;
		}

		/** {@code from} moved towards {@code to} by at most {@code step}. */
		static Vector3f towards(Vector3f from, Vector3f to, float step) {
			Vector3f d = new Vector3f();
			d.sub(to, from);
			float l = d.length();
			if (l > step) {
				d.scale(step / l);
			}
			from.add(d);
			return from;
		}

		/** Core on the anchor: dock it there the way the yard docks its own designs. */
		public void attach(Ship ship) {
			if (!attached) {
				attached = scm.createDockingRelation(ship, false);
			}
		}

		private void toWorld(Vector3f blockPos) {
			blockPos.x -= SegmentData.SEG_HALF;
			blockPos.y -= SegmentData.SEG_HALF;
			blockPos.z -= SegmentData.SEG_HALF;
			Transform t = yard.getWorldTransform();
			t.transform(blockPos);
		}
	}

	static final class Entry {
		final long fleetDbId;
		final int fleetIndex;
		String uid;
		final String name;
		final String blueprint;
		Step step = Step.EN_ROUTE;
		long stepSince = System.currentTimeMillis();
		long oldDbId;
		int designId = -1;
		boolean sawWork;
		Approach approach;
		long offlineMs;
		long offlineTotal;
		long nextPayAttempt;

		Entry(long fleetDbId, int fleetIndex, String uid, String name, String blueprint) {
			this.fleetDbId = fleetDbId;
			this.fleetIndex = fleetIndex;
			this.uid = uid;
			this.name = name;
			this.blueprint = blueprint;
		}

		void step(Step s) {
			step = s;
			stepSince = System.currentTimeMillis();
			sawWork = false;
		}
	}

	static final class Yard {
		final String stationUid;
		final String stationName;
		final int faction;
		final List<Entry> queue = new ArrayList<>();
		final Vector3i sector;
		/** Fleets ordered here. One whose command is no longer "move to this sector" was re-ordered by a player. */
		final java.util.Set<Long> fleets = new java.util.HashSet<>();
		int end;
		long loadedWithoutYardSince;
		long lastOffline;
		String status = "";

		Yard(String stationUid, String stationName, int faction, Vector3i sector) {
			this.stationUid = stationUid;
			this.stationName = stationName;
			this.faction = faction;
			this.sector = new Vector3i(sector);
		}

		boolean stillOrdered(Fleet fleet) {
			FleetCommand c = fleet.getCurrentCommand();
			return c != null && c.getCommand() == FleetCommandTypes.MOVE_FLEET.ordinal() && sector.equals(c.getArgs()[0]);
		}
	}

	final Map<String, Yard> yards = new LinkedHashMap<>();
	/**
	 * Fleet dbid -> what it was doing before it was sent to a yard, given back once its last ship is done.
	 * ponytail: not saved, so a fleet mid-repair over a restart just stays at the yard; save it with the queue if that bites.
	 */
	private final Map<Long, FleetCommand> previousOrders = new HashMap<>();
	/** Station UID -> its shipyard computer's block index, as last seen loaded: where an unloaded repair pays from. */
	final Map<String, Long> yardIndexByStation = new HashMap<>();
	/** Ship UID -> its block counts when it last unloaded: what an unloaded repair refunds. Fleet ships only. */
	final Map<String, org.schema.game.common.controller.ElementCountMap> counts = new HashMap<>();
	/** Ship entity id -> where it is being flown. Read by the AI on the same thread as the tick. */
	private final Map<Integer, Approach> approaches = new HashMap<>();

	public static ShipyardRepairs get() {
		return instance;
	}

	public static void reset() {
		instance = new ShipyardRepairs();
	}

	/** The docking mixin's question: is this ship being flown to a shipyard, and how. */
	public static Approach approach(Ship ship) {
		return instance.approaches.get(ship.getId());
	}

	// ------------------------------------------------------------------
	// Orders

	/**
	 * A player orders a fleet to repair at a station. Whose fleet and whose station are checked here: the
	 * request names both, so neither is believed. With {@code onlyUid} just that one ship is queued; the fleet still
	 * flies there with it, and the rest wait parked beside the yard.
	 */
	public void order(GameServerState state, PlayerState p, long fleetDbId, String stationUid, String stationName, Vector3i sector, String onlyUid) {
		Faction f = state.getFactionManager().getFaction(p.getFactionId());
		Fleet fleet = state.getFleetManager().getByFleetDbId(fleetDbId);
		if (f == null || fleet == null || fleet.isEmpty() || !fleet.canAccess(p.getName()) || fleet.getFlagShip().getFactionId() != f.getIdFaction()) {
			tell(p, Lng.str("You can't order that fleet."));
			return;
		}
		for (Yard y : yards.values()) {
			if (!y.fleets.contains(fleetDbId)) {
				continue;
			}
			//one more of its ships at the yard it is already headed to is fine; anything else is a second order
			if (onlyUid == null || !y.stationUid.equals(stationUid)) {
				tell(p, Lng.str("%s is already ordered to repair at %s.", fleet.getName(), y.stationName));
				return;
			}
			for (Entry e : y.queue) {
				if (e.uid.equals(onlyUid)) {
					tell(p, Lng.str("%s is already in the repair queue at %s.", e.name, y.stationName));
					return;
				}
			}
		}
		Yard yard = yards.computeIfAbsent(stationUid, k -> new Yard(stationUid, stationName, f.getIdFaction(), sector));
		if (yard.faction != f.getIdFaction()) {
			tell(p, Lng.str("That shipyard is not ours."));
			return;
		}
		int queued = 0;
		List<FleetMember> members = fleet.getMembers();
		for (int i = 0; i < members.size(); i++) {
			FleetMember m = members.get(i);
			if (m.getDockedToRootDbId() > 0 || onlyUid != null && !onlyUid.equals(m.UID)) {
				continue; //turrets and docked craft come back with their mothership's blueprint
			}
			String bp = FactionDesigns.server().blueprintFor(f, m.UID, m.name, p.getCatalog().getAvailableCatalog());
			if (bp == null) {
				tell(p, Lng.str("Skipping %s: no blueprint assigned in Faction Designs, and none matches its name.", m.name));
				continue;
			}
			yard.queue.add(new Entry(fleet.dbid, i, m.UID, m.name, bp));
			queued++;
		}
		if (queued == 0) {
			if (yard.queue.isEmpty()) {
				yards.remove(stationUid);
			}
			return;
		}
		if (yard.fleets.add(fleet.dbid)) {
			FleetCommand was = fleet.getCurrentCommand();
			if (was != null) {
				previousOrders.put(fleet.dbid, new FleetCommand(was.getCommandType(), fleet, was.getArgs()));
			}
			fleet.getFleetManager().executeCommand(new FleetCommand(FleetCommandTypes.MOVE_FLEET, fleet, new Vector3i(sector)));
		}
		tell(p, Lng.str("%s ships of %s heading to %s for repairs.", queued, fleet.getName(), stationName));
	}

	/** Drops every ship at this station that is not already inside the yard. */
	public void cancel(PlayerState p, String stationUid) {
		Yard y = yards.get(stationUid);
		if (y == null || y.faction != p.getFactionId()) {
			return;
		}
		for (Iterator<Entry> it = y.queue.iterator(); it.hasNext(); ) {
			Entry e = it.next();
			if (e.step == Step.REPAIRING || e.step == Step.DOCKING && e.approach != null && e.approach.attached) {
				continue;
			}
			release(e);
			it.remove();
		}
		forgetFleetsWithoutEntries((GameServerState) p.getState(), y);
		tell(p, Lng.str("Repair queue at %s cancelled.", y.stationName));
	}

	// ------------------------------------------------------------------
	// Tick

	public void update(GameServerState state, long now) {
		parked.clear();
		labelled.clear();
		for (Yard y : yards.values()) {
			labelled.addAll(y.fleets);
		}
		for (Iterator<Yard> it = yards.values().iterator(); it.hasNext(); ) {
			Yard y = it.next();
			update(state, y, now);
			forgetFleetsWithoutEntries(state, y);
			if (y.queue.isEmpty()) {
				it.remove();
			}
		}
		//a flagship no longer waiting for a yard (its fleet is done, re-ordered, or it is not the flagship any more)
		for (Iterator<Map.Entry<Integer, Approach>> it = approaches.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Approach> a = it.next();
			if (a.getValue().phase == Phase.PARK && !parked.contains(a.getKey())) {
				it.remove();
				if (state.getLocalAndRemoteObjectContainer().getLocalObjects().get(a.getKey()) instanceof Ship ship) {
					letGo(ship);
				}
			}
		}
		//fleets still here get their step, and ones that left this tick their own state's name back
		for (Yard y : yards.values()) {
			labelled.addAll(y.fleets);
		}
		for (long id : labelled) {
			Fleet fleet = state.getFleetManager().getByFleetDbId(id);
			if (fleet != null) {
				FleetLabels.refresh(fleet);
			}
		}
	}

	/** Fleets ordered to a yard at the start or end of this tick: the ones whose order label may have changed. */
	private final java.util.Set<Long> labelled = new java.util.HashSet<>();

	/** What the fleet's order reads (FleetLabels): its furthest-along ship's step, or null if it is not ordered to a yard. */
	public String labelFor(long fleetDbId) {
		Step furthest = null;
		for (Yard y : yards.values()) {
			if (!y.fleets.contains(fleetDbId)) {
				continue;
			}
			for (Entry e : y.queue) {
				if (e.fleetDbId == fleetDbId && (furthest == null || e.step.ordinal() > furthest.ordinal())) {
					furthest = e.step;
				}
			}
		}
		return furthest == null ? null : Lng.str("SHIPYARD REPAIR: %s", furthest.label.toUpperCase(Locale.ENGLISH));
	}

	/** Entity ids of the flagships parked this tick. */
	private final java.util.Set<Integer> parked = new java.util.HashSet<>();

	/**
	 * Each fleet ordered here waits beside the yard rather than in front of it: its flagship is flown to a parking spot
	 * and the rest hold formation on it, repaired ships included, so they do not pile up at the yard's mouth. A flagship
	 * whose own turn it is, or that is in the yard, is left alone.
	 */
	private void park(GameServerState state, Yard y, Entry active, SegmentController station, ShipyardCollectionManager scm, int axis) {
		int slot = 0;
		for (long id : y.fleets) {
			Fleet fleet = state.getFleetManager().getByFleetDbId(id);
			int at = slot++;
			FleetMember flag = fleet == null ? null : fleet.getFlagShip();
			if (flag == null || !(flag.getLoaded() instanceof Ship ship) || ship.getSectorId() != station.getSectorId()
					|| ship.railController.isDockedOrDirty() || active != null && ship.getUniqueIdentifier().equals(active.uid)) {
				continue;
			}
			Approach a = approaches.get(ship.getId());
			if (a != null && a.phase != Phase.PARK) {
				continue; //going in or out
			}
			parked.add(ship.getId());
			if (a == null || !inDocking(ship)) {
				a = new Approach(station, scm, anchorOf(scm), axis, y.end, Phase.PARK);
				a.slot = at;
				request(ship, a);
			}
		}
	}

	private void update(GameServerState state, Yard y, long now) {
		SegmentController station = state.getSegmentControllersByName().get(y.stationUid);
		if (station == null) {
			y.loadedWithoutYardSince = 0;
			UnloadedRepairs.update(this, state, y, now);
			return;
		}
		y.lastOffline = 0;
		for (Entry e : y.queue) {
			if (e.step == Step.OFFLINE) {
				e.step(Step.EN_ROUTE); //the area loaded before it was paid for: the yard does it properly
			}
		}
		ShipyardCollectionManager scm = workingYard(station);
		if (scm == null) {
			y.status = Lng.str("No working shipyard on this station (needs a valid, powered yard with a core anchor).");
			if (y.loadedWithoutYardSince == 0) {
				y.loadedWithoutYardSince = now;
			} else if (now - y.loadedWithoutYardSince > NO_YARD_GIVE_UP_MS) {
				for (Entry e : new ArrayList<>(y.queue)) {
					drop(state, y, e, Lng.str("%s has no working shipyard.", y.stationName));
				}
			}
			return;
		}
		y.loadedWithoutYardSince = 0;
		yardIndexByStation.put(y.stationUid, scm.getControllerElement().getAbsoluteIndex());
		int axis = axisOf(scm);

		Entry active = null;
		for (Entry e : new ArrayList<>(y.queue)) {
			Fleet fleet = state.getFleetManager().getByFleetDbId(e.fleetDbId);
			if (fleet == null) {
				drop(state, y, e, null);
				continue;
			}
			if (e.step.ordinal() < Step.REPAIRING.ordinal() && !y.stillOrdered(fleet)) {
				drop(state, y, e, Lng.str("%s was given another order; %s left the repair queue.", fleet.getName(), e.name));
				continue;
			}
			Ship ship = ship(state, e.uid);
			if (e.step == Step.EN_ROUTE) {
				if (ship != null && ship.getSectorId() == station.getSectorId()) {
					arrive(state, y, e, ship, station, scm, axis, fleet);
				}
				continue;
			}
			if (e.step == Step.DOCKING && e.approach != null && ship != null && !inDocking(ship)
					&& !ship.railController.isDockedOrDirty()
					&& ship.getAiConfiguration().getAiEntityState().getDockStatus() != ShipAIEntity.DockStatus.FAILED) {
				request(ship, e.approach); //knocked out of its run in (sector reload, a restart): back on it
			}
			if (active == null) {
				active = e;
			}
		}
		if (active != null) {
			step(state, y, active, station, scm, axis, now);
		}
		park(state, y, active, station, scm, axis);
		//getStateDescription() is the client's: on the server it always reads "out of service"
		ShipyardState now_ = scm.getCurrentStateServer();
		y.status = (now_ != null ? now_.getClientShortDescription() : "") + (free ? "  [" + Lng.str("repairs free") + "]" : "");
	}

	private void arrive(GameServerState state, Yard y, Entry e, Ship ship, SegmentController station, ShipyardCollectionManager scm, int axis, Fleet fleet) {
		Vector3i min = new Vector3i(), max = new Vector3i();
		scm.getYardBounds(min, max);
		float length = max.getCoord(axis) - min.getCoord(axis) + 1;
		//ponytail: rough pre-check, longest side against the yard's length; the yard measures it exactly once docked
		if (2 * halfSize(ship) > length) {
			drop(state, y, e, Lng.str("%s is too large for the shipyard at %s and was skipped.", e.name, y.stationName));
			return;
		}
		if (y.end == 0) {
			//ships go in at whichever end the first one turns up at, and out the other
			Vector3f anchor = new Vector3f();
			ElementCollection.getPosFromIndex(anchorOf(scm), anchor);
			Vector3f local = new Vector3f(ship.getWorldTransform().origin);
			Transform inv = new Transform(station.getWorldTransform());
			inv.inverse();
			inv.transform(local);
			y.end = coord(local, axis) + SegmentData.SEG_HALF >= coord(anchor, axis) ? 1 : -1;
		}
		//waits with its fleet until its turn; served in the order they turn up, not fleet order
		e.step(Step.HOLDING);
		y.queue.remove(e);
		int at = 0;
		while (at < y.queue.size() && y.queue.get(at).step != Step.EN_ROUTE) {
			at++;
		}
		y.queue.add(at, e);
	}

	/** Its turn: fly it in. @return false if its AI cannot start yet (still moving, fighting): try next tick */
	private boolean startDocking(Yard y, Entry e, Ship ship, SegmentController station, ShipyardCollectionManager scm, int axis) {
		if (ship == null) {
			return false;
		}
		Approach parkedAt = approaches.get(ship.getId());
		if (parkedAt != null && parkedAt.phase == Phase.PARK) {
			approaches.remove(ship.getId());
			letGo(ship); //out of its parking state first; docking starts next tick
			return false;
		}
		Approach a = new Approach(station, scm, anchorOf(scm), axis, y.end, Phase.DOCK);
		if (!request(ship, a)) {
			return false;
		}
		e.approach = a;
		e.step(Step.DOCKING);
		handOverFlagship(ship);
		return true;
	}

	/**
	 * The rest of a fleet holds formation around its flagship, so a flagship flying into the yard would drag them all in
	 * after it: the next ship in the fleet leads while it is away. {@link #rejoin} puts the rebuilt ship back in its place.
	 * ponytail: a repair that fails leaves the old flagship at the back; the player can reorder the fleet by hand.
	 */
	private static void handOverFlagship(Ship ship) {
		Fleet fleet = ship.getFleet();
		if (fleet != null && fleet.getMembers().size() > 1 && fleet.isFlagShip(ship)) {
			List<FleetMember> m = fleet.getMembers();
			m.add(m.remove(0));
			fleet.save();
			fleet.sendFleet();
		}
	}

	/** The one ship the yard is working on now, moved along one step at a time. */
	private void step(GameServerState state, Yard y, Entry e, SegmentController station, ShipyardCollectionManager scm, int axis, long now) {
		ShipyardState st = scm.getCurrentStateServer();
		Ship ship = ship(state, e.uid);
		switch (e.step) {
			case HOLDING -> {
				if (st instanceof DesignLoaded && scm.getCurrentDocked() != null
						&& designIdByUid(scm, scm.getCurrentDocked().getUniqueIdentifier()) >= 0) {
					//one of ours, left loaded by a restart mid-way through making it
					scm.handleShipyardCommandOnServer(y.faction, ShipyardCommandType.UNLOAD_DESIGN, new Object[0]);
					return;
				}
				if (ship == null || !(st instanceof WaitingForShipyardOrder)) {
					return;
				}
				e.designId = designId(scm, e.blueprint);
				if (e.designId >= 0) {
					startDocking(y, e, ship, station, scm, axis);
				} else {
					scm.handleShipyardCommandOnServer(y.faction, ShipyardCommandType.CATALOG_TO_DESIGN, new Object[]{DESIGN_PREFIX + e.blueprint, e.blueprint});
					e.step(Step.DESIGN);
				}
			}
			case DESIGN -> {
				if (st instanceof DesignLoaded) {
					scm.handleShipyardCommandOnServer(y.faction, ShipyardCommandType.UNLOAD_DESIGN, new Object[0]);
				} else if (st instanceof WaitingForShipyardOrder) {
					e.designId = designId(scm, e.blueprint);
					if (e.designId >= 0) {
						if (!startDocking(y, e, ship, station, scm, axis)) {
							e.step(Step.HOLDING); //design is there now; the ship goes in from the usual wait
						}
					} else if (e.sawWork || now - e.stepSince > 10000) {
						drop(state, y, e, Lng.str("Could not make a shipyard design from blueprint %s for %s (yard inventory full, or the blueprint is gone?).", e.blueprint, e.name));
					}
				} else {
					e.sawWork = true;
				}
			}
			case DOCKING -> {
				if (ship == null) {
					return;
				}
				ShipAIEntity ai = ship.getAiConfiguration().getAiEntityState();
				if (ai.getDockStatus() == ShipAIEntity.DockStatus.FAILED && scm.getCurrentDocked() != ship) {
					drop(state, y, e, Lng.str("%s could not dock at %s: %s", e.name, y.stationName, ai.getDockFailReason()));
					return;
				}
				if (scm.getCurrentDocked() != ship || !(st instanceof NormalShipLoaded)) {
					return;
				}
				if (!scm.getFitInfo().tested && now - e.stepSince < 120000) {
					scm.computeFit(scm.getFitInfo());
					return;
				}
				if (scm.getFitInfo().tested && !scm.getFitInfo().fits) {
					scm.undockRequestedFromShipyard();
					drop(state, y, e, Lng.str("%s does not fit the shipyard at %s (%s) and was skipped.", e.name, y.stationName, scm.getFitInfo().describeOverflow()));
					return;
				}
				release(e);
				e.oldDbId = ship.dbId;
				scm.handleShipyardCommandOnServer(y.faction, ShipyardCommandType.REPAIR_FROM_DESIGN, new Object[]{e.designId});
				e.step(Step.REPAIRING);
			}
			case REPAIRING -> {
				if (!(st instanceof NormalShipLoaded)) {
					e.sawWork = true;
					return;
				}
				if (!e.sawWork) {
					if (now - e.stepSince > 10000) {
						scm.undockRequestedFromShipyard();
						drop(state, y, e, Lng.str("The shipyard at %s would not start repairing %s.", y.stationName, e.name));
					}
					return;
				}
				SegmentController rebuilt = scm.getCurrentDocked();
				if (rebuilt == null && now - e.stepSince < 30000) {
					return; //after a restart the docked ship loads after the station
				}
				if (rebuilt == null) {
					drop(state, y, e, Lng.str("%s was lost during its repair at %s.", e.name, y.stationName));
				} else if (rebuilt.dbId == e.oldDbId) {
					scm.undockRequestedFromShipyard();
					drop(state, y, e, Lng.str("The repair of %s at %s failed; it was left as it was.", e.name, y.stationName));
				} else {
					rejoin(state, y, e, rebuilt);
					scm.undockRequestedFromShipyard();
					e.step(Step.LEAVING);
				}
			}
			case LEAVING -> {
				if (e.approach != null && e.approach.arrived) {
					drop(state, y, e, null);
				} else if (now - e.stepSince > LEAVE_TIMEOUT_MS) {
					//never flew out: put it out past the far end, or it sits on the anchor and blocks the yard for good
					if (ship != null && !ship.railController.isDockedOrDirty()) {
						Vector3f out = new Approach(station, scm, anchorOf(scm), axis, y.end, Phase.EXIT).gate(ship, false);
						ship.warpTransformable(out.x, out.y, out.z, true, null);
					}
					drop(state, y, e, null);
				} else if (ship != null && !ship.railController.isDockedOrDirty()
						&& ship.getPhysicsDataContainer().getObject() instanceof RigidBody body) {
					if (e.approach == null) {
						e.approach = new Approach(station, scm, anchorOf(scm), axis, y.end, Phase.EXIT);
					}
					//led out from its docking state (ShipyardDockingMixin), where its fleet AI does not brake it to a stop
					//every update as an idle fleet member's does; until its AI can take that, the yard flies it from here.
					//A freshly built ship's AI is often not active yet, and an inactive AI's docking state never updates
					if (!ship.getAiConfiguration().isActiveAI() || !inDocking(ship) && !request(ship, e.approach)) {
						e.approach.measured(e.approach.lead(ship, body));
					}
				}
			}
			default -> {
			}
		}
	}

	/** The rebuilt ship takes the old one's place in its fleet, and its blueprint assignment. */
	private void rejoin(GameServerState state, Yard y, Entry e, SegmentController rebuilt) {
		FactionDesigns.server().moved(y.faction, e.uid, rebuilt.getUniqueIdentifier());
		Fleet fleet = state.getFleetManager().getByFleetDbId(e.fleetDbId);
		e.uid = rebuilt.getUniqueIdentifier();
		if (fleet == null) {
			return;
		}
		removeFromFleet(state, fleet, e.oldDbId);
		Fleet other = state.getFleetManager().getByEntity(rebuilt);
		if (other != null && other != fleet) {
			removeFromFleet(state, other, rebuilt.dbId); //the yard's station was in a fleet, which took it
		}
		if (!fleet.isMember(rebuilt.dbId)) {
			addToFleet(state, fleet, rebuilt.dbId, e.fleetIndex);
		}
		tellOwner(state, fleet, Lng.str("%s repaired at %s.", e.name, y.stationName));
	}

	/**
	 * Adds an entity to a fleet at a place in its order. Through the fleet manager, not the fleet, so its entity-to-fleet
	 * index knows: without that the ship never learns it is in a fleet, its AI never switches on, and the fleet panel shows
	 * it with no engagement range.
	 */
	static void addToFleet(GameServerState state, Fleet fleet, long dbId, int index) {
		state.getFleetManager().requestShipAdd(fleet, dbId);
		List<FleetMember> m = fleet.getMembers();
		if (!m.isEmpty() && m.get(m.size() - 1).entityDbId == dbId) {
			m.add(Math.min(index, m.size() - 1), m.remove(m.size() - 1));
			fleet.save();
			fleet.sendFleet();
		}
	}

	/** Takes an entity out of a fleet through the fleet manager, which also forgets it in its index. */
	static void removeFromFleet(GameServerState state, Fleet fleet, long dbId) {
		for (FleetMember m : fleet.getMembers()) {
			if (m.entityDbId == dbId) {
				state.getFleetManager().requestFleetMemberRemove(fleet, m);
				return;
			}
		}
	}

	// ------------------------------------------------------------------
	// Queue view

	/** Server: the queue at a station, for a member of the station's faction. */
	public Tag queueTag(String stationUid, int faction) {
		Yard y = yards.get(stationUid);
		List<Tag> rows = new ArrayList<>();
		rows.add(new Tag(Tag.Type.STRING, null, stationUid));
		rows.add(new Tag(Tag.Type.STRING, null, y == null || y.faction != faction ? "" : y.status));
		if (y != null && y.faction == faction) {
			GameServerState st = GameServerState.instance;
			for (Entry e : y.queue) {
				Fleet fleet = st.getFleetManager().getByFleetDbId(e.fleetDbId);
				String step = e.step == Step.OFFLINE && e.offlineTotal > 0
						? e.step.label + " " + Math.min(100, e.offlineMs * 100 / e.offlineTotal) + "%" : e.step.label;
				Approach a = e.approach;
				if (a != null && a.lastDist >= 0) {
					//where the AI is flying it: say which leg and how far, so a stall shows where it stalled
					String leg = a.phase == Phase.EXIT ? Lng.str("out") : a.staged ? Lng.str("into the yard") : a.captured ? Lng.str("lining up") : Lng.str("to the yard entrance");
					String held = a.blockedBy;
					step += " (" + leg + ", " + Math.round(a.lastDist) + "m" + (held != null ? ", " + Lng.str("waiting for %s to clear", held) : "") + ")";
				}
				rows.add(new Tag(Tag.Type.STRING, null, e.name + "  -  " + (fleet != null ? fleet.getName() : "?") + "  -  "
						+ e.blueprint + "  -  " + step));
			}
		}
		rows.add(FinishTag.INST);
		return new Tag(Tag.Type.STRUCT, null, rows.toArray(new Tag[0]));
	}

	/**
	 * Server: the shipyard's "Inputs Needed" while it has no build of its own on: the components the ships still queued
	 * at its station need, each its blueprint's less what its old ship gives back, and how many of them the yard and its
	 * connected chests hold. Null if nothing is queued there.
	 * ponytail: one ship's refund is not counted towards the next's cost, so the total can read a little high.
	 */
	public ElementCountMap[] queuedGoal(ShipyardCollectionManager scm) {
		Yard y = yards.get(scm.getSegmentController().getUniqueIdentifier());
		if (y == null || y.queue.isEmpty()) {
			return null;
		}
		GameServerState state = GameServerState.instance;
		ElementCountMap need = new ElementCountMap();
		for (Entry e : y.queue) {
			BlueprintEntry bp = e.step == Step.LEAVING ? null : UnloadedRepairs.blueprint(e.blueprint);
			if (bp == null) {
				continue;
			}
			//one in the yard is already giving its blocks back into the yard's inventory
			Ship ship = e.step == Step.REPAIRING ? null : ship(state, e.uid);
			ElementCountMap had = ship != null ? ship.getElementClassCountMap() : e.step == Step.REPAIRING ? null : counts.get(e.uid);
			UnloadedRepairs.cost(RepairComponents.components(bp.getElementCountMapWithChildren()),
					had == null ? null : RepairComponents.components(had), need, new ElementCountMap());
		}
		ElementCountMap have = new ElementCountMap();
		for (short t : ElementKeyMap.typeList()) {
			if (need.get(t) > 0) {
				have.inc(t, Math.min(need.get(t), RepairComponents.onHand(scm, t)));
			}
		}
		return new ElementCountMap[]{need, have};
	}

	// ------------------------------------------------------------------
	// Saving

	public Tag toTag() {
		List<Tag> ys = new ArrayList<>();
		for (Yard y : yards.values()) {
			List<Tag> es = new ArrayList<>();
			for (Entry e : y.queue) {
				es.add(new Tag(Tag.Type.STRUCT, null, new Tag[]{
						new Tag(Tag.Type.LONG, null, e.fleetDbId), new Tag(Tag.Type.INT, null, e.fleetIndex),
						new Tag(Tag.Type.STRING, null, e.uid), new Tag(Tag.Type.STRING, null, e.name),
						new Tag(Tag.Type.STRING, null, e.blueprint), new Tag(Tag.Type.BYTE, null, (byte) e.step.ordinal()),
						new Tag(Tag.Type.LONG, null, e.oldDbId), new Tag(Tag.Type.INT, null, e.designId), FinishTag.INST}));
			}
			es.add(FinishTag.INST);
			ys.add(new Tag(Tag.Type.STRUCT, null, new Tag[]{
					new Tag(Tag.Type.STRING, null, y.stationUid), new Tag(Tag.Type.STRING, null, y.stationName),
					new Tag(Tag.Type.INT, null, y.faction), new Tag(Tag.Type.VECTOR3i, null, new Vector3i(y.sector)),
					new Tag(Tag.Type.INT, null, y.end), new Tag(Tag.Type.STRUCT, null, es.toArray(new Tag[0])), FinishTag.INST}));
		}
		ys.add(FinishTag.INST);
		List<Tag> idx = new ArrayList<>();
		for (Map.Entry<String, Long> i : yardIndexByStation.entrySet()) {
			idx.add(new Tag(Tag.Type.STRING, null, i.getKey()));
			idx.add(new Tag(Tag.Type.LONG, null, i.getValue()));
		}
		idx.add(FinishTag.INST);
		List<Tag> cs = new ArrayList<>();
		for (Map.Entry<String, org.schema.game.common.controller.ElementCountMap> c : counts.entrySet()) {
			cs.add(new Tag(Tag.Type.STRING, null, c.getKey()));
			cs.add(countsTag(c.getValue()));
		}
		cs.add(FinishTag.INST);
		return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.STRUCT, null, ys.toArray(new Tag[0])),
				new Tag(Tag.Type.STRUCT, null, idx.toArray(new Tag[0])), new Tag(Tag.Type.STRUCT, null, cs.toArray(new Tag[0])), FinishTag.INST});
	}

	/** Only the kinds of block a ship has, as type/count pairs: a full count array per fleet ship would be most of the file. */
	private static Tag countsTag(org.schema.game.common.controller.ElementCountMap m) {
		List<Tag> t = new ArrayList<>();
		for (short type : ElementKeyMap.typeList()) {
			if (m.get(type) > 0) {
				t.add(new Tag(Tag.Type.SHORT, null, type));
				t.add(new Tag(Tag.Type.INT, null, m.get(type)));
			}
		}
		t.add(FinishTag.INST);
		return new Tag(Tag.Type.STRUCT, null, t.toArray(new Tag[0]));
	}

	private static org.schema.game.common.controller.ElementCountMap countsFrom(Tag tag) {
		org.schema.game.common.controller.ElementCountMap m = new org.schema.game.common.controller.ElementCountMap();
		Tag[] t = tag.getStruct();
		for (int i = 0; i + 1 < t.length && t[i].getType() == Tag.Type.SHORT; i += 2) {
			m.inc(t[i].getShort(), t[i + 1].getInt());
		}
		return m;
	}

	// ------------------------------------------------------------------
	// Block counts

	/**
	 * Server: a ship or station unloading. A fleet ship's block counts are kept, since they cannot change while it
	 * is unloaded and an unloaded repair refunds them; a station's working shipyard is remembered for paying from.
	 */
	public void onUnload(SegmentController sc) {
		if (sc instanceof Ship s) {
			if (s.isInFleet() && s.getFactionId() > 0 && !s.railController.isDocked()) {
				counts.put(s.getUniqueIdentifier(), new org.schema.game.common.controller.ElementCountMap(s.getElementClassCountMap()));
			}
		} else {
			ShipyardCollectionManager scm = workingYard(sc);
			if (scm != null) {
				yardIndexByStation.put(sc.getUniqueIdentifier(), scm.getControllerElement().getAbsoluteIndex());
			}
		}
	}

	/** Whether the station had a working shipyard when it last unloaded. */
	public boolean yardRemembered(String stationUid) {
		return yardIndexByStation.containsKey(stationUid);
	}

	/** Server: an entity deleted for good. */
	public void onDeleted(SegmentController sc) {
		counts.remove(sc.getUniqueIdentifier());
		yardIndexByStation.remove(sc.getUniqueIdentifier());
	}

	/**
	 * Back from a save. Nothing is flying on a fresh load, so a ship that was in line or on its way in goes back to
	 * "en route" and lines up again when it is next seen at the yard; one that was leaving is done. A repair in
	 * progress stays one: the yard finishes it and the rebuilt ship is picked up from the yard as usual.
	 */
	public void fromTag(Tag tag) {
		yards.clear();
		approaches.clear();
		yardIndexByStation.clear();
		counts.clear();
		Tag[] top = tag.getStruct();
		Tag[] idx = top[1].getStruct();
		for (int i = 0; i + 1 < idx.length && idx[i].getType() == Tag.Type.STRING; i += 2) {
			yardIndexByStation.put(idx[i].getString(), idx[i + 1].getLong());
		}
		Tag[] cs = top[2].getStruct();
		for (int i = 0; i + 1 < cs.length && cs[i].getType() == Tag.Type.STRING; i += 2) {
			counts.put(cs[i].getString(), countsFrom(cs[i + 1]));
		}
		Tag[] ys = top[0].getStruct();
		for (int i = 0; i < ys.length && ys[i].getType() == Tag.Type.STRUCT; i++) {
			Tag[] t = ys[i].getStruct();
			Yard y = new Yard(t[0].getString(), t[1].getString(), t[2].getInt(), (Vector3i) t[3].getValue());
			y.end = t[4].getInt();
			Tag[] es = t[5].getStruct();
			for (int j = 0; j < es.length && es[j].getType() == Tag.Type.STRUCT; j++) {
				Tag[] u = es[j].getStruct();
				Entry e = new Entry(u[0].getLong(), u[1].getInt(), u[2].getString(), u[3].getString(), u[4].getString());
				Step was = Step.values()[u[5].getByte()];
				e.oldDbId = u[6].getLong();
				e.designId = u[7].getInt();
				if (was == Step.LEAVING) {
					continue;
				}
				e.step(was == Step.REPAIRING ? Step.REPAIRING : Step.EN_ROUTE);
				e.sawWork = was == Step.REPAIRING;
				y.queue.add(e);
				y.fleets.add(e.fleetDbId);
			}
			if (!y.queue.isEmpty()) {
				yards.put(y.stationUid, y);
			}
		}
	}

	// ------------------------------------------------------------------
	// Helpers

	private boolean request(Ship ship, Approach a) {
		ShipAIEntity ai = ship.getAiConfiguration().getAiEntityState();
		approaches.put(ship.getId(), a);
		if (ai.requestDock(a.yard, a.anchor)) {
			return true;
		}
		approaches.remove(ship.getId()); //not in a state that can start one yet (still moving): next tick
		return false;
	}

	/** Lets the ship go: out of the docking state if it is still in it, back to its fleet's orders. */
	private void release(Entry e) {
		if (e.approach == null) {
			return;
		}
		Approach gone = e.approach;
		e.approach = null;
		Ship ship = ship(GameServerState.instance, e.uid);
		if (ship == null) {
			approaches.values().removeIf(a -> a == gone);
			return;
		}
		approaches.remove(ship.getId());
		letGo(ship);
	}

	/** Out of the docking state, if it is in it, back to its fleet's orders. */
	private static void letGo(Ship ship) {
		if (inDocking(ship)) {
			ShipAIEntity ai = ship.getAiConfiguration().getAiEntityState();
			ai.setDockStatus(ShipAIEntity.DockStatus.NONE); //not a failure: AIDocking.onExit would call it one
			try {
				ai.getCurrentProgram().getMachine().getFsm().getCurrentState().stateTransition(Transition.RESTART);
			} catch (FSMException ex) {
				ex.printStackTrace();
			}
		}
	}

	static boolean inDocking(Ship ship) {
		ShipAIEntity ai = ship.getAiConfiguration().getAiEntityState();
		return ai.getCurrentProgram() != null && ai.getCurrentProgram().getMachine().getFsm().getCurrentState() instanceof AIDocking;
	}

	void drop(GameServerState state, Yard y, Entry e, String why) {
		release(e);
		y.queue.remove(e);
		if (why != null) {
			tellOwner(state, state.getFleetManager().getByFleetDbId(e.fleetDbId), why);
		}
	}

	/** Fleets with no ship left in the queue are done here: each goes back to its old order, unless a player gave it a new one. */
	private void forgetFleetsWithoutEntries(GameServerState state, Yard y) {
		y.fleets.removeIf(id -> {
			if (y.queue.stream().anyMatch(e -> e.fleetDbId == id)) {
				return false;
			}
			FleetCommand was = previousOrders.remove(id);
			Fleet fleet = state.getFleetManager().getByFleetDbId(id);
			if (was != null && fleet != null && y.stillOrdered(fleet)) {
				fleet.getFleetManager().executeCommand(was);
			}
			return true;
		});
	}

	private static Ship ship(GameServerState state, String uid) {
		return state.getSegmentControllersByName().get(uid) instanceof Ship s ? s : null;
	}

	/** The station's first shipyard that can take an order now. ponytail: one yard per station serves its queue. */
	static ShipyardCollectionManager workingYard(SegmentController station) {
		if (!(station instanceof ManagedSegmentController<?> m) || !(m.getManagerContainer() instanceof ShipyardManagerContainerInterface c)) {
			return null;
		}
		for (ShipyardCollectionManager scm : c.getShipyard().getCollectionManagers()) {
			if (scm.isValid() && scm.isPowered() && scm.getConnectedCorePositionBlock4() != Long.MIN_VALUE && scm.getCurrentStateServer() != null) {
				return scm;
			}
		}
		return null;
	}

	private static long anchorOf(ShipyardCollectionManager scm) {
		return ElementCollection.getPosIndexFrom4(scm.getConnectedCorePositionBlock4());
	}

	/** The yard's stacking axis, or its longest side while the engine has not worked that out. */
	private static int axisOf(ShipyardCollectionManager scm) {
		int a = scm.getStackingAxis();
		if (a >= 0 && a <= 2) {
			return a;
		}
		Vector3i min = new Vector3i(), max = new Vector3i();
		scm.getYardBounds(min, max);
		int best = 0;
		for (int i = 1; i < 3; i++) {
			if (max.getCoord(i) - min.getCoord(i) > max.getCoord(best) - min.getCoord(best)) {
				best = i;
			}
		}
		return best;
	}

	/** A design of ours (made from a blueprint by this queue) with this virtual entity UID, or -1. */
	private static int designIdByUid(ShipyardCollectionManager scm, String uid) {
		for (VirtualBlueprintMetaItem d : scm.getDesignList()) {
			if (uid.equals(d.UID) && d.virtualName != null && d.virtualName.startsWith(DESIGN_PREFIX)) {
				return d.getId();
			}
		}
		return -1;
	}

	private static int designId(ShipyardCollectionManager scm, String blueprint) {
		for (VirtualBlueprintMetaItem d : scm.getDesignList()) {
			if ((DESIGN_PREFIX + blueprint).equals(d.virtualName)) {
				return d.getId();
			}
		}
		return -1;
	}

	static float halfSize(SegmentController s) {
		Vector3f size = new Vector3f();
		size.sub(s.getSegmentBuffer().getBoundingBox().max, s.getSegmentBuffer().getBoundingBox().min);
		return Math.max(size.x, Math.max(size.y, size.z)) * 0.5f;
	}

	private static void setCoord(Vector3f v, int axis, float value) {
		switch (axis) {
			case 0 -> v.x = value;
			case 1 -> v.y = value;
			default -> v.z = value;
		}
	}

	private static float coord(Vector3f v, int axis) {
		return axis == 0 ? v.x : axis == 1 ? v.y : v.z;
	}

	/** Distance from {@code p} to the line segment {@code a}-{@code b}. */
	static float distanceToSegment(Vector3f p, Vector3f a, Vector3f b) {
		Vector3f ab = new Vector3f(), ap = new Vector3f();
		ab.sub(b, a);
		ap.sub(p, a);
		float len2 = ab.lengthSquared();
		float t = len2 > 0 ? Math.max(0, Math.min(1, ap.dot(ab) / len2)) : 0;
		ab.scale(t);
		ap.sub(ab);
		return ap.length();
	}

	private static float distance(Vector3f a, Vector3f b) {
		Vector3f d = new Vector3f();
		d.sub(a, b);
		return d.length();
	}

	private static void tell(PlayerState p, String msg) {
		p.sendServerMessagePlayerInfo(new Object[]{msg});
	}

	private static void tellOwner(GameServerState state, Fleet fleet, String msg) {
		PlayerState p = fleet == null ? null : state.getPlayerFromNameIgnoreCaseWOException(fleet.getOwner());
		if (p != null) {
			tell(p, msg);
		}
	}
}
