package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.fleet.FleetScrollableListNew;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.gui.FleetMassColumns;

/**
 * Client: a Mass column, the fleet's total, on the vanilla fleet list. Appended last so the vanilla
 * columns keep their indices.
 */
@Mixin(value = FleetScrollableListNew.class, remap = false)
public abstract class FleetMassColumnMixin extends ScrollableTableList<Fleet> {

	private FleetMassColumnMixin(InputState state, float width, float height, GUIElement p) {
		super(state, width, height, p);
	}

	@Inject(method = "initColumns()V", at = @At("TAIL"))
	private void betterFactions$massColumn(CallbackInfo ci) {
		addFixedWidthColumn(FleetMassColumns.title(), FleetMassColumns.WIDTH, FleetMassColumns.FLEET_ORDER);
	}

	@ModifyArg(method = "updateListEntries", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/client/view/gui/fleet/FleetScrollableListNew$FleetRow;<init>(Lorg/schema/game/client/view/gui/fleet/FleetScrollableListNew;Lorg/schema/schine/input/InputState;Lorg/schema/game/common/data/fleet/Fleet;[Lorg/schema/schine/graphicsengine/forms/gui/GUIElement;)V"),
			index = 3)
	private GUIElement[] betterFactions$massCell(FleetScrollableListNew outer, InputState state, Fleet f, GUIElement[] cells) {
		return FleetMassColumns.withFleetCell(outer, f, cells);
	}
}
