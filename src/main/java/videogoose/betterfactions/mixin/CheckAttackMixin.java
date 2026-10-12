package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.EditableSendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.battle.BattleTracker;

/**
 * Tells the battle tracker about every hit the engine lets through. {@code checkAttack} is where the
 * engine decides a hit counts (protected sectors, invulnerable entities and god mode say no), so a
 * {@code true} from it is a hit whatever fired it.
 */
@Mixin(value = EditableSendableSegmentController.class, remap = false)
public abstract class CheckAttackMixin {

	@Inject(method = "checkAttack(Lorg/schema/game/common/controller/damage/Damager;ZZ)Z", at = @At("RETURN"))
	private void betterFactions$recordHit(Damager from, boolean a, boolean b, CallbackInfoReturnable<Boolean> cir) {
		EditableSendableSegmentController self = (EditableSendableSegmentController) (Object) this;
		if (cir.getReturnValueZ() && from != null && self.isOnServer()) {
			BattleTracker.get().onHit(self, from);
		}
	}
}
