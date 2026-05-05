package videogoose.betterfactions.gui.faction.management;

import api.common.GameClient;
import api.common.GameCommon;
import org.hsqldb.lib.StringComparator;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.data.persistent.faction.FactionRank;
import videogoose.betterfactions.manager.FactionManager;
import videogoose.betterfactions.mixin.BetterFactionAccessor;
import videogoose.betterfactions.mixin.BetterMemberAccessor;
import videogoose.betterfactions.utils.PermissionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;

/**
 * FactionMembersList
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/15/2021
 */
public class FactionMembersList extends ScrollableTableList<FactionPermission> {

    private GUIAncor anchor;
    private FactionManagementTab managementTab;

    public FactionMembersList(InputState inputState, GUIAncor anchor, FactionManagementTab managementTab) {
        super(inputState, anchor.getWidth(), anchor.getHeight(), anchor);
        this.anchor = anchor;
        this.managementTab = managementTab;
        anchor.attach(this);
        ((GameClientState) inputState).getFactionManager().addObserver(this);
    }

    @Override
    public ArrayList<FactionPermission> getElementList() {
        Faction faction = FactionManager.getFaction(GameClient.getClientPlayerState());
        if(faction == null) return new ArrayList<>();
        return new ArrayList<>(faction.getMembersUID().values());
    }

