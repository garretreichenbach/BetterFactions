package thederpgamer.betterfactions.gui.faction.diplomacy.war;

import api.utils.gui.GUIInputDialog;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/15/2021]
 */
public class PeaceDealDialog extends GUIInputDialog {

    @Override
    public PeaceDealPanel createPanel() {
        return new PeaceDealPanel(getState(), this);
    }

    @Override
    public PeaceDealPanel getInputPanel() {
        return (PeaceDealPanel) super.getInputPanel();
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
}
