package videogoose.betterfactions.mixin;

import org.schema.game.client.controller.PlayerButtonTilesInput;
import org.schema.game.client.view.gui.fleet.FleetOptionButtons;
import org.schema.game.common.data.fleet.Fleet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.fleet.FleetIntercepts;
import videogoose.betterfactions.gui.RepairDialogs;

/**
 * "Shipyard Repair" and "Intercept" in the fleet menu's Order dialog, next to the vanilla orders. Repair opens the same yard
 * picker as the fleet's row under Faction > Assets. The tiles are built in {@link RepairDialogs} and {@link FleetIntercepts}: a mixin
 * cannot carry anonymous classes of its own.
 */
@Mixin(value = FleetOptionButtons.class, remap = false)
public abstract class FleetRepairOrderMixin {

	@Unique
	private Fleet betterFactions$ordering;

	@Inject(method = "popupNewOrderDialog(Lorg/schema/game/common/data/fleet/Fleet;)V", at = @At("HEAD"))
	private void betterFactions$remember(Fleet fleet, CallbackInfo ci) {
		betterFactions$ordering = fleet;
	}

	@Redirect(method = "popupNewOrderDialog(Lorg/schema/game/common/data/fleet/Fleet;)V",
			at = @At(value = "INVOKE", target = "Lorg/schema/game/client/controller/PlayerButtonTilesInput;activate()V"))
	private void betterFactions$addRepairTile(PlayerButtonTilesInput a) {
		RepairDialogs.addRepairTile(a, betterFactions$ordering);
		FleetIntercepts.addTile(a, betterFactions$ordering);
		videogoose.betterfactions.fleet.TradeFleetOrders.addTiles(a, betterFactions$ordering);
		a.activate();
	}
}
