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
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.faction.FactionData;

/**
 * FactionActionsPanel.java
 * <Description>
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class FactionActionsPanel extends GUIAncor {

    private GUIScrollablePanel scrollPanel;
    private GUIHorizontalButtonTablePane categories;

    private Faction faction;

    public FactionActionsPanel(InputState inputState, float width, float height) {
        super(inputState, width, height);
    }

    @Override
    public void onInit() {
        recreateButtonPane();
    }

    public void recreateButtonPane() {
        scrollPanel = new GUIScrollablePanel(getWidth(), getHeight(), this, getState());
        categories = new GUIHorizontalButtonTablePane(getState(), 1, 1, scrollPanel);
        categories.onInit();

        final GUIHorizontalButtonTablePane factionPane = categories.addSubButtonPane(0, 0, GUIHorizontalArea.HButtonColor.BLUE, "FACTION");
        factionPane.onInit();
        int fPos = 0;
        factionPane.addButton(0, fPos, new Object() {
            @Override
            public String toString() {
                return Lng.str("Faction Invites (%s)", GameClient.getClientPlayerState().getFactionController().getInvitesIncoming().size()).toUpperCase();
            }
        }, GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    (new FactionIncomingInvitesPlayerInputNew(GameClient.getClientState())).activate();
                }
            }

            @Override
            public boolean isOccluded() {
                return factionPane.hide;
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return true;
            }
        });
        fPos ++;

        if(GameClient.getClientPlayerState().getFactionId() == 0) {
            factionPane.addButton(0, fPos, "CREATE FACTION", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        (new FactionDialogNew(GameClient.getClientState(),"Create Faction")).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return factionPane.hide;
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return true;
                }
            });
            fPos ++;
        } else {
            factionPane.addButton(0, fPos, "LEAVE FACTION", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
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
                    return factionPane.hide;
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return true;
                }
            });
            fPos ++;
        }


        scrollPanel.setContent(categories);
        scrollPanel.onInit();
        attach(scrollPanel);
        /*
        (categories = new GUIElementList(getState())).onInit();
        categories.add(new GUIListElement(createFactionDropDown(GameClient.getClientPlayerState()), getState()));
        if(GameClient.getClientPlayerState().getFactionId() != 0) {
            FactionMember player = FactionUtils.getPlayerFactionMember();
            if(player.hasPermission("diplomacy.[ANY]")) categories.add(new GUIListElement(createDiplomacyDropDown(player, player.getFactionData()), getState()));
            if(player.hasPermission("federation.[ANY]") && player.getFactionData().getFederationId() != -1) categories.add(new GUIListElement(createFederationDropDown(player, player.getFactionData()), getState()));
            if(player.hasPermission("trade.[ANY]")) categories.add(new GUIListElement(createTradeDropDown(player, player.getFactionData()), getState()));
        }



         */
        /*
        (categoryButtonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, this)).onInit();
        int categoryIndex = 0;
        int factionButtonIndex = 0;
        int diplomacyButtonIndex = 0;
        int federationButtonIndex = 0;
        int tradeButtonIndex = 0;

        (factionButtonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, this)).onInit();
        (diplomacyButtonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, this)).onInit();
        (federationButtonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, this)).onInit();
        (tradeButtonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, this)).onInit();

        final int playerFactionId = GameClient.getClientPlayerState().getFactionId();

        categoryButtonPane.addButton(0, categoryIndex, "FACTION", GUIHorizontalArea.HButtonColor.PINK, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 1");
                    toggleButtonPane(factionButtonPane);
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return true;
            }
        });
        categoryIndex ++;

        factionButtonPane.addButton(0, factionButtonIndex, "", GUIHorizontalArea.HButtonType.TEXT_FIELD, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 1");
                    toggleButtonPane(factionButtonPane);
                }
            }

            @Override
            public boolean isOccluded() {
                return true;
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return false;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return false;
            }
        });
        factionButtonIndex ++;

        factionButtonPane.addButton(0, factionButtonIndex, new Object() {
            @Override
            public String toString() {
                return Lng.str("Faction Invites (%s)", GameClient.getClientPlayerState().getFactionController().getInvitesIncoming().size());
            }
        }.toString(), GUIHorizontalArea.HButtonType.BUTTON_BLUE_MEDIUM, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 2");
                    (new FactionIncomingInvitesPlayerInputNew(GameClient.getClientState())).activate();
                }
            }

            @Override
            public boolean isOccluded() {
                return factionButtonPane.isInvisible();
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return !factionButtonPane.isInvisible();
            }

            @Override
            public boolean isActive(InputState inputState) {
                return !factionButtonPane.isInvisible();
            }
        });
        factionButtonIndex ++;

        if(playerFactionId != 0) {
            if(faction != null) {
                if(faction.getIdFaction() == playerFactionId) {
                    factionButtonPane.addButton(0, factionButtonIndex, "LEAVE FACTION", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
                        @Override
                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                            if (mouseEvent.pressedLeftMouse()) {
                                getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                (new PlayerGameOkCancelInput("CONFIRM", GameClient.getClientState(), "Confirm", "Do you really want to leave this faction?\n" + "You'll be unable to access any ship/structure that belongs to this\n" + "faction except for your ships that use the Personal Permission Rank.\n\n" + "If you are the last member, the faction will also automatically disband!") {
                                    public void onDeactivate() {

                                    }

                                    public void pressedOK() {
                                        getState().getController().queueUIAudio("0022_menu_ui - cancel");
                                        System.err.println("[CLIENT][FactionControlManager] leaving Faction");
                                        getState().getPlayer().getFactionController().leaveFaction();
                                        deactivate();
                                    }
                                }).activate();
                            }
                        }

                        @Override
                        public boolean isOccluded() {
                            return factionButtonPane.isInvisible();
                        }
                    }, new GUIActivationCallback() {
                        @Override
                        public boolean isVisible(InputState inputState) {
                            return !factionButtonPane.isInvisible();
                        }

                        @Override
                        public boolean isActive(InputState inputState) {
                            return !factionButtonPane.isInvisible();
                        }
                    });
                    factionButtonIndex ++;
                } else {
                    final FactionMember playerFactionMember = FactionUtils.getPlayerFactionMember();
                    if(playerFactionMember.hasPermission("diplomacy.[ANY]")) {
                        diplomacyButtonPane.addButton(0, diplomacyButtonIndex, "", GUIHorizontalArea.HButtonType.TEXT_FIELD, new GUICallback() {
                            @Override
                            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                if(mouseEvent.pressedLeftMouse()) {
                                    getState().getController().queueUIAudio("0022_menu_ui - select 1");
                                    toggleButtonPane(diplomacyButtonPane);
                                }
                            }

                            @Override
                            public boolean isOccluded() {
                                return true;
                            }
                        }, new GUIActivationCallback() {
                            @Override
                            public boolean isVisible(InputState inputState) {
                                return false;
                            }

                            @Override
                            public boolean isActive(InputState inputState) {
                                return false;
                            }
                        });
                        diplomacyButtonIndex ++;

                        categoryButtonPane.addButton(0, categoryIndex, "DIPLOMACY", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
                            @Override
                            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                if(mouseEvent.pressedLeftMouse()) {
                                    getState().getController().queueUIAudio("0022_menu_ui - select 1");
                                    toggleButtonPane(diplomacyButtonPane);
                                }
                            }

                            @Override
                            public boolean isOccluded() {
                                return false;
                            }
                        }, new GUIActivationCallback() {
                            @Override
                            public boolean isVisible(InputState inputState) {
                                return true;
                            }

                            @Override
                            public boolean isActive(InputState inputState) {
                                return true;
                            }
                        });
                        categoryIndex ++;

                        if(playerFactionMember.hasPermission("diplomacy.ally")) {
                            if(faction.getFriends().contains(playerFactionMember.getFactionData().getFaction())) {
                                diplomacyButtonPane.addButton(0, diplomacyButtonIndex, "REMOVE ALLY", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
                                    @Override
                                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                        if (mouseEvent.pressedLeftMouse()) {
                                            getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                            (new PlayerGameOkCancelInput("CONFIRM", GameClient.getClientState(), "Confirm", "Do you really want to leave this faction?\n" + "You'll be unable to access any ship/structure that belongs to this\n" + "faction except for your ships that use the Personal Permission Rank.\n\n" + "If you are the last member, the faction will also automatically disband!") {
                                                public void onDeactivate() {

                                                }

                                                public void pressedOK() {
                                                    getState().getController().queueUIAudio("0022_menu_ui - cancel");
                                                    GameCommon.getGameState().getFactionManager().setRelationServer(playerFactionId, faction.getIdFaction(), FactionRelation.RType.NEUTRAL.code);
                                                    BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
                                                    deactivate();
                                                }
                                            }).activate();
                                        }
                                    }

                                    @Override
                                    public boolean isOccluded() {
                                        return diplomacyButtonPane.isInvisible() || !playerFactionMember.hasPermission("diplomacy.ally") || faction.getFriends().contains(playerFactionMember.getFactionData().getFaction());
                                    }
                                }, new GUIActivationCallback() {
                                    @Override
                                    public boolean isVisible(InputState inputState) {
                                        return !diplomacyButtonPane.isInvisible() && !playerFactionMember.hasPermission("diplomacy.ally") && !faction.getFriends().contains(playerFactionMember.getFactionData().getFaction());
                                    }

                                    @Override
                                    public boolean isActive(InputState inputState) {
                                        return !diplomacyButtonPane.isInvisible() && !playerFactionMember.hasPermission("diplomacy.ally") && !faction.getFriends().contains(playerFactionMember.getFactionData().getFaction());
                                    }
                                });
                                diplomacyButtonIndex ++;
                            } else if(!faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction())) {
                                diplomacyButtonPane.addButton(0, diplomacyButtonIndex, "OFFER ALLIANCE", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                                    @Override
                                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                        if (mouseEvent.pressedLeftMouse()) {
                                            getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                            //Todo: Faction alliance offer message panel
                                        }
                                    }

                                    @Override
                                    public boolean isOccluded() {
                                        return diplomacyButtonPane.isInvisible() || !playerFactionMember.hasPermission("diplomacy.ally") || faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                    }
                                }, new GUIActivationCallback() {
                                    @Override
                                    public boolean isVisible(InputState inputState) {
                                        return !diplomacyButtonPane.isInvisible() && playerFactionMember.hasPermission("diplomacy.ally") && !faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                    }

                                    @Override
                                    public boolean isActive(InputState inputState) {
                                        return !diplomacyButtonPane.isInvisible() && playerFactionMember.hasPermission("diplomacy.ally") && !faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                    }
                                });
                                diplomacyButtonIndex ++;
                            }
                        }

                        if(playerFactionMember.hasPermission("diplomacy.war")) {
                            if(!faction.getFriends().contains(playerFactionMember.getFactionData().getFaction())) {
                                if(faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction())) {
                                    //Todo: Handle peace deals and options
                                    diplomacyButtonPane.addButton(0, diplomacyButtonIndex, "OFFER PEACE", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
                                        @Override
                                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                            if (mouseEvent.pressedLeftMouse()) {
                                                getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                                //Todo: Offer peace panel
                                            }
                                        }

                                        @Override
                                        public boolean isOccluded() {
                                            return diplomacyButtonPane.isInvisible() || !playerFactionMember.hasPermission("diplomacy.war") || !faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                        }
                                    }, new GUIActivationCallback() {
                                        @Override
                                        public boolean isVisible(InputState inputState) {
                                            return !diplomacyButtonPane.isInvisible() && playerFactionMember.hasPermission("diplomacy.war") && faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                        }

                                        @Override
                                        public boolean isActive(InputState inputState) {
                                            return !diplomacyButtonPane.isInvisible() && playerFactionMember.hasPermission("diplomacy.war") && faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                        }
                                    });
                                    diplomacyButtonIndex ++;
                                } else {
                                    diplomacyButtonPane.addButton(0, diplomacyButtonIndex, "DECLARE WAR", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
                                        @Override
                                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                            if(mouseEvent.pressedLeftMouse()) {
                                                getState().getController().queueUIAudio("0022_menu_ui - select 2");
                                                //Todo: War goal panel
                                            }
                                        }

                                        @Override
                                        public boolean isOccluded() {
                                            return diplomacyButtonPane.isInvisible() || !playerFactionMember.hasPermission("diplomacy.war") || faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                        }
                                    }, new GUIActivationCallback() {
                                        @Override
                                        public boolean isVisible(InputState inputState) {
                                            return !diplomacyButtonPane.isInvisible() && playerFactionMember.hasPermission("diplomacy.war") && faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                        }

                                        @Override
                                        public boolean isActive(InputState inputState) {
                                            return !diplomacyButtonPane.isInvisible() && playerFactionMember.hasPermission("diplomacy.war") && faction.getEnemies().contains(playerFactionMember.getFactionData().getFaction());
                                        }
                                    });
                                    diplomacyButtonIndex ++;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            factionButtonPane.addButton(0, factionButtonIndex, "CREATE FACTION", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        getState().getController().queueUIAudio("0022_menu_ui - select 2");
                        (new FactionDialogNew(GameClient.getClientState(), Lng.str("Create Faction"))).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return factionButtonPane.isInvisible();
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return !factionButtonPane.isInvisible();
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return !factionButtonPane.isInvisible();
                }
            });
            factionButtonIndex ++;
        }

        attach(factionButtonPane);
        factionButtonPane.setVisibility(1);
        toggleButtonPane(factionButtonPane);

        attach(diplomacyButtonPane);
        diplomacyButtonPane.setVisibility(1);
        toggleButtonPane(diplomacyButtonPane);

        attach(federationButtonPane);
        federationButtonPane.setVisibility(1);
        toggleButtonPane(federationButtonPane);

        attach(tradeButtonPane);
        tradeButtonPane.setVisibility(1);
        toggleButtonPane(tradeButtonPane);

        attach(categoryButtonPane);

         */
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    private void addActions(final PlayerState playerState) {

        /*
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

         */
    }

    private String getFedDialogString(FactionData fromFaction, FactionData toFaction) {
        return "Form a new Federation between " + fromFaction.getFactionName() + " and " + toFaction.getFactionName() + ".\nIf both factions accept the proposal, they will become members of the new Federation and will be able to closely collaborate with each other.";
    }
}
