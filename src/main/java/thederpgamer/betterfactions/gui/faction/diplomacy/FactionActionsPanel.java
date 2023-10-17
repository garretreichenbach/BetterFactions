package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.manager.ingame.faction.FactionDialogNew;
import org.schema.game.client.view.gui.faction.newfaction.FactionIncomingInvitesPlayerInputNew;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.persistent.faction.FactionMember;
import thederpgamer.betterfactions.data.persistent.faction.FactionRank;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;
import thederpgamer.betterfactions.gui.faction.diplomacy.war.PeaceDealDialog;
import thederpgamer.betterfactions.manager.FactionManager;

import javax.annotation.Nullable;

/**
 * <Description>
 *
 * @version 1.0 - [01/30/2021]
 * @author TheDerpGamer
 */
public class FactionActionsPanel extends GUIAncor {

    private GUIHorizontalButtonTablePane buttonPane;
    private Faction faction;
    private boolean initialized = false;

    public FactionActionsPanel(InputState inputState, float width, float height) {
        super(inputState, width, height);
    }

    @Override
    public void onInit() {
        buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, this);
        attach(buttonPane);

        buttonPane.getPos().x += 2;
        buttonPane.getPos().y += 2;
        initialized = true;
    }

    public void recreateButtonPane() {
        if(!initialized) onInit();
        buttonPane.onInit();
        int pos = 0;

        buttonPane.addButton(0, pos, new Object() {
            @Override
            public String toString() {
                return Lng.str("Faction Invites (%s)", GameClient.getClientPlayerState().getFactionController().getInvitesIncoming().size()).toUpperCase();
            }
        }, GUIHorizontalArea.HButtonColor.PINK, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    (new FactionIncomingInvitesPlayerInputNew(GameClient.getClientState())).activate();
                }
            }

            @Override
            public boolean isOccluded() {
                return !checkActive();
            }
        }, new GUIActivationHighlightCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return checkActive();
            }

            @Override
            public boolean isHighlighted(InputState inputState) {
                return GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerMailManager().isTreeActive();
            }
        });
        pos ++;

        if(GameClient.getClientPlayerState().getFactionId() == 0) {
            buttonPane.addRow();
            buttonPane.addButton(0, 1, "CREATE FACTION", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        (new FactionDialogNew(GameClient.getClientState(),"Create Faction")).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return !checkActive();
                }
            }, new GUIActivationHighlightCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return checkActive();
                }

                @Override
                public boolean isHighlighted(InputState inputState) {
                    return false;
                }
            });
            pos ++;
        } else {
            buttonPane.addRow();
            buttonPane.addButton(0, 1, "LEAVE FACTION", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        (new PlayerGameOkCancelInput("CONFIRM", GameClient.getClientState(), "Confirm", "Do you really want to leave this faction?\n" + "You'll be unable to access any ship/structure that belongs to this\n" + "faction except for your ships that use the Personal Permission Rank.\n\n" + "If you are the last member, the faction will also automatically disband!") {
                            public void onDeactivate() {

                            }

                            public void pressedOK() {
                                getState().getController().queueUIAudio("0022_menu_ui - cancel");
                                System.err.println("[CLIENT][FactionControlManager] leaving Faction");
                                getState().getPlayer().getFactionController().leaveFaction();
                                deactivate();
                                onInit();
                            }
                        }).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return !checkActive();
                }
            }, new GUIActivationHighlightCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return checkActive();
                }

                @Override
                public boolean isHighlighted(InputState inputState) {
                    return false;
                }
            });
            pos ++;
        }

        if(GameClient.getClientPlayerState().getFactionId() != 0) {
            final FactionMember player = FactionManager.getPlayerFactionMember(GameClient.getClientPlayerState().getName());
            if(player != null && faction.getIdFaction() != player.getFactionId()) {
                final Faction playerFaction = player.getFactionData().getFaction();
                if(playerFaction.getMembersUID().size() == 1) { //Todo: Temp fix for events not firing
                    FactionRank founderRank = new FactionRank("Founder", 4, "*");
                    player.getFactionData().addRank(founderRank);
                    player.setRank(founderRank);
                }
                if(player.hasPermission("diplomacy.[ANY]")) {
                    if(player.hasPermission("diplomacy.ally")) {
                        if(faction.getFriends().contains(player.getFactionData().getFaction())) {
                            buttonPane.addRow();
                            buttonPane.addButton(0, pos, "REMOVE ALLY", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                                @Override
                                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                    if(mouseEvent.pressedLeftMouse()) {
                                        getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                        FactionMessageSendDialog dialog = new FactionMessageSendDialog();
                                        dialog.getInputPanel().createPanel(playerFaction, faction, FactionMessage.MessageType.ALLIANCE_BREAK);
                                        dialog.activate();
                                        //(new FactionMessageSendDialog("REMOVE ALLY", playerFaction, faction, FactionMessage.MessageType.ALLIANCE_BREAK)).activate();
                                        /*
                                        (new PlayerGameOkCancelInput("CONFIRM", GameClient.getClientState(), "Confirm", "Do you really want to leave this faction?\n" + "You'll be unable to access any ship/structure that belongs to this\n" + "faction except for your ships that use the Personal Permission Rank.\n\n" + "If you are the last member, the faction will also automatically disband!") {
                                            public void onDeactivate() {

                                            }

                                            public void pressedOK() {
                                                getState().getController().queueUIAudio("0022_menu_ui - cancel");
                                                Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().setRelationServer(player.getFactionId(), faction.getIdFaction(), FactionRelation.RType.NEUTRAL.code);
                                                BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
                                                deactivate();
                                            }
                                        }).activate();
                                         */
                                    }
                                }

                                @Override
                                public boolean isOccluded() {
                                    return !checkActive();
                                }
                            }, new GUIActivationHighlightCallback() {
                                @Override
                                public boolean isVisible(InputState inputState) {
                                    return true;
                                }

                                @Override
                                public boolean isActive(InputState inputState) {
                                    return checkActive();
                                }

                                @Override
                                public boolean isHighlighted(InputState inputState) {
                                    return false;
                                }
                            });
                            pos ++;
                        } else if(!faction.getEnemies().contains(playerFaction)) {
                            buttonPane.addRow();
                            buttonPane.addButton(0, pos, "OFFER ALLIANCE", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                                @Override
                                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                    if(mouseEvent.pressedLeftMouse()) {
                                        getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                        //(new FactionMessageSendDialog("OFFER ALLIANCE", playerFaction, faction, FactionMessage.MessageType.ALLIANCE_OFFER)).activate();
                                        FactionMessageSendDialog dialog = new FactionMessageSendDialog();
                                        dialog.getInputPanel().createPanel(playerFaction, faction, FactionMessage.MessageType.ALLIANCE_OFFER);
                                        dialog.activate();
                                    }
                                }

                                @Override
                                public boolean isOccluded() {
                                    return !checkActive();
                                }
                            }, new GUIActivationHighlightCallback() {
                                @Override
                                public boolean isVisible(InputState inputState) {
                                    return true;
                                }

                                @Override
                                public boolean isActive(InputState inputState) {
                                    return checkActive();
                                }

                                @Override
                                public boolean isHighlighted(InputState inputState) {
                                    return false;
                                }
                            });
                            pos ++;
                        }
                    }

                    if(player.hasPermission("diplomacy.war")) {
                        if(!faction.getFriends().contains(player.getFactionData().getFaction())) {
                            if(faction.getEnemies().contains(player.getFactionData().getFaction())) {
                                //Todo: Handle peace deals and options
                                buttonPane.addRow();
                                buttonPane.addButton(0, pos, "OFFER PEACE", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
                                    @Override
                                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                        if(mouseEvent.pressedLeftMouse()) {
                                            getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                            PeaceDealDialog dialog = new PeaceDealDialog();
                                            dialog.getInputPanel().createPanel();
                                            dialog.activate();
                                        }
                                    }

                                    @Override
                                    public boolean isOccluded() {
                                        return !checkActive();
                                    }
                                }, new GUIActivationHighlightCallback() {
                                    @Override
                                    public boolean isVisible(InputState inputState) {
                                        return true;
                                    }

                                    @Override
                                    public boolean isActive(InputState inputState) {
                                        return checkActive();
                                    }

                                    @Override
                                    public boolean isHighlighted(InputState inputState) {
                                        return false;
                                    }
                                });
                                pos ++;
                            } else {
                                buttonPane.addRow();
                                buttonPane.addButton(0, pos, "DECLARE WAR", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
                                    @Override
                                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                        if(mouseEvent.pressedLeftMouse()) {
                                            getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                            //Todo: War goal panel
                                        }
                                    }

                                    @Override
                                    public boolean isOccluded() {
                                        return !checkActive();
                                    }
                                }, new GUIActivationHighlightCallback() {
                                    @Override
                                    public boolean isVisible(InputState inputState) {
                                        return true;
                                    }

                                    @Override
                                    public boolean isActive(InputState inputState) {
                                        return checkActive();
                                    }

                                    @Override
                                    public boolean isHighlighted(InputState inputState) {
                                        return false;
                                    }
                                });
                                pos ++;
                            }
                        }
                    }
                }
            }

            if(player.hasPermission("federation.[ANY]") && faction.getIdFaction() != player.getFactionId() && player.getFactionData().getFederationId() != -1) {

            }

            if(player.hasPermission("trade.[ANY]") && faction.getIdFaction() != player.getFactionId()) {

            }
        }
    }

    private boolean checkActive() {
        return getState().getController().getPlayerInputs().isEmpty();
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(@Nullable Faction faction) {
        this.faction = faction;
    }

    /*
    private void addActions(final PlayerState playerState) {
        if (faction != null && playerState.getFactionId() != 0 && faction.getIdFaction() != FactionManager.getFaction(playerState).getIdFaction()) {
            final GUIElementList diplomacyButtonList = new GUIElementList(getState());
            diplomacyButtonList.onInit();
            diplomacyButtonList.getPos().x += 2;
            diplomacyButtonList.getPos().y += 2;

            final FactionData fromFaction = FactionManager.getPlayerFactionData();
            final FactionData toFaction = FactionManager.getFactionData(faction);
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

        if (FactionManager.inFaction(playerState)) {
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

            if (FactionManager.getPlayerFactionData().getFederationId() != -1) {
                GUIButtonListElement leaveFederationButton = new GUIButtonListElement(getState(), GUIHorizontalArea.HButtonColor.ORANGE, Lng.str("Leave Federation"), new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse()) {
                            (new PlayerGameOkCancelInput("CONFIRM", GameClient.getClientState(), Lng.str("Confirm"), Lng.str("Do you really want to leave this federation?\n" + "You'll be unable to access any ship/structure that belongs to this\n" + "federation except for your ships that use the Personal Permission Rank.\n\n" + "If your faction is the last member, the federation will also automatically disband!")) {
                                public void onDeactivate() {

                                }

                                public void pressedOK() {
                                    FactionData playerFaction = FactionManager.getPlayerFactionData();
                                    if(playerFaction.getFederation().getMembers().size() <= 1) {
                                        FederationManager.disbandFederation(playerFaction.getFederation());
                                    } else {
                                        FederationManager.leaveFederation(playerFaction);
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

        if (FactionManager.inFaction(GameClient.getClientPlayerState())) {
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
     */

    private String getFedDialogString(FactionData fromFaction, FactionData toFaction) {
        return "Form a new Federation between " + fromFaction.getFactionName() + " and " + toFaction.getFactionName() + ".\nIf both factions accept the proposal, they will become members of the new Federation and will be able to closely collaborate with each other.";
    }
}
