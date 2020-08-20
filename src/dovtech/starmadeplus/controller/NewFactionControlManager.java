package dovtech.starmadeplus.controller;

import dovtech.starmadeplus.gui.faction.relations.NewFactionRelationDialog;
import org.schema.common.util.StringTools;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.PlayerGameTextInput;
import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.controller.manager.ingame.faction.*;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.faction.FactionIncomingInvitesPlayerInput;
import org.schema.game.client.view.gui.faction.FactionOutgoingInvitesPlayerInput;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionInvite;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.camera.CameraMouseState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;

public class NewFactionControlManager extends FactionControlManager {

    public static final String CREATE_FACTION = "CREATE_FACTION";
    public static final String LEAVE_FACTION = "LEAVE_FACTION";
    public static final String PERSONAL = "PERSONAL";
    public static final String HUB = "HUB";
    public static final String LOCAL = "LOCAL";
    public static final String POST_NEWS = "POST_NEWS";
    public static final String EDIT_DESCRIPTION = "EDIT_DESC";
    public static final String VIEW_INCOMING_INVITE_FACTION = "INCOMING_INVITES";
    public static final String VIEW_OUTGOING_INVITE_FACTION = "OUTGOING_INVITES";
    public static final String INVITE_PLAYER_TO_FACTION = "INVITE";
    public static final String FACTION_ROLES = "ROLES";
    public static final String PERSONAL_ENEMIES = "PERSONAL_ENEMIES";
    private PersonalFactionControlManager personalFactionControlManager;
    private FactionHubControlManager factionHubControlManager;
    private LocalFactionControlManager localFactionControlManager;
    private NewFactionRelationDialog factionRelationDialog;

    public NewFactionControlManager(GameClientState gameClientState) {
        super(gameClientState);
        this.initialize();
    }

