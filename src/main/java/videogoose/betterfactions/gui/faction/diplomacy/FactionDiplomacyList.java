package videogoose.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.common.util.CompareTools;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.manager.FactionManager;
import videogoose.betterfactions.manager.FederationManager;
import videogoose.betterfactions.mixin.BetterFactionAccessor;

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
public class FactionDiplomacyList extends ScrollableTableList<Faction> {

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
        this.addColumn("Name", 15.0F, new Comparator<Faction>() {
            public int compare(Faction o1, Faction o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        this.addColumn("Federation", 15.0F, new Comparator<Faction>() {
            public int compare(Faction o1, Faction o2) {
                String federationName1 = getFederationName(o1);
                String federationName2 = getFederationName(o2);
                return federationName1.compareTo(federationName2);
            }
        });

        this.addColumn("Members", 7.0F, new Comparator<Faction>() {
            public int compare(Faction o1, Faction o2) {
                return CompareTools.compare(o1.getMembersUID().size(), o2.getMembersUID().size());
            }
        });

        this.addColumn("Relation", 10.0F, new Comparator<Faction>() {
            public int compare(Faction o1, Faction o2) {
                return getRelationString(o1).compareTo(getRelationString(o2));
            }
        });

        this.addTextFilter(new GUIListFilterText<Faction>() {
            public boolean isOk(String s, Faction faction) {
                return faction.getName().toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<Faction, String>(relationValues) {
            public boolean isOk(String s, Faction faction) {
                if(s.equalsIgnoreCase("ALL")) {
                    return true;
                } else if(GameClient.getClientPlayerState().getFactionId() != 0 && faction.getIdFaction() == GameClient.getClientPlayerState().getFactionId()) {
                    return s.equalsIgnoreCase("ALLIED") || s.equalsIgnoreCase("IN FEDERATION") || s.equalsIgnoreCase("OWN FACTION");
                } else return s.equalsIgnoreCase(getRelationString(faction));
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
    public Collection<Faction> getElementList() {
        return new ArrayList<>(GameCommon.getGameState().getFactionManager().getFactionCollection());
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<Faction> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        for(Faction faction : set) {
            if(faction != null) {
                GUITextOverlayTable nameTextElement;
                String factionName = faction.getName();
                if(faction.getIdFaction() == org.schema.game.common.data.player.faction.FactionManager.TRAIDING_GUILD_ID) factionName = "Trading Guild";
                (nameTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(factionName);
                GUIClippedRow nameRowElement;
                (nameRowElement = new GUIClippedRow(this.getState())).attach(nameTextElement);

                String federationName = getFederationName(faction);
                GUITextOverlayTable federationTextElement;
                (federationTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(federationName);
                GUIClippedRow federationRowElement;
                (federationRowElement = new GUIClippedRow(this.getState())).attach(federationTextElement);

                int members = faction.getMembersUID().size();
                GUITextOverlayTable membersTextElement;
                (membersTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple((members <= 0) ? "N/A" : members + " members");
                GUIClippedRow membersRowElement;
                (membersRowElement = new GUIClippedRow(this.getState())).attach(membersTextElement);

                GUITextOverlayTable relationTextElement;
                (relationTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(getRelationString(faction));
                GUIClippedRow relationRowElement;
                (relationRowElement = new GUIClippedRow(this.getState())).attach(relationTextElement);

                FactionDiplomacyListRow factionDiplomacyListRow = new FactionDiplomacyListRow(getState(), faction, nameRowElement, federationRowElement, membersRowElement, relationRowElement);
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

    private String getRelationString(Faction faction) {
        int playerFactionId = GameClient.getClientPlayerState().getFactionId();
        if(playerFactionId != 0) {
            if(playerFactionId == faction.getIdFaction()) return "Own Faction";
            Faction playerFaction = GameCommon.getGameState().getFactionManager().getFaction(playerFactionId);
            int playerFedId = ((BetterFactionAccessor) playerFaction).getFederationId();
            int factionFedId = ((BetterFactionAccessor) faction).getFederationId();
            if(playerFedId != -1 && factionFedId != -1 && playerFedId == factionFedId) return "In Federation";
            FactionRelation.RType relation = GameCommon.getGameState().getFactionManager().getRelation(faction.getIdFaction(), playerFactionId);
            if(relation.equals(FactionRelation.RType.ENEMY)) return "At War";
            else if(relation.equals(FactionRelation.RType.FRIEND)) return "Allied";
            else return "Neutral";
        } else {
            if(faction.getPersonalEnemies().contains(GameClient.getClientPlayerState().getName())) return "Personal Enemy";
            return "Neutral";
        }
    }

    private String getFederationName(Faction faction) {
        int fedId = ((BetterFactionAccessor) faction).getFederationId();
        if(fedId != -1) {
            videogoose.betterfactions.data.persistent.federation.FederationData fed = FederationManager.getFederation(fedId);
            if(fed != null) return fed.getName();
        }
        return "Non-Aligned";
    }

    public class FactionDiplomacyListRow extends ScrollableTableList<Faction>.Row {

        public FactionDiplomacyListRow(InputState inputState, Faction faction, GUIElement... guiElements) {
            super(inputState, faction, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.setAllwaysOneSelected(true);
        }

        @Override
        public void clickedOnRow() {
            super.clickedOnRow();
            if(!GameCommon.getGameState().getFactionManager().existsFaction(f.getIdFaction())) {
                diplomacyTab.updateTab();
            } else diplomacyTab.setSelectedFaction(f);
        }
    }
}
