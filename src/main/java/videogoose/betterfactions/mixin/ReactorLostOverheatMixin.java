package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SegmentControllerHpController;
import org.schema.game.common.controller.Ship;
import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server: a root ship whose last reactor block is shot away overheats. The engine judges a ship with no reactors
 * only by its core, so a gutted hull with its core intact (an NPC miner, typically) floated on at "Reactor HP 0/1",
 * never dying, never counted as a loss. Only damage does it: a reactor taken out in build mode is not a death.
 */
@Mixin(value = SegmentControllerHpController.class, remap = false)
public abstract class ReactorLostOverheatMixin {

	/** Damage this recent took the last reactor, as the engine's own HP fail-safe reckons it. */
	@Unique
	private static final long betterFactions$DAMAGE_MS = 5000;

	@Unique
	private boolean betterFactions$hadReactor;

	@Inject(method = "updateLocal", at = @At("HEAD"))
	private void betterFactions$reactorLost(Timer timer, CallbackInfo ci) {
		SegmentControllerHpController self = (SegmentControllerHpController) (Object) this;
		SegmentController sc = self.getSegmentController();
		if (!sc.isOnServer()) {
			return;
		}
		if (sc.hasAnyReactors()) {
			betterFactions$hadReactor = true;
			return;
		}
		if (!betterFactions$hadReactor) {
			return;
		}
		betterFactions$hadReactor = false;
		if (sc instanceof Ship && sc.railController.isRoot() && !sc.isCoreOverheating() && !self.isRebooting()
				&& sc.getState().getUpdateTime() - sc.lastDamageTaken < betterFactions$DAMAGE_MS) {
			self.triggerOverheating();
		}
	}
}
