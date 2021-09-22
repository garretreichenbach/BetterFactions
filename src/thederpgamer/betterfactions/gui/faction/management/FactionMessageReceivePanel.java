package thederpgamer.betterfactions.gui.faction.management;

import api.utils.gui.GUIInputDialogPanel;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/22/2021]
 */
public class FactionMessageReceivePanel extends GUIInputDialogPanel {

    private final FactionMessage message;
    private GUITextOverlay messageOverlay;
    private GUIActivatableTextBar titleTextBar;
    private GUIActivatableTextBar messageTextBar;

    public FactionMessageReceivePanel(InputState inputState,  FactionMessage message, GUICallback guiCallback) {
        super(inputState, "FactionMessageReceivePanel", message.title, message.message, 500, 300, guiCallback);
        this.message = message;
        setOkButtonText("SEND");
    }

}
