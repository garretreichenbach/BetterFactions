package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.SegmentController;
import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.Wrecks;

/**
 * When a root ship's overheat timer runs out, it becomes a wreck instead of being destroyed (see
 * {@link Wrecks}); rebooting one makes it a ship again.
 */
@Mixin(value = SegmentController.class, remap = false)
public abstract class WreckOverheatMixin {

	@Inject(method = "updateCoreOverheating", at = @At("HEAD"), cancellable = true)
	private void betterFactions$wreck(Timer timer, long time, CallbackInfo ci) {
		SegmentController self = (SegmentController) (Object) this;
		if (self.isOnServer() && self.isCoreOverheating()
				&& time - self.getCoreTimerStarted() > self.getCoreTimerDuration()
				&& Wrecks.get().tryWreck(self, time)) {
			ci.cancel();
		}
	}

	@Inject(method = "stopCoreOverheating", at = @At("HEAD"))
	private void betterFactions$rebooted(CallbackInfo ci) {
		SegmentController self = (SegmentController) (Object) this;
		if (self.isOnServer()) {
			Wrecks.get().onRebooted(self.getUniqueIdentifier());
		}
	}
}
