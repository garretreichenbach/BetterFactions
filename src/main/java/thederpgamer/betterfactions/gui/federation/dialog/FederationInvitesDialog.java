package thederpgamer.betterfactions.gui.federation.dialog;

import api.common.GameClient;
import api.utils.gui.SimplePopup;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.view.gui.faction.newfaction.FactionInvitationsAbstractScrollListNew;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.input.InputState;
import org.schema.schine.network.client.ClientState;
import thederpgamer.betterfactions.data.old.federation.FactionMessage;
import thederpgamer.betterfactions.manager.FactionManagerOld;
import thederpgamer.betterfactions.manager.data.FederationManager;

import javax.vecmath.Vector4f;

/**
 * FederationInvitesDialog.java
 * <Description>
 *
 * @since 02/09/2021
 * @author TheDerpGamer
 */
public class FederationInvitesDialog extends PlayerGameOkCancelInput {

    public FederationInvitesDialog() {
        super("FederationInvitesDialog", GameClient.getClientState(), "Federation Invitations", "");
        getInputPanel().onInit();
        FederationInvitesPanel panel = new FederationInvitesPanel(GameClient.getClientState(), this.getInputPanel().getContent());
        panel.onInit();
        getInputPanel().getContent().attach(panel);
    }

    @Override
    public void onDeactivate() {

    }

    @Override
    public void pressedOK() {

    }

    private class FederationInvitesPanel extends FactionInvitationsAbstractScrollListNew {

        public FederationInvitesPanel(ClientState clientState, GUIElement guiElement) {
            super(clientState, guiElement);
        }

        @Override
        protected void updateInvitationList(GUIElementList elementList) {
            elementList.clear();
            int i = 0;
            FactionDataOld factionData = FactionManagerOld.getPlayerFactionData(GameClient.getClientPlayerState().getName());
            for(FactionMessage message : factionData.getInbox()) {
                if(message.messageType.equals(FactionMessage.MessageType.FEDERATION_INVITE)) {
                    elementList.add(new IncomingInvitationListElement(getState(), message, i));
                    i ++;
                }
            }
        }

        private class IncomingInvitationListElement extends GUIListElement implements GUICallback {
            private GUIColoredRectangle bg;
            private FactionMessage invite;

            public IncomingInvitationListElement(InputState state, FactionMessage invite, int i) {
                super(state);
                this.invite = invite;
                this.bg = new GUIColoredRectangle(this.getState(), 410.0F, 45.0F, i % 2 == 0 ? new Vector4f(0.1F, 0.1F, 0.1F, 1.0F) : new Vector4f(0.2F, 0.2F, 0.2F, 2.0F));
                this.bg.renderMode = 2;
                this.setContent(this.bg);
                this.setSelectContent(this.bg);
            }

            public void draw() {
                this.bg.setWidth(FederationInvitesPanel.this.dependent.getWidth());
                super.draw();
            }

            public void onInit() {
                super.onInit();

                GUITextOverlay fromOverlay = new GUITextOverlay(200, 30, FontLibrary.getBlenderProMedium16(), this.getState());
                fromOverlay.autoWrapOn = bg;
                fromOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
                fromOverlay.setTextSimple(invite.title + "\nFrom: " + invite.getSender().getName() + " | " + invite.date);

                GUITextOverlay messageOverlay = new GUITextOverlay(200, 50, FontLibrary.FontSize.SMALL.getFont(), getState());
                messageOverlay.autoWrapOn = bg;
                messageOverlay.setFont(FontLibrary.FontSize.SMALL.getFont());
                messageOverlay.setTextSimple(invite.message);

                GUITextButton declineButton = new GUITextButton(this.getState(), 50, 20, "Decline", this);
                declineButton.setUserPointer("DECLINE");
                GUITextButton acceptButton = new GUITextButton(this.getState(), 50, 20, "Accept", this);
                acceptButton.setUserPointer("ACCEPT");

                this.bg.attach(fromOverlay);
                this.bg.attach(messageOverlay);
                this.bg.attach(declineButton);
                this.bg.attach(acceptButton);
                acceptButton.getPos().x = 310.0F;
                declineButton.getPos().x = 250.0F;
            }

            public void callback(GUIElement element, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    if("ACCEPT".equals(element.getUserPointer())) {
                        if(FactionManagerOld.getFactionData(invite.getRecipient()).getFederationId() != -1) {
                            new SimplePopup(getState(), Lng.str("Cannot accept invitation"), Lng.str("Your faction is already part of a federation and cannot join another one."));
                        } else {
                            FederationManager.getFederation(FactionManagerOld.getFactionData(invite.getRecipient())).addMember(FactionManagerOld.getFactionData(invite.getRecipient()));
                        }
                        return;
                    }

                    if("DECLINE".equals(element.getUserPointer())) {
                        FactionManagerOld.getFactionData(invite.getSender()).getInbox().remove(invite);
                    }
                }
            }

            public boolean isOccluded() {
                return false;
            }
        }
    }
}
