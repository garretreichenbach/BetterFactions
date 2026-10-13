package videogoose.betterfactions.gui;

import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.Senate;

import javax.vecmath.Vector4f;
import java.util.Locale;

import static videogoose.betterfactions.gui.InstitutionsContent.*;

/**
 * Governance › Senate: the faction's open votes as cards, each with its tally and the ballot buttons.
 * Rebuilt whenever the server sends the votes again, which it does after every ballot.
 */
public class SenateContent extends GUIAnchor {

    private static final int CARD_HEIGHT = 128;
    /** Extra room for a justification: up to three wrapped lines. */
    private static final int REASON_HEIGHT = 52;
    private static final int BUTTON_WIDTH = 96;
    private static final int COUNTS_WIDTH = 190;
    private static final Vector4f TRACK = new Vector4f(0.04f, 0.06f, 0.08f, 1.0f);
    private static final Vector4f FOR = new Vector4f(0.37f, 0.69f, 1.0f, 1.0f);
    private static final Vector4f AGAINST = new Vector4f(0.91f, 0.54f, 0.23f, 1.0f);
    private static final Vector4f MARK = new Vector4f(0.91f, 0.95f, 1.0f, 1.0f);

    private final GUIElementList list;
    private int builtVersion = -1;

    public SenateContent(InputState state, GUIActiveInterface active) {
        super(state);
        activationInterface = active;
        list = new GUIElementList(state);
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
        //before drawing, never while the list is being drawn
        if (Senate.client().version() != builtVersion) {
            builtVersion = Senate.client().version();
            list.clear();
            Faction own = getState().getFaction();
            if (own != null) {
                for (Senate.Vote v : Senate.client().of(own.getIdFaction())) {
                    GUIAnchor c = card(own, v);
                    list.add(new GUIListElement(c, c, getState()));
                }
                if (list.isEmpty()) {
                    GUIAnchor c = new GUIAnchor(getState(), 400, 40);
                    GUITextOverlay t = text(getState(), 14, DIM, Lng.str("The Senate has nothing before it. Any member may propose from the Diplomacy tab."));
                    t.setPos(12, 12, 0);
                    c.attach(t);
                    list.add(new GUIListElement(c, c, getState()));
                }
            }
        }
        super.draw();
    }

