package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.inventory.inventorynew.SecondaryInventoryPanelNew;
import org.schema.game.client.view.gui.inventory.inventorynew.ShipyardOptionsChestButtonPanel;
import org.schema.game.common.controller.SegmentController;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import videogoose.betterfactions.gui.RepairDialogs;

/**
 * A third column on the shipyard computer's order buttons: the repair queue at this station, and a way to
 * send everyone waiting in it home. The buttons are built in {@link RepairDialogs}: a mixin cannot carry
 * anonymous classes of its own.
 */
@Mixin(value = ShipyardOptionsChestButtonPanel.class, remap = false)
public abstract class ShipyardQueueButtonMixin {

	@Shadow
	private SegmentController segmentController;
	@Shadow
	private SecondaryInventoryPanelNew panel;

	@ModifyArg(method = "onInit()V", at = @At(value = "INVOKE", ordinal = 0,
			target = "Lorg/schema/schine/graphicsengine/forms/gui/newgui/GUIHorizontalButtonTablePane;<init>(Lorg/schema/schine/input/InputState;IILorg/schema/schine/graphicsengine/forms/gui/GUIElement;)V"), index = 1)
	private int betterFactions$thirdColumn(int columns) {
		return 3;
	}

	/** The last of the vanilla order buttons; ours go in after it. */
	@Redirect(method = "onInit()V", at = @At(value = "INVOKE", ordinal = 3,
			target = "Lorg/schema/schine/graphicsengine/forms/gui/newgui/GUIHorizontalButtonTablePane;addButton(IILjava/lang/Object;Lorg/schema/schine/graphicsengine/forms/gui/newgui/GUIHorizontalArea$HButtonType;Lorg/schema/schine/graphicsengine/forms/gui/GUICallback;Lorg/schema/schine/graphicsengine/forms/gui/GUIActivationCallback;)Lorg/schema/schine/graphicsengine/forms/gui/newgui/GUIHorizontalArea;"))
	private GUIHorizontalArea betterFactions$addQueueButtons(GUIHorizontalButtonTablePane ac, int x, int y, Object text,
			GUIHorizontalArea.HButtonType type, GUICallback callback, GUIActivationCallback act) {
		GUIHorizontalArea last = ac.addButton(x, y, text, type, callback, act);
		RepairDialogs.addQueueButtons(ac, segmentController, panel);
		return last;
	}
}
