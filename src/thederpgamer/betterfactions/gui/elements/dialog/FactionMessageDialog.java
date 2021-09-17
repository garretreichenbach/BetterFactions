package thederpgamer.betterfactions.gui.elements.dialog;

import api.utils.gui.GUIInputDialog;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/16/2021]
 */
public class FactionMessageDialog extends GUIInputDialog {

    public FactionMessageDialog() {

    }

    @Override
    public FactionMessagePanel createPanel() {
        return new FactionMessagePanel(getState(), this);
    }

    @Override
    public void callback(GUIElement callingElement, MouseEvent mouseEvent) {
        if(!isOccluded() && mouseEvent.pressedLeftMouse()) {
            switch(((String) callingElement.getUserPointer()).toUpperCase()) {
                case "X":
                case "CANCEL":
                    deactivate();
                    break;
                case "OK":

                    break;
            }
        }
    }

    public void sendMessage() {

    }
}
