package thederpgamer.betterfactions.gui.faction.news;

import api.common.GameClient;
import org.hsqldb.lib.StringComparator;
import org.schema.common.util.CompareTools;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.old.faction.FactionDataOld;
import thederpgamer.betterfactions.data.old.federation.FederationData;
import thederpgamer.betterfactions.manager.FactionManagerOld;
import thederpgamer.betterfactions.utils.FactionNewsUtils;

import java.util.*;

/**
 * FactionNewsScrollableList.java
 * <Description>
 *
 * @since 02/10/2021
 * @author TheDerpGamer
 */
public class FactionNewsScrollableList extends ScrollableTableList<FactionNewsEntry> {

    private FactionNewsTab newsTab;
    private GUIAncor anchor;

    public FactionNewsScrollableList(InputState inputState, GUIAncor anchor, FactionNewsTab newsTab) {
        super(inputState, 100, 100, anchor);
        this.newsTab = newsTab;
        this.anchor = anchor;
        this.anchor.attach(this);
    }

    private boolean getFilterValue(FactionNewsEntry newsEntry, FactionNewsEntry.FactionNewsType newsType) {
        PlayerState playerState = GameClient.getClientPlayerState();
        if(newsType.equals(FactionNewsEntry.FactionNewsType.ALL)) {
            return true;
        } else if(newsType.equals(FactionNewsEntry.FactionNewsType.RELEVANT)) {
            if(newsEntry.hasSubject() && playerState.getFactionId() != 0) {
                Object subject = newsEntry.getSubject();
                if(subject instanceof FactionDataOld) {
                    FactionDataOld factionData = (FactionDataOld) subject;
                    return playerState.getFactionId() == factionData.getFactionId();
                } else if(subject instanceof FederationData) {
                    if(Objects.requireNonNull(FactionManagerOld.getPlayerFactionData(GameClient.getClientPlayerState().getName())).getFederationId() != -1) {
                        FederationData federationData = (FederationData) subject;
                        return federationData.getId() == Objects.requireNonNull(FactionManagerOld.getPlayerFactionData(GameClient.getClientPlayerState().getName())).getFederationId();
                    }
                }
            }
            return false;
        } else {
            return newsEntry.type.equals(newsType);
        }
    }

    @Override
    public void initColumns() {
        new StringComparator();

        this.addColumn(Lng.str("Date"), 7.0F, new Comparator<FactionNewsEntry>() {
            public int compare(FactionNewsEntry o1, FactionNewsEntry o2) {
                return CompareTools.compare(Date.parse(o1.date), Date.parse(o2.date));
            }
        });

        this.addColumn(Lng.str("Title"), 15.0F, new Comparator<FactionNewsEntry>() {
            public int compare(FactionNewsEntry o1, FactionNewsEntry o2) {
                return o1.title.compareTo(o2.title);
            }
        });

        this.addColumn(Lng.str("Type"), 8.5F, new Comparator<FactionNewsEntry>() {
            public int compare(FactionNewsEntry o1, FactionNewsEntry o2) {
                return o1.type.compareTo(o2.type);
            }
        });

        this.addTextFilter(new GUIListFilterText<FactionNewsEntry>() {
            public boolean isOk(String s, FactionNewsEntry newsEntry) {
                return newsEntry.title.toLowerCase().contains(s.toLowerCase());
            }
        }, ControllerElement.FilterRowStyle.LEFT);

        this.addDropdownFilter(new GUIListFilterDropdown<FactionNewsEntry, FactionNewsEntry.FactionNewsType>(FactionNewsEntry.FactionNewsType.values()) {
            public boolean isOk(FactionNewsEntry.FactionNewsType type, FactionNewsEntry entry) {
                return(getFilterValue(entry, type));
            }
        }, new CreateGUIElementInterface<FactionNewsEntry.FactionNewsType>() {
            @Override
            public GUIElement create(FactionNewsEntry.FactionNewsType factionNewsType) {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(factionNewsType.toString());
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(factionNewsType.toString());
                anchor.attach(dropDown);
                return anchor;
            }

            @Override
            public GUIElement createNeutral() {
                /*
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(FactionNewsEntry.FactionNewsType.ALL.toString());
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(FactionNewsEntry.FactionNewsType.ALL.toString());
                anchor.attach(dropDown);
                return anchor;
                 */
                return null;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        this.activeSortColumnIndex = 0;
    }

    @Override
    public ArrayList<FactionNewsEntry> getElementList() {
        return new ArrayList<>(FactionNewsUtils.getNewsMap().values());
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<FactionNewsEntry> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);

        for(FactionNewsEntry newsEntry : set) {

            GUITextOverlayTable dateTextElement;
            (dateTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(newsEntry.date);
            GUIClippedRow dateRowElement;
            (dateRowElement = new GUIClippedRow(this.getState())).attach(dateTextElement);

            GUITextOverlayTable titleTextElement;
            (titleTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(newsEntry.title);
            GUIClippedRow titleRowElement;
            (titleRowElement = new GUIClippedRow(this.getState())).attach(titleTextElement);

            GUITextOverlayTable typeTextElement;
            (typeTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(newsEntry.type.toString());
            GUIClippedRow typeRowElement;
            (typeRowElement = new GUIClippedRow(this.getState())).attach(typeTextElement);

            FactionNewsListRow newsListRow = new FactionNewsListRow(this.getState(), newsEntry, dateRowElement, titleRowElement, typeRowElement);
            newsListRow.expanded = new GUIElementList(getState());

            GUITextOverlay textOverlay = new GUITextOverlay(100, 100, getState());
            textOverlay.autoWrapOn = newsListRow.bg;
            textOverlay.setFont(FontLibrary.FontSize.SMALL.getFont());
            textOverlay.setTextSimple(newsEntry.text);
            textOverlay.onInit();
            textOverlay.setPos(newsListRow.expanded.getPos());
            textOverlay.getPos().x += 2;
            textOverlay.getPos().y += 2;
            textOverlay.setWidth((int) (newsListRow.bg.getWidth() - 4));
            textOverlay.setHeight((int) (newsListRow.bg.getHeight() - 4));

            newsListRow.expanded.add(new GUIListElement(textOverlay, textOverlay, getState()));
            newsListRow.expanded.attach(textOverlay);
            newsListRow.onInit();
            guiElementList.add(newsListRow);
        }
        guiElementList.updateDim();
    }

    public class FactionNewsListRow extends ScrollableTableList<FactionNewsEntry>.Row {

        public FactionNewsListRow(InputState inputState, FactionNewsEntry newsEntry, GUIElement... guiElements) {
            super(inputState, newsEntry, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.setAllwaysOneSelected(true);
        }
    }
}
