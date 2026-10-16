package videogoose.betterfactions.mixin;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.fleet.missions.machines.states.FleetState;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.controller.GameServerController;
import org.schema.game.server.controller.SectorSwitch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.fleet.NpcFleetFreeze;

/**
 * Server: a frozen NPC fleet's loaded members are not sent on toward the fleet's goal, and are not warped toward it
 * after three minutes stuck in one sector (vanilla's unstick, which would otherwise end a freeze). The rest of
 * {@code handleLoadedMoving} still runs: it is also what sets attacking and sentry fleets searching for targets,
 * and without it a frozen fleet stops fighting and its battle goes quiet and ends.
 * {@link FleetMoveFreezeMixin} stops the ones already flying.
 */
@Mixin(value = FleetState.class, remap = false)
public abstract class FleetLoadedFreezeMixin {

	@Inject(method = "moveLoadedToGoal", at = @At("HEAD"), cancellable = true)
	private void betterFactions$frozen(Ship s, Vector3i goal, CallbackInfo ci) {
		if (NpcFleetFreeze.frozen(((FleetState) (Object) this).getEntityState())) {
			ci.cancel();
		}
	}

	@Redirect(method = "handleLoadedMoving", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/server/controller/GameServerController;queueSectorSwitch(Lorg/schema/game/common/data/world/SimpleTransformableSendableObject;Lorg/schema/common/util/linAlg/Vector3i;IZZZ)Lorg/schema/game/server/controller/SectorSwitch;"))
	private SectorSwitch betterFactions$noUnstick(GameServerController c, SimpleTransformableSendableObject<?> o, Vector3i to,
			int type, boolean a, boolean b, boolean d) {
		return NpcFleetFreeze.frozen(((FleetState) (Object) this).getEntityState()) ? null : c.queueSectorSwitch(o, to, type, a, b, d);
	}
}
