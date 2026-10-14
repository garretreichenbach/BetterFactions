package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.CivicType;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.FactionCivics;
import videogoose.betterfactions.civics.Senate;

import javax.vecmath.Vector4f;
import java.util.function.BooleanSupplier;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;

/**
 * Governance › Institutions: one card per institution with the button that adopts or dissolves it, and
 * what that does to upkeep. Every held institution also gets its own Governance sub-tab.
 */
public class InstitutionsContent extends GUIAnchor {

    private static final int CARD_HEIGHT = 92;
    private static final int BUTTON_WIDTH = 120;
    static final Vector4f CARD = new Vector4f(0.075f, 0.105f, 0.145f, 1.0f);
    static final Vector4f CARD_HELD = new Vector4f(0.085f, 0.155f, 0.225f, 1.0f);
    static final Vector4f TEXT = new Vector4f(0.84f, 0.88f, 0.92f, 1.0f);
    static final Vector4f DIM = new Vector4f(0.56f, 0.64f, 0.73f, 1.0f);
    static final Vector4f BLUE = new Vector4f(0.49f, 0.75f, 1.0f, 1.0f);
    static final Vector4f ORANGE = new Vector4f(0.94f, 0.64f, 0.40f, 1.0f);

    private GUIElementList list;

    public InstitutionsContent(InputState state, GUIActiveInterface active) {
        super(state);
        activationInterface = active;
    }

    @Override
    public GameClientState getState() {
        return (GameClientState) super.getState();
    }

