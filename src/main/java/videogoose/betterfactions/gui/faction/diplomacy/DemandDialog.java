package videogoose.betterfactions.gui.faction.diplomacy;

import api.utils.gui.GUIInputDialog;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;

/**
 * Dialog for sending non-hostile demands to another faction.
 * Rejected demands generate a casus belli for the demanding faction.
 */
public class DemandDialog extends GUIInputDialog {

    private Faction from;
    private Faction to;

    public void setFactions(Faction from, Faction to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public DemandPanel createPanel() {
        DemandPanel panel = new DemandPanel(getState(), this);
        if (from != null && to != null) panel.createPanel(from, to);
        return panel;
    }

    @Override
    public DemandPanel getInputPanel() {
        return (DemandPanel) super.getInputPanel();
    }

    @Override
    public void callback(GUIElement element, MouseEvent mouseEvent) {
        if (mouseEvent.pressedLeftMouse()) {
            switch (((String) element.getUserPointer()).toUpperCase()) {
                case "X", "CANCEL" -> deactivate();
                case "OK", "SEND" -> {
                    getInputPanel().sendDemand();
                    deactivate();
                }
            }
        }
    }
}
