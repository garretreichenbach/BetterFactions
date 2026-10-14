package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.Ship;
import org.schema.game.server.ai.AIShipControllerStateUnit;
import org.schema.game.server.ai.ShipAIEntity;
import org.schema.game.server.ai.program.fleetcontrollable.states.FleetMovingToSector;
import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.fleet.NpcFleetFreeze;

/** Server: a loaded ship of a frozen NPC fleet that is flying to a sector stops where it is. */
@Mixin(value = FleetMovingToSector.class, remap = false)
public abstract class FleetMoveFreezeMixin {

	@Inject(method = "updateAI", at = @At("HEAD"), cancellable = true)
	private void betterFactions$frozen(AIShipControllerStateUnit unit, Timer timer, Ship entity, ShipAIEntity s, CallbackInfo ci) {
		if (NpcFleetFreeze.frozen(entity.getFleet())) {
			entity.getNetworkObject().moveDir.set(0, 0, 0);
			s.stop();
			ci.cancel();
		}
	}
}
