package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.manager.ingame.faction.FactionDialogNew;
import org.schema.game.client.view.gui.faction.newfaction.FactionIncomingInvitesPlayerInputNew;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.gui.elements.VerticalButtonPane;
import thederpgamer.betterfactions.utils.FactionUtils;

/**
 * FactionActionsPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionActionsPanel extends GUIInnerTextbox {

    private VerticalButtonPane buttonPane;
    private Faction faction;

    public FactionActionsPanel(InputState inputState, GUIAncor anchor) {
        super(inputState);
        anchor.attach(this);
        faction = null;
    }

    @Override
    public void onInit() {
        super.onInit();
        (buttonPane = new VerticalButtonPane(getState(), 4)).onInit();
        addActions(GameClient.getClientPlayerState());
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    private void addActions(final PlayerState playerState) {
        if(faction != null && faction.getIdFaction() != FactionUtils.getFaction(playerState).getIdFaction()) {

        } else {
            buttonPane.addButton(Lng.str("Create Faction"), GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                        (new FactionDialogNew(GameClient.getClientState(), Lng.str("Create Faction"))).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });

            if (FactionUtils.inFaction(playerState)) {
                buttonPane.addButton(Lng.str("Leave Faction"), GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse()) {
                            (new PlayerGameOkCancelInput("CONFIRM", GameClient.getClientState(), Lng.str("Confirm"), Lng.str("Do you really want to leave this faction?\n" + "You'll be unable to access any ship/structure that belongs to this\n" + "faction except for your ships that use the Personal Permission Rank.\n\n" + "If you are the last member, the faction will also automatically disband!")) {
                                public void onDeactivate() {
                                }

                                public void pressedOK() {
                                    System.err.println("[CLIENT][FactionControlManager] leaving Faction");
                                    this.getState().getPlayer().getFactionController().leaveFaction();
                                    this.deactivate();
                                }
                            }).activate();
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return FactionUtils.inFaction(playerState);
                    }
                });
            }

            buttonPane.addButton(new Object() {
                @Override
                public String toString() {
                    return Lng.str("Received Invites (%s)", GameClient.getClientPlayerState().getFactionController().getInvitesIncoming().size());
                }
            }.toString(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        (new FactionIncomingInvitesPlayerInputNew(GameClient.getClientState())).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
        }
    }
}
