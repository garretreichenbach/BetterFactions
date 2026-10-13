package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.server.ai.ShipAIEntity;
import org.schema.game.server.ai.program.common.states.AIDocking;
import org.schema.schine.ai.stateMachines.FSMException;
import org.schema.schine.ai.stateMachines.Transition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.repair.ShipyardRepairs;

import com.bulletphysics.dynamics.RigidBody;

import javax.vecmath.Vector3f;

/**
 * Flies a ship ordered to a shipyard repair to the yard entrance; from there the yard leads it in onto the core
 * anchor, and out past the far end afterwards ({@link ShipyardRepairs.Approach#lead}). The AI docking state's own
 * steering aims a rail docker at a rail pickup, and a shipyard has neither, so for these ships its update is
 * replaced; its movement ({@code updateAI}) and its place in the fleet AI's state machine are kept.
 */
@Mixin(value = AIDocking.class, remap = false)
public abstract class ShipyardDockingMixin {

	/** Inside this of the yard entrance the AI steers by velocity (see below); outside it flies straight at full speed. */
	private static final float SLOW = 150.0f;
	/** Speed wanted per metre still to go, and its cap, in m/s. */
	private static final float SPEED_PER_METRE = 0.25f;
	private static final float MAX_APPROACH_SPEED = 30.0f;
	private static final long NO_PROGRESS_TIMEOUT = 60000;

	@Shadow
	private Vector3f movingDir;
	@Shadow
	private float dist;
	@Shadow
	private float bestDist;
	@Shadow
	private long lastProgress;
	@Unique
	private int betterFactions$lastLeg = -1;

	/**
	 * Keeps ships on their way to the yard from flying into each other: the vanilla formation's separation
	 * (FleetFormationingAbstract), a push away from every nearby entity in the sector with an up/down bias so two
	 * ships slide past rather than stall nose to nose. Only runs before the yard captures the ship, so the yard is
	 * pushed off from too, fading out from {@link #SLOW} of the entrance to nothing at capture so it cannot hold the
	 * ship back from the entrance.
	 */
	@Unique
	private void betterFactions$separate(Ship ship, SegmentController yard, float distToEntrance) {
		if (ship.getProximityObjects().isEmpty()) {
			return;
		}
		float yardWeight = Math.max(0, Math.min(1, (distToEntrance - ShipyardRepairs.Approach.CAPTURE) / (SLOW - ShipyardRepairs.Approach.CAPTURE)));
		Vector3f from = ship.getWorldTransform().origin;
		Vector3f prox = new Vector3f(ship.proximityVector);
		float strength = 0; //the strongest push any one entity asks for
		for (int id : ship.getProximityObjects()) {
			if (ship.getState().getLocalAndRemoteObjectContainer().getLocalObjects().get(id) instanceof SegmentController c
					&& c.getSectorId() == ship.getSectorId()) {
				float weight = c == yard ? yardWeight : 1;
				Vector3f away = new Vector3f();
				away.sub(from, c.getWorldTransform().origin);
				if (weight > 0 && away.lengthSquared() > 0) {
					away.normalize();
					prox.scaleAdd(weight, away, prox);
					strength = Math.max(strength, weight);
				}
			}
		}
		if (strength == 0) {
			return;
		}
		prox.y += prox.y > 0 ? 0.5f : -0.5f;
		prox.normalize();
		prox.scale(10 * strength);
		if (movingDir.lengthSquared() > 0) {
			movingDir.normalize();
			movingDir.scale(0.9f);
		}
		movingDir.add(prox);
	}

	@Inject(method = "onUpdate()Z", at = @At("HEAD"), cancellable = true)
	private void betterFactions$flyToShipyard(CallbackInfoReturnable<Boolean> cir) throws FSMException {
		AIDocking self = (AIDocking) (Object) this;
		Ship ship = self.getEntity();
		ShipyardRepairs.Approach a = ShipyardRepairs.approach(ship);
		if (a == null) {
			return;
		}
		cir.setReturnValue(false);
		ShipAIEntity ai = (ShipAIEntity) self.getEntityState();

		if (ship.railController.isDockedAndExecuted()) {
			ai.setDockStatus(ShipAIEntity.DockStatus.DOCKED);
			self.stateTransition(Transition.DOCK_COMPLETED);
			return;
		}
		if (a.yard.isMarkedForPermanentDelete() || a.yard.getSectorId() != ship.getSectorId()) {
			ai.failDock("shipyard not in this sector");
			self.stateTransition(Transition.RESTART);
			return;
		}

		Vector3f target = a.entrance(ship);
		movingDir.sub(target, ship.getWorldTransform().origin);
		dist = movingDir.length();
		RigidBody body = ship.getPhysicsDataContainer().getObject() instanceof RigidBody b ? b : null;
		long now = System.currentTimeMillis();

		boolean leading = body != null && a.leading(dist);
		//vanilla avoidance steers off everything but the docking target: off the yard while far out, not once close in
		//(where our own fading push takes over) or once the yard has it
		ai.setDockingTarget(leading || dist < SLOW ? a.yard : null);
		if (leading) {
			movingDir.set(0, 0, 0); //the yard flies it from here, not its thrusters
			dist = a.lead(ship, body);
		} else {
			if (dist < SLOW) {
				//moveTo thrusts flat out along whatever it is given and only brakes on a near-reversal, so aiming at the
				//point itself overshoots and swings back and forth across it. Aim at the velocity error instead: a speed
				//towards the point that falls off as it gets close, less what the ship is already doing.
				Vector3f velocity = new Vector3f();
				if (body != null) {
					body.getLinearVelocity(velocity);
				}
				movingDir.normalize();
				movingDir.scale(Math.min(MAX_APPROACH_SPEED, dist * SPEED_PER_METRE));
				movingDir.sub(velocity);
				if (movingDir.lengthSquared() < 0.0001f) {
					movingDir.set(0, 0, 0.01f); //keeps physics awake, as vanilla does
				}
			}
			betterFactions$separate(ship, a.yard, dist);
		}
		a.measured(dist);

		int leg = a.leg();
		if (a.phase() != ShipyardRepairs.Phase.DOCK || leg != betterFactions$lastLeg) {
			//leaving has its own timeout in ShipyardRepairs; each leg in is measured on its own
			betterFactions$lastLeg = leg;
			bestDist = a.phase() == ShipyardRepairs.Phase.DOCK ? dist : Float.MAX_VALUE;
			lastProgress = now;
		} else if (dist < bestDist - 1.0f) {
			bestDist = dist;
			lastProgress = now;
		} else if (now - lastProgress > NO_PROGRESS_TIMEOUT) {
			ai.failDock("no progress for " + NO_PROGRESS_TIMEOUT / 1000 + "s, " + Math.round(dist) + "m from the shipyard");
			self.stateTransition(Transition.RESTART);
		}
	}
}
