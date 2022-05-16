package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import org.schema.common.util.CompareTools;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRelationship;
import thederpgamer.betterfactions.manager.data.FactionDataManager;
import thederpgamer.betterfactions.manager.data.FactionRelationshipManager;
import thederpgamer.betterfactions.manager.data.FederationManager;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

/**
 * FactionDiplomacyList.java
 * <Description>
 * ==================================================
 * Created 02/07/2021
 * @author TheDerpGamer
 */
public class FactionDiplomacyList extends ScrollableTableList<FactionData> {

    private final FactionDiplomacyTab diplomacyTab;
    private final String[] relationValues = {
            "All",
            "Neutral",
            "Allied",
            "In Federation",
            "At War",
            "Personal Enemy"
    };

    public FactionDiplomacyList(InputState inputState, GUIAncor anchor, FactionDiplomacyTab diplomacyTab) {
        super(inputState, 100, 100, anchor);
        this.diplomacyTab = diplomacyTab;
        anchor.attach(this);
        ((GameClientState) inputState).getFactionManager().addObserver(this);
    }

    @Override
    public void initColumns() {
        this.addColumn("Name", 15.0F, new Comparator<FactionData>() {
            public int compare(FactionData o1, FactionData o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        this.addColumn("Federation", 15.0F, new Comparator<FactionData>() {
            public int compare(FactionData o1, FactionData o2) {
                String federationName1 = (o1.getFederation() != null) ? FederationManager.instance.getFederation(o1).getName() : "Non-Aligned";
                String federationName2 = (o2.getFederation() != null) ? FederationManager.instance.getFederation(o2).getName() : "Non-Aligned";
                return federationName1.compareTo(federationName2);
            }
        });

        this.addColumn("Members", 7.0F, new Comparator<FactionData>() {
            public int compare(FactionData o1, FactionData o2) {
                return CompareTools.compare(o1.getMembers().size(), o2.getMembers().size());
            }
        });

        this.addColumn("Relation", 10.0F, new Comparator<FactionData>() {
            public int compare(FactionData o1, FactionData o2) {
                FactionRelationship relationshipA = FactionRelationshipManager.instance.getRelationship(o1, o2);
                FactionRelationship relationshipB = FactionRelationshipManager.instance.getRelationship(o2, o1);
                return Float.compare(relationshipA.getOpinion(), relationshipB.getOpinion());
            }
        });

        this.addTextFilter(new GUIListFilterText<FactionData>() {
            public boolean isOk(String s, FactionData faction) {
                return faction.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<FactionData, String>(FactionRelationship.RelationType.stringValues()) {
            public boolean isOk(String relationType, FactionData faction) {
                if(relationType.equals("ALL")) return true;
                else {
                    FactionRelationship relationship = FactionRelationshipManager.instance.getRelationship(FactionDataManager.instance.getPlayerFaction(GameClient.getClientPlayerState()), faction);
                    for(FactionRelationship.Relationship relation : relationship.getRelations()) {
                        if(relation.getRelationType().name.equals(relationType)) return true;
                    }
                    return false;
                }
            }
        }, new CreateGUIElementInterface<String>() {
            @Override
            public GUIElement create(String s) {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(s.toUpperCase());
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(s.toUpperCase());
                anchor.attach(dropDown);
                return anchor;
            }

            @Override
            public GUIElement createNeutral() {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple("ALL");
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer("ALL");
                anchor.attach(dropDown);
                return anchor;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        this.activeSortColumnIndex = 0;
    }

    @Override
    public Collection<FactionData> getElementList() {
        return FactionDataManager.instance.getCache().asMap().values();
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<FactionData> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        for(FactionData factionData : set) {
            if(factionData != null) {
                GUITextOverlayTable nameTextElement;
                String factionName = factionData.getName();
                if(factionData.getId() == org.schema.game.common.data.player.faction.FactionManager.TRAIDING_GUILD_ID) factionName = "Trading Guild";
                (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(factionName);
                GUIClippedRow nameRowElement;
                (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

                String federationName = (FederationManager.instance.getFederation(factionData) != null) ? FederationManager.instance.getFederation(factionData).getName() : "Non-Aligned";
                GUITextOverlayTable federationTextElement;
                (federationTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(federationName);
                GUIClippedRow federationRowElement;
                (federationRowElement = new GUIClippedRow(this.getState())).attach(federationTextElement);

                int members = factionData.getMembers().size();
                GUITextOverlayTable membersTextElement;
                (membersTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple((members <= 0) ? "N/A" : members + " members");
                GUIClippedRow membersRowElement;
                (membersRowElement = new GUIClippedRow(this.getState())).attach(membersTextElement);

                GUITextOverlayTable relationTextElement;
                FactionRelationship relationship = FactionRelationshipManager.instance.getRelationship(FactionDataManager.instance.getPlayerFaction(GameClient.getClientPlayerState()), factionData);
                (relationTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple("Opinion: " + relationship.toString());
                GUIClippedRow relationRowElement;
                (relationRowElement = new GUIClippedRowToolTip(this.getState(), relationship.getFullString())).attach(relationTextElement);

                FactionDiplomacyListRow factionDiplomacyListRow = new FactionDiplomacyListRow(getState(), factionData, nameRowElement, federationRowElement, membersRowElement, relationRowElement);
                factionDiplomacyListRow.onInit();
                guiElementList.add(factionDiplomacyListRow);
            }
        }
        guiElementList.updateDim();
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        ((GameClientState) getState()).getFactionManager().deleteObserver(this);
    }

    public class GUIClippedRowToolTip extends GUIClippedRow implements TooltipProvider {

        private final String hoverText;
        private GUIToolTip toolTip;

        public GUIClippedRowToolTip(InputState inputState, String hoverText) {
            super(inputState);
            this.hoverText = hoverText;
        }

        @Override
        public void onInit() {
            super.onInit();
            (toolTip = new GUIToolTip(getState(), hoverText, this)).onInit();
        }

        @Override
        public void drawToolTip() {
            checkMouseInside();
            if(isInside() || toolTip.isDrawableTooltip()) toolTip.draw();
        }
    }

    public class FactionDiplomacyListRow extends ScrollableTableList<FactionData>.Row {

        public FactionDiplomacyListRow(InputState inputState, FactionData factionData, GUIElement... guiElements) {
            super(inputState, factionData, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.setAllwaysOneSelected(true);
        }

        @Override
        public void clickedOnRow() {
            super.clickedOnRow();
            diplomacyTab.setSelectedFaction(f.getFaction());
        }
    }
}
