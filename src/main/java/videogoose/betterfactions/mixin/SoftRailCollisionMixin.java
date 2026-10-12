package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.rails.RailCollisionScheduler;
import org.schema.game.common.controller.rails.RailController;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import videogoose.betterfactions.battle.BattleTracker;

/**
 * In a soft-loaded battle sector, turrets turn and rails move without asking whether they would clip.
 * Nobody is there to see a turret swing through its own hull. Docking keeps its check (it goes through
 * {@code checkNow} and {@code submit}, which this leaves alone): where a ship ends up docked outlives
 * the battle.
 */
@Mixin(value = RailController.class, remap = false)
public abstract class SoftRailCollisionMixin {

	@Shadow
	@Final
	private SegmentController self;

	@Redirect(method = {"update", "updateAutoRailMovementPos", "updateAutoRailMovementRot"}, at = @At(value = "INVOKE",
			target = "Lorg/schema/game/common/controller/rails/RailCollisionScheduler;checkPotentialCollisionWithRail(Lorg/schema/game/common/controller/SegmentController;[Lorg/schema/game/common/controller/SegmentController;Z)Z"))
	private boolean betterFactions$skipWhenUnwatched(RailCollisionScheduler scheduler, SegmentController docked, SegmentController[] with, boolean axisAligned) {
		if (self.isOnServer() && BattleTracker.isSoftLoaded(self.getSectorId())) {
			return false;
		}
		return scheduler.checkPotentialCollisionWithRail(docked, with, axisAligned);
	}
}
