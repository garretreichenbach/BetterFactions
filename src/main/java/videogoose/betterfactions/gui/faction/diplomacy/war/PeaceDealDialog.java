package videogoose.betterfactions.gui.faction.diplomacy.war;

import api.utils.gui.GUIInputDialog;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import videogoose.betterfactions.data.serializeable.war.WarData;

/**
 * Dialog for peace deal negotiations
 */
public class PeaceDealDialog extends GUIInputDialog {

    private WarData warData;

    public void setWarData(WarData warData) {
        this.warData = warData;
    }

    @Override
    public PeaceDealPanel createPanel() {
        PeaceDealPanel panel = new PeaceDealPanel(getState(), this);
        if (warData != null) panel.createPanel(warData);
        return panel;
    }

    @Override
    public PeaceDealPanel getInputPanel() {
        return (PeaceDealPanel) super.getInputPanel();
    }

    @Override
    public void callback(GUIElement element, MouseEvent mouseEvent) {
        if (mouseEvent.pressedLeftMouse()) {
            switch (((String) element.getUserPointer()).toUpperCase()) {
                case "X", "CANCEL" -> deactivate();
                case "OK", "SEND" -> {
                    getInputPanel().sendMessage();
                    deactivate();
                }
            }
        }
    }
}
