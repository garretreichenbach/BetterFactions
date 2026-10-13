package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.elements.shipyard.ShipyardCollectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.gui.RepairDialogs;

/**
 * The shipyard computer's "Inputs Needed" button (its only caller) is also on while ships are queued for repair at the
 * station, so the yard can be stocked before they dock. What it lists then is {@link ShipyardRepairGoalMixin}'s.
 */
@Mixin(value = ShipyardCollectionManager.class, remap = false)
public abstract class ShipyardInputsButtonMixin {

	@Inject(method = "isCurrentStateWithGoalListClient()Z", at = @At("RETURN"), cancellable = true)
	private void betterFactions$queuedRepairs(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && RepairDialogs.queued(((ShipyardCollectionManager) (Object) this).getSegmentController().getUniqueIdentifier())) {
			cir.setReturnValue(true);
		}
	}
}
