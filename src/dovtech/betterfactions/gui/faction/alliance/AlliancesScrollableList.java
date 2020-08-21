package dovtech.betterfactions.gui.faction.alliance;

import dovtech.betterfactions.BetterFactions;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import dovtech.betterfactions.faction.government.AllianceGovernmentType;
import org.hsqldb.lib.StringComparator;
import org.schema.common.util.CompareTools;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class AlliancesScrollableList extends ScrollableTableList<Alliance> implements GUIActiveInterface {

    private AllianceListRow selectedRow;

    public AlliancesScrollableList(InputState state, float var2, float var3, GUIElement guiElement) {
        super(state, var2, var3, guiElement);
    }

    @Override
    public void initColumns() {
        new StringComparator();

        this.addColumn("Name", 7.0F, new Comparator<Alliance>() {
            public int compare(Alliance o1, Alliance o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        this.addColumn("Government", 5.0F, new Comparator<Alliance>() {
            public int compare(Alliance o1, Alliance o2) {
                return o1.getGovernmentType().displayName.compareTo(o2.getGovernmentType().displayName);
            }
        });

        this.addColumn("Member Count", 5.0F, new Comparator<Alliance>() {
            public int compare(Alliance o1, Alliance o2) {
                return CompareTools.compare(o1.getMembers().size(), o2.getMembers().size());
            }
        });

        this.addTextFilter(new GUIListFilterText<Alliance>() {
            public boolean isOk(String s, Alliance alliance) {
                return alliance.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<Alliance, AllianceGovernmentType>(AllianceGovernmentType.values()) {
            public boolean isOk(AllianceGovernmentType allianceGovernmentType, Alliance alliance) {
                return alliance.getGovernmentType().equals(allianceGovernmentType);
            }

        }, new CreateGUIElementInterface<AllianceGovernmentType>() {
            public GUIElement create(AllianceGovernmentType allianceGovernmentType) {
                GUIAncor anchor = new GUIAncor(AlliancesScrollableList.this.getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, AlliancesScrollableList.this.getState())).setTextSimple(allianceGovernmentType.displayName);
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(allianceGovernmentType);
                anchor.attach(dropDown);
                return anchor;
            }

            public GUIElement createNeutral() {
                GUIAncor anchor = new GUIAncor(AlliancesScrollableList.this.getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, AlliancesScrollableList.this.getState())).setTextSimple("All");
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.attach(dropDown);
                return anchor;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        this.activeSortColumnIndex = 0;
    }

    @Override
    protected Collection<Alliance> getElementList() {
        return BetterFactions.getInstance().getFactionAlliances().values();
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<Alliance> set) {

        for(Alliance alliance : BetterFactions.getInstance().getFactionAlliances().values()) {

            GUITextOverlayTable nameTextElement;
            (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(alliance.getName());
            GUIClippedRow nameRowElement;
            (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

            GUITextOverlayTable governmentTextElement;
            (governmentTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(alliance.getGovernmentType().displayName);
            GUIClippedRow governmentRowElement;
            (governmentRowElement = new GUIClippedRow(this.getState())).attach(governmentTextElement);

            GUITextOverlayTable sizeTextElement;
            (sizeTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(String.valueOf(alliance.getMembers().size()));
            GUIClippedRow sizeRowElement;
            (sizeRowElement = new GUIClippedRow(this.getState())).attach(sizeTextElement);

            AllianceListRow allianceListRow;
            (allianceListRow = new AllianceListRow(this.getState(), alliance, nameRowElement, governmentRowElement, sizeRowElement)).onInit();
            guiElementList.addWithoutUpdate(allianceListRow);
        }
        guiElementList.updateDim();
    }

    public class AllianceListRow extends ScrollableTableList<Alliance>.Row {
        private Alliance alliance;

        public AllianceListRow(InputState inputState, Alliance alliance, GUIElement... guiElements) {
            super(inputState, alliance, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.alliance = alliance;
        }

        @Override
        public void clickedOnRow() {
            super.clickedOnRow();
            AlliancesScrollableList.this.selectedRow = this;
        }

        public Alliance getAlliance() {
            return alliance;
        }
    }
}
