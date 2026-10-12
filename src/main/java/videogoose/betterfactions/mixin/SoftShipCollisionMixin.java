package videogoose.betterfactions.mixin;

import com.bulletphysics.collision.dispatch.CollisionObject;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.physics.CollisionDispatcherExt;
import org.schema.game.common.data.physics.RigidBodySegmentController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.battle.BattleTracker;
import videogoose.betterfactions.gui.BattleAdminList;

/**
 * In a soft-loaded battle sector, ships and stations pass through each other: no contact points, no
 * collision response. Hits are unaffected, since projectiles, missiles and beams find their targets
 * by ray tests against the broadphase, which never asks this. Ramming does no damage there.
 *
 * <p>The watching admin's client runs its own physics on the same ships, and ship-on-ship narrow phase
 * is by far its most expensive part, so it skips them too.</p>
 */
@Mixin(value = CollisionDispatcherExt.class, remap = false)
public abstract class SoftShipCollisionMixin {

	@Inject(method = "needsCollision", at = @At("HEAD"), cancellable = true)
	private void betterFactions$passThroughWhenUnwatched(CollisionObject body0, CollisionObject body1, CallbackInfoReturnable<Boolean> cir) {
		if (body0 instanceof RigidBodySegmentController a && body1 instanceof RigidBodySegmentController b) {
			SegmentController sc = a.getSegmentController();
			//a watching client only knows it is watching, not which sectors are soft; all it has loaded is the battle
			boolean soft = sc.isOnServer()
					? BattleTracker.isSoftLoaded(sc.getSectorId()) || BattleTracker.isSoftLoaded(b.getSegmentController().getSectorId())
					: BattleAdminList.isWatching();
			if (soft) {
				cir.setReturnValue(false);
			}
		}
	}
}
