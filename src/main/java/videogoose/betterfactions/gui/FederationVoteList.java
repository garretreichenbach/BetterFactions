package videogoose.betterfactions.gui;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.Perms;
import videogoose.betterfactions.diplomacy.Federation;
import videogoose.betterfactions.diplomacy.FederationCommand;

import javax.vecmath.Vector4f;
import java.util.function.BooleanSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.Supplier;

/**
 * Everything the bloc is deciding: one card per open vote, with how far along it is and the two
 * buttons that answer it.
 *
 * <p>A card rather than a table row, because the thing worth showing is a proportion - for, against,
 * not yet voted - and that is a bar, not three numbers in columns.</p>
 */
public class FederationVoteList extends GUIAnchor {

    private static final int CARD_HEIGHT = 64;
    private static final int BAR_HEIGHT = 7;
    private static final Vector4f DIM = new Vector4f(0.55f, 0.58f, 0.60f, 1.0f);
    private static final Vector4f FOR = new Vector4f(0.31f, 0.50f, 0.35f, 1.0f);
    private static final Vector4f AGAINST = new Vector4f(0.50f, 0.31f, 0.31f, 1.0f);
    private static final Vector4f PENDING = new Vector4f(0.22f, 0.26f, 0.29f, 1.0f);

    private final Supplier<Federation> federation;
    private final GUIElementList list;
    /**
     * Which votes the cards were built for. Cards read their numbers live, so they only need building
     * again when a vote opens or closes.
     */
    private final IntArrayList built = new IntArrayList();

    public FederationVoteList(InputState state, Supplier<Federation> federation) {
        super(state);
        this.federation = federation;
        this.list = new GUIElementList(state);
    }

    @Override
    public GameClientState getState() {
        return (GameClientState) super.getState();
    }

