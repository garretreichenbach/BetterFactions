package videogoose.betterfactions.mixin;

import org.schema.game.common.data.player.faction.FactionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.DiplomacyManager;

/**
 * Pays tribute and publishes the mod's point lines on the engine's own faction point income turn, right
 * after every faction's gain and expenses for the period are settled, so the tributary's earnings for
 * the turn are known.
 */
@Mixin(value = FactionManager.class, remap = false)
public abstract class TributeTurnMixin {

	@Inject(method = "updateFactionPoints", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
			target = "Lorg/schema/game/common/data/player/faction/FactionManager;cleanupOrphanedNPCFactions(Lorg/schema/game/server/data/GameServerState;)V"))
	private void betterFactions$payTribute(CallbackInfo ci) {
		DiplomacyManager.of(true).onPointTurn();
	}
}
