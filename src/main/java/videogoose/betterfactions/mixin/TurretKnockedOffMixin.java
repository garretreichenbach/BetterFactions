package videogoose.betterfactions.mixin;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.rails.RailController;
import org.schema.game.common.controller.rails.RailRelation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.Wrecks;

/** Remembers which root a turret came off, so it can be removed when that root wrecks. */
@Mixin(value = RailController.class, remap = false)
public abstract class TurretKnockedOffMixin {

	@Shadow
	@Final
	private SegmentController self;
	@Shadow
	public RailRelation previous;

	@Inject(method = "onUndockedServer", at = @At("HEAD"))
	private void betterFactions$knockedOff(Vector3i currentRailContact, CallbackInfo ci) {
		if (previous != null && previous.isTurretDock()) {
			SegmentController root = previous.rail.getSegmentController().railController.getRoot();
			Wrecks.get().onTurretKnockedOff(self.getUniqueIdentifier(), root.getUniqueIdentifier());
		}
	}
}
