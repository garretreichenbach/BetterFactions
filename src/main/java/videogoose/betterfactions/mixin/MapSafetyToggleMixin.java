package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.mapgui.MapToolsPanel;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.trade.SafetyMapOverlay;

/** The map's tools panel gets a "Trade Safety" checkbox beside its own ({@link SafetyMapOverlay#addToggle}). */
@Mixin(value = MapToolsPanel.class, remap = false)
public abstract class MapSafetyToggleMixin {

	@Inject(method = "init", at = @At("TAIL"))
	private void betterFactions$safetyToggle(CallbackInfo ci) {
		SafetyMapOverlay.addToggle((GUIAnchor) (Object) this);
	}
}
