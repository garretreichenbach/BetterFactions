package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionRole;
import org.schema.game.common.data.player.faction.FactionRoles;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUICheckBoxTextPair;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.FactionCivics;

import static videogoose.betterfactions.gui.InstitutionsContent.*;

/**
 * Senate rules: its name, who sits (every active member, or chosen ranks) and how long a vote stays open. Saved
 * on OK; the server checks the rank and caps the hours again.
 */
public final class SenateRulesDialog extends PlayerGameOkCancelInput {

    private static final int[] HOURS = {6, 12, 24, 48, 72};
    private static final int ROW = 26;

    private final Faction own;
    /** As in {@link FactionCivics#senateSeats}: 0 seats every rank. */
    private int seats;
    private int hours;
    private boolean veto;
    /** Remembered so switching to every member and back keeps the ranks that were ticked. */
    private int chosen;
    private GUIActivatableTextBar name;

    public SenateRulesDialog(GameClientState state, Faction own) {
        this(state, own, Civics.client().of(own.getIdFaction()));
    }

    private SenateRulesDialog(GameClientState state, Faction own, FactionCivics c) {
        super("SENATE_RULES_V2", state, 620, 640, Lng.str("%s rules", c.senateName()), "");
        this.own = own;
        seats = c.senateSeats;
        hours = c.senateHours;
        veto = c.senateVeto;
        chosen = seats != 0 ? seats : 1 << FactionRoles.INDEX_ADMIN_ROLE;
        getInputPanel().setOkButtonText(Lng.str("SAVE RULES"));
        getInputPanel().onInit();
        build(getInputPanel().getContent(), c.senateName);
    }

    private void build(GUIAnchor box, String current) {
        GUITextOverlay nameLabel = text(getState(), 16, TEXT, Lng.str("NAME"));
        nameLabel.setPos(4, 4, 0);
        box.attach(nameLabel);
        //a fixed-size box: the bar sizes itself to what it depends on, and would fill the dialog otherwise
        GUIAnchor nameBox = new GUIAnchor(getState(), 300, 30);
        nameBox.setPos(4, 24, 0);
        name = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, FactionCivics.MAX_SENATE_NAME, 1,
                Lng.str("Senate"), nameBox, DiplomacyProposeDialog.TEXT, t -> t);
        name.onInit();
        name.setText(current);
        nameBox.attach(name);
        box.attach(nameBox);

        int top = 60;
        GUITextOverlay who = text(getState(), 16, TEXT, Lng.str("WHO SITS"));
        who.setPos(4, top + 4, 0);
        box.attach(who);
        GUITextButton all = toggle(150, Lng.str("ALL MEMBERS"), () -> seats == 0, () -> seats = 0);
        GUITextButton ranks = toggle(150, Lng.str("CHOSEN RANKS"), () -> seats != 0, () -> seats = chosen);
        all.setPos(4, top + 26, 0);
        ranks.setPos(162, top + 26, 0);
        box.attach(all);
        box.attach(ranks);

        //top rank down, as the ladder reads everywhere else
        FactionRole[] roles = own.getRoles().getRoles();
        int y = top + 60;
        for (int i = FactionRoles.MAX_ROLES - 1; i >= FactionRoles.MAX_ROLES - own.getRoles().getCount(); i--) {
            int rank = i;
            boolean founder = rank == FactionRoles.INDEX_ADMIN_ROLE;
            GUICheckBoxTextPair box2 = new GUICheckBoxTextPair(getState(), new Object() {
                @Override
                public String toString() {
                    return founder ? Lng.str("%s (%s active, always sits)", roles[rank].name, active(rank))
                            : Lng.str("%s (%s active)", roles[rank].name, active(rank));
                }
            }, 400, 24) {
                @Override
                public boolean isActivated() {
                    return (chosen & (1 << rank)) != 0;
                }

                @Override
                public void activate() {
                    chosen |= 1 << rank;
                    seats = chosen;
                }

                @Override
                public void deactivate() {
                    if (!founder) {
                        chosen &= ~(1 << rank);
                        seats = chosen;
                    }
                }

                @Override
                public void draw() {
                    //the ranks only matter when it is ranks that sit
                    if (seats != 0) {
                        super.draw();
                    }
                }
            };
            box2.setPos(12, y, 0);
            box.attach(box2);
            y += ROW;
        }

