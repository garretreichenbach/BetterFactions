package dovtech.betterfactions.gui.faction.relations;

import dovtech.betterfactions.faction.diplo.relations.FactionMessage;
import dovtech.betterfactions.gui.faction.FactionMessageDialog;
import org.schema.game.client.controller.manager.ingame.faction.FactionRelationDialog;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;

public class NewFactionRelationDialog extends FactionRelationDialog {

    private Faction from;
    private Faction to;
    private NewFactionRelationEditPanel relationEditPanel;

    public NewFactionRelationDialog(GameClientState state, Faction from, Faction to) {
        super(state, from, to);
        this.from = from;
        this.to = to;
        this.relationEditPanel = new NewFactionRelationEditPanel(state, from, to, this);
        this.relationEditPanel.setOkButton(false);
    }

    @Override
    public void callback(GUIElement callingGuiElement, MouseEvent event) {
        if(event.pressedLeftMouse()) {
            if(callingGuiElement.getUserPointer().equals("INVITE_TO_PACT")) {
                openNewFactionMessageDialog(FactionMessage.MessageType.ALLIANCE_INVITE);
            } else if(callingGuiElement.getUserPointer().equals("ASK_TO_JOIN_PACT")) {
                openNewFactionMessageDialog(FactionMessage.MessageType.JOIN_ALLIANCE_INVITE);
            }
        }
    }

    @Override
    public GUIElement getInputPanel() {
        return this.relationEditPanel;
    }

    private void openNewFactionMessageDialog(FactionMessage.MessageType messageType) {
        FactionMessageDialog messageDialog = new FactionMessageDialog(getState(), this, from, to, messageType);
        messageDialog.sendMessage();
    }
}