    @Override
    public void onInit() {
        super.onInit();
        list = new GUIElementList(getState());
        for (CivicType t : CivicRegistry.all()) {
            if (t.getKind() == CivicType.Kind.INSTITUTION) {
                GUIAnchor c = card(t);
                list.add(new GUIListElement(c, c, getState()));
            }
        }
        list.setPos(1, 0, 0);
        GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, this, getState());
        sc.setContent(list);
        sc.onInit();
        attach(sc);
    }

    private int own() {
        Faction f = getState().getFaction();
        return f == null ? 0 : f.getIdFaction();
    }

    private boolean held(String id) {
        return Civics.client().of(own()).institutions.contains(id);
    }

    private GUIAnchor card(CivicType t) {
        String id = t.getId();
        GUIColoredRectangle bg = new GUIColoredRectangle(getState(), 10, CARD_HEIGHT - 6, new Vector4f(CARD)) {
            @Override
            public void draw() {
                getColor().set(held(id) ? CARD_HELD : CARD);
                super.draw();
            }
        };
        GUIAnchor descBox = new GUIAnchor(getState(), 10, CARD_HEIGHT);
        GUITextOverlay desc = text(getState(), 14, DIM, t.getDescription());
        BooleanSupplier mayEdit = () -> getState().getPlayer().getFactionController().hasPermissionEditPermission();
        GUITextButton adopt = FactionSettingsContent.button(getState(), BUTTON_WIDTH, Lng.str("ADOPT"),
                GUITextButton.ColorPalette.OK, () -> mayEdit.getAsBoolean() && !held(id), () -> Civics.requestInstitution(id, true));
        GUITextButton dissolve = FactionSettingsContent.button(getState(), BUTTON_WIDTH, Lng.str("DISSOLVE"),
                GUITextButton.ColorPalette.HOSTILE, () -> mayEdit.getAsBoolean() && held(id), () -> confirmDissolve(t));
        GUITextOverlay note = text(getState(), 14, DIM, new Object() {
            @Override
            public String toString() {
                FactionCivics c = Civics.client().of(own());
                int n = c.institutions.size();
                int base = BFConfig.institutionUpkeep();
                return held(id)
                        ? Lng.str("saves %s FP a turn", FactionCivics.institutionUpkeep(n, base) - FactionCivics.institutionUpkeep(n - 1, base))
                        : Lng.str("+%s FP a turn", FactionCivics.institutionUpkeep(n + 1, base) - FactionCivics.institutionUpkeep(n, base));
            }
        });

        GUITextOverlay name = text(getState(), 16, TEXT, t.getDisplayName());
        GUITextOverlay tag = text(getState(), 14, BLUE, new Object() {
            @Override
            public String toString() {
                return held(id) ? Lng.str("HELD") : "";
            }
        });
        GUIAnchor a = new GUIAnchor(getState(), 100, CARD_HEIGHT) {
            @Override
            public void draw() {
                setWidth(Math.max(400, InstitutionsContent.this.getWidth() - 8));
                bg.setWidth(getWidth() - 8);
                descBox.setWidth(getWidth() - BUTTON_WIDTH - 50);
                adopt.setPos(getWidth() - BUTTON_WIDTH - 20, 14, 0);
                dissolve.setPos(getWidth() - BUTTON_WIDTH - 20, 14, 0);
                note.setPos(getWidth() - BUTTON_WIDTH - 20, 44, 0);
                tag.setPos(26 + name.getMaxLineWidth(), 13, 0);
                //grow to fit the wrapped description; its height is from the last frame's wrap
                int h = Math.max(CARD_HEIGHT, 34 + desc.getTextHeight() + 12);
                if (h != (int) getHeight()) {
                    setHeight(h);
                    bg.setHeight(h - 6);
                    list.updateDim();
                }
                super.draw();
            }
        };
        bg.setPos(4, 3, 0);
        a.attach(bg);
        name.setPos(16, 12, 0);
        a.attach(name);
        a.attach(tag);
        desc.autoWrapOn = descBox;
        desc.setPos(16, 34, 0);
        a.attach(desc);
        a.attach(adopt);
        a.attach(dissolve);
        a.attach(note);
        return a;
    }

    private void confirmDissolve(CivicType t) {
        int open = CivicRegistry.SENATE.equals(t.getId()) ? Senate.client().of(own()).size() : 0;
        new PlayerGameOkCancelInput("CONFIRM", getState(), Lng.str("Dissolve the %s?", t.getDisplayName()),
                open > 0 ? Lng.str("Its %s open votes are dropped and nothing they would have done happens.", open)
                        : Lng.str("It can be adopted again later.")) {
            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void onDeactivate() {
            }

            @Override
            public void pressedOK() {
                deactivate();
                Civics.requestInstitution(t.getId(), false);
            }
        }.activate();
    }

    /** The upkeep summary beside the cards: the total, and what each further institution would cost. */
    String summary() {
        FactionCivics c = Civics.client().of(own());
        int n = c.institutions.size();
        int base = BFConfig.institutionUpkeep();
        videogoose.betterfactions.civics.Council council = videogoose.betterfactions.civics.Council.client();
        int extra = council.extraSeats(own());
        boolean treasury = council.perk(own(), videogoose.betterfactions.civics.Council.TREASURY);
        StringBuilder b = new StringBuilder(Lng.str("UPKEEP\n%s FP per income turn.\n\nEach institution costs more than the one before it. Dropping one is free and lowers the rest.\n",
                Math.round(council.institutionUpkeep(own(), c, base))));
        if (extra > 0) {
            b.append(Lng.str("%s Council seats added by vote: +%s FP.\n", extra, extra * base));
        }
        int charters = videogoose.betterfactions.civics.Corporations.client().extraCharters(own());
        if (charters > 0) {
            b.append(Lng.str("%s corporations past the free charters: +%s FP.\n", charters, charters * base));
        }
        if (treasury) {
            b.append(Lng.str("The Council's treasury seat is devoted: %s\n", videogoose.betterfactions.civics.Council.perk(videogoose.betterfactions.civics.Council.TREASURY)));
        }
        b.append(Lng.str("\nCOST LADDER\n"));
        for (int i = 1; i <= Math.max(4, n + 1); i++) {
            b.append(Lng.str("%s held: %s FP%s\n", i, FactionCivics.institutionUpkeep(i, base), i == n ? Lng.str("  (now)") : ""));
        }
        return b.append(Lng.str("\nAdopting or dissolving needs the Edit ranks permission.")).toString();
    }

    static GUITextOverlay text(InputState state, int size, Vector4f color, Object s) {
        GUITextOverlay o = new GUITextOverlay(10, 10, size == 16 ? FontLibrary.getBlenderProMedium16() : FontLibrary.getBlenderProMedium14(), state);
        o.setTextSimple(s);
        o.setColor(color);
        return o;
    }
}
