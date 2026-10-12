package videogoose.betterfactions.gui;

import videogoose.betterfactions.DiplomacyManager;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.data.GameClientState;

import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.diplomacy.Federation;
import videogoose.betterfactions.diplomacy.FederationCommand;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.GUITextButton;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Every bloc on the server, on the left of the federation tab.
 *
 * <p>All of them rather than only your own, because the question this screen mostly answers is who
 * is standing with whom - a bloc you are not in is exactly the thing you want to see before picking
 * a fight with one of its members.</p>
 *
 * <p>The last column is where an invitation is answered. An invite addressed to your faction has no
 * other home: it belongs to the bloc that sent it, and this is the row that bloc is already on.</p>
 */
public class FederationList extends ScrollableTableList<Federation> {

    private final FactionPanelTabs panel;

    public FederationList(InputState state, GUIElement p, FactionPanelTabs panel) {
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

    private String leaderName(Federation f) {
        Faction l = client().getFactionManager().getFaction(f.leader);
        return l != null ? l.getName() : String.valueOf(f.leader);
    }

    /**
     * Where the viewer's own faction stands with this bloc, which is the only per-row thing that is
     * about the reader rather than about the bloc.
     */
    private String standing(Federation f) {
        int own = ownFactionId();
        if (own == 0) {
            return "-";
        }
        if (f.isMember(own)) {
            return f.leader == own ? Lng.str("You lead it") : Lng.str("Member");
        }
        if (f.isInvited(own)) {
            return Lng.str("Invited");
        }
        return "-";
    }

    @Override
    public void initColumns() {
        addColumn(Lng.str("Federation"), 3.0f, (o1, o2) -> o1.name.compareToIgnoreCase(o2.name));
        addFixedWidthColumn(Lng.str("Size"), 50, (o1, o2) -> o1.members.size() - o2.members.size());
        addColumn(Lng.str("Led by"), 2.0f, (o1, o2) -> leaderName(o1).compareToIgnoreCase(leaderName(o2)));
        addColumn(Lng.str("You"), 2.0f, (o1, o2) -> standing(o1).compareToIgnoreCase(standing(o2)));

        addTextFilter(new GUIListFilterText<>() {
            @Override
            public boolean isOk(String input, Federation f) {
                return f.name.toLowerCase(Locale.ENGLISH).contains(input.toLowerCase(Locale.ENGLISH));
            }
        }, Lng.str("SEARCH"), ControllerElement.FilterRowStyle.FULL);

        activeSortColumnIndex = 0;
    }

    @Override
    protected Collection<Federation> getElementList() {
        return new ObjectArrayList<Federation>(DiplomacyManager.client().getFederations());
    }

    @Override
    public void updateListEntries(GUIElementList mainList, Set<Federation> collection) {
        mainList.deleteObservers();
        mainList.addObserver(this);
        for (Federation f : collection) {
            GUITextOverlayTable nameText = text(f.name);
            GUITextOverlayTable sizeText = text(new Object() {
                @Override
                public String toString() {
                    return String.valueOf(f.members.size());
                }
            });
            GUITextOverlayTable leadText = text(new Object() {
                @Override
                public String toString() {
                    return leaderName(f);
                }
            });

            GUIAnchor youCell = new GUIAnchor(getState(), 10, 24);
            GUITextOverlayTable youText = text(new Object() {
                @Override
                public String toString() {
                    return standing(f);
                }
            });
            youCell.attach(youText);
            //the answer lives on the row that asked the question
            GUITextButton accept = button(Lng.str("ACCEPT"), GUITextButton.ColorPalette.OK, 62,
                    () -> f.isInvited(ownFactionId()),
                    () -> send(new FederationCommand(FederationCommand.Kind.ACCEPT_INVITE, f.id)));
            GUITextButton decline = button(Lng.str("NO"), GUITextButton.ColorPalette.CANCEL, 34,
                    () -> f.isInvited(ownFactionId()),
                    () -> send(new FederationCommand(FederationCommand.Kind.DECLINE_INVITE, f.id)));
            accept.setPos(0, 0, 0);
            decline.setPos(66, 0, 0);
            youCell.attach(accept);
            youCell.attach(decline);

            GUIClippedRow nameRow = new GUIClippedRow(getState());
            nameRow.attach(nameText);
            GUIClippedRow sizeRow = new GUIClippedRow(getState());
            sizeRow.attach(sizeText);
            GUIClippedRow leadRow = new GUIClippedRow(getState());
            leadRow.attach(leadText);

            FederationRow r = new FederationRow(getState(), f, nameRow, sizeRow, leadRow, youCell);
            r.onInit();
            mainList.addWithoutUpdate(r);
        }
        mainList.updateDim();
    }

    private GUITextOverlayTable text(Object value) {
        GUITextOverlayTable t = new GUITextOverlayTable(10, 10, getState());
        t.setTextSimple(value);
        t.getPos().y = 5;
        return t;
    }

    /**
     * A row button that is simply not drawn when it does not apply, which is also what makes it
     * unclickable - the same shape the member list's promote and kick buttons use.
     */
    private GUITextButton button(String label, GUITextButton.ColorPalette palette, int width,
                                 java.util.function.BooleanSupplier shown, Runnable onPress) {
        return new GUITextButton(getState(), width, 22, palette, label, new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse() && shown.getAsBoolean()) {
                    onPress.run();
                }
            }

            @Override
            public boolean isOccluded() {
                return !shown.getAsBoolean();
            }
        }) {
            @Override
            public void draw() {
                if (shown.getAsBoolean()) {
                    super.draw();
                }
            }
        };
    }

    private void send(FederationCommand c) {
        DiplomacyManager.client().sendFederationCommand(c);
    }

    private class FederationRow extends ScrollableTableList<Federation>.Row {
        public FederationRow(InputState state, Federation f, GUIElement... elements) {
            super(state, f, elements);
            highlightSelect = true;
            highlightSelectSimple = true;
            setAllwaysOneSelected(true);
        }

        @Override
        protected void clickedOnRow() {
            panel.onSelectFederation(f);
            super.clickedOnRow();
        }
    }
}
