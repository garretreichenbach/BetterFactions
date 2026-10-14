package videogoose.betterfactions.mixin;

import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.fleet.FleetUnloadedAction;
import org.schema.game.common.data.fleet.FleetUnloadedActionMoveTo;
import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.FleetUpkeep;
import videogoose.betterfactions.fleet.NpcFleetFreeze;

/**
 * Server: an unloaded fleet member's sector-to-sector move. Vanilla takes {@code FLEET_OUT_OF_SECTOR_MOVEMENT} per
 * sector for every fleet; here that time is divided by {@link FleetUpkeep#speedFactor}, so a fleet of fast ships
 * crosses space faster. The whole fleet moves at its slowest member's speed, so members still arrive together.
 *
 * <p>While {@code /npc_fleets_frozen} is on, NPC fleets' unloaded members do not move at all.</p>
 */
@Mixin(value = FleetUnloadedActionMoveTo.class, remap = false)
public abstract class FleetUnloadedMoveMixin extends FleetUnloadedAction {

	private FleetUnloadedMoveMixin(FleetMember member, Fleet fleet) {
		super(member, fleet);
	}

	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	private void betterFactions$frozen(Timer time, CallbackInfoReturnable<Boolean> cir) {
		if (NpcFleetFreeze.frozen(fleet)) {
			reset(time); //resumes a full sector's time after the thaw, not at once
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "getTimeTakenPerSector", at = @At("RETURN"), cancellable = true)
	private void betterFactions$fleetSpeed(CallbackInfoReturnable<Long> cir) {
		float reference = BFConfig.fleetSpeedReferenceTmr();
		if (reference <= 0 || fleet == null) {
			return;
		}
		// ponytail: recomputed per member per check (a map lookup per member); cache per fleet if fleets get huge
		float[] tmrs = new float[fleet.getMembers().size()];
		for (int i = 0; i < tmrs.length; i++) {
			tmrs[i] = FleetUpkeep.get().thrustMassRatioOf(fleet.getMembers().get(i).UID);
		}
		cir.setReturnValue((long) (cir.getReturnValueJ() / FleetUpkeep.speedFactor(tmrs, reference, 0.25f, 4f)));
	}
}
