package videogoose.betterfactions.mixin;

import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetManager;
import org.schema.game.network.objects.remote.FleetCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import videogoose.betterfactions.fleet.FleetIntercepts;
import videogoose.betterfactions.fleet.TradeFleetOrders;

/**
 * Server: an order a player sends a fleet ends whatever standing order the mod gave it (raid, escort, hold).
 * Only commands arriving from clients pass here; the mod's own re-issued commands go straight to executeCommand.
 */
@Mixin(value = FleetManager.class, remap = false)
public abstract class FleetCommandReceiveMixin {

	@Redirect(method = "updateFromNetworkObject", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/common/data/fleet/FleetManager;executeCommand(Lorg/schema/game/network/objects/remote/FleetCommand;)V"))
	private void betterFactions$playerOrder(FleetManager self, FleetCommand c) {
		Fleet fleet = self.isOnServer() ? self.getByFleetDbId(c.fleetDbId) : null;
		if (fleet != null) {
			TradeFleetOrders.get().cancel(fleet);
			FleetIntercepts.get().cancel(fleet);
		}
		self.executeCommand(c);
	}
}
