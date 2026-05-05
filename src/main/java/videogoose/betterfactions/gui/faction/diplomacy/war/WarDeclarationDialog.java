package videogoose.betterfactions.gui.faction.diplomacy.war;

import api.utils.gui.GUIInputDialog;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;

/**
 * Dialog for declaring war on a faction with war goal selection.
 */
public class WarDeclarationDialog extends GUIInputDialog {

    private Faction from;
    private Faction to;

    public void setFactions(Faction from, Faction to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public WarDeclarationPanel createPanel() {
        WarDeclarationPanel panel = new WarDeclarationPanel(getState(), this);
        if (from != null && to != null) panel.createPanel(from, to);
        return panel;
    }

    @Override
    public WarDeclarationPanel getInputPanel() {
        return (WarDeclarationPanel) super.getInputPanel();
    }

    @Override
    public void callback(GUIElement element, MouseEvent mouseEvent) {
        if (mouseEvent.pressedLeftMouse()) {
            switch (((String) element.getUserPointer()).toUpperCase()) {
                case "X", "CANCEL" -> deactivate();
                case "OK", "DECLARE" -> {
                    getInputPanel().declareWar();
                    deactivate();
                }
            }
        }
    }
}