    @Override
    public void onInit() {
        super.onInit();
        list.setPos(1, 0, 0);
        GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, this, getState());
        sc.setContent(list);
        sc.onInit();
        attach(sc);
    }

    @Override
    public void draw() {
        Federation f = federation.get();
        IntArrayList now = new IntArrayList();
        if (f != null) {
            for (Federation.FederationVote v : f.votes) {
                now.add(v.id);
            }
        }
        if (!now.equals(built)) {
            built.clear();
            built.addAll(now);
            list.clear();
            if (now.isEmpty()) {
                GUITextOverlay none = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
                none.setTextSimple(Lng.str("Nothing is being voted on."));
                none.setColor(DIM);
                none.setPos(8, 8, 0);
                GUIAnchor box = new GUIAnchor(getState(), 200, 30);
                box.attach(none);
                list.add(new GUIListElement(box, box, getState()));
            }
            for (int i = 0; i < now.size(); i++) {
                GUIAnchor card = card(now.getInt(i));
                list.add(new GUIListElement(card, card, getState()));
            }
        }
        super.draw();
    }

    private Federation.FederationVote vote(int id) {
        Federation f = federation.get();
        return f != null ? f.vote(id) : null;
    }

    private int own() {
        return getState().getPlayer().getFactionId();
    }

    private boolean mayVote(int id) {
        Federation f = federation.get();
        Federation.FederationVote v = vote(id);
        return v != null && f.mayVote(v, own()) && Perms.mine(Perms.FEDERATION_VOTE);
    }

    /**
     * For, against and outstanding weight, in that order.
     */
    private double[] weights(int id) {
        Federation f = federation.get();
        Federation.FederationVote v = vote(id);
        double[] w = new double[3];
        if (v == null) {
            return w;
        }
        IntToDoubleFunction weight = DiplomacyManager.client().voteWeight(f);
        for (int i = 0; i < f.members.size(); i++) {
            int m = f.members.getInt(i);
            if (!f.mayVote(v, m)) {
                continue;
            }
            int slot = !v.hasVoted(m) ? 2 : v.ballots.get(m) != 0 ? 0 : 1;
            w[slot] += weight.applyAsDouble(m);
        }
        return w;
    }

    private GUIAnchor card(int id) {
        GUIAnchor c = new GUIAnchor(getState(), 100, CARD_HEIGHT) {
            @Override
            public void draw() {
                setWidth(Math.max(200, FederationVoteList.this.getWidth() - 8));
                super.draw();
            }
        };

        GUITextOverlay title = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
        title.setTextSimple(new Object() {
            @Override
            public String toString() {
                Federation.FederationVote v = vote(id);
                if (v == null) {
                    return "";
                }
                return Lng.str("%s   closes in %s", DiplomacyManager.client().describeVote(v),
                        DiplomacyTreatyList.roughly(v.closes - System.currentTimeMillis()));
            }
        });
        title.setPos(8, 4, 0);

        GUIColoredRectangle yes = new GUIColoredRectangle(getState(), 1, BAR_HEIGHT, new Vector4f(FOR));
        GUIColoredRectangle no = new GUIColoredRectangle(getState(), 1, BAR_HEIGHT, new Vector4f(AGAINST));
        GUIColoredRectangle pending = new GUIColoredRectangle(getState(), 1, BAR_HEIGHT, new Vector4f(PENDING));
        GUIAnchor bar = new GUIAnchor(getState(), 10, BAR_HEIGHT) {
            @Override
            public void draw() {
                setWidth(Math.max(10, c.getWidth() - 16));
                double[] w = weights(id);
                double total = w[0] + w[1] + w[2];
                float x = 0;
                GUIColoredRectangle[] parts = {yes, no, pending};
                for (int i = 0; i < 3; i++) {
                    float width = total > 0 ? (float) (w[i] / total) * super.getWidth() : 0;
                    parts[i].setWidth(Math.max(0, width - 2));
                    parts[i].setPos(x, 0, 0);
                    x += width;
                }
                super.draw();
            }
        };
        bar.attach(yes);
        bar.attach(no);
        bar.attach(pending);
        bar.setPos(8, 26, 0);

        GUITextOverlay summary = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        summary.setTextSimple(new Object() {
            @Override
            public String toString() {
                return summary(id);
            }
        });
        summary.setColor(DIM);
        summary.setPos(8, 40, 0);

        GUITextButton voteYes = button(Lng.str("FOR"), GUITextButton.ColorPalette.OK, () -> mayVote(id), true, id);
        GUITextButton voteNo = button(Lng.str("AGAINST"), GUITextButton.ColorPalette.HOSTILE, () -> mayVote(id), false, id);
        GUIAnchor buttons = new GUIAnchor(getState(), 140, 22) {
            @Override
            public void draw() {
                setPos(c.getWidth() - 150, 2, 0);
                super.draw();
            }
        };
        voteYes.setPos(0, 0, 0);
        voteNo.setPos(56, 0, 0);
        buttons.attach(voteYes);
        buttons.attach(voteNo);

        c.attach(title);
        c.attach(bar);
        c.attach(summary);
        c.attach(buttons);
        return c;
    }

    private String summary(int id) {
        Federation f = federation.get();
        Federation.FederationVote v = vote(id);
        if (v == null) {
            return "";
        }
        int yes = 0;
        int no = 0;
        int pending = 0;
        for (int i = 0; i < f.members.size(); i++) {
            int m = f.members.getInt(i);
            if (!f.mayVote(v, m)) {
                continue;
            }
            if (!v.hasVoted(m)) {
                pending++;
            } else if (v.ballots.get(m) != 0) {
                yes++;
            } else {
                no++;
            }
        }
        String rule = switch (f.threshold()) {
            case SIMPLE_MAJORITY -> Lng.str("simple majority");
            case TWO_THIRDS -> Lng.str("two thirds");
            case UNANIMOUS -> Lng.str("everyone");
        };
        if (f.hasCharterFlag(Federation.CHARTER_WEIGHT_BY_POWER)) {
            rule = Lng.str("%s by power", rule);
        }
        String yours;
        if (!f.mayVote(v, own())) {
            yours = Lng.str("You do not vote on this.");
        } else if (!v.hasVoted(own())) {
            yours = Lng.str("You have not voted.");
        } else {
            yours = v.ballots.get(own()) != 0 ? Lng.str("You voted for.") : Lng.str("You voted against.");
        }
        String binding = "";
        if (v.kind == Federation.FederationVote.Kind.WAR) {
            binding = f.hasCharterFlag(Federation.CHARTER_BIND_SUPPORTERS)
                    ? Lng.str(" Binds only those who vote for it.")
                    : Lng.str(" Binds every member either way.");
        }
        return Lng.str("%s for, %s against, %s not voted - %s. %s%s", yes, no, pending, rule, yours, binding);
    }

    private GUITextButton button(String label, GUITextButton.ColorPalette palette, BooleanSupplier shown,
                                 boolean yes, int id) {
        return new GUITextButton(getState(), 52, 20, palette, label, new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                Federation f = federation.get();
                if (event.pressedLeftMouse() && shown.getAsBoolean() && f != null) {
                    DiplomacyManager.client().sendFederationCommand(
                            new FederationCommand(FederationCommand.Kind.CAST, f.id).target(id).value(yes ? 1 : 0));
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
}
