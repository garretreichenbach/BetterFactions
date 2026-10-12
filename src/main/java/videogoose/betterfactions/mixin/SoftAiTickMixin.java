package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.Ship;
import org.schema.game.server.ai.ShipAIEntity;
import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.battle.BattleTracker;

/**
 * In a soft-loaded battle sector, ship AI runs one tick in {@code soft_loaded_ai_tick_divisor}. Both
 * sides lose the same reaction time and turn rate, so it costs accuracy, not fairness. Physics and
 * weapons still tick every frame; a ship keeps flying on its last orders between AI ticks.
 */
@Mixin(value = ShipAIEntity.class, remap = false)
public abstract class SoftAiTickMixin {

	@Inject(method = "updateOnActive(Lorg/schema/schine/graphicsengine/core/Timer;)V", at = @At("HEAD"), cancellable = true)
	private void betterFactions$throttleWhenUnwatched(Timer timer, CallbackInfo ci) {
		Ship ship = ((ShipAIEntity) (Object) this).getEntity();
		if (ship != null && ship.isOnServer() && BattleTracker.skipAiTick(ship.getSectorId(), ship.getId())) {
			ci.cancel();
		}
	}
}
