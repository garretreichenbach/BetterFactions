package videogoose.betterfactions.mixin;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.Universe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.battle.BattleTracker;

/**
 * A player keeps the 26 sectors around them loaded. An admin watching a battle keeps none: only the
 * battle sector, which the tracker already keeps alive, so watching costs the server nothing extra.
 */
@Mixin(value = Universe.class, remap = false)
public abstract class SpectatorPingMixin {

	@Inject(method = "pingSectorBlock", at = @At("HEAD"), cancellable = true)
	private void betterFactions$spectatorsLoadNothing(Vector3i currentSector, CallbackInfo ci) {
		if (BattleTracker.onlySpectatorsAt(currentSector)) {
			ci.cancel();
		}
	}
}
