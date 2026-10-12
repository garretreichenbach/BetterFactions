package videogoose.betterfactions.mixin;

import org.schema.game.server.controller.GameServerController;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.network.objects.Sendable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import videogoose.betterfactions.battle.BattleTracker;

/**
 * Times every entity update in a battle sector, for the admin profiler. The server loop is where the
 * whole update happens (each subclass calls super), so the call is timed there, not in any one class.
 * Entities outside battles pay one map lookup. A paused battle with nobody in it is not updated at all,
 * and in a soft-loaded one each ship is updated one tick in {@code soft_loaded_entity_tick_divisor}.
 */
@Mixin(value = GameServerController.class, remap = false)
public abstract class BattleLoadMixin {

	@Redirect(method = "update", at = @At(value = "INVOKE",
			target = "Lorg/schema/schine/network/objects/Sendable;updateLocal(Lorg/schema/schine/graphicsengine/core/Timer;)V"))
	private void betterFactions$timeBattleEntity(Sendable s, Timer timer) {
		if (s instanceof SimpleTransformableSendableObject<?> o && BattleTracker.isBattle(o.getSectorId())) {
			if (BattleTracker.isFrozen(o.getSectorId())) {
				return; //paused with nobody there: the sector holds still
			}
			int every = BattleTracker.entityTickEvery(o);
			if (every == 0) {
				return;
			}
			long start = System.nanoTime();
			if (every == 1) {
				s.updateLocal(timer);
			} else {
				//this tick stands for the ones it skipped: regen, cooldowns and the like keep their pace
				TimerAccessor t = (TimerAccessor) timer;
				double delta = t.betterFactions$delta();
				t.betterFactions$setDelta(delta * every);
				try {
					s.updateLocal(timer);
				} finally {
					t.betterFactions$setDelta(delta);
				}
			}
			BattleTracker.addCost(o.getSectorId(), System.nanoTime() - start);
		} else {
			s.updateLocal(timer);
		}
	}
}
