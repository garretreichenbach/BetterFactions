package dovtech.betterfactions.gui.faction.alliance;

import dovtech.betterfactions.BetterFactions;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import dovtech.betterfactions.faction.government.FactionGovernmentType;
import org.hsqldb.lib.StringComparator;
import org.schema.common.util.CompareTools;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class AllianceMemberList extends ScrollableTableList<Faction> {

    private AllianceMemberListRow selectedRow;
    private Alliance alliance;

    public AllianceMemberList(InputState var1, float var2, float var3, GUIElement var4, Alliance alliance) {
        super(var1, var2, var3, var4);
        this.alliance = alliance;
    }

    @Override
    public void initColumns() {
        new StringComparator();

        this.addColumn("Name", 7.0F, new Comparator<Faction>() {
            public int compare(Faction o1, Faction o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        this.addColumn("Government", 5.0F, new Comparator<Faction>() {
            public int compare(Faction o1, Faction o2) {
                try {
                    return BetterFactions.getInstance().getFactionStats(o1).getGovernmentType().toString().compareTo(BetterFactions.getInstance().getFactionStats(o2).getGovernmentType().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });

        this.addColumn("Member Count", 5.0F, new Comparator<Faction>() {
            public int compare(Faction o1, Faction o2) {
                return CompareTools.compare(o1.getMembersUID().values().size(), o2.getMembersUID().values().size());
            }
        });

        this.addTextFilter(new GUIListFilterText<Faction>() {
            public boolean isOk(String s, Faction faction) {
                return faction.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<Faction, FactionGovernmentType>(FactionGovernmentType.values()) {
            public boolean isOk(FactionGovernmentType factionGovernmentType, Faction faction) {
                try {
                    return BetterFactions.getInstance().getFactionStats(faction).getGovernmentType().equals(factionGovernmentType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
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
    protected Collection<Faction> getElementList() {
        return alliance.getMembers();
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<Faction> set) {

        for(Faction faction : alliance.getMembers()) {
            try {

                GUITextOverlayTable nameTextElement;
                (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(faction.getName());
                GUIClippedRow nameRowElement;
                (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

                GUITextOverlayTable governmentTextElement;
                (governmentTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(BetterFactions.getInstance().getFactionStats(faction).getGovernmentType().displayName);
                GUIClippedRow governmentRowElement;
                (governmentRowElement = new GUIClippedRow(this.getState())).attach(governmentTextElement);

                GUITextOverlayTable sizeTextElement;
                (sizeTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(String.valueOf(faction.getMembersUID().values().size()));
                GUIClippedRow sizeRowElement;
                (sizeRowElement = new GUIClippedRow(this.getState())).attach(sizeTextElement);

                AllianceMemberListRow memberListRow;
                (memberListRow = new AllianceMemberListRow(this.getState(), faction, nameRowElement, governmentRowElement, sizeRowElement)).onInit();
                guiElementList.addWithoutUpdate(memberListRow);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        guiElementList.updateDim();
    }

    public class AllianceMemberListRow extends ScrollableTableList<Faction>.Row {
        private Faction faction;

        public AllianceMemberListRow(InputState inputState, Faction faction, GUIElement... guiElements) {
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

        public Faction getFaction() {
            return faction;
        }
    }
}
