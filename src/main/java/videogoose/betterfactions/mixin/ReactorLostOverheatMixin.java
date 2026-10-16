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
 * Server: a root ship with no reactor dies of damage like any other. The engine judges a ship with no reactors
 * only by its core, so two kinds of hull floated on at "Reactor HP 0/1", never dying, never counted as a loss:
 * <ul>
 *   <li>one whose last reactor block was shot away (an NPC miner, typically): it overheats then;</li>
 *   <li>one that never had a reactor: vanilla's NPC designs still run the old power system, and with old power
 *   disallowed the engine treats them as new power with no reactor. It overheats once damage has taken
 *   {@link #betterFactions$OLD_POWER_OVERHEAT} of the blocks it was seen with, old power's own overheat line.</li>
 * </ul>
 * Only damage does it: blocks removed in build mode are not a death.
 */
@Mixin(value = SegmentControllerHpController.class, remap = false)
public abstract class ReactorLostOverheatMixin {

	/** Damage this recent took the last reactor, as the engine's own HP fail-safe reckons it. */
	@Unique
	private static final long betterFactions$DAMAGE_MS = 5000;

	/** blockBehaviorConfig's old-power ("noreactor") OVERHEATING condition. */
	@Unique
	private static final double betterFactions$OLD_POWER_OVERHEAT = 0.5;

	@Unique
	private boolean betterFactions$hadReactor;

	/**
	 * Most blocks seen on a reactorless hull since it loaded.
	 * ponytail: not saved, so a hull unloaded half-wrecked counts from what is left; persist it if that matters.
	 */
	@Unique
	private int betterFactions$peakBlocks;

	@Inject(method = "updateLocal", at = @At("HEAD"))
	private void betterFactions$reactorLost(Timer timer, CallbackInfo ci) {
		SegmentControllerHpController self = (SegmentControllerHpController) (Object) this;
		SegmentController sc = self.getSegmentController();
		if (!sc.isOnServer()) {
			return;
		}
		if (sc.hasAnyReactors()) {
			betterFactions$hadReactor = true;
			betterFactions$peakBlocks = 0;
			return;
		}
		if (!(sc instanceof Ship) || !sc.railController.isRoot() || sc.isCoreOverheating() || self.isRebooting()) {
			return;
		}
		boolean damaged = sc.getState().getUpdateTime() - sc.lastDamageTaken < betterFactions$DAMAGE_MS;
		if (betterFactions$hadReactor) {
			betterFactions$hadReactor = false;
			if (damaged) {
				self.triggerOverheating();
			}
			return;
		}
		if (sc.isUsingOldPower() || !sc.isFullyLoaded()) {
			return; //old power allowed: the engine's own HP conditions handle it
		}
		int blocks = sc.getTotalElements();
		betterFactions$peakBlocks = Math.max(betterFactions$peakBlocks, blocks);
		if (damaged && blocks <= betterFactions$peakBlocks * betterFactions$OLD_POWER_OVERHEAT) {
			self.triggerOverheating();
		}
	}
}
