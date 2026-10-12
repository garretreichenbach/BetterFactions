package videogoose.betterfactions.mixin;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.Universe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.battle.BattleTracker;

import java.io.IOException;

/**
 * A player keeps the 26 sectors around them loaded. An admin watching a battle keeps only their own:
 * the battle sector, which the tracker keeps alive anyway, so watching costs the server nothing extra.
 * Their own sector is still pinged: if the tracker lets go first (over the soft cap, the battle ending
 * before the jump back lands), it would unload under them, and their client would spam
 * "sector yet not loaded" for every entity it still holds there.
 */
@Mixin(value = Universe.class, remap = false)
public abstract class SpectatorPingMixin {

	@Inject(method = "pingSectorBlock", at = @At("HEAD"), cancellable = true)
	private void betterFactions$spectatorsLoadNothing(Vector3i currentSector, CallbackInfo ci) throws IOException {
		if (BattleTracker.onlySpectatorsAt(currentSector)) {
			((Universe) (Object) this).getSector(currentSector).ping();
			ci.cancel();
		}
	}
}
