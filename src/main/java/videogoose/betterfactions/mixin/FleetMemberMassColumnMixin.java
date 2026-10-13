package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.fleet.FleetMemberScrollableListNew;
import org.schema.game.common.data.fleet.FleetMember;
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
 * Client: a Mass column, each ship's own, on the vanilla fleet member list.
 */
@Mixin(value = FleetMemberScrollableListNew.class, remap = false)
public abstract class FleetMemberMassColumnMixin extends ScrollableTableList<FleetMember> {

	private FleetMemberMassColumnMixin(InputState state, float width, float height, GUIElement p) {
		super(state, width, height, p);
	}

	@Inject(method = "initColumns()V", at = @At("TAIL"))
	private void betterFactions$massColumn(CallbackInfo ci) {
		addFixedWidthColumn(FleetMassColumns.title(), FleetMassColumns.WIDTH, FleetMassColumns.MEMBER_ORDER);
	}

	@ModifyArg(method = "updateListEntries", at = @At(value = "INVOKE",
			target = "Lorg/schema/game/client/view/gui/fleet/FleetMemberScrollableListNew$FleetMemberRow;<init>(Lorg/schema/game/client/view/gui/fleet/FleetMemberScrollableListNew;Lorg/schema/schine/input/InputState;Lorg/schema/game/common/data/fleet/FleetMember;[Lorg/schema/schine/graphicsengine/forms/gui/GUIElement;)V"),
			index = 3)
	private GUIElement[] betterFactions$massCell(FleetMemberScrollableListNew outer, InputState state, FleetMember m, GUIElement[] cells) {
		return FleetMassColumns.withMemberCell(outer, m, cells);
	}
}
