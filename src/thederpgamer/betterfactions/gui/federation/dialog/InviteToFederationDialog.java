package thederpgamer.betterfactions.gui.federation.dialog;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.KeyEventInterface;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;
import thederpgamer.betterfactions.data.persistent.federation.FederationData;
import thederpgamer.betterfactions.gui.elements.dialog.FactionMessagePanel;

/**
 * InviteToFederationDialog.java
 * <Description>
 * ==================================================
 * Created 02/08/2021
 * @author TheDerpGamer
 */
public class InviteToFederationDialog extends PlayerInput {

    private FactionMessagePanel messagePanel;

    public InviteToFederationDialog(Faction from, Faction to, FederationData federationData) {
        super(GameClient.getClientState());
        messagePanel = new FactionMessagePanel(GameClient.getClientState(), this, getDefaultTitle(from, federationData), from, to);
        messagePanel.setCallback(this);
    }

    @Override
    public void onDeactivate() {
        messagePanel.cleanUp();
    }

    @Override
    public void handleMouseEvent(MouseEvent mouseEvent) {

    }

    @Override
    public void handleKeyEvent(KeyEventInterface e) {
        super.handleKeyEvent(e);
    }

    @Override
    public GUIElement getInputPanel() {
        return messagePanel;
    }

    @Override
    public void callback(GUIElement callingGuiElement, MouseEvent event) {
        if(!isOccluded()) {
            if(event.pressedLeftMouse()) {
                if (callingGuiElement.getUserPointer().equals("OK")) {
                    if(messagePanel.isValid()) {
                        messagePanel.sendMessage(FactionMessage.MessageType.FEDERATION_INVITE);
                        deactivate();
                    }
                }
                if (callingGuiElement.getUserPointer().equals("CANCEL") || callingGuiElement.getUserPointer().equals("X")) {
                    cancel();
                }
            }
        }
    }

    private String getDefaultTitle(Faction from, FederationData federationData) {
        return Lng.str("Invitation from " + from.getName() + " to " + federationData.getName() + ".");
    }
}
