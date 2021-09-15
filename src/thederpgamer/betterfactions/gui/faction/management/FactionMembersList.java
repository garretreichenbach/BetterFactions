package thederpgamer.betterfactions.gui.faction.management;

import api.common.GameClient;
import org.hsqldb.lib.StringComparator;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionMember;
import thederpgamer.betterfactions.data.faction.FactionRank;
import thederpgamer.betterfactions.manager.FactionManager;

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
public class FactionMembersList extends ScrollableTableList<FactionMember> {

    private FactionManagementTab managementTab;

    public FactionMembersList(InputState inputState, GUIAncor anchor, FactionManagementTab managementTab) {
        super(inputState, 100, 100, anchor);
        this.managementTab = managementTab;
        anchor.attach(this);
        ((GameClientState) inputState).getFactionManager().addObserver(this);
    }

    @Override
    public ArrayList<FactionMember> getElementList() {
        return Objects.requireNonNull(FactionManager.getPlayerFactionData(GameClient.getClientPlayerState().getName())).getMembers();
    }

    @Override
    public void initColumns() {
        new StringComparator();

        addColumn("Name", 7.5f, new Comparator<FactionMember>() {
            @Override
            public int compare(FactionMember o1, FactionMember o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        addColumn("Rank", 8.0f, new Comparator<FactionMember>() {
            @Override
            public int compare(FactionMember o1, FactionMember o2) {
                return Integer.compare(o1.getRank().getRankLevel(), o2.getRank().getRankLevel());
            }
        });

        addColumn("Status", 6.5f, new Comparator<FactionMember>() {
            @Override
            public int compare(FactionMember o1, FactionMember o2) {
                return Boolean.compare(o1.isOnline(), o2.isOnline());
            }
        });

        addColumn("Location", 7.0f, new Comparator<FactionMember>() {
            @Override
            public int compare(FactionMember o1, FactionMember o2) {
                Vector3i homebaseSector = o1.getFactionData().getHomebaseSector();
                Vector3i o1Location = o1.getLocation();
                Vector3i o2Location = o2.getLocation();

                double distance1 = -1;
                double distance2 = -1;
                if(homebaseSector != null) {
                    if(o1Location != null) distance1 = Math.abs(Vector3i.getDisatance(o1Location, homebaseSector));
                    if(o2Location != null) distance2 = Math.abs(Vector3i.getDisatance(o2Location, homebaseSector));
                }
                return Double.compare(distance1, distance2);
            }
        });

        addTextFilter(new GUIListFilterText<FactionMember>() {
            public boolean isOk(String s, FactionMember factionMember) {
                return factionMember.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<FactionMember, String>(getFactionRanksString()) {
            public boolean isOk(String s, FactionMember factionMember) {
                if(s.equalsIgnoreCase("ALL")) return true;
                else return s.equalsIgnoreCase(factionMember.getRank().getRankName());
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
        for(FactionRank rank : Objects.requireNonNull(FactionManager.getPlayerFactionData(GameClient.getClientPlayerState().getName())).getRanks()) {
            ranksStringList.add(rank.getRankName());
        }
        return ranksStringList.toArray(new String[0]);
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<FactionMember> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        FactionMember playerFactionMember = FactionManager.getPlayerFactionMember(GameClient.getClientPlayerState().getName());
        for(FactionMember factionMember : set) {
            if(factionMember != null) {
                GUITextOverlayTable nameTextElement;
                (nameTextElement = new GUITextOverlayTable(10, 10, getState())).setTextSimple(factionMember.getName());
                GUIClippedRow nameRowElement;
                (nameRowElement = new GUIClippedRow(getState())).attach(nameTextElement);

                FactionMembersListRow factionMembersListRow = new FactionMembersListRow(getState(), factionMember, nameRowElement);
                factionMembersListRow.expanded = new GUIElementList(getState());
                GUIAncor anchor = new GUIAncor(getState(), getWidth() - 4, 28.0f);
                if(playerFactionMember != null && playerFactionMember.hasPermission("manage.members.[ANY]")) anchor.attach(redrawButtonPane(factionMember, playerFactionMember, anchor));
                factionMembersListRow.expanded.add(new GUIListElement(anchor, getState()));
                factionMembersListRow.onInit();
                guiElementList.add(factionMembersListRow);
            }
        }
        guiElementList.updateDim();
    }

    public void redrawList() {
        flagDirty();
        handleDirty();
    }

    private GUIHorizontalButtonTablePane redrawButtonPane(final FactionMember factionMember, FactionMember playerFactionMember, GUIAncor anchor) {
        GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, anchor);
        buttonPane.onInit();
        final FactionData factionData = playerFactionMember.getFactionData();
        int buttonIndex = 0;
        if(playerFactionMember.getRank().getRankLevel() >= factionMember.getRank().getRankLevel()) {
            if(playerFactionMember.hasPermission("manage.members.kick")) {
                buttonPane.addButton(buttonIndex, 0, "KICK", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        getState().getController().queueUIAudio("0022_menu_ui - cancel");
                        factionData.removeMember(factionMember.getName());
                        redrawList();
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
                buttonIndex ++;
            }

            if(playerFactionMember.hasPermission("manage.members.ranks")) {
                buttonPane.addButton(buttonIndex, 0, "EDIT RANK", GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        getState().getController().queueUIAudio("0022_menu_ui - enter");
                        //Todo: Rank Editor
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
                buttonIndex ++;
            }
        }

        return buttonPane;
    }

    public class FactionMembersListRow extends ScrollableTableList<FactionMember>.Row {

        public FactionMembersListRow(InputState inputState, FactionMember factionMember, GUIElement... guiElements) {
            super(inputState, factionMember, guiElements);
            highlightSelect = true;
            highlightSelectSimple = true;
            setAllwaysOneSelected(true);
        }
    }
}
