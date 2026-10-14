package videogoose.betterfactions.mixin;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.fleet.missions.machines.states.FleetState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.fleet.NpcFleetFreeze;

/**
 * Server: a frozen NPC fleet's loaded members are not sent on toward the fleet's goal, and are not warped toward it
 * after three minutes stuck in one sector (vanilla's unstick, which would otherwise end a freeze).
 * {@link FleetMoveFreezeMixin} stops the ones already flying.
 */
@Mixin(value = FleetState.class, remap = false)
public abstract class FleetLoadedFreezeMixin {

	@Inject(method = "handleLoadedMoving", at = @At("HEAD"), cancellable = true)
	private void betterFactions$frozen(FleetMember flagShip, Vector3i goal, CallbackInfo ci) {
		if (NpcFleetFreeze.frozen(((FleetState) (Object) this).getEntityState())) {
			ci.cancel();
		}
	}
}