        int ty = y + 6;
        GUITextOverlay timeout = text(getState(), 16, TEXT, Lng.str("VOTE TIMEOUT"));
        timeout.setPos(4, ty, 0);
        box.attach(timeout);
        int x = 4;
        GUITextButton def = toggle(130, Lng.str("DEFAULT (%sh)", BFConfig.senateVoteHours()), () -> hours == 0, () -> hours = 0);
        def.setPos(x, ty + 22, 0);
        box.attach(def);
        x += 138;
        for (int h : HOURS) {
            if (h > BFConfig.senateMaxVoteHours()) {
                continue;
            }
            GUITextButton b = toggle(60, h + "h", () -> hours == h, () -> hours = h);
            b.setPos(x, ty + 22, 0);
            box.attach(b);
            x += 66;
        }

        int vy = ty + 56;
        GUITextOverlay vetoLabel = text(getState(), 16, TEXT, Lng.str("LEADER VETO"));
        vetoLabel.setPos(4, vy, 0);
        box.attach(vetoLabel);
        GUITextButton on = toggle(60, Lng.str("ON"), () -> veto, () -> veto = true);
        GUITextButton off = toggle(60, Lng.str("OFF"), () -> !veto, () -> veto = false);
        on.setPos(140, vy - 2, 0);
        off.setPos(206, vy - 2, 0);
        box.attach(on);
        box.attach(off);
        GUIAnchor vetoWrap = new GUIAnchor(getState(), 580, 40);
        GUITextOverlay vetoNote = text(getState(), 14, DIM, Lng.str("A vote that passes waits for the founder to assent or veto, until its time runs out (at least an hour), then takes effect. The founder's own proposals take effect at once."));
        vetoNote.autoWrapOn = vetoWrap;
        vetoNote.setPos(4, vy + 26, 0);
        box.attach(vetoNote);
        ty = vy + 20;

        GUIAnchor wrap = new GUIAnchor(getState(), 580, 80);
        GUITextOverlay summary = text(getState(), 14, DIM, new Object() {
            @Override
            public String toString() {
                return summary();
            }
        });
        summary.autoWrapOn = wrap;
        summary.setPos(4, ty + 56, 0);
        box.attach(summary);
    }

    private int active(int rank) {
        int n = 0;
        for (FactionPermission p : own.getMembersUID().values()) {
            if (p.role == rank && p.isActiveMember()) {
                n++;
            }
        }
        return n;
    }

    private String summary() {
        int n = 0;
        for (FactionPermission p : own.getMembersUID().values()) {
            if (p.isActiveMember() && (seats == 0 || (seats & (1 << p.role)) != 0)) {
                n++;
            }
        }
        n = Math.max(1, n);
        int h = hours > 0 ? hours : BFConfig.senateVoteHours();
        return Lng.str("Under these rules %s senators sit, and a vote carries as soon as %s are for it. Otherwise it closes after %s hours and goes to whichever side cast more. Any member may still propose; a proposer outside the Senate casts no ballot. Open votes keep the timeout they opened with.",
                n, n / 2 + 1, h);
    }

    /** A button that shows whether its option is the one chosen. */
    private GUITextButton toggle(int width, Object label, java.util.function.BooleanSupplier on, Runnable pick) {
        return new GUITextButton(getState(), width, 24, GUITextButton.ColorPalette.OK, label, new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse()) {
                    pick.run();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        }) {
            @Override
            public void draw() {
                setColorPalette(on.getAsBoolean() ? ColorPalette.OK : ColorPalette.TRANSPARENT);
                super.draw();
            }
        };
    }

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
        Civics.requestSenateRules(seats, hours, veto, FactionCivics.cleanSenateName(name.getText()));
    }
}
