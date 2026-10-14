package videogoose.betterfactions.mixin;

import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client: vanilla drops a resent fleet whose dbid and member list match the one it has, since Fleet.equals looks at
 * nothing else, so a member's mass, HP or engage range sent from the server never lands. Always apply it.
 */
@Mixin(value = FleetManager.class, remap = false)
public abstract class FleetResendMixin {

	@Redirect(method = "updateFromNetworkObject",
			at = @At(value = "INVOKE", target = "Lorg/schema/game/common/data/fleet/Fleet;equals(Ljava/lang/Object;)Z"))
	private boolean betterFactions$alwaysApply(Fleet mine, Object remote) {
		return mine.isOnServer() && mine.equals(remote);
	}
}
