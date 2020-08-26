package dovtech.betterfactions.gui.faction.alliance;

import dovtech.betterfactions.BetterFactions;
import dovtech.betterfactions.faction.BetterFaction;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import dovtech.betterfactions.faction.government.FactionGovernmentType;
import org.hsqldb.lib.StringComparator;
import org.schema.common.util.CompareTools;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class AllianceMemberList extends ScrollableTableList<BetterFaction> {

    private AllianceMemberListRow selectedRow;
    private Alliance alliance;

    public AllianceMemberList(InputState var1, float var2, float var3, GUIElement var4, Alliance alliance) {
        super(var1, var2, var3, var4);
        this.alliance = alliance;
    }

    @Override
    public void initColumns() {
        new StringComparator();

        this.addColumn("Name", 7.0F, new Comparator<BetterFaction>() {
            public int compare(BetterFaction o1, BetterFaction o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        this.addColumn("Government", 5.0F, new Comparator<BetterFaction>() {
            public int compare(BetterFaction o1, BetterFaction o2) {
                return o1.getGovernmentType().compareTo(o2.getGovernmentType());
            }
        });

        this.addColumn("Member Count", 5.0F, new Comparator<BetterFaction>() {
            public int compare(BetterFaction o1, BetterFaction o2) {
                try {
                    return CompareTools.compare(o1.getMembers().size(), o2.getMembers().size());
                } catch (PlayerNotFountException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });

        this.addTextFilter(new GUIListFilterText<BetterFaction>() {
            public boolean isOk(String s, BetterFaction faction) {
                return faction.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<BetterFaction, FactionGovernmentType>(FactionGovernmentType.values()) {
            public boolean isOk(FactionGovernmentType factionGovernmentType, BetterFaction faction) {
                return faction.getGovernmentType().equals(factionGovernmentType);
            }

        }, new CreateGUIElementInterface<FactionGovernmentType>() {
            public GUIElement create(FactionGovernmentType factionGovernmentType) {
                GUIAncor anchor = new GUIAncor(AllianceMemberList.this.getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, AllianceMemberList.this.getState())).setTextSimple(factionGovernmentType.displayName);
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(factionGovernmentType);
                anchor.attach(dropDown);
                return anchor;
            }

            public GUIElement createNeutral() {
                GUIAncor anchor = new GUIAncor(AllianceMemberList.this.getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, AllianceMemberList.this.getState())).setTextSimple("All");
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.attach(dropDown);
                return anchor;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        this.activeSortColumnIndex = 0;
    }

    @Override
    protected Collection<BetterFaction> getElementList() {
        return alliance.getMembers();
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<BetterFaction> set) {

        for(BetterFaction faction : alliance.getMembers()) {
            try {

                GUITextOverlayTable nameTextElement;
                (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(faction.getName());
                GUIClippedRow nameRowElement;
                (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

                GUITextOverlayTable governmentTextElement;
                (governmentTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(faction.getGovernmentType().displayName);
                GUIClippedRow governmentRowElement;
                (governmentRowElement = new GUIClippedRow(this.getState())).attach(governmentTextElement);

                GUITextOverlayTable sizeTextElement;
                (sizeTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(String.valueOf(faction.getMembers().size()));
                GUIClippedRow sizeRowElement;
                (sizeRowElement = new GUIClippedRow(this.getState())).attach(sizeTextElement);

                AllianceMemberListRow memberListRow;
                (memberListRow = new AllianceMemberListRow(this.getState(), faction, nameRowElement, governmentRowElement, sizeRowElement)).onInit();
                guiElementList.addWithoutUpdate(memberListRow);
            } catch(PlayerNotFountException e) {
                e.printStackTrace();
            }
        }
        guiElementList.updateDim();
    }

    public class AllianceMemberListRow extends ScrollableTableList<BetterFaction>.Row {
        private BetterFaction faction;

        public AllianceMemberListRow(InputState inputState, BetterFaction faction, GUIElement... guiElements) {
            super(inputState, faction, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.faction = faction;
        }

        @Override
        public void clickedOnRow() {
            super.clickedOnRow();
            AllianceMemberList.this.selectedRow = this;
        }

        public BetterFaction getFaction() {
            return faction;
        }
    }
}