    @Override
    public void callback(GUIElement var1, MouseEvent var2) {
        if (var2.getEventButtonState() && var2.getEventButton() == 0) {
            if ("PERSONAL".equals(var1.getUserPointer())) {
                this.activate(this.personalFactionControlManager);
                this.setChanged();
                this.notifyObservers();
                return;
            }

            if ("HUB".equals(var1.getUserPointer())) {
                this.activate(this.factionHubControlManager);
                this.setChanged();
                this.notifyObservers();
                return;
            }

            if ("LOCAL".equals(var1.getUserPointer())) {
                this.activate(this.localFactionControlManager);
                this.setChanged();
                this.notifyObservers();
                return;
            }

            if ("INCOMING_INVITES".equals(var1.getUserPointer())) {
                (new FactionIncomingInvitesPlayerInput(this.getState(), this)).activate();
                return;
            }

            if ("OUTGOING_INVITES".equals(var1.getUserPointer())) {
                (new FactionOutgoingInvitesPlayerInput(this.getState(), this)).activate();
                return;
            }

            if ("EDIT_DESC".equals(var1.getUserPointer())) {
                this.suspend(true);
                Faction var4 = this.getOwnFaction();
                (new PlayerTextAreaInput("FactionControlManager_DESCPAN_FAC", this.getState(), 140, 5, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_16, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_17, var4 != null ? var4.getDescription() : Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_19) {
                    public String[] getCommandPrefixes() {
                        return null;
                    }

                    public boolean isOccluded() {
                        return false;
                    }

                    public String handleAutoComplete(String var1, TextCallback var2, String var3) throws PrefixNotFoundException {
                        return null;
                    }

                    public void onDeactivate() {
                        NewFactionControlManager.this.suspend(false);
                    }

                    public void onFailedTextCheck(String var1) {
                    }

                    public boolean onInput(String var1) {
                        return this.getState().getPlayer().getFactionController().editDescriptionClient(var1);
                    }
                }).activate();
                return;
            }

            if ("POST_NEWS".equals(var1.getUserPointer())) {
                this.suspend(true);
                this.getOwnFaction();
                PlayerTextAreaInput var6 = new PlayerTextAreaInput("FactionControlManager_POST_NEWS_OLD", this.getState(), 140, 5, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_20, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_21, "") {
                    public String[] getCommandPrefixes() {
                        return null;
                    }

                    public String handleAutoComplete(String var1, TextCallback var2, String var3) throws PrefixNotFoundException {
                        return null;
                    }

                    public boolean isOccluded() {
                        return false;
                    }

                    public void onDeactivate() {
                        NewFactionControlManager.this.suspend(false);
                    }

                    public void onFailedTextCheck(String var1) {
                    }

                    public boolean onInput(String var1) {
                        return this.getState().getPlayer().getFactionController().postNewsClient(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_22, var1);
                    }
                };
                var1 = null;
                var6.activate();
                return;
            }

            if ("CREATE_FACTION".equals(var1.getUserPointer())) {
                if (this.getState().getController().getPlayerInputs().isEmpty()) {
                    FactionDialog var3 = new FactionDialog(this.getState(), Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_23, this);
                    this.getState().getController().getPlayerInputs().add(var3);
                    this.suspend(true);
                    this.setChanged();
                    this.notifyObservers();
                    return;
                }
            } else if ("LEAVE_FACTION".equals(var1.getUserPointer())) {
                if (this.getState().getController().getPlayerInputs().isEmpty()) {
                    PlayerGameOkCancelInput var10000 = new PlayerGameOkCancelInput("FactionControlManager_LEAVE_FACTION", this.getState(), Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_24, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_25) {
                        public void onDeactivate() {
                        }

                        public boolean isOccluded() {
                            return false;
                        }

                        public void pressedOK() {
                            System.err.println("[CLIENT][FactionControlManager] leaving Faction");
                            this.getState().getPlayer().getFactionController().leaveFaction();
                            this.deactivate();
                        }
                    };
                    var1 = null;
                    var10000.activate();
                    return;
                }
            } else if ("PERSONAL_ENEMIES".equals(var1.getUserPointer()) && this.getState().getController().getPlayerInputs().isEmpty()) {
                Faction var5 = this.getOwnFaction();
                var1 = null;
                if (var5 != null) {
                    this.openPersonalEnemyInput();
                    return;
                }

                this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_1, 0.0F);
            }
        }

    }

    @Override
    public boolean isOccluded() {
        return false;
    }

    @Override
    public FactionHubControlManager getFactionHubControlManager() {
        return this.factionHubControlManager;
    }

    @Override
    public LocalFactionControlManager getLocalFactionControlManager() {
        return this.localFactionControlManager;
    }

    @Override
    public Faction getOwnFaction() {
        int var1 = this.getState().getPlayer().getFactionId();
        return this.getState().getFactionManager().getFaction(var1);
    }

    @Override
    public PersonalFactionControlManager getPersonalFactionControlManager() {
        return this.personalFactionControlManager;
    }

    private void initialize() {
        this.personalFactionControlManager = new PersonalFactionControlManager(this.getState());
        this.factionHubControlManager = new FactionHubControlManager(this.getState());
        this.localFactionControlManager = new LocalFactionControlManager(this.getState());
        this.getControlManagers().add(this.personalFactionControlManager);
        this.getControlManagers().add(this.factionHubControlManager);
        this.getControlManagers().add(this.localFactionControlManager);
        this.personalFactionControlManager.setActive(true);
    }

    @Override
    public void onSwitch(boolean var1) {
        if (var1) {
            this.getState().getController().queueUIAudio("0022_menu_ui - swoosh scroll large");
            this.setChanged();
            this.notifyObservers();
        } else {
            this.getState().getController().queueUIAudio("0022_menu_ui - swoosh scroll small");
        }

        this.getState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().getInShipControlManager().getShipControlManager().getShipExternalFlightController().suspend(var1);
        this.getState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().getInShipControlManager().getShipControlManager().getSegmentBuildController().suspend(var1);
        super.onSwitch(var1);
    }

    @Override
    public void update(Timer var1) {
        CameraMouseState.setGrabbed(false);
        super.update(var1);
    }

    @Override
    public void openDeclareWarDialog(final Faction var1, final Faction var2) {
        this.suspend(true);
        (new PlayerTextAreaInput("FactionControlManager_openDeclareWarDialog", this.getState(), 140, 5, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_18, "") {
            public String[] getCommandPrefixes() {
                return null;
            }

            public String handleAutoComplete(String var1x, TextCallback var2x, String var3) throws PrefixNotFoundException {
                return null;
            }

            public boolean isOccluded() {
                return false;
            }

            public void onDeactivate() {
                NewFactionControlManager.this.suspend(false);
            }

            public void onFailedTextCheck(String var1x) {
            }

            public boolean onInput(String var1x) {
                this.getState().getFactionManager().sendRelationshipOffer(this.getState().getPlayerName(), var1.getIdFaction(), var2.getIdFaction(), FactionRelation.RType.ENEMY.code, var1x, false);
                return true;
            }
        }).activate();
    }

    @Override
    public void openInvitePlayerDialog() {
        if (this.getState().getPlayer().getFactionController().getInvitesOutgoing().size() >= 5) {
            this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_2, 0.0F);
        } else {
            this.suspend(true);
            (new PlayerGameTextInput("FactionControlManager_openInvitePlayerDialog", this.getState(), 50, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_3, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_4) {
                public void onDeactivate() {
                    NewFactionControlManager.this.suspend(false);
                }

                public String[] getCommandPrefixes() {
                    return null;
                }

                public boolean onInput(String var1) {
                    if (this.getState().getPlayer().getFactionId() != 0) {
                        FactionInvite var2 = new FactionInvite(this.getState().getPlayerName(), var1, this.getState().getPlayer().getFactionId(), System.currentTimeMillis());
                        this.getState().getFactionManager().sendInviteClient(var2);
                        this.getState().getController().popupInfoTextMessage(StringTools.format(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_5, new Object[]{var1}), 0.0F);
                    }

                    return true;
                }

                public String handleAutoComplete(String var1, TextCallback var2, String var3) throws PrefixNotFoundException {
                    return this.getState().onAutoComplete(var1, this, var3);
                }

                public boolean isOccluded() {
                    return false;
                }

                public void onFailedTextCheck(String var1) {
                }
            }).activate();
        }
    }

    @Override
    public void openOfferAllyDialog(final Faction var1, final Faction var2) {
        this.suspend(true);
        (new PlayerTextAreaInput("FactionControlManager_openOfferAllyDialog", this.getState(), 140, 5, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_6, "") {
            public String[] getCommandPrefixes() {
                return null;
            }

            public String handleAutoComplete(String var1x, TextCallback var2x, String var3) throws PrefixNotFoundException {
                return null;
            }

            public boolean isOccluded() {
                return false;
            }

            public void onDeactivate() {
                NewFactionControlManager.this.suspend(false);
            }

            public void onFailedTextCheck(String var1x) {
            }

            public boolean onInput(String var1x) {
                this.getState().getFactionManager().sendRelationshipOffer(this.getState().getPlayerName(), var1.getIdFaction(), var2.getIdFaction(), FactionRelation.RType.FRIEND.code, var1x, false);
                return true;
            }
        }).activate();
    }

    @Override
    public void openOfferPeaceDialog(final Faction var1, final Faction var2) {
        this.suspend(true);
        (new PlayerTextAreaInput("FactionControlManager_openOfferPeaceDialog", this.getState(), 140, 5, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_7, "") {
            public String[] getCommandPrefixes() {
                return null;
            }

            public String handleAutoComplete(String var1x, TextCallback var2x, String var3) throws PrefixNotFoundException {
                return null;
            }

            public boolean isOccluded() {
                return false;
            }

            public void onDeactivate() {
                NewFactionControlManager.this.suspend(false);
            }

            public void onFailedTextCheck(String var1x) {
            }

            public boolean onInput(String var1x) {
                this.getState().getFactionManager().sendRelationshipOffer(this.getState().getPlayerName(), var1.getIdFaction(), var2.getIdFaction(), FactionRelation.RType.NEUTRAL.code, var1x, false);
                return true;
            }
        }).activate();
    }

    @Override
    public void openOpenOffers() {
        this.suspend(true);
        (new FactionOfferDialog(this.getState())).activate();
    }

    @Override
    public void openPersonalEnemyInput() {
        if (this.getState().getPlayer().getFactionId() == 0) {
            this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_10, 0.0F);
        } else {
            Faction var1;
            if ((var1 = this.getState().getFactionManager().getFaction(this.getState().getPlayer().getFactionId())) == null) {
                this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_11, 0.0F);
            } else {
                this.suspend(true);
                (new FactionPersonalEnemyDialog(this.getState(), var1)).activate();
            }
        }
    }

    @Override
    public void openRelationShipInput(int var1) {
        if (this.getState().getPlayer().getFactionId() == 0) {
            this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_8, 0.0F);
        } else {
            Faction var2 = this.getState().getFactionManager().getFaction(this.getState().getPlayer().getFactionId());
            Faction var4 = this.getState().getFactionManager().getFaction(var1);
            if (var2 == null) {
                this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_9, 0.0F);
            } else if (var4 == null) {
                this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_12, 0.0F);
            } else if (var2 == var4) {
                this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_13, 0.0F);
            } else if (var4.getIdFaction() < 0) {
                this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_14, 0.0F);
            } else {
                FactionPermission var3;
                if ((var3 = (FactionPermission)var2.getMembersUID().get(this.getState().getPlayer().getName())) != null && var3.hasRelationshipPermission(var2)) {
                    this.suspend(true);
                    this.factionRelationDialog = new NewFactionRelationDialog(this.getState(), var2, var4);
                    this.factionRelationDialog.activate();
                    //(new FactionRelationDialog(this.getState(), var2, var4)).activate();
                } else {
                    this.getState().getController().popupAlertTextMessage(Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_FACTION_FACTIONCONTROLMANAGER_15, 0.0F);
                }
            }
        }
    }

    @Override
    public void revokeAlly(Faction var1, Faction var2) {
        this.getState().getFactionManager().sendRelationshipOffer(this.getState().getPlayerName(), var1.getIdFaction(), var2.getIdFaction(), FactionRelation.RType.FRIEND.code, "REVOKE", true);
    }

    @Override
    public void revokePeaceOffer(Faction var1, Faction var2) {
        this.getState().getFactionManager().sendRelationshipOffer(this.getState().getPlayerName(), var1.getIdFaction(), var2.getIdFaction(), FactionRelation.RType.NEUTRAL.code, "REVOKE", true);
    }

    @Override
    public void revokeAllyOffer(Faction var1, Faction var2) {
        this.getState().getFactionManager().sendRelationshipOffer(this.getState().getPlayerName(), var1.getIdFaction(), var2.getIdFaction(), FactionRelation.RType.FRIEND.code, "REVOKE", true);
    }
}