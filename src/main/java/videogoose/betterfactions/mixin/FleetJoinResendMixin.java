package videogoose.betterfactions.mixin;

import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetManager;
import org.schema.game.common.data.player.PlayerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/**
 * Server: vanilla's join only sends the fleets loadByOwner loads itself, so a fleet already cached (the mod's
 * orders, upkeep, or a fleet still cached from an earlier session) never reaches the client until it changes.
 * Resend every cached fleet the player can see, at the point vanilla sends its own.
 */
@Mixin(value = FleetManager.class, remap = false)
public abstract class FleetJoinResendMixin {

	@Inject(method = "onJoinedPlayer", at = @At("TAIL"))
	private void betterFactions$resendCached(PlayerState player, CallbackInfo ci) {
		for (Fleet f : new ArrayList<>(((FleetManager) (Object) this).fleetCache.values())) {
			if (!f.isAnyNPCFleet() && f.canAccess(player.getName())) {
				f.sendFleet();
			}
		}
	}
}
