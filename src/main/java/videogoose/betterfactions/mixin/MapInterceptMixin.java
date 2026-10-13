package videogoose.betterfactions.mixin;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.controller.manager.ingame.map.MapControllerManager;
import org.schema.game.client.controller.manager.ingame.map.MapEntryOptionsMenu;
import org.schema.game.client.view.gui.RadialMenu;
import org.schema.game.client.view.gui.RadialMenuDialog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.fleet.FleetIntercepts;

/** "Intercept" on the map's radial menu, after Move To, Attack and Defend, while fleets are selected. */
@Mixin(value = MapEntryOptionsMenu.class, remap = false)
public abstract class MapInterceptMixin {

	@Shadow
	private Vector3i getEntryPos() {
		return null;
	}

	@Inject(method = "createMenu(Lorg/schema/game/client/view/gui/RadialMenuDialog;)Lorg/schema/game/client/view/gui/RadialMenu;", at = @At("RETURN"))
	private void betterFactions$addIntercept(RadialMenuDialog dialog, CallbackInfoReturnable<RadialMenu> cir) {
		if (!MapControllerManager.selectedFleets.isEmpty()) {
			FleetIntercepts.addRadialItem(cir.getReturnValue(), (RadialMenuDialog) (Object) this, getEntryPos());
		}
	}
}
