package videogoose.betterfactions.mixin;

import com.bulletphysics.dynamics.RigidBody;
import org.schema.game.common.data.physics.ModifiedDynamicsWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import videogoose.betterfactions.gui.BattleAdminList;

/**
 * No anti-tunnelling sweeps while watching a battle. Every convex body moving fast enough (in a battle,
 * mostly debris shards from destroyed blocks) sweeps a sphere through the world each physics step, and
 * the engine's sphere sweep skips the bounding-box test, so each one is tested against every ship's
 * blocks. With a fleet in view that was most of the watching client's frame. Debris may now pass
 * through a hull, which nobody watching from a free camera will mind.
 */
@Mixin(value = ModifiedDynamicsWorld.class, remap = false)
public abstract class WatchCcdMixin {

	@Redirect(method = "integrateTransforms", at = @At(value = "INVOKE",
			target = "Lcom/bulletphysics/dynamics/RigidBody;getCcdSquareMotionThreshold()F"))
	private float betterFactions$noSweepWhileWatching(RigidBody body) {
		return BattleAdminList.isWatching() ? 0f : body.getCcdSquareMotionThreshold();
	}
}
