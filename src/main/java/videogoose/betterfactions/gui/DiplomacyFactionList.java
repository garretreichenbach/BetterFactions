package videogoose.betterfactions.gui;

import videogoose.betterfactions.DiplomacyManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.data.GameClientState;

import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.FactionRelation;
import videogoose.betterfactions.diplomacy.DiplomacyEntity;
import videogoose.betterfactions.diplomacy.FactionStrength;
import videogoose.betterfactions.diplomacy.FactionTreaty;
import videogoose.betterfactions.diplomacy.TreatyType;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Everyone you can have a foreign policy with, on the left of the diplomacy tab.
 *
 * <p>Player factions now hold the same opinion ledger and the same treaties, so
 * there is one list.</p>
 *
 * <p>The treaties column is the point of the screen: it is the only place in the game that answers
 * who you have what with, without clicking through every faction in turn.</p>
 */
public class DiplomacyFactionList extends ScrollableTableList<Faction> {

    /**
     * Filter values. Keyed on these rather than on RType so that "has treaty" can join in.
     */
    private static final Integer ALL = 0;
    private static final Integer AT_WAR = 1;
    private static final Integer NEUTRAL = 2;
    private static final Integer ALLIED = 3;
    private static final Integer TREATIED = 4;

    /**
     * Shown in the treaties column when a pair has signed nothing.
     */
    private static final String NO_TREATIES = "-";

    private final FactionPanelTabs panel;

    public DiplomacyFactionList(InputState state, GUIElement p, FactionPanelTabs panel) {
        super(state, 100, 100, p);
        this.panel = panel;
        ((GameClientState) state).getFactionManager().addObserver(this);
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        ((GameClientState) getState()).getFactionManager().deleteObserver(this);
    }

    private GameClientState client() {
        return (GameClientState) getState();
    }

    private int ownFactionId() {
        return client().getPlayer().getFactionId();
    }

    private FactionRelation.RType relationTo(Faction f) {
        return client().getFactionManager().getRelation(client().getPlayerName(), ownFactionId(), f.getIdFaction());
    }

    /**
     * Their opinion of us: of the viewer's faction, or of the viewer when they have none. The same
     * number the overview's opinion line shows.
     */
    private int standing(Faction f) {
        int own = ownFactionId();
        long subject = own != 0 ? own : client().getPlayer().getDbId();
        DiplomacyEntity e = DiplomacyManager.client().diplomacyOf(f).entities.get(subject);
        return e != null ? e.getPoints() : 0;
    }

    private static long power(Faction f) {
        return Math.round(FactionStrength.of(f));
    }

    /**
     * Treaty type names in force between the viewer's faction and this one.
     */
    private String treatySummary(Faction f) {
        int own = ownFactionId();
        if (own == 0) {
            return NO_TREATIES;
        }
        List<FactionTreaty> held = DiplomacyManager.client().getTreaties(own, f.getIdFaction()).getTreaties();
        if (held.isEmpty()) {
            return NO_TREATIES;
        }
        StringBuilder b = new StringBuilder();
        for (FactionTreaty t : held) {
            TreatyType type = t.getType();
            if (!b.isEmpty()) {
                b.append(", ");
            }
            b.append(t.getDisplayName());
        }
        return b.toString();
    }

