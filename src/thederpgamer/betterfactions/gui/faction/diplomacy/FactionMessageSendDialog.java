package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.utils.gui.GUIInputDialog;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/16/2021]
 */
public class FactionMessageSendDialog extends GUIInputDialog {

    @Override
    public FactionMessageSendPanel createPanel() {
        return new FactionMessageSendPanel(getState(), this);
    }

    @Override
    public FactionMessageSendPanel getInputPanel() {
        return (FactionMessageSendPanel) super.getInputPanel();
    }

    @Override
    public void callback(GUIElement element, MouseEvent mouseEvent) {
        if(mouseEvent.pressedLeftMouse()) {
            switch(((String) element.getUserPointer()).toUpperCase()) {
                case "X":
                case "CANCEL":
                    deactivate();
                    break;
                case "OK":
                case "SEND":
                    getInputPanel().sendMessage();
                    deactivate();
                    break;
            }
        }
    }

    public String getTitleText() {
        return getInputPanel().getTitleText();
    }

    public String getMessageText() {
        return getInputPanel().getMessageText();
    }
}
