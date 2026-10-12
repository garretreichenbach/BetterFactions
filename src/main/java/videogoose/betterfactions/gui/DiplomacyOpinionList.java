package videogoose.betterfactions.gui;

import videogoose.betterfactions.DiplomacyManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.diplomacy.DiplomacyEntity;
import videogoose.betterfactions.diplomacy.OpinionModifier;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.ControllerElement.FilterPos;
import org.schema.schine.graphicsengine.forms.gui.newgui.ControllerElement.FilterRowStyle;
import org.schema.schine.graphicsengine.forms.gui.newgui.CreateGUIElementInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea.HButtonType;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIListFilterDropdown;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIListFilterText;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTable;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITextOverlayTableDropDown;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;

/**
 * Why a faction thinks what it thinks of you: the opinion ledger, itemised.
 *
 * <p>The tab used to carry two of these side by side - one for the player, one for their faction -
 * which spent half the panel rendering the same table twice. They are one list with a toggle now,
 * which is what freed the room for the treaty list.</p>
 *
 * <p>Typed to {@link Faction} rather than to an NPC one. Every faction keeps a ledger since the
 * opinion engine moved off {@code NPCFaction}; for player factions nothing acts on it, so what shows
 * here is a record rather than a forecast.</p>
 */
public class DiplomacyOpinionList extends ScrollableTableList<OpinionModifier> {

    private static final Integer ALL = 0;
    private static final Integer TURN = 1;
    private static final Integer STATIC = 2;

    private Faction toFaction;
    /**
     * Whose opinion entry is shown: the viewer's faction id, or their own player db id.
     */
    private boolean showingFaction = true;

    public DiplomacyOpinionList(InputState state, GUIElement p) {
        super(state, 100, 100, p);
        ((GameClientState) state).getFactionManager().addObserver(this);
    }

    @Override
    public void cleanUp() {
        super.cleanUp();
        ((GameClientState) getState()).getFactionManager().deleteObserver(this);
        if (toFaction != null) {
            DiplomacyManager.client().diplomacyOf(toFaction).deleteObserver(this);
        }
    }

    /**
     * Points the list at another faction's ledger; null clears it.
     */
    public void setToFaction(Faction f) {
        if (toFaction == f) {
            return;
        }
        if (toFaction != null) {
            DiplomacyManager.client().diplomacyOf(toFaction).deleteObserver(this);
        }
        toFaction = f;
        if (toFaction != null) {
            DiplomacyManager.client().diplomacyOf(toFaction).addObserver(this);
        }
        flagDirty();
    }

    private GameClientState client() {
        return (GameClientState) getState();
    }

    /**
     * True when the viewer has a faction to show an entry for; otherwise only the personal one exists.
     */
    private boolean hasOwnFaction() {
        return client().getPlayer().getFactionId() != 0;
    }

    private long subjectId() {
        return showingFaction && hasOwnFaction()
                ? client().getPlayer().getFactionId()
                : client().getPlayer().getDbId();
    }

    /**
     * The entry the list is showing, or null if there is nothing to show yet.
     */
    public DiplomacyEntity getShownEntity() {
        return toFaction == null ? null : DiplomacyManager.client().diplomacyOf(toFaction).entities.get(subjectId());
    }

    public boolean isShowingFaction() {
        return showingFaction && hasOwnFaction();
    }

    @Override
    public void initColumns() {
        addColumn(Lng.str("Modifier"), 3f, new Comparator<OpinionModifier>() {
            @Override
            public int compare(OpinionModifier o1, OpinionModifier o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        addFixedWidthColumn(Lng.str("Value"), 140, new Comparator<OpinionModifier>() {
            @Override
            public int compare(OpinionModifier o1, OpinionModifier o2) {
                return o1.getValue() - o2.getValue();
            }
        });

        addTextFilter(new GUIListFilterText<OpinionModifier>() {
            @Override
            public boolean isOk(String input, OpinionModifier e) {
                return e.getName().toLowerCase(Locale.ENGLISH).contains(input.toLowerCase(Locale.ENGLISH));
            }
        }, Lng.str("SEARCH"), FilterRowStyle.LEFT);

        addDropdownFilter(new GUIListFilterDropdown<OpinionModifier, Integer>(ALL, TURN, STATIC) {
            @Override
            public boolean isOk(Integer input, OpinionModifier f) {
                if (TURN.equals(input)) {
                    return !f.isStatic();
                }
                if (STATIC.equals(input)) {
                    return f.isStatic();
                }
                return true;
            }
        }, new CreateGUIElementInterface<Integer>() {
            @Override
            public GUIElement create(Integer o) {
                GUIAnchor c = new GUIAnchor(getState(), 10, 24);
                GUITextOverlayTableDropDown a = new GUITextOverlayTableDropDown(10, 10, getState());
                if (TURN.equals(o)) {
                    a.setTextSimple(Lng.str("TURN MODIFIERS"));
                } else if (STATIC.equals(o)) {
                    a.setTextSimple(Lng.str("STATIC MODIFIERS"));
                } else {
                    a.setTextSimple(Lng.str("ALL"));
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
        }, FilterRowStyle.RIGHT);

        //label is recomputed every frame, so the one button is both the toggle and its state
        addButton(new GUICallback() {
            @Override
            public boolean isOccluded() {
                return !isActive();
            }

            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse() && hasOwnFaction()) {
                    showingFaction = !showingFaction;
                    flagDirty();
                }
            }
        }, new Object() {
            @Override
            public String toString() {
                return isShowingFaction() ? Lng.str("OPINION OF: YOUR FACTION") : Lng.str("OPINION OF: YOU");
            }
        }, HButtonType.BUTTON_BLUE_MEDIUM, FilterRowStyle.FULL, FilterPos.BOTTOM);

        activeSortColumnIndex = 0;
    }

    @Override
    protected Collection<OpinionModifier> getElementList() {
        DiplomacyEntity ent = getShownEntity();
        if (ent == null) {
            return Collections.emptyList();
        }
        List<OpinionModifier> d = new ObjectArrayList<OpinionModifier>();
        d.addAll(ent.getDynamicMap().values());
        d.addAll(ent.getStaticMap().values());
        return d;
    }

    @Override
    public void updateListEntries(GUIElementList mainList, Set<OpinionModifier> collection) {
        mainList.deleteObservers();
        mainList.addObserver(this);
        for (final OpinionModifier f : collection) {
            GUITextOverlayTable nameText = new GUITextOverlayTable(10, 10, getState());
            GUITextOverlayTable valueText = new GUITextOverlayTable(10, 10, getState());

            nameText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return f.getName();
                }
            });
            valueText.setTextSimple(new Object() {
                @Override
                public String toString() {
                    return f.getValue() + (f.isStatic() ? "" : Lng.str(" / turn"));
                }
            });

            nameText.getPos().y = 5;
            valueText.getPos().y = 5;

            GUIClippedRow nameRow = new GUIClippedRow(getState());
            nameRow.attach(nameText);
            GUIClippedRow valueRow = new GUIClippedRow(getState());
            valueRow.attach(valueText);

            OpinionModifierRow r = new OpinionModifierRow(getState(), f, nameRow, valueRow);
            r.onInit();
            mainList.addWithoutUpdate(r);
        }
        mainList.updateDim();
    }

    private class OpinionModifierRow extends Row {
        public OpinionModifierRow(InputState state, OpinionModifier f, GUIElement... elements) {
            super(state, f, elements);
            this.highlightSelect = true;
        }
    }
}
