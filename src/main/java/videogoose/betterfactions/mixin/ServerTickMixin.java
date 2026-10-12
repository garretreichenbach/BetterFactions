package videogoose.betterfactions.mixin;

import org.schema.game.server.controller.GameServerController;
import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.battle.BattleTracker;

/**
 * Times every server tick, so soft-loaded battles only run on time the server has to spare. The
 * engine's own tick profiler only measures while its config switch is on.
 */
@Mixin(value = GameServerController.class, remap = false)
public abstract class ServerTickMixin {

	@Unique
	private long betterFactions$tickStart;

	@Inject(method = "update(Lorg/schema/schine/graphicsengine/core/Timer;)V", at = @At("HEAD"))
	private void betterFactions$tickStart(Timer timer, CallbackInfo ci) {
		betterFactions$tickStart = System.nanoTime();
	}

	@Inject(method = "update(Lorg/schema/schine/graphicsengine/core/Timer;)V", at = @At("RETURN"))
	private void betterFactions$tickEnd(Timer timer, CallbackInfo ci) {
		BattleTracker.recordTick(System.nanoTime() - betterFactions$tickStart);
	}
}
