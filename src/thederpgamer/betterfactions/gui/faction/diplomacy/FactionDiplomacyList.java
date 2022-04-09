package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.common.util.CompareTools;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.old.faction.FactionDataOld;
import thederpgamer.betterfactions.manager.FactionManagerOld;
import thederpgamer.betterfactions.manager.FederationManager;

import java.util.ArrayList;
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
public class FactionDiplomacyList extends ScrollableTableList<FactionDataOld> {

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
        this.addColumn("Name", 15.0F, new Comparator<FactionDataOld>() {
            public int compare(FactionDataOld o1, FactionDataOld o2) {
                return o1.getFactionName().compareTo(o2.getFactionName());
            }
        });

        this.addColumn("Federation", 15.0F, new Comparator<FactionDataOld>() {
            public int compare(FactionDataOld o1, FactionDataOld o2) {
                String federationName1 = (o1.getFederationId() != -1) ? FederationManager.getFederation(o1).getName() : "Non-Aligned";
                String federationName2 = (o2.getFederationId() != -1) ? FederationManager.getFederation(o2).getName() : "Non-Aligned";
                return federationName1.compareTo(federationName2);
            }
        });

        this.addColumn("Members", 7.0F, new Comparator<FactionDataOld>() {
            public int compare(FactionDataOld o1, FactionDataOld o2) {
                return CompareTools.compare(GameCommon.getGameState().getFactionManager().getFaction(o1.getFactionId()).getMembersUID().size(), GameCommon.getGameState().getFactionManager().getFaction(o2.getFactionId()).getMembersUID().size());
            }
        });

        this.addColumn("Relation", 10.0F, new Comparator<FactionDataOld>() {
            public int compare(FactionDataOld o1, FactionDataOld o2) {
                return o1.getRelationString().compareTo(o2.getRelationString());
            }
        });

        this.addTextFilter(new GUIListFilterText<FactionDataOld>() {
            public boolean isOk(String s, FactionDataOld faction) {
                return faction.getFactionName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<FactionDataOld, String>(relationValues) {
            public boolean isOk(String s, FactionDataOld faction) {
                if(s.equalsIgnoreCase("ALL")) {
                    return true;
                } else if(GameClient.getClientPlayerState().getFactionId() != 0 && faction.getFactionId() == GameClient.getClientPlayerState().getFactionId()) {
                    return s.equalsIgnoreCase("ALLIED") || s.equalsIgnoreCase("IN FEDERATION") || s.equalsIgnoreCase("OWN FACTION");
                } else return s.equalsIgnoreCase(faction.getRelationString());
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
                return null;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        this.activeSortColumnIndex = 0;
    }

    @Override
    public Collection<FactionDataOld> getElementList() {
        return new ArrayList<>(FactionManagerOld.getFactionDataMap().values());
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<FactionDataOld> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        for(FactionDataOld factionData : set) {
            if(factionData != null) {
                GUITextOverlayTable nameTextElement;
                String factionName = factionData.getFactionName();
                if(factionData.getFactionId() == org.schema.game.common.data.player.faction.FactionManager.TRAIDING_GUILD_ID) factionName = "Trading Guild";
                (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(factionName);
                GUIClippedRow nameRowElement;
                (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

                String federationName = (FederationManager.getFederation(factionData) != null) ? FederationManager.getFederation(factionData).getName() : "Non-Aligned";
                GUITextOverlayTable federationTextElement;
                (federationTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(federationName);
                GUIClippedRow federationRowElement;
                (federationRowElement = new GUIClippedRow(this.getState())).attach(federationTextElement);

                int members = GameCommon.getGameState().getFactionManager().getFaction(factionData.getFactionId()).getMembersUID().size();
                GUITextOverlayTable membersTextElement;
                (membersTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple((members <= 0) ? "N/A" : members + " members");
                GUIClippedRow membersRowElement;
                (membersRowElement = new GUIClippedRow(this.getState())).attach(membersTextElement);

                GUITextOverlayTable relationTextElement;
                (relationTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(factionData.getRelationString());
                GUIClippedRow relationRowElement;
                (relationRowElement = new GUIClippedRow(this.getState())).attach(relationTextElement);

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

    public class FactionDiplomacyListRow extends ScrollableTableList<FactionDataOld>.Row {

        public FactionDiplomacyListRow(InputState inputState, FactionDataOld factionData, GUIElement... guiElements) {
            super(inputState, factionData, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.setAllwaysOneSelected(true);
        }

        @Override
        public void clickedOnRow() {
            super.clickedOnRow();
            if(!GameCommon.getGameState().getFactionManager().existsFaction(f.getFactionId())) {
                FactionManagerOld.removeFactionData(f);
                diplomacyTab.updateTab();
            } else diplomacyTab.setSelectedFaction(GameCommon.getGameState().getFactionManager().getFaction(f.getFactionId()));
        }
    }
}