    @Override
    public void initColumns() {
        new StringComparator();

        addColumn("Name", 7.5f, new Comparator<FactionPermission>() {
            @Override
            public int compare(FactionPermission o1, FactionPermission o2) {
                return o1.playerUID.compareTo(o2.playerUID);
            }
        });

        addColumn("Rank", 8.0f, new Comparator<FactionPermission>() {
            @Override
            public int compare(FactionPermission o1, FactionPermission o2) {
                FactionRank r1 = ((BetterMemberAccessor) o1).getCustomRank();
                FactionRank r2 = ((BetterMemberAccessor) o2).getCustomRank();
                int level1 = r1 != null ? r1.getRankLevel() : 0;
                int level2 = r2 != null ? r2.getRankLevel() : 0;
                return Integer.compare(level1, level2);
            }
        });

        addColumn("Status", 6.5f, new Comparator<FactionPermission>() {
            @Override
            public int compare(FactionPermission o1, FactionPermission o2) {
                boolean online1 = GameCommon.getPlayerFromName(o1.playerUID) != null;
                boolean online2 = GameCommon.getPlayerFromName(o2.playerUID) != null;
                return Boolean.compare(online1, online2);
            }
        });

        addColumn("Location", 7.0f, new Comparator<FactionPermission>() {
            @Override
            public int compare(FactionPermission o1, FactionPermission o2) {
                PlayerState p1 = GameCommon.getPlayerFromName(o1.playerUID);
                PlayerState p2 = GameCommon.getPlayerFromName(o2.playerUID);
                Vector3i o1Location = p1 != null ? p1.getCurrentSector() : null;
                Vector3i o2Location = p2 != null ? p2.getCurrentSector() : null;

                double distance1 = -1;
                double distance2 = -1;
                // Compare by distance from origin as a simple fallback
                if(o1Location != null) distance1 = Math.abs(Vector3i.getDisatance(o1Location, new Vector3i()));
                if(o2Location != null) distance2 = Math.abs(Vector3i.getDisatance(o2Location, new Vector3i()));
                return Double.compare(distance1, distance2);
            }
        });

        addTextFilter(new GUIListFilterText<FactionPermission>() {
            public boolean isOk(String s, FactionPermission factionMember) {
                return factionMember.playerUID.toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<FactionPermission, String>(getFactionRanksString()) {
            public boolean isOk(String s, FactionPermission factionMember) {
                if(s.equalsIgnoreCase("ALL")) return true;
                FactionRank rank = ((BetterMemberAccessor) factionMember).getCustomRank();
                return rank != null && s.equalsIgnoreCase(rank.getRankName());
            }

        }, new CreateGUIElementInterface<String>() {
            @Override
            public GUIElement create(String s) {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(s);
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(s);
                anchor.attach(dropDown);
                return anchor;
            }

            @Override
            public GUIElement createNeutral() {
                return null;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        this.activeSortColumnIndex = 0;
    }

    private String[] getFactionRanksString() {
        ArrayList<String> ranksStringList = new ArrayList<>();
        ranksStringList.add("ALL");
        Faction faction = FactionManager.getFaction(GameClient.getClientPlayerState());
        if(faction != null) {
            for(FactionRank rank : ((BetterFactionAccessor) faction).getRanks()) {
                ranksStringList.add(rank.getRankName().toUpperCase());
            }
        }
        return ranksStringList.toArray(new String[0]);
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<FactionPermission> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        FactionPermission playerFactionMember = FactionManager.getPlayerMember(GameClient.getClientPlayerState().getName());
        assert playerFactionMember != null;
        for(FactionPermission factionMember : set) {
            GUITextOverlayTable nameTextElement;
            (nameTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(factionMember.playerUID);
            GUIClippedRow nameRowElement;
            (nameRowElement = new GUIClippedRow(getState())).attach(nameTextElement);

            FactionRank rank = ((BetterMemberAccessor) factionMember).getCustomRank();
            GUITextOverlayTable rankTextElement;
            String rankStr = rank != null ? rank.getRankName() + "[" + rank.getRankLevel() + "]" : "None";
            (rankTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(rankStr);
            GUIClippedRow rankRowElement;
            (rankRowElement = new GUIClippedRow(getState())).attach(rankTextElement);

            boolean isOnline = GameCommon.getPlayerFromName(factionMember.playerUID) != null;
            GUITextOverlayTable statusTextElement;
            (statusTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(isOnline ? "ONLINE" : "OFFLINE");
            GUIClippedRow statusRowElement;
            (statusRowElement = new GUIClippedRow(getState())).attach(statusTextElement);

            PlayerState memberPlayer = GameCommon.getPlayerFromName(factionMember.playerUID);
            Vector3i location = memberPlayer != null ? memberPlayer.getCurrentSector() : null;
            GUITextOverlayTable locationTextElement;
            (locationTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(location != null ? location.toString() : "");
            GUIClippedRow locationRowElement;
            (locationRowElement = new GUIClippedRow(getState())).attach(locationTextElement);

            FactionMembersListRow factionMembersListRow = new FactionMembersListRow(getState(), factionMember, nameRowElement, rankRowElement, statusRowElement, locationRowElement);
            if(PermissionUtils.hasPermission(playerFactionMember, "manage.members.[ANY]")) {
                GUIAncor anchor = new GUIAncor(getState(), this.anchor.getWidth() - 28.0f, 28.0f);
                anchor.attach(redrawButtonPane(factionMember, playerFactionMember, anchor));
                factionMembersListRow.expanded = new GUIElementList(getState());
                factionMembersListRow.expanded.add(new GUIListElement(anchor, getState()));
                factionMembersListRow.expanded.attach(anchor);
            }
            factionMembersListRow.onInit();
            guiElementList.addWithoutUpdate(factionMembersListRow);
        }
        guiElementList.updateDim();
    }

    public void redrawList() {
        flagDirty();
        handleDirty();
    }

    private GUIHorizontalButtonTablePane redrawButtonPane(final FactionPermission factionMember, FactionPermission playerFactionMember, GUIAncor anchor) {
        GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 0, 1, anchor);
        buttonPane.onInit();
        int buttonIndex = 0;
        FactionRank playerRank = ((BetterMemberAccessor) playerFactionMember).getCustomRank();
        FactionRank memberRank = ((BetterMemberAccessor) factionMember).getCustomRank();
        int playerRankLevel = playerRank != null ? playerRank.getRankLevel() : 0;
        int memberRankLevel = memberRank != null ? memberRank.getRankLevel() : 0;
        if(playerRankLevel >= memberRankLevel) {
            if(PermissionUtils.hasPermission(playerFactionMember, "manage.members.kick") && factionMember != playerFactionMember) {
                buttonPane.addColumn();
                buttonPane.addButton(buttonIndex, 0, "KICK", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        getState().getController().queueUIAudio("0022_menu_ui - cancel");
                        //Todo: Send kick packet to server
                        redrawList();
                    }

                    @Override
                    public boolean isOccluded() {
                        return !getState().getController().getPlayerInputs().isEmpty();
                    }
                }, new GUIActivationCallback() {
                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return getState().getController().getPlayerInputs().isEmpty();
                    }
                });
                buttonIndex ++;
            }

            if(PermissionUtils.hasPermission(playerFactionMember, "manage.members.ranks")) {
                buttonPane.addColumn();
                buttonPane.addButton(buttonIndex, 0, "EDIT RANK", GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        getState().getController().queueUIAudio("0022_menu_ui - enter");
                        //Todo: Rank Editor
                    }
                    @Override
                    public boolean isOccluded() {
                        return !getState().getController().getPlayerInputs().isEmpty();
                    }
                }, new GUIActivationCallback() {
                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return getState().getController().getPlayerInputs().isEmpty();
                    }
                });
                buttonIndex ++;
            }
        }
        return buttonPane;
    }

    public class FactionMembersListRow extends ScrollableTableList<FactionPermission>.Row {

        public FactionMembersListRow(InputState inputState, FactionPermission factionMember, GUIElement... guiElements) {
            super(inputState, factionMember, guiElements);
            highlightSelect = true;
            highlightSelectSimple = true;
            setAllwaysOneSelected(true);
        }
    }
}
