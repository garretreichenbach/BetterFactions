package videogoose.betterfactions.gui;

import org.schema.game.client.data.GameClientState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUICheckBoxTextPair;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.diplomacy.Federation;
import videogoose.betterfactions.diplomacy.FederationCommand;

import javax.vecmath.Vector4f;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

/**
 * The bloc's rules: who chairs it, what goes to a vote, what carries one, and whom a carried vote
 * binds.
 *
 * <p>The same anatomy as the faction's own settings - name, what it does, the control - one scope
 * up. Every control shows the rule in force and sends the whole charter back with one rule changed;
 * when the charter itself goes to a vote, that send opens the vote instead, so a chair cannot quietly
 * switch the votes off.</p>
 */
public class FederationGovernance extends GUIAnchor {

    private static final int ROW_HEIGHT = 40;
    private static final int CONTROL_GUTTER = 260;
    private static final Vector4f DIM = new Vector4f(0.55f, 0.58f, 0.60f, 1.0f);
    private static final Vector4f DECISION = new Vector4f(0.77f, 0.69f, 0.51f, 1.0f);
    /**
     * Chair terms offered, in days; 0 holds until handed over.
     */
    private static final int[] TERMS = {0, 7, 30, 90};

    private final Supplier<Federation> federation;

    public FederationGovernance(InputState state, Supplier<Federation> federation) {
        super(state);
        this.federation = federation;
    }

    @Override
    public GameClientState getState() {
        return (GameClientState) super.getState();
    }

    /**
     * Only a member whose rank speaks for it, and only the chair unless rule changes go to a vote.
     * Not a gate - the server checks the same - just whether a click does anything.
     */
    private boolean mayEdit() {
        Federation f = federation.get();
        int own = getState().getPlayer().getFactionId();
        return f != null && f.isMember(own) && Perms.mine(Perms.FEDERATION_VOTE)
                && (f.leader == own || f.hasCharterFlag(Federation.CHARTER_VOTE_CHARTER));
    }

    private long flags() {
        Federation f = federation.get();
        return f != null ? f.charterFlags : 0;
    }

    private void propose(LongUnaryOperator change) {
        Federation f = federation.get();
        if (f == null || !mayEdit()) {
            return;
        }
        DiplomacyManager.client().sendFederationCommand(new FederationCommand(FederationCommand.Kind.SET_CHARTER, f.id)
                .text(f.charter).value(change.applyAsLong(f.charterFlags)));
    }

