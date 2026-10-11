package videogoose.betterfactions.gui;

import videogoose.betterfactions.DiplomacyManager;

import videogoose.betterfactions.Perms;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.diplomacy.FactionStrength;
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
import java.util.Collections;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Who is in the bloc, who chairs it, who has been asked, and how much each of them weighs.
 *
 * <p>Ballots are not here: a bloc can have several votes open at once, so how each member answered
 * belongs to the vote rather than to the member.</p>
 */
public class FederationMemberList extends ScrollableTableList<FederationMemberList.Seat> {

    private final Supplier<Federation> federation;

    public FederationMemberList(InputState state, GUIElement p, Supplier<Federation> federation) {
        super(state, 100, 100, p);
        this.federation = federation;
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

    /**
     * One faction's place in the bloc.
     *
     * <p>Compared on everything it shows rather than on the faction id alone. The list refreshes
     * whenever anything about any faction changes, and a row whose element does not equal its own
     * twin is destroyed and rebuilt every time - taking the reader's selection with it - while a row
     * that never differs would keep showing yesterday's chair.</p>
     */
    public static final class Seat {
        public final int factionId;
        public final boolean invited;
        public final boolean leads;

        Seat(int factionId, boolean invited, boolean leads) {
            this.factionId = factionId;
            this.invited = invited;
            this.leads = leads;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Seat)) {
                return false;
            }
            Seat s = (Seat) o;
            return factionId == s.factionId && invited == s.invited && leads == s.leads;
        }

        @Override
        public int hashCode() {
            return factionId * 31 + (invited ? 2 : 0) + (leads ? 1 : 0);
        }
    }

    private String nameOf(Seat s) {
        Faction f = client().getFactionManager().getFaction(s.factionId);
        return f != null ? f.getName() : String.valueOf(s.factionId);
    }

    private static String roleOf(Seat s) {
        if (s.invited) {
            return Lng.str("Invited");
        }
        return s.leads ? Lng.str("Chair") : Lng.str("Member");
    }

    private long powerOf(Seat s) {
        Faction f = client().getFactionManager().getFaction(s.factionId);
        return f != null ? Math.round(FactionStrength.of(f)) : 0;
    }

    @Override
    public void initColumns() {
        addColumn(Lng.str("Faction"), 3.0f, (o1, o2) -> nameOf(o1).compareToIgnoreCase(nameOf(o2)));
        addFixedWidthColumn(Lng.str("Role"), 56, (o1, o2) -> roleOf(o1).compareToIgnoreCase(roleOf(o2)));
        addFixedWidthColumn(Lng.str("Power"), 48, (o1, o2) -> Long.compare(powerOf(o2), powerOf(o1)));
        addFixedWidthColumn(Lng.str(""), 108, (o1, o2) -> 0);
        activeSortColumnIndex = 1;
    }

    @Override
    protected Collection<Seat> getElementList() {
        Federation f = federation.get();
        if (f == null) {
            return Collections.emptyList();
        }
        ObjectArrayList<Seat> out = new ObjectArrayList<Seat>();
        for (int i = 0; i < f.members.size(); i++) {
            int id = f.members.getInt(i);
            out.add(new Seat(id, false, f.leader == id));
        }
        for (int i = 0; i < f.invites.size(); i++) {
            out.add(new Seat(f.invites.getInt(i), true, false));
        }
        return out;
    }

    /**
     * Whether the viewer may act on the bloc at all: their rank speaks for their faction, and their
     * faction is in this bloc. Not a permission check - the server does that - just the difference
     * between drawing a button and not.
     */
    private boolean mayAct() {
        Federation f = federation.get();
        int own = ownFactionId();
        return f != null && own != 0 && f.isMember(own)
                && Perms.mine(Perms.FEDERATION_VOTE);
    }

    private boolean leads() {
        Federation f = federation.get();
        return f != null && f.leader == ownFactionId();
    }

    @Override
    public void updateListEntries(GUIElementList mainList, Set<Seat> collection) {
        mainList.deleteObservers();
        mainList.addObserver(this);
        for (Seat s : collection) {
            GUIClippedRow nameRow = new GUIClippedRow(getState());
            nameRow.attach(text(new Object() {
                @Override
                public String toString() {
                    return nameOf(s);
                }
            }));
            GUIClippedRow roleRow = new GUIClippedRow(getState());
            roleRow.attach(text(roleOf(s)));
            GUIClippedRow powerRow = new GUIClippedRow(getState());
            powerRow.attach(text(new Object() {
                @Override
                public String toString() {
                    return String.valueOf(powerOf(s));
                }
            }));

            GUIAnchor actions = new GUIAnchor(getState(), 10, 24);
            if (s.invited) {
                GUITextButton cancel = button(Lng.str("CANCEL"), GUITextButton.ColorPalette.CANCEL, 60,
                        () -> mayAct() && leads(),
                        () -> send(FederationCommand.Kind.CANCEL_INVITE, s.factionId));
                cancel.setPos(0, 0, 0);
                actions.attach(cancel);
            } else if (!s.leads) {
                //read per frame rather than per row: the charter can change without the row changing
                BooleanSupplier byVote = () -> federation.get() != null
                        && federation.get().hasCharterFlag(Federation.CHARTER_VOTE_EXPULSION);
                GUITextButton expel = button(new Object() {
                            @Override
                            public String toString() {
                                return byVote.getAsBoolean() ? Lng.str("VOTE OUT") : Lng.str("EXPEL");
                            }
                        }, GUITextButton.ColorPalette.CANCEL, 56,
                        () -> mayAct() && (leads() || byVote.getAsBoolean()) && s.factionId != ownFactionId(),
                        () -> send(FederationCommand.Kind.EXPEL, s.factionId));
                GUITextButton lead = button(Lng.str("CHAIR"), GUITextButton.ColorPalette.OK, 46,
                        () -> mayAct() && leads(),
                        () -> send(FederationCommand.Kind.TRANSFER_LEAD, s.factionId));
                expel.setPos(0, 0, 0);
                lead.setPos(60, 0, 0);
                actions.attach(expel);
                actions.attach(lead);
            }

            SeatRow r = new SeatRow(getState(), s, nameRow, roleRow, powerRow, actions);
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

    private GUITextButton button(Object label, GUITextButton.ColorPalette palette, int width,
                                 BooleanSupplier shown, Runnable onPress) {
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

    private void send(FederationCommand.Kind kind, int target) {
        Federation f = federation.get();
        if (f == null) {
            return;
        }
        DiplomacyManager.client().sendFederationCommand(new FederationCommand(kind, f.id).target(target));
    }

    private class SeatRow extends ScrollableTableList<Seat>.Row {
        public SeatRow(InputState state, Seat s, GUIElement... elements) {
            super(state, s, elements);
            highlightSelect = true;
            highlightSelectSimple = true;
        }
    }
}
