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
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.gui.elements.list.GUIButtonListElement;
import thederpgamer.betterfactions.utils.FactionUtils;

/**
 * FactionActionsPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionActionsPanel extends GUIInnerTextbox {

    private Faction faction;
    private GUIElementList mainButtonList;
    private GUIDropdownBackground federationActionsBG;
    private GUIDropdownBackground diplomacyActionsBG;
    private GUIDropdownBackground tradeActionsBG;
    private GUIDropdownBackground optionsActionsBG;

    public FactionActionsPanel(InputState inputState, GUIAncor anchor) {
        super(inputState);
        anchor.attach(this);
    }

    @Override
    public void onInit() {
        super.onInit();
        mainButtonList = new GUIElementList(getState());
        addActions(GameClient.getClientPlayerState());
        getContent().attach(mainButtonList);
    }

    @Override
    public void draw() {
        super.draw();

        if(federationActionsBG.isInvisible()) {
            federationActionsBG.cleanUp();
        } else {
            federationActionsBG.draw();
        }

        if(diplomacyActionsBG.isInvisible()) {
            diplomacyActionsBG.cleanUp();
        } else {
            diplomacyActionsBG.draw();
        }

        if(tradeActionsBG.isInvisible()) {
            tradeActionsBG.cleanUp();
        } else {
            tradeActionsBG.draw();
        }

        if(optionsActionsBG.isInvisible()) {
            optionsActionsBG.cleanUp();
        } else {
            optionsActionsBG.draw();
        }
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    private void addActions(final PlayerState playerState) {
        (diplomacyActionsBG = new GUIDropdownBackground(getState(), (int) getWidth(), (int) getHeight())).onInit();
        diplomacyActionsBG.setVisibility(2);

        (federationActionsBG = new GUIDropdownBackground(getState(), (int) getWidth(), (int) getHeight())).onInit();
        federationActionsBG.setVisibility(2);

        (tradeActionsBG = new GUIDropdownBackground(getState(), (int) getWidth(), (int) getHeight())).onInit();
        tradeActionsBG.setVisibility(2);

        (optionsActionsBG = new GUIDropdownBackground(getState(), (int) getWidth(), (int) getHeight())).onInit();
        optionsActionsBG.setVisibility(2);

        if(faction != null && faction.getIdFaction() != FactionUtils.getFaction(playerState).getIdFaction()) {
            final GUIElementList diplomacyButtonList = new GUIElementList(getState());
            diplomacyButtonList.onInit();
            diplomacyActionsBG.attach(diplomacyButtonList);
            diplomacyActionsBG.setWidth(diplomacyButtonList.getWidth());
            diplomacyActionsBG.setHeight((diplomacyButtonList.size() * diplomacyButtonList.get(0).getHeight()) + 2);
            GUIButtonListElement diplomacyButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, Lng.str("Diplomacy"), new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        if(diplomacyActionsBG.getVisibility() == 1) {
                            diplomacyActionsBG.setVisibility(2);
                        } else {
                            diplomacyActionsBG.setVisibility(1);
                        }
                        diplomacyButtonList.updateDim();
                        mainButtonList.updateDim();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
            diplomacyButton.onInit();
            diplomacyButton.attach(diplomacyActionsBG);
            diplomacyActionsBG.getPos().x = diplomacyButton.getPos().x + 2;
            diplomacyActionsBG.getPos().y = diplomacyButton.getPos().y + diplomacyButton.getHeight() + 2;
            for(GUIListElement element : diplomacyButtonList) {
                ((GUIButtonListElement) element).setButtonWidth(diplomacyButtonList.width - 4);
                element.getPos().x += 2;
                element.getPos().y += 2;
            }
            mainButtonList.add(diplomacyButton);
            if(FactionUtils.getFactionData(FactionUtils.getFaction(playerState)).getFederationId() != -1) {
                final GUIElementList federationButtonList = new GUIElementList(getState());
                federationButtonList.onInit();
                federationActionsBG.attach(federationButtonList);
                federationActionsBG.setWidth(federationButtonList.getWidth());
                federationActionsBG.setHeight(((federationButtonList.size() * federationButtonList.get(0).getHeight()) + federationButtonList.size()) + 2);
                GUIButtonListElement federationButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.GREEN, Lng.str("Federation"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if(mouseEvent.pressedLeftMouse()) {
                            if(federationActionsBG.getVisibility() == 1) {
                                federationActionsBG.setVisibility(2);
                            } else {
                                federationActionsBG.setVisibility(1);
                            }
                            federationButtonList.updateDim();
                            mainButtonList.updateDim();
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                federationButton.onInit();
                federationButton.attach(federationActionsBG);
                federationActionsBG.getPos().x = federationButton.getPos().x + 2;
                federationActionsBG.getPos().y = federationButton.getPos().y + federationButton.getHeight() + 2;
                for(GUIListElement element : federationButtonList) {
                    ((GUIButtonListElement) element).setButtonWidth(federationButtonList.width - 4);
                    element.getPos().x += 2;
                    element.getPos().y += 2;
                }
                mainButtonList.add(federationButton);
            }

            final GUIElementList tradeButtonList = new GUIElementList(getState());
            tradeButtonList.onInit();
            tradeActionsBG.attach(tradeButtonList);
            tradeActionsBG.setWidth(tradeButtonList.getWidth());
            tradeActionsBG.setHeight(((tradeButtonList.size() * tradeButtonList.get(0).getHeight()) + tradeButtonList.size()) + 2);
            GUIButtonListElement tradeButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.YELLOW, Lng.str("Trade"), new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        if(tradeActionsBG.getVisibility() == 1) {
                            tradeActionsBG.setVisibility(2);
                        } else {
                            tradeActionsBG.setVisibility(1);
                        }
                        tradeButtonList.updateDim();
                        mainButtonList.updateDim();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
            tradeButton.onInit();
            tradeButton.attach(tradeActionsBG);
            tradeActionsBG.getPos().x = tradeButton.getPos().x + 2;
            tradeActionsBG.getPos().y = tradeButton.getPos().y + tradeButton.getHeight() + 2;
            for(GUIListElement element : tradeButtonList) {
                ((GUIButtonListElement) element).setButtonWidth(tradeButtonList.width - 4);
                element.getPos().x += 2;
                element.getPos().y += 2;
            }
            mainButtonList.add(tradeButton);
        }

        final GUIElementList optionsButtonList = new GUIElementList(getState());
        optionsButtonList.onInit();

        GUIButtonListElement createFactionButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.GREEN, Lng.str("Create Faction"), new GUICallback() {
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
        });
        createFactionButton.onInit();
        optionsButtonList.add(createFactionButton);

        if(FactionUtils.inFaction(playerState)) {
            GUIButtonListElement leaveFactionButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.ORANGE, Lng.str("Leave Faction"), new GUICallback() {
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
            });
            leaveFactionButton.onInit();
            optionsButtonList.add(leaveFactionButton);
        }

        GUIButtonListElement receivedInvitesButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new Object() {
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
        });
        receivedInvitesButton.onInit();
        optionsButtonList.add(receivedInvitesButton);

        optionsActionsBG.attach(optionsButtonList);
        optionsActionsBG.setWidth(optionsButtonList.getWidth());
        optionsActionsBG.setHeight(((optionsButtonList.size() * optionsButtonList.get(0).getHeight()) + optionsButtonList.size()) + 2);
        GUIButtonListElement optionsButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.PINK, Lng.str("Options"), new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    if(optionsActionsBG.getVisibility() == 1) {
                        optionsActionsBG.setVisibility(2);
                    } else {
                        optionsActionsBG.setVisibility(1);
                    }
                    optionsButtonList.updateDim();
                    mainButtonList.updateDim();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });
        optionsButton.onInit();
        optionsButton.attach(optionsActionsBG);
        optionsActionsBG.getPos().x = optionsButton.getPos().x + 2;
        optionsActionsBG.getPos().y = optionsButton.getPos().y + optionsButton.getHeight() + 2;
        for(GUIListElement element : optionsButtonList) {
            ((GUIButtonListElement) element).setButtonWidth(optionsButtonList.width - 4);
            element.getPos().x += 2;
            element.getPos().y += 2;
        }
        mainButtonList.add(optionsButton);
    }
}
