package videogoose.betterfactions.mixin;

import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.fleet.FleetLabels;

/**
 * Server: a fleet under one of the mod's orders (intercepting, shipyard repair) shows that order in the fleet list
 * and on the map, whatever state its vanilla command is in. Every state change sends its name out through here.
 */
@Mixin(value = FleetManager.class, remap = false)
public abstract class FleetMissionLabelMixin {

	@Inject(method = "submitMissionChangeToClients(Lorg/schema/game/common/data/fleet/Fleet;)V", at = @At("HEAD"))
	private void betterFactions$label(Fleet fleet, CallbackInfo ci) {
		String label = fleet.isOnServer() ? FleetLabels.of(fleet) : null;
		if (label != null) {
			fleet.missionString = label;
		}
	}
}