    @Override
    public void onInit() {
        super.onInit();
        GUIElementList list = new GUIElementList(getState());

        add(list, row(Lng.str("The chair"), Lng.str("Who speaks for the federation and opens votes."),
                cycle(() -> {
                    int days = federation.get() != null ? federation.get().rotationDays() : 0;
                    return days == 0 ? Lng.str("Holds until handed over") : Lng.str("Rotates every %s days", days);
                }, () -> propose(fl -> {
                    Federation probe = new Federation();
                    probe.charterFlags = fl;
                    int i = 0;
                    while (i < TERMS.length && TERMS[i] != probe.rotationDays()) {
                        i++;
                    }
                    return Federation.withRotationDays(fl, TERMS[(i + 1) % TERMS.length]);
                })), false));

        GUIAnchor votes = new GUIAnchor(getState(), 4 * 76, 24);
        votes.attach(flag(Lng.str("Admit"), Federation.CHARTER_VOTE_ADMISSION, 0));
        votes.attach(flag(Lng.str("Expel"), Federation.CHARTER_VOTE_EXPULSION, 76));
        votes.attach(flag(Lng.str("War"), Federation.CHARTER_VOTE_WAR, 152));
        votes.attach(flag(Lng.str("Charter"), Federation.CHARTER_VOTE_CHARTER, 228));
        add(list, row(Lng.str("Put to a vote"), Lng.str("Everything else the chair decides alone."), votes, false));

        add(list, row(Lng.str("Carried by"), Lng.str("How much of the federation it takes to pass one."),
                cycle(() -> switch (federation.get() != null ? federation.get().threshold() : Federation.Threshold.SIMPLE_MAJORITY) {
                    case SIMPLE_MAJORITY -> Lng.str("Simple majority");
                    case TWO_THIRDS -> Lng.str("Two thirds");
                    case UNANIMOUS -> Lng.str("Everyone");
                }, () -> propose(fl -> {
                    Federation probe = new Federation();
                    probe.charterFlags = fl;
                    Federation.Threshold[] all = Federation.Threshold.values();
                    return Federation.withThreshold(fl, all[(probe.threshold().ordinal() + 1) % all.length]);
                })), false));

        add(list, row(Lng.str("Each vote weighs"),
                Lng.str("One faction one vote, or by power - whether a big member can be outvoted by small ones."),
                cycle(() -> (flags() & Federation.CHARTER_WEIGHT_BY_POWER) != 0 ? Lng.str("By power") : Lng.str("The same"),
                        () -> propose(fl -> fl ^ Federation.CHARTER_WEIGHT_BY_POWER)), false));

        add(list, row(Lng.str("A carried war"),
                Lng.str("Binds a member that voted against it, or only those that voted for."),
                cycle(() -> (flags() & Federation.CHARTER_BIND_SUPPORTERS) != 0
                                ? Lng.str("Binds supporters only") : Lng.str("Binds everyone"),
                        () -> propose(fl -> fl ^ Federation.CHARTER_BIND_SUPPORTERS)), true));

        list.setPos(1, 0, 0);
        GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, this, getState());
        sc.setContent(list);
        sc.onInit();
        attach(sc);
    }

    private void add(GUIElementList list, GUIAnchor e) {
        list.add(new GUIListElement(e, e, getState()));
    }

    /**
     * @param decision the one row that changes what membership means rather than how it is counted,
     *                 so it is drawn apart from the bookkeeping above it
     */
    private GUIAnchor row(Object name, Object description, GUIElement control, boolean decision) {
        GUIAnchor r = new GUIAnchor(getState(), 100, ROW_HEIGHT) {
            @Override
            public void draw() {
                setWidth(Math.max(CONTROL_GUTTER, FederationGovernance.this.getWidth() - 8));
                control.setPos(getWidth() - control.getWidth() - 10, 8, 0);
                super.draw();
            }
        };
        GUITextOverlay n = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
        n.setTextSimple(name);
        n.setPos(8, 4, 0);
        if (decision) {
            n.setColor(DECISION);
        }
        GUITextOverlay d = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        d.setTextSimple(description);
        d.setColor(DIM);
        d.setPos(8, 22, 0);
        r.attach(n);
        r.attach(d);
        r.attach(control);
        return r;
    }

    /**
     * A button that shows the rule in force and steps to the next value on a click.
     *
     * <p>ponytail: a cycling button rather than a dropdown - every list here has two to four
     * values. Swap for a dropdown if one grows past that.</p>
     */
    private GUITextButton cycle(Supplier<String> value, Runnable next) {
        return new GUITextButton(getState(), 170, 22, GUITextButton.ColorPalette.OK, new Object() {
            @Override
            public String toString() {
                return value.get();
            }
        }, new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse() && getState().getController().getPlayerInputs().isEmpty()) {
                    next.run();
                }
            }

            @Override
            public boolean isOccluded() {
                return !mayEdit();
            }
        });
    }

    private GUICheckBoxTextPair flag(String label, long bit, int x) {
        GUICheckBoxTextPair c = new GUICheckBoxTextPair(getState(), label, 50, 24) {
            @Override
            public boolean isActivated() {
                return (flags() & bit) != 0;
            }

            @Override
            public void activate() {
                propose(fl -> fl | bit);
            }

            @Override
            public void deactivate() {
                propose(fl -> fl & ~bit);
            }
        };
        c.setPos(x, 0, 0);
        return c;
    }
}
