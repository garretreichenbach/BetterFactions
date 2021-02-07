package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.manager.ingame.faction.FactionDialogNew;
import org.schema.game.client.view.gui.faction.newfaction.FactionIncomingInvitesPlayerInputNew;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButton;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.gui.elements.buttons.ExpandableButton;
import thederpgamer.betterfactions.utils.FactionUtils;

/**
 * FactionActionsPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionActionsPanel extends GUIInnerTextbox {

    private GUIActiveInterface activeInterface;
    private GUIElementList mainButtonList;
    private Faction faction;

    public FactionActionsPanel(InputState inputState, GUIActiveInterface activeInterface) {
        super(inputState);
        this.activeInterface = activeInterface;
    }

    @Override
    public void onInit() {
        super.onInit();
        (mainButtonList = new GUIElementList(getState())).onInit();
        addActions(GameClient.getClientPlayerState());
        getContent().attach(mainButtonList);
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    private void addActions(final PlayerState playerState) {
        if(faction != null && faction.getIdFaction() != FactionUtils.getFaction(playerState).getIdFaction()) {
            ExpandableButton diplomacyButton = new ExpandableButton(getState(), this, Lng.str("Diplomacy"), GUIHorizontalArea.HButtonType.BUTTON_BLUE_LIGHT);
            diplomacyButton.onInit();
            GUIListElement diplomacyButtonElement = new GUIListElement(diplomacyButton, getState());
            diplomacyButtonElement.onInit();
            mainButtonList.add(diplomacyButtonElement);

            if(FactionUtils.getFactionData(FactionUtils.getFaction(playerState)).getFederationId() != -1) {
                ExpandableButton federationButton = new ExpandableButton(getState(), this, Lng.str("Federation"), GUIHorizontalArea.HButtonColor.GREEN);
                federationButton.onInit();
                GUIListElement federationButtonElement = new GUIListElement(federationButton, getState());
                federationButtonElement.onInit();
                mainButtonList.add(federationButtonElement);
            }

            ExpandableButton tradeButton = new ExpandableButton(getState(), this, Lng.str("Trade"), GUIHorizontalArea.HButtonColor.YELLOW);
            tradeButton.onInit();
            GUIListElement tradeButtonElement = new GUIListElement(tradeButton, getState());
            tradeButtonElement.onInit();
            mainButtonList.add(tradeButtonElement);
        }

        ExpandableButton optionsButton = new ExpandableButton(getState(), this, Lng.str("Options"), GUIHorizontalArea.HButtonColor.PINK);
        optionsButton.onInit();
        createOptions(optionsButton.getExpandedPanel());
        GUIListElement optionsButtonElement = new GUIListElement(optionsButton, getState());
        optionsButtonElement.onInit();
        mainButtonList.add(optionsButtonElement);
    }

    private void createOptions(GUIScrollablePanel scrollablePanel) {
        PlayerState playerState = GameClient.getClientPlayerState();
        GUIElementList optionsButtonList = new GUIElementList(getState());
        optionsButtonList.onInit();
        optionsButtonList.setScrollPane(scrollablePanel);
        scrollablePanel.setContent(optionsButtonList);
        scrollablePanel.onInit();

        GUIHorizontalButton createFactionButton = new GUIHorizontalButton(getState(), GUIHorizontalArea.HButtonColor.GREEN, Lng.str("Create Faction"), new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                    (new FactionDialogNew(GameClient.getClientState(), Lng.str("Create Faction"))).activate();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        }, activeInterface, null);
        createFactionButton.onInit();
        GUIListElement createFactionButtonElement = new GUIListElement(createFactionButton, getState());
        createFactionButtonElement.onInit();
        optionsButtonList.add(createFactionButtonElement);

        if(FactionUtils.inFaction(playerState)) {
            GUIHorizontalButton leaveFactionButton = new GUIHorizontalButton(getState(), GUIHorizontalArea.HButtonColor.ORANGE, Lng.str("Leave Faction"), new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
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
                    return false;
                }
            }, activeInterface, null);
            leaveFactionButton.onInit();
            GUIListElement leaveFactionButtonElement = new GUIListElement(leaveFactionButton, getState());
            leaveFactionButtonElement.onInit();
            optionsButtonList.add(leaveFactionButtonElement);
        }

        GUIHorizontalButton receivedInvitesButton = new GUIHorizontalButton(getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new Object() {
            @Override
            public String toString() {
                return Lng.str("Received Invites (%s)", GameClient.getClientPlayerState().getFactionController().getInvitesIncoming().size());
            }
        }.toString(), new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    (new FactionIncomingInvitesPlayerInputNew(GameClient.getClientState())).activate();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        }, activeInterface, null);
        receivedInvitesButton.onInit();
        GUIListElement receivedInvitesButtonElement = new GUIListElement(receivedInvitesButton, getState());
        receivedInvitesButtonElement.onInit();
        optionsButtonList.add(receivedInvitesButtonElement);
    }
}