    private GUIAnchor card(Faction own, Senate.Vote v) {
        boolean hasReason = !v.reason.isEmpty();
        int h = CARD_HEIGHT + (hasReason ? REASON_HEIGHT : 0);
        int n = Math.max(1, v.senators);
        int yes = v.count(true), no = v.count(false);
        Byte mine = v.ballots.get(getState().getPlayer().getName().toLowerCase(Locale.ENGLISH));
        boolean sits = Senate.sits(own, getState().getPlayer().getName());
        boolean leading = yes > no;

        GUIColoredRectangle bg = new GUIColoredRectangle(getState(), 10, h - 6, new Vector4f(CARD));
        bg.setPos(4, 3, 0);
        GUITextOverlay desc = text(getState(), 16, TEXT, v.description);
        desc.setPos(16, 12, 0);
        GUITextOverlay by = text(getState(), 14, DIM, new Object() {
            @Override
            public String toString() {
                return Lng.str("Proposed by %s, closes in %s", v.proposer,
                        DiplomacyTreatyList.roughly(Math.max(0, v.closes - System.currentTimeMillis())));
            }
        });
        by.setPos(16, 32, 0);
        boolean held = v.held();
        boolean leader = Senate.leads(own, getState().getPlayer().getName());
        GUITextOverlay status = text(getState(), 14, held || leading ? BLUE : ORANGE,
                held ? Lng.str("AWAITING THE LEADER") : leading ? Lng.str("LEADING") : Lng.str("TRAILING"));
        GUITextOverlay need = text(getState(), 14, DIM, held ? new Object() {
            @Override
            public String toString() {
                return Lng.str("takes effect in %s unless vetoed", DiplomacyTreatyList.roughly(Math.max(0, v.holdUntil - System.currentTimeMillis())));
            }
        } : Lng.str("%s more for to carry", n / 2 + 1 - yes));

        GUIAnchor reasonBox = new GUIAnchor(getState(), 10, REASON_HEIGHT);
        GUITextOverlay reason = text(getState(), 14, TEXT, Lng.str("\"%s\" - %s", v.reason, v.proposer));
        reason.autoWrapOn = reasonBox;
        reason.setPos(16, 54, 0);

        int barY = (hasReason ? REASON_HEIGHT : 0) + 60;
        GUIColoredRectangle track = new GUIColoredRectangle(getState(), 10, 12, new Vector4f(TRACK));
        GUIColoredRectangle forBar = new GUIColoredRectangle(getState(), 0, 12, new Vector4f(FOR));
        GUIColoredRectangle againstBar = new GUIColoredRectangle(getState(), 0, 12, new Vector4f(AGAINST));
        GUIColoredRectangle mark = new GUIColoredRectangle(getState(), 2, 22, new Vector4f(MARK));
        GUITextOverlay counts = text(getState(), 14, TEXT, Lng.str("%s for, %s against, %s left", yes, no, Math.max(0, n - yes - no)));

        //the side voted for keeps its colour; until you vote both have theirs
        //once passed it is the leader's: assent or veto in place of the ballots, which no longer count
        GUITextButton forButton = held
                ? FactionSettingsContent.button(getState(), BUTTON_WIDTH, Lng.str("ASSENT"), GUITextButton.ColorPalette.FRIENDLY,
                () -> leader, () -> Senate.requestLead(v.id, true))
                : FactionSettingsContent.button(getState(), BUTTON_WIDTH, Lng.str("FOR"),
                mine != null && mine == 0 ? GUITextButton.ColorPalette.TRANSPARENT : GUITextButton.ColorPalette.FRIENDLY,
                () -> sits, () -> Senate.requestCast(v.id, true));
        GUITextButton againstButton = held
                ? FactionSettingsContent.button(getState(), BUTTON_WIDTH, Lng.str("VETO"), GUITextButton.ColorPalette.HOSTILE,
                () -> leader, () -> Senate.requestLead(v.id, false))
                : FactionSettingsContent.button(getState(), BUTTON_WIDTH, Lng.str("AGAINST"),
                mine != null && mine == 1 ? GUITextButton.ColorPalette.TRANSPARENT : GUITextButton.ColorPalette.HOSTILE,
                () -> sits, () -> Senate.requestCast(v.id, false));
        int buttonY = barY + 26;
        forButton.setPos(16, buttonY, 0);
        againstButton.setPos(24 + BUTTON_WIDTH, buttonY, 0);
        GUITextOverlay note = text(getState(), 14, DIM, held ? (leader ? Lng.str("The Senate passed it. Assent to carry it out now, or veto it.")
                : Lng.str("The Senate passed it. It waits for the leader to assent or veto."))
                : !sits ? Lng.str("You do not sit in the Senate.")
                : mine == null ? Lng.str("You have not voted.")
                : mine == 1 ? Lng.str("You voted for. You can change your vote until it closes.")
                : Lng.str("You voted against. You can change your vote until it closes."));
        note.setPos(36 + 2 * BUTTON_WIDTH, buttonY + 4, 0);

        GUIAnchor a = new GUIAnchor(getState(), 100, h) {
            @Override
            public void draw() {
                setWidth(Math.max(500, SenateContent.this.getWidth() - 8));
                bg.setWidth(getWidth() - 8);
                reasonBox.setWidth(getWidth() - 200);
                status.setPos(getWidth() - 20 - status.getMaxLineWidth(), 12, 0);
                need.setPos(getWidth() - 20 - need.getMaxLineWidth(), 30, 0);
                int barWidth = (int) getWidth() - 40 - COUNTS_WIDTH;
                track.setWidth(barWidth);
                forBar.setWidth(barWidth * Math.min(yes, n) / (float) n);
                againstBar.setWidth(barWidth * Math.min(no, n) / (float) n);
                againstBar.setPos(16 + barWidth - againstBar.getWidth(), barY, 0);
                mark.setPos(16 + barWidth / 2f - 1, barY - 5, 0);
                counts.setPos(28 + barWidth, barY - 1, 0);
                super.draw();
            }
        };
        track.setPos(16, barY, 0);
        forBar.setPos(16, barY, 0);
        a.attach(bg);
        a.attach(desc);
        a.attach(by);
        a.attach(status);
        a.attach(need);
        if (hasReason) {
            a.attach(reason);
        }
        a.attach(track);
        a.attach(forBar);
        a.attach(againstBar);
        a.attach(mark);
        a.attach(counts);
        a.attach(forButton);
        a.attach(againstButton);
        a.attach(note);
        return a;
    }

    /** Beside the cards: who sits and how a vote is decided. */
    String summary() {
        Faction own = getState().getFaction();
        int n = own == null ? 1 : Senate.seats(own);
        videogoose.betterfactions.civics.FactionCivics rules = videogoose.betterfactions.civics.Civics.client().of(own == null ? 0 : own.getIdFaction());
        return Lng.str("THE SENATE\n%s senators, %s to carry early.\n\nVotes stay open %s hours. A vote carries the moment a majority of senators is for it. At the close, the side with more ballots wins; a tie is lost.\n\n%s Any member may propose from the Diplomacy tab, with a justification for the senators to read.",
                n, n / 2 + 1, rules.voteHours(BFConfig.senateVoteHours(), BFConfig.senateMaxVoteHours()),
                rules.senateSeats == 0 ? Lng.str("Every active member sits.") : Lng.str("Only chosen ranks sit; a proposer outside the Senate casts no ballot."));
    }
}
