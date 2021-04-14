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
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.gui.elements.list.GUIButtonListElement;
import thederpgamer.betterfactions.gui.federation.dialog.CreateFederationDialog;
import thederpgamer.betterfactions.gui.federation.dialog.FederationInvitesDialog;
import thederpgamer.betterfactions.gui.federation.dialog.InviteToFederationDialog;
import thederpgamer.betterfactions.gui.federation.dialog.RequestJoinFederationDialog;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.FederationUtils;

/**
 * FactionActionsPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionActionsPanel extends GUIAncor {

    private Faction faction;
    private GUIElementList mainButtonList;

    private GUIDropdownBackground diplomacyActionsBG;
    private GUIDropdownBackground federationActionsBG;
    private GUIDropdownBackground tradeActionsBG;
    private GUIDropdownBackground factionActionsBG;

    private GUIButtonListElement diplomacyButton;
    private GUIButtonListElement federationButton;
    private GUIButtonListElement tradeButton;
    private GUIButtonListElement factionButton;

    public FactionActionsPanel(InputState inputState) {
        super(inputState);
    }

    @Override
    public void onInit() {
        super.onInit();
        mainButtonList = new GUIElementList(getState());
        mainButtonList.onInit();
        addActions(GameClient.getClientPlayerState());
        attach(mainButtonList);
        resetPositions();
    }

    @Override
    public void draw() {
        super.draw();

        if (diplomacyActionsBG.isInvisible()) {
            diplomacyActionsBG.cleanUp();
        } else {
            diplomacyActionsBG.draw();
        }

        if (federationActionsBG.isInvisible()) {
            federationActionsBG.cleanUp();
        } else {
            federationActionsBG.draw();
        }

        if (tradeActionsBG.isInvisible()) {
            tradeActionsBG.cleanUp();
        } else {
            tradeActionsBG.draw();
        }

        if (factionActionsBG.isInvisible()) {
            factionActionsBG.cleanUp();
        } else {
            factionActionsBG.draw();
        }
    }

    private void resetPositions() {
        int offset = 2;
        int buttons = 0;

        if(!diplomacyActionsBG.isInvisible()) offset += diplomacyActionsBG.getHeight();
        if(!federationActionsBG.isInvisible()) offset += federationActionsBG.getHeight();
        if(!tradeActionsBG.isInvisible()) offset += tradeActionsBG.getHeight();
        if(!federationActionsBG.isInvisible()) offset += federationActionsBG.getHeight();

        if(diplomacyButton != null) {
            diplomacyButton.getPos().y = offset + (24 * buttons);
            diplomacyActionsBG.getPos().y = diplomacyButton.getPos().y + 26;
            diplomacyActionsBG.getPos().x = diplomacyButton.getPos().x;
            buttons ++;
        }

        if(federationButton != null) {
            federationButton.getPos().y = offset + (24 * buttons);
            federationActionsBG.getPos().y = federationButton.getPos().y + 26;
            federationActionsBG.getPos().x = federationButton.getPos().x;
            buttons ++;
        }

        if(tradeButton != null) {
            tradeButton.getPos().y = offset + (24 * buttons);
            tradeActionsBG.getPos().y = tradeButton.getPos().y + 26;
            tradeActionsBG.getPos().x = tradeButton.getPos().x;
            buttons ++;
        }

        if(factionButton != null) {
            factionButton.getPos().y = offset + (24 * buttons);
            factionActionsBG.getPos().y = factionButton.getPos().y + 26;
            factionActionsBG.getPos().x = factionButton.getPos().x;
            buttons ++;
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

        (factionActionsBG = new GUIDropdownBackground(getState(), (int) getWidth(), (int) getHeight())).onInit();
        factionActionsBG.setVisibility(2);

        if (faction != null && playerState.getFactionId() != 0 && faction.getIdFaction() != FactionUtils.getFaction(playerState).getIdFaction()) {
            final GUIElementList diplomacyButtonList = new GUIElementList(getState());
            diplomacyButtonList.onInit();
            diplomacyButtonList.getPos().x += 2;
            diplomacyButtonList.getPos().y += 2;

            final FactionData fromFaction = FactionUtils.getPlayerFactionData();
            final FactionData toFaction = FactionUtils.getFactionData(faction);
            String relation = Lng.str(toFaction.getRelationString());

            //Todo: Permissions check

            if (!toFaction.getFaction().isNPC()) {
                GUIButtonListElement sendMessageButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.YELLOW, Lng.str("Send Message"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                            //Todo
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                sendMessageButton.onInit();
                diplomacyButtonList.add(sendMessageButton);
            }

            GUIButtonListElement viewRelationsButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_PINK_LIGHT, Lng.str("View Relations"), new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                        //Todo
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
            viewRelationsButton.onInit();
            diplomacyButtonList.add(viewRelationsButton);

            if (relation.equals(Lng.str("Allied")) || relation.equals(Lng.str("In Federation")) || relation.equals(Lng.str("Vassal")) || relation.equals(Lng.str("Master"))) {
                GUIButtonListElement requestResourcesButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_DARK, Lng.str("Request Resources"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                            //Todo
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                requestResourcesButton.onInit();
                diplomacyButtonList.add(requestResourcesButton);

                if (fromFaction.getFaction().getEnemies().size() != 0) {
                    //Todo: Check if has non-aggression pact with other side
                    GUIButtonListElement requestMilitaryAidButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_RED_LIGHT, Lng.str("Request Military Aid"), new GUICallback() {
                        @Override
                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                            if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                                //Todo
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }
                    });
                    requestMilitaryAidButton.onInit();
                    diplomacyButtonList.add(requestMilitaryAidButton);
                }
            }

            if (relation.equals(Lng.str("Neutral"))) {
                //Todo: Check if already has non-aggression pact
                GUIButtonListElement nonAggressionPactButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_GREY_MEDIUM, Lng.str("Non-Aggression Pact"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                            //Todo
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                nonAggressionPactButton.onInit();
                diplomacyButtonList.add(nonAggressionPactButton);

                //Todo: Check if already offering protection
                if (!toFaction.getFaction().isNPC()) {
                    GUIButtonListElement offerProtectionButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, Lng.str("Offer Protection"), new GUICallback() {
                        @Override
                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                            if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                                //Todo
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }
                    });
                    offerProtectionButton.onInit();
                    diplomacyButtonList.add(offerProtectionButton);
                }

                //Todo: Check if already has protection
                GUIButtonListElement requestProtectionButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_LIGHT, Lng.str("Request Protection"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                            //Todo
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                requestProtectionButton.onInit();
                diplomacyButtonList.add(requestProtectionButton);

                if (!toFaction.getFaction().isNPC()) {
                    GUIButtonListElement offerAllianceButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.GREEN, Lng.str("Offer Alliance"), new GUICallback() {
                        @Override
                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                            if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                                //Todo
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }
                    });
                    offerAllianceButton.onInit();
                    diplomacyButtonList.add(offerAllianceButton);
                }

                GUIButtonListElement makeDemandButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.ORANGE, Lng.str("Make Demand"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                            //Todo
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                makeDemandButton.onInit();
                diplomacyButtonList.add(makeDemandButton);
            }

            if (relation.equals(Lng.str("At War")) || relation.equals(Lng.str("Personal Enemy"))) {
                GUIButtonListElement requestPeaceButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.RED, Lng.str("Request Peace"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                            //Todo
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                requestPeaceButton.onInit();
                diplomacyButtonList.add(requestPeaceButton);
            }

            if (relation.equals(Lng.str("Neutral"))) {
                //Todo: Check if has non-aggression pact
                GUIButtonListElement declareWarButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.RED, Lng.str("Declare War"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                            //Todo
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                declareWarButton.onInit();
                diplomacyButtonList.add(declareWarButton);
            }

            diplomacyActionsBG.attach(diplomacyButtonList);
            diplomacyActionsBG.setWidth(diplomacyButtonList.getWidth());
            diplomacyActionsBG.setHeight(((diplomacyButtonList.size() * 24) + diplomacyButtonList.size()) + 2);
            diplomacyButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.PINK, Lng.str("Diplomacy"), new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        if (diplomacyActionsBG.getVisibility() == 1) {
                            diplomacyActionsBG.setVisibility(2);
                        } else {
                            diplomacyActionsBG.setVisibility(1);
                        }
                        diplomacyButtonList.updateDim();
                        mainButtonList.updateDim();
                        resetPositions();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
            diplomacyButton.onInit();
            diplomacyButton.getPos().x += 2;
            diplomacyButton.attach(diplomacyActionsBG);
            mainButtonList.add(diplomacyButton);
            for (GUIListElement element : diplomacyButtonList) {
                ((GUIButtonListElement) element).setButtonWidth(diplomacyButtonList.width - 4);
            }

            final GUIElementList federationButtonList = new GUIElementList(getState());
            federationButtonList.onInit();
            federationButtonList.getPos().x += 2;
            federationButtonList.getPos().y += 2;

            if ((relation.equals(Lng.str("Neutral")) || relation.equals(Lng.str("Allied"))) && fromFaction.getFederationId() == -1 && toFaction.getFederationId() == -1 && !toFaction.getFaction().isNPC()) {
                GUIButtonListElement createFederationButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.GREEN, Lng.str("Create Federation"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                            (new CreateFederationDialog(fromFaction, toFaction, getFedDialogString(fromFaction, toFaction))).activate();
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                createFederationButton.onInit();
                federationButtonList.add(createFederationButton);
            }

            if (relation.equals(Lng.str("Neutral")) || relation.equals(Lng.str("Allied")) && !toFaction.getFaction().isNPC()) {
                //Todo: Invite permission check
                if (fromFaction.getFederationId() != -1 && toFaction.getFederationId() == -1) {
                    GUIButtonListElement inviteToFederationButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.GREEN, Lng.str("Create Federation"), new GUICallback() {
                        @Override
                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                            if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                                (new InviteToFederationDialog(fromFaction.getFaction(), toFaction.getFaction(), fromFaction.getFederation())).activate();
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }
                    });
                    inviteToFederationButton.onInit();
                    federationButtonList.add(inviteToFederationButton);
                }

                if (fromFaction.getFederationId() == -1 && toFaction.getFederationId() != -1) {
                    GUIButtonListElement requestJoinFederationButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.GREEN, Lng.str("Create Federation"), new GUICallback() {
                        @Override
                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                            if (mouseEvent.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                                (new RequestJoinFederationDialog(fromFaction.getFaction(), toFaction.getFaction(), toFaction.getFederation())).activate();
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }
                    });
                    requestJoinFederationButton.onInit();
                    federationButtonList.add(requestJoinFederationButton);
                }
            }

            federationActionsBG.attach(federationButtonList);
            federationActionsBG.setWidth(federationButtonList.getWidth());
            federationActionsBG.setHeight(((federationButtonList.size() * 24) + federationButtonList.size()) + 2);
            federationButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.PINK, Lng.str("Federation"), new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        if (federationActionsBG.getVisibility() == 1) {
                            federationActionsBG.setVisibility(2);
                        } else {
                            federationActionsBG.setVisibility(1);
                        }
                        federationButtonList.updateDim();
                        mainButtonList.updateDim();
                        resetPositions();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
            federationButton.onInit();
            federationButton.getPos().x += 2;
            federationButton.attach(federationActionsBG);
            mainButtonList.add(federationButton);
            for (GUIListElement element : federationButtonList) {
                ((GUIButtonListElement) element).setButtonWidth(federationButtonList.width - 4);
            }

            final GUIElementList tradeButtonList = new GUIElementList(getState());
            tradeButtonList.onInit();
            tradeButtonList.getPos().x += 2;
            tradeButtonList.getPos().y += 2;
            tradeActionsBG.attach(tradeButtonList);
            tradeActionsBG.setWidth(tradeButtonList.getWidth());
            tradeActionsBG.setHeight(((tradeButtonList.size() * 24) + tradeButtonList.size()) + 2);
            tradeButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.PINK, Lng.str("Trade"), new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        if (tradeActionsBG.getVisibility() == 1) {
                            tradeActionsBG.setVisibility(2);
                        } else {
                            tradeActionsBG.setVisibility(1);
                        }
                        tradeButtonList.updateDim();
                        mainButtonList.updateDim();
                        resetPositions();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
            tradeButton.onInit();
            tradeButton.getPos().x += 2;
            tradeButton.attach(tradeActionsBG);
            mainButtonList.add(tradeButton);
            for (GUIListElement element : tradeButtonList) {
                ((GUIButtonListElement) element).setButtonWidth(tradeButtonList.width - 4);
            }
        }

        final GUIElementList factionButtonList = new GUIElementList(getState());
        factionButtonList.onInit();
        factionButtonList.getPos().x += 2;
        factionButtonList.getPos().y += 2;

        GUIButtonListElement createFactionButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.GREEN, Lng.str("Create Faction"), new GUICallback() {
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
        createFactionButton.onInit();
        factionButtonList.add(createFactionButton);

        if (FactionUtils.inFaction(playerState)) {
            GUIButtonListElement leaveFactionButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.ORANGE, Lng.str("Leave Faction"), new GUICallback() {
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
                    return false;
                }
            });
            leaveFactionButton.onInit();
            factionButtonList.add(leaveFactionButton);

            if (FactionUtils.getPlayerFactionData().getFederationId() != -1) {
                GUIButtonListElement leaveFederationButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.ORANGE, Lng.str("Leave Federation"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse()) {
                            (new PlayerGameOkCancelInput("CONFIRM", GameClient.getClientState(), Lng.str("Confirm"), Lng.str("Do you really want to leave this federation?\n" + "You'll be unable to access any ship/structure that belongs to this\n" + "federation except for your ships that use the Personal Permission Rank.\n\n" + "If your faction is the last member, the federation will also automatically disband!")) {
                                public void onDeactivate() {

                                }

                                public void pressedOK() {
                                    FactionData playerFaction = FactionUtils.getPlayerFactionData();
                                    if(playerFaction.getFederation().getMembers().size() <= 1) {
                                        FederationUtils.disbandFederation(playerFaction.getFederation());
                                    } else {
                                        FederationUtils.leaveFederation(playerFaction);
                                    }
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
                leaveFederationButton.onInit();
                factionButtonList.add(leaveFederationButton);
            }

        }
        GUIButtonListElement factionInvitesButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new Object() {
            @Override
            public String toString() {
                return Lng.str("Faction Invites (%s)", GameClient.getClientPlayerState().getFactionController().getInvitesIncoming().size());
            }
        }.toString(), new GUICallback() {
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
        factionInvitesButton.onInit();
        factionButtonList.add(factionInvitesButton);

        if (FactionUtils.inFaction(GameClient.getClientPlayerState())) {
            GUIButtonListElement federationInvitesButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new Object() {
                @Override
                public String toString() {
                    return Lng.str("Federation Invites (%s)", GameClient.getClientPlayerState().getFactionController().getInvitesIncoming().size());
                }
            }.toString(), new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        (new FederationInvitesDialog()).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
            federationInvitesButton.onInit();
            factionButtonList.add(federationInvitesButton);
        }

        factionActionsBG.attach(factionButtonList);
        factionActionsBG.setWidth(factionButtonList.getWidth());
        factionActionsBG.setHeight(((factionButtonList.size() * 24) + factionButtonList.size()) + 2);
        factionButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.PINK, Lng.str("Faction"), new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if (mouseEvent.pressedLeftMouse()) {
                    if (factionActionsBG.getVisibility() == 1) {
                        factionActionsBG.setVisibility(2);
                    } else {
                        factionActionsBG.setVisibility(1);
                    }
                    factionButtonList.updateDim();
                    mainButtonList.updateDim();
                    resetPositions();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });
        factionButton.onInit();
        factionButton.getPos().x += 2;
        factionButton.attach(factionActionsBG);
        mainButtonList.add(factionButton);
        for (GUIListElement element : factionButtonList) {
            ((GUIButtonListElement) element).setButtonWidth(factionButtonList.width - 4);
        }
    }

    private String getFedDialogString(FactionData fromFaction, FactionData toFaction) {
        return "Form a new Federation between " + fromFaction.getFactionName() + " and " + toFaction.getFactionName() + ".\nIf both factions accept the proposal, they will become members of the new Federation and will be able to closely collaborate with each other.";
    }
}
