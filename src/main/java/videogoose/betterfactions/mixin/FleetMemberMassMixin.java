package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.fleet.FleetMember;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Server: a fleet member's mass as the fleet menu shows it. Vanilla takes it from the physics body, which is
 * zero for a ship docked to anything (its mass rides on the root) and for one not yet given a body, only
 * reads it when the member is added or its sector loads, and never stores it - so after a restart every
 * member reads 0. Here it is the block mass with docks, and it is kept current while the ship is loaded.
 */
@Mixin(value = FleetMember.class, remap = false)
public abstract class FleetMemberMassMixin {

	@Shadow
	private double lastMass;

	@Shadow
	public abstract SegmentController getLoaded();

	@Shadow
	public abstract boolean isOnServer();

	@Redirect(method = {"<init>(Lorg/schema/game/common/controller/SegmentController;)V", "loadEntityInSectorWithDocks"},
			at = @At(value = "INVOKE", target = "Lorg/schema/game/common/controller/SegmentController;getTotalPhysicalMass()F"))
	private float betterFactions$blockMass(SegmentController sc) {
		return sc.getMassWithoutDockIncludingStation();
	}

	/**
	 * Rides the fleet's periodic stats check, which resends the fleet to clients when this answers true.
	 * Only a change of more than half a percent counts, so cargo trickling in does not resend it every check.
	 */
	@Inject(method = "consumeStatsChanged()Z", at = @At("HEAD"), cancellable = true)
	private void betterFactions$refreshMass(CallbackInfoReturnable<Boolean> cir) {
		if (!isOnServer()) {
			return;
		}
		SegmentController sc = getLoaded();
		if (sc == null) {
			return;
		}
		double m = sc.getMassWithoutDockIncludingStation();
		if (Math.abs(m - lastMass) > Math.max(1, lastMass * 0.005)) {
			lastMass = m;
			cir.setReturnValue(true);
		}
	}
}
