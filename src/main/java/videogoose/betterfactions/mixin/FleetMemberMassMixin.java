package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.FleetUpkeep;

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
		float m = sc.getMassWithoutDockIncludingStation();
		if (sc.isOnServer()) {
			FleetUpkeep.get().sawMass(sc.getUniqueIdentifier(), m);
		}
		return m;
	}

	/**
	 * A member read from the database has no mass until its ship loads; start it at the last one seen. A ship
	 * never seen loaded (spawned straight into the database) falls back to the blueprint it was spawned from.
	 */
	@Inject(method = "<init>(Lorg/schema/game/server/data/GameServerState;JLjava/lang/String;Ljava/lang/String;Lorg/schema/common/util/linAlg/Vector3i;JJ)V",
			at = @At("RETURN"))
	private void betterFactions$savedMass(CallbackInfo ci) {
		FleetMember self = (FleetMember) (Object) this;
		lastMass = FleetUpkeep.get().massOf(self.UID);
		if (lastMass <= 0 && self.name != null) {
			lastMass = blueprintMass(self.name);
		}
	}

	/** todo: guesses by the spawn naming "<blueprint>_<millis>"; a renamed ship stays 0 until it loads. */
	private static double blueprintMass(String shipName) {
		String bp = shipName.replaceFirst("_\\d+$", "");
		for (BlueprintEntry e : BluePrintController.active.readBluePrints()) {
			if (e.getName().equals(bp)) {
				return e.getMass();
			}
		}
		return 0;
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
		FleetUpkeep.get().sawMass(sc.getUniqueIdentifier(), m);
		if (Math.abs(m - lastMass) > Math.max(1, lastMass * 0.005)) {
			lastMass = m;
			cir.setReturnValue(true);
		}
	}
}
