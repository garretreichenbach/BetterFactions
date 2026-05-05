package videogoose.betterfactions.mixin;

import org.schema.game.common.data.player.faction.FactionRelation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

/**
 * Mixin for FactionRelation to handle custom relation types (NON_AGGRESSION, FEDERATION)
 * in the string parsing method. The actual enum values are injected at runtime via ReflectionUtils.
 */
@Mixin(FactionRelation.class)
public abstract class MixinFactionRelation {

	@Inject(method = "getRelationFromString", at = @At("HEAD"), cancellable = true)
	private static void betterfactions$handleCustomRelations(String string, CallbackInfoReturnable<Byte> cir) {
		String lower = string.toLowerCase(Locale.ENGLISH);
		if ("federation".equals(lower)) {
			cir.setReturnValue(CustomRelationType.FEDERATION);
		} else if ("non-aggression".equals(lower)) {
			cir.setReturnValue(CustomRelationType.NON_AGGRESSION);
		}
	}
}