    @Override
    public void initColumns() {
        addColumn(Lng.str("Name"), 3.0f, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
        addFixedWidthColumn(Lng.str("Relation"), 90, (o1, o2) -> relationTo(o1).sortWeight - relationTo(o2).sortWeight);
        addColumn(Lng.str("Standing"), 2.0f, (o1, o2) -> Integer.compare(standing(o1), standing(o2)));
        addColumn(Lng.str("Power"), 2.0f, (o1, o2) -> Long.compare(power(o1), power(o2)));

        addTextFilter(new GUIListFilterText<>() {
            @Override
            public boolean isOk(String input, Faction f) {
                return f.getName().toLowerCase(Locale.ENGLISH).contains(input.toLowerCase(Locale.ENGLISH));
            }
        }, Lng.str("SEARCH"), ControllerElement.FilterRowStyle.LEFT);

        addDropdownFilter(new GUIListFilterDropdown<>(ALL, AT_WAR, NEUTRAL, ALLIED, TREATIED) {
            @Override
            public boolean isOk(Integer input, Faction f) {
                if (ALL.equals(input)) {
                    return true;
                }
                if (TREATIED.equals(input)) {
                    return !NO_TREATIES.equals(treatySummary(f));
                }
                FactionRelation.RType r = relationTo(f);
                if (AT_WAR.equals(input)) {
                    return r == FactionRelation.RType.ENEMY;
                }
                if (ALLIED.equals(input)) {
                    return r == FactionRelation.RType.FRIEND;
                }
                return r == FactionRelation.RType.NEUTRAL;
            }
        }, new CreateGUIElementInterface<>() {
            @Override
            public GUIElement create(Integer o) {
                GUIAnchor c = new GUIAnchor(getState(), 10, 24);
                GUITextOverlayTableDropDown a = new GUITextOverlayTableDropDown(10, 10, getState());
                if (ALL.equals(o)) {
                    a.setTextSimple(Lng.str("ALL"));
                } else if (AT_WAR.equals(o)) {
                    a.setTextSimple(Lng.str("AT WAR"));
                } else if (NEUTRAL.equals(o)) {
                    a.setTextSimple(Lng.str("NEUTRAL"));
                } else if (ALLIED.equals(o)) {
                    a.setTextSimple(Lng.str("ALLIED"));
                } else {
                    a.setTextSimple(Lng.str("HAS TREATY"));
                }
                a.setPos(4, 4, 0);
                c.setUserPointer(o);
                c.attach(a);
                return c;
            }

            @Override
            public GUIElement createNeutral() {
                return null; // default is all
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        activeSortColumnIndex = 0;
    }

    @Override
    protected Collection<Faction> getElementList() {
        List<Faction> d = new ObjectArrayList<Faction>();
        int own = ownFactionId();
        for (Faction f : client().getFactionManager().getFactionCollection()) {
            //there is no foreign policy to have with yourself
            if (f.getIdFaction() != own && isParty(f)) {
                d.add(f);
            }
        }
        return d;
    }

    /**
     * Whether a faction is somebody you can have diplomacy with. Id 0 is "no faction", and the
     * Trading Guild is the engine's shop owner rather than a polity - the NPC traders who fly for it
     * are a faction of their own, and that is the one listed.
     */
    public static boolean isParty(Faction f) {
        return f.getIdFaction() != 0 && f.getIdFaction() != FactionManager.TRAIDING_GUILD_ID && f.isShowInHub();
    }

    @Override
    public void updateListEntries(GUIElementList mainList, Set<Faction> collection) {
        mainList.deleteObservers();
        mainList.addObserver(this);
        for (Faction f : collection) {
            GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
            GUITextOverlayTable treatyText = new GUITextOverlayTable(10, 10, getState());
            GUITextOverlayTable relationText = new GUITextOverlayTable(10, 10, getState()) {
                @Override
                public void draw() {
                    setColor(org.schema.game.client.view.gui.shiphud.newhud.ColorPalette
                            .getColorDefault(relationTo(f), false));
                    super.draw();
                }
            };

            nameText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return f.getName() + (f.isNPC() ? " " + Lng.str("(NPC)") : "");
                }
            });
            relationText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return relationTo(f).getName();
                }
            });
            treatyText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    int points = standing(f);
                    return points > 0 ? "+" + points : String.valueOf(points);
                }
            });

            GUITextOverlayTable powerText = new GUITextOverlayTable(10, 10, getState());
            powerText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return String.valueOf(power(f));
                }
            });

            nameText.getPos().y = 5;
            relationText.getPos().y = 5;
            treatyText.getPos().y = 5;
            powerText.getPos().y = 5;

            GUIClippedRow nameRow = new GUIClippedRow(getState());
            nameRow.attach(nameText);
            GUIClippedRow relRow = new GUIClippedRow(getState());
            relRow.attach(relationText);
            GUIClippedRow treatyRow = new GUIClippedRow(getState());
            treatyRow.attach(treatyText);
            GUIClippedRow powerRow = new GUIClippedRow(getState());
            powerRow.attach(powerText);

            FactionRow r = new FactionRow(getState(), f, nameRow, relRow, treatyRow, powerRow);
            r.onInit();
            mainList.addWithoutUpdate(r);
        }
        mainList.updateDim();
    }

    private class FactionRow extends ScrollableTableList<Faction>.Row {
        public FactionRow(InputState state, Faction f, GUIElement... elements) {
            super(state, f, elements);
            highlightSelect = true;
            highlightSelectSimple = true;
            setAllwaysOneSelected(true);
        }

        @Override
        protected void clickedOnRow() {
            panel.onSelectFaction(f);
            super.clickedOnRow();
        }
    }
}
