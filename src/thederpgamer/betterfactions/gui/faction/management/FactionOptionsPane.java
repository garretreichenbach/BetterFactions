package thederpgamer.betterfactions.gui.faction.management;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerFactionPointDialogNew;
import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.view.gui.faction.newfaction.FactionOutgoingInvitesPlayerInputNew;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.InputChecker;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.persistent.faction.FactionMember;
import thederpgamer.betterfactions.manager.FactionManager;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/14/2021]
 */
public class FactionOptionsPane extends GUIAncor {

    private FactionManagementTab managementTab;
    private GUIHorizontalButtonTablePane buttonPane;

    public FactionOptionsPane(InputState inputState, FactionManagementTab managementTab) {
        super(inputState, managementTab.getWidth(), 28 * 3);
        this.managementTab = managementTab;
    }

    @Override
    public void onInit() {
        final FactionMember playerFactionMember = FactionManager.getPlayerFactionMember(GameClient.getClientPlayerState().getName());
        (buttonPane = new GUIHorizontalButtonTablePane(getState(), 2, 3, this)).onInit();
        buttonPane.addButton(0, 0, "INVITE", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 1");
                    GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getFactionControlManager().openInvitePlayerDialog();
                }
            }

            @Override
            public boolean isOccluded() {
                return !playerFactionMember.hasPermission("manage.members.invite") || !canActivate();
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return playerFactionMember.hasPermission("manage.members.invite") && canActivate();
            }
        });

        buttonPane.addButton(1, 0, new Object() {
            @Override
            public String toString() {
                return Lng.str("PENDING INVITES (%s)", GameClient.getClientPlayerState().getFactionController().getInvitesOutgoing().size());
            }
        }, GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 1");
                    (new FactionOutgoingInvitesPlayerInputNew(GameClient.getClientState())).activate();
                }
            }

            @Override
            public boolean isOccluded() {
                return !playerFactionMember.hasPermission("manage.members.invite") || !canActivate();
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return playerFactionMember.hasPermission("manage.members.invite") && canActivate();
            }
        });

        buttonPane.addButton(0, 1, new Object() {
            @Override
            public String toString() {
                return Lng.str("VIEW FP (%s)", GameClient.getClientPlayerState().getFactionController().getFactionPoints());
            }
        }, GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 2");
                    (new PlayerFactionPointDialogNew(GameClient.getClientState())).activate();
                }
            }

            @Override
            public boolean isOccluded() {
                return !playerFactionMember.hasPermission("manage.fp") || !canActivate();
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return playerFactionMember.hasPermission("manage.fp") && canActivate();
            }
        });

        buttonPane.addButton(1, 1, "SET DESCRIPTION", GUIHorizontalArea.HButtonColor.PINK, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 3");
                    Faction ownFaction = playerFactionMember.getFactionData().getFaction();
                    PlayerTextAreaInput t = new PlayerTextAreaInput("FactionOptionFactionContent_EDIT_FACTION_DESC", GameClient.getClientState(), 140, 5, Lng.str("Edit Faction Description"), "", (ownFaction != null) ? ownFaction.getDescription() : Lng.str("ERROR: NO FACTION")) {
                        @Override
                        public String[] getCommandPrefixes() {
                            return null;
                        }

                        @Override
                        public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
                            return null;
                        }

                        @Override
                        public void onFailedTextCheck(String msg) {
                        }

                        @Override
                        public boolean isOccluded() {
                            return false;
                        }

                        @Override
                        public void onDeactivate() {
                        }

                        @Override
                        public boolean onInput(String entry) {
                            return getState().getPlayer().getFactionController().editDescriptionClient(entry);
                        }
                    };
                    t.activate();
                    t.setInputChecker(new InputChecker() {
                        @Override
                        public boolean check(String entry, TextCallback callback) {
                            return true;
                        }
                    });
                }
            }

            @Override
            public boolean isOccluded() {
                return !playerFactionMember.hasPermission("manage.info") || !canActivate();
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return playerFactionMember.hasPermission("manage.info") && canActivate();
            }
        });

        buttonPane.addButton(0, 2, "MANAGE RANKS", GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 4");
                    //Todo
                }
            }

            @Override
            public boolean isOccluded() {
                return !playerFactionMember.hasPermission("manage.members.ranks") || !canActivate();
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return playerFactionMember.hasPermission("manage.members.ranks") && canActivate();
            }
        });

        buttonPane.addButton(1, 2, "ADVANCED PERMISSIONS", GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    getState().getController().queueUIAudio("0022_menu_ui - select 4");
                    //Todo
                }
            }

            @Override
            public boolean isOccluded() {
                return !playerFactionMember.hasPermission("manage.members.ranks") || !canActivate();
            }
        }, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return playerFactionMember.hasPermission("manage.members.ranks") && canActivate();
            }
        });

        attach(buttonPane);
    }

    private boolean canActivate() {
        return getState().getController().getPlayerInputs().isEmpty();
    }
}
