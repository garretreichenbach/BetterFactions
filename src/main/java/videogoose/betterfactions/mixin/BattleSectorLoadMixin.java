package videogoose.betterfactions.mixin;

import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.Universe;
import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;

import videogoose.betterfactions.battle.BattleTracker;

/**
 * Times a battle sector's own update (physics, projectiles, pulses), for the admin profiler. Entity
 * updates are timed separately by {@link BattleLoadMixin}. A paused battle with nobody in it skips its
 * sector update entirely, so projectiles and drift hold still too.
 */
@Mixin(value = Universe.class, remap = false)
public abstract class BattleSectorLoadMixin {

	@Redirect(method = "updateSector", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/common/data/world/Sector;update(Lorg/schema/schine/graphicsengine/core/Timer;)V"))
	private void betterFactions$timeBattleSector(Sector sector, Timer timer) throws IOException {
		if (BattleTracker.isBattle(sector.getId())) {
			if (BattleTracker.isFrozen(sector.getId())) {
				return;
			}
			long start = System.nanoTime();
			sector.update(timer);
			BattleTracker.addCost(sector.getId(), System.nanoTime() - start);
		} else {
			sector.update(timer);
		}
	}
}
