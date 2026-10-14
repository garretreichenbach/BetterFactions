package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerGameDropDownInput;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.PlayerTextInput;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.Council;
import videogoose.betterfactions.civics.CouncilLore;

import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

import static videogoose.betterfactions.gui.InstitutionsContent.*;

/**
 * Governance › Council: the councilors as a ring, each node their seat, name, agenda in brief and favour;
 * the selected one's details beside it, with what can be done about their agenda. Rebuilt when the server
 * sends the Council again, the civics that shape it change, or another seat is picked.
 */
public class CouncilContent extends GUIAnchor {

    private static final int RING_TOP = 70;
    private static final int CX = 320;
    private static final int CY = RING_TOP + 290;
    private static final int R = 220;
    private static final int NODE_W = 176;
    private static final int NODE_H = 110;
    private static final int DETAIL_X = 660;
    private static final int DETAIL_W = 380;
    private static final int WIDTH = DETAIL_X + DETAIL_W + 10;
    private static final int HEIGHT = RING_TOP + 600;
    private static final Vector4f TRACK = new Vector4f(0.04f, 0.06f, 0.08f, 1.0f);
    private static final Vector4f RING = new Vector4f(0.17f, 0.24f, 0.32f, 1.0f);
    private static final Vector4f WARN = new Vector4f(0.12f, 0.09f, 0.06f, 1.0f);

    private final GUIAnchor content;
    private int selected;
    private int builtVersion = -1;
    private int builtCivics = -1;
    private int builtSelected = -1;

    public CouncilContent(InputState state, GUIActiveInterface active) {
        super(state);
        activationInterface = active;
        content = new GUIAnchor(state, WIDTH, HEIGHT);
    }

    @Override
    public GameClientState getState() {
        return (GameClientState) super.getState();
    }

    @Override
    public void onInit() {
        super.onInit();
        GUIScrollablePanel sc = new GUIScrollablePanel(10, 10, this, getState());
        sc.setContent(content);
        sc.onInit();
        attach(sc);
    }

    @Override
    public void draw() {
        //before drawing, never while the content is being drawn
        if (Council.client().version() != builtVersion || Civics.client().version() != builtCivics || selected != builtSelected) {
            builtVersion = Council.client().version();
            builtCivics = Civics.client().version();
            builtSelected = selected;
            content.detachAll();
            build();
        }
        super.draw();
    }

    private void build() {
        Faction own = getState().getFaction();
        Council.State st = own == null ? null : Council.client().of(own.getIdFaction());
        if (st == null || st.seats.isEmpty()) {
            GUITextOverlay t = text(getState(), 14, DIM, Lng.str("The Council is gathering. Its first councilors take their seats within a minute."));
            t.setPos(12, 12, 0);
            content.attach(t);
            return;
        }
        CouncilLore.Flavour flavour = CouncilLore.of(Civics.client().of(own.getIdFaction()), st.style);
        Vector4f accent = accent(flavour);
        selected = Math.min(selected, st.seats.size() - 1);
        builtSelected = selected;

        String councilName = st.title(flavour);
        GUITextOverlay title = text(getState(), 16, TEXT, Lng.str("%s  -  %s SEATS", councilName.toUpperCase(java.util.Locale.ROOT), st.seats.size()));
        title.setPos(8, 6, 0);
        GUITextOverlay shaped = text(getState(), 14, accent, Lng.str("SHAPED BY %s", CouncilLore.shapedBy(flavour).toUpperCase(java.util.Locale.ROOT)));

        //blank goes back to the name its civics give it
        GUITextButton rename = FactionSettingsContent.button(getState(), 90, Lng.str("RENAME"), GUITextButton.ColorPalette.NEUTRAL,
                () -> editor(own), () -> askName(Lng.str("Rename %s (blank for %s)", councilName, CouncilLore.title(flavour)), st.name,
                        name -> Council.requestRename(-1, name)));
        rename.setPos(WIDTH - 100, 4, 0);
        content.attach(rename);
        //only offered when the civics allow more than the plain Council
        java.util.List<CouncilLore.Flavour> styles = CouncilLore.available(Civics.client().of(own.getIdFaction()));
        GUITextButton style = FactionSettingsContent.button(getState(), 90, Lng.str("STYLE"), GUITextButton.ColorPalette.NEUTRAL,
                () -> editor(own) && styles.size() > 1, () -> pickStyle(styles, flavour));
        style.setPos(WIDTH - 196, 4, 0);
        content.attach(style);
        GUIAnchor loreBox = new GUIAnchor(getState(), WIDTH - 20, 40);
        GUITextOverlay lore = st.suspended()
                ? text(getState(), 14, ORANGE, Lng.str("Suspended while the faction had no active member. It resumes within a minute, its deadlines moved on by the time away."))
                : text(getState(), 14, DIM, CouncilLore.lore(flavour));
        lore.autoWrapOn = loreBox;
        lore.setPos(8, 30, 0);
        content.attach(title);
        aligned(shaped, WIDTH - 208, 8, 1);
        content.attach(lore);

        //the ring itself: a dotted circle, then the seats round it
        for (int i = 0; i < 72; i++) {
            double a = i * Math.PI * 2 / 72;
            GUIColoredRectangle dot = new GUIColoredRectangle(getState(), 3, 3, new Vector4f(i % 2 == 0 ? accent : RING));
            dot.setPos((float) (CX + R * Math.cos(a)) - 1, (float) (CY + R * Math.sin(a)) - 1, 0);
            content.attach(dot);
        }
        centre(own, st, flavour);
        for (int i = 0; i < st.seats.size(); i++) {
            double a = -Math.PI / 2 + i * Math.PI * 2 / st.seats.size();
            node(st, st.seats.get(i), i, flavour, accent, (int) (CX + R * Math.cos(a)) - NODE_W / 2, (int) (CY + R * Math.sin(a)) - NODE_H / 2);
        }
        detail(own, st, st.seats.get(selected), flavour, accent);
    }

    private void centre(Faction own, Council.State st, CouncilLore.Flavour flavour) {
        int members = 0;
        for (Council.Seat s : st.seats) {
            if (s.held()) {
                members++;
            }
        }
        GUITextOverlay name = text(getState(), 14, TEXT, st.title(flavour).toUpperCase(java.util.Locale.ROOT));

        GUITextOverlay met = text(getState(), 14, DIM, Lng.str("%s agendas met", st.fulfilled));

        GUITextOverlay held = text(getState(), 14, DIM, Lng.str("%s held by members", members));

        boolean senate = Civics.client().holds(own.getIdFaction(), CivicRegistry.SENATE);
        GUITextButton add = FactionSettingsContent.button(getState(), 150, Lng.str("+ NEW SEAT"), GUITextButton.ColorPalette.NEUTRAL,
                () -> (editor(own) || senate) && st.seats.size() < Council.MAX_SEATS && !Council.openRoles(st).isEmpty(),
                () -> pickRole(own, st, flavour, senate));
        add.setPos(CX - 75, CY + 24, 0);
        aligned(name, CX, CY - 38, 0.5f);
        aligned(met, CX, CY - 18, 0.5f);
        aligned(held, CX, CY, 0.5f);
        content.attach(add);
    }

    private void node(Council.State st, Council.Seat seat, int i, CouncilLore.Flavour flavour, Vector4f accent, int x, int y) {
        boolean on = i == selected;
        IntFunction<String> names = this::factionName;
        GUIColoredRectangle bg = new GUIColoredRectangle(getState(), NODE_W, NODE_H, new Vector4f(on ? CARD_HELD : CARD));
        bg.setPos(x, y, 0);
        content.attach(bg);
        if (on) {
            GUIColoredRectangle bar = new GUIColoredRectangle(getState(), NODE_W, 2, new Vector4f(accent));
            bar.setPos(x, y, 0);
            content.attach(bar);
        }
        GUITextOverlay title = text(getState(), 14, accent, CouncilLore.seat(flavour, seat.archetype).toUpperCase(java.util.Locale.ROOT));
        title.setPos(x + 9, y + 7, 0);
        content.attach(title);
        Council.Seat rival = Council.clashWith(st, seat);
        String tag = seat.held() ? Lng.str("MEMBER") : rival != null ? Lng.str("CLASH") : "";
        if (!tag.isEmpty()) {
            GUITextOverlay t = text(getState(), 14, seat.held() ? BLUE : ORANGE, tag);
            aligned(t, x + NODE_W - 9, y + 7, 1);
        }
        GUITextOverlay name = text(getState(), 16, TEXT, seat.name);
        name.setPos(x + 9, y + 26, 0);
        content.attach(name);
        //two lines of brief, clear of the favour bar at the bottom
        GUIAnchor wrap = new GUIAnchor(getState(), NODE_W - 18, 36);
        GUITextOverlay brief = text(getState(), 14, DIM, seat.agenda != null ? Council.brief(seat.agenda, names)
                : seat.held() && !seat.options.isEmpty() ? Lng.str("Choosing an agenda") : Lng.str("Considering"));
        brief.autoWrapOn = wrap;
        brief.setPos(x + 9, y + 48, 0);
        content.attach(brief);
        if (!seat.held()) {
            GUIColoredRectangle track = new GUIColoredRectangle(getState(), NODE_W - 18, 4, new Vector4f(TRACK));
            track.setPos(x + 9, y + NODE_H - 10, 0);
            GUIColoredRectangle fill = new GUIColoredRectangle(getState(), (NODE_W - 18) * seat.favour / 100f, 4, new Vector4f(favourColour(seat.favour)));
            fill.setPos(x + 9, y + NODE_H - 10, 0);
            content.attach(track);
            content.attach(fill);
        }
        //the whole node picks the seat
        GUITextButton pick = new GUITextButton(getState(), NODE_W, NODE_H, GUITextButton.ColorPalette.TRANSPARENT, "", new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse()) {
                    selected = i;
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });
        pick.setPos(x, y, 0);
        content.attach(pick);
    }

    private void detail(Faction own, Council.State st, Council.Seat seat, CouncilLore.Flavour flavour, Vector4f accent) {
        String me = getState().getPlayer().getName();
        boolean editor = editor(own);
        boolean senate = Civics.client().holds(own.getIdFaction(), CivicRegistry.SENATE);
        IntFunction<String> names = this::factionName;
        int x = DETAIL_X;
        int y = RING_TOP;
        GUIColoredRectangle bg = new GUIColoredRectangle(getState(), DETAIL_W, 600, new Vector4f(CARD));
        bg.setPos(x, y, 0);
        content.attach(bg);
        x += 16;
        y += 14;
        int w = DETAIL_W - 32;
        y = line(text(getState(), 14, accent, CouncilLore.seat(flavour, seat.archetype).toUpperCase(java.util.Locale.ROOT)), x, y, 18);
        if (!seat.held()) {
            int at = selected;
            GUITextButton rename = FactionSettingsContent.button(getState(), 80, Lng.str("RENAME"), GUITextButton.ColorPalette.NEUTRAL,
                    () -> editor, () -> askName(Lng.str("Rename %s", seat.name), seat.name, name -> Council.requestRename(at, name)));
            rename.setPos(x + w - 80, y, 0);
            content.attach(rename);
        }
        y = line(text(getState(), 16, TEXT, seat.name), x, y, 22);
        if (seat.held()) {
            y = wrapped(DIM, Lng.str("A member holds this seat, voted in by the Senate. No favour to keep, and they choose its agenda."), x, y, w, 40);
        } else {
            y = line(text(getState(), 14, DIM, CouncilLore.traits(flavour, seat.archetype)), x, y, 24);
            GUITextOverlay label = text(getState(), 14, DIM, Lng.str("FAVOUR"));
            label.setPos(x, y, 0);
            GUIColoredRectangle track = new GUIColoredRectangle(getState(), w - 150, 8, new Vector4f(TRACK));
            track.setPos(x + 62, y + 4, 0);
            GUIColoredRectangle fill = new GUIColoredRectangle(getState(), (w - 150) * seat.favour / 100f, 8, new Vector4f(favourColour(seat.favour)));
            fill.setPos(x + 62, y + 4, 0);
            GUITextOverlay mood = text(getState(), 14, favourColour(seat.favour),
                    seat.favour >= 75 ? Lng.str("Devoted") : seat.favour >= 35 ? Lng.str("Content") : Lng.str("Restless"));
            mood.setPos(x + w - 78, y, 0);
            content.attach(label);
            content.attach(track);
            content.attach(fill);
            content.attach(mood);
            y += 28;
        }
        boolean active = Council.devoted(seat);
        y = wrapped(active ? BLUE : DIM, active ? Lng.str("Perk, active: %s", Council.perk(seat.archetype))
                : Lng.str("While devoted: %s", Council.perk(seat.archetype)), x, y, w, 36);
        GUIColoredRectangle rule = new GUIColoredRectangle(getState(), w, 1, new Vector4f(RING));
        rule.setPos(x, y, 0);
        content.attach(rule);
        y += 10;
        y = line(text(getState(), 14, DIM, Lng.str("AGENDA")), x, y, 20);

        Council.Agenda a = seat.agenda;
        if (a != null) {
            y = wrapped(accent, "\"" + CouncilLore.quote(flavour, a.objective, factionName(a.target)) + "\"", x, y, w, 40);
            y = wrapped(TEXT, Council.describe(a, names), x, y, w, 40);
            GUITextOverlay when = text(getState(), 14, DIM, new Object() {
                @Override
                public String toString() {
                    String left = DiplomacyTreatyList.roughly(Math.max(0, a.deadline - System.currentTimeMillis()));
                    return a.backedBy.isEmpty() ? Lng.str("%s left, not yet backed", left) : Lng.str("%s left, backed by %s", left, a.backedBy);
                }
            });
            y = line(when, x, y, 22);
            GUIColoredRectangle track = new GUIColoredRectangle(getState(), w - 70, 10, new Vector4f(TRACK));
            track.setPos(x, y + 3, 0);
            GUIColoredRectangle fill = new GUIColoredRectangle(getState(), 0, 10, new Vector4f(BLUE)) {
                @Override
                public void draw() {
                    setWidth((w - 70) * a.progress(System.currentTimeMillis()) / (float) Math.max(1, a.count));
                    super.draw();
                }
            };
            fill.setPos(x, y + 3, 0);
            GUITextOverlay count = text(getState(), 14, BLUE, new Object() {
                @Override
                public String toString() {
                    return a.progress(System.currentTimeMillis()) + " / " + a.count;
                }
            });
            count.setPos(x + w - 60, y, 0);
            content.attach(track);
            content.attach(fill);
            content.attach(count);
            y += 26;
            Council.Seat rival = Council.clashWith(st, seat);
            if (rival != null) {
                GUIColoredRectangle warn = new GUIColoredRectangle(getState(), w, 40, new Vector4f(WARN));
                warn.setPos(x, y, 0);
                content.attach(warn);
                wrapped(ORANGE, Lng.str("Clashes with %s: meeting one agenda drops the other, and its councilor is displeased.", rival.name), x + 8, y + 4, w - 16, 36);
                y += 46;
            }
            y = line(text(getState(), 14, TEXT, Lng.str("Meet +%s FP     Fail -%s FP     Refuse -%s FP", a.reward, a.penalty, a.refuseCost())), x, y, 28);
            GUITextButton back = FactionSettingsContent.button(getState(), 150, a.backedBy.isEmpty() ? Lng.str("BACK IT") : Lng.str("BACKED"),
                    GUITextButton.ColorPalette.FRIENDLY, a.backedBy::isEmpty, () -> Council.requestAction(0, selected, 0));
            back.setPos(x, y, 0);
            GUITextButton refuse = FactionSettingsContent.button(getState(), 150, Lng.str("REFUSE"), GUITextButton.ColorPalette.HOSTILE,
                    () -> editor, () -> confirmRefuse(seat, a));
            refuse.setPos(x + w - 150, y, 0);
            content.attach(back);
            content.attach(refuse);
            y += 34;
        } else if (seat.held() && !seat.options.isEmpty()) {
            boolean mine = seat.member.equalsIgnoreCase(me);
            y = wrapped(DIM, mine ? Lng.str("Choose your seat's agenda. Failing it costs the same as a councilor's; choose none and the seat asks nothing.")
                    : Lng.str("Waiting for %s to choose an agenda.", seat.member), x, y, w, 40);
            for (int k = 0; k < seat.options.size(); k++) {
                Council.Agenda o = seat.options.get(k);
                int option = k;
                GUITextButton b = FactionSettingsContent.button(getState(), w, Lng.str("%s  (+%s FP)", Council.describe(o, names), o.reward),
                        GUITextButton.ColorPalette.NEUTRAL, () -> mine, () -> Council.requestAction(2, selected, option));
                b.setPos(x, y, 0);
                content.attach(b);
                y += 28;
            }
        } else {
            y = wrapped(DIM, new Object() {
                @Override
                public String toString() {
                    return Lng.str("Considering what to ask next: about %s.", DiplomacyTreatyList.roughly(Math.max(0, seat.nextAt - System.currentTimeMillis())));
                }
            }, x, y, w, 24);
        }

        y += 6;
        if (seat.held() ? senate : editor || senate) {
            int at = selected;
            GUITextButton change = FactionSettingsContent.button(getState(), w,
                    seat.held() ? Lng.str("PROPOSE REMOVAL (SENATE VOTE)") : Lng.str("REPLACE THIS COUNCILOR..."),
                    GUITextButton.ColorPalette.NEUTRAL, null, () -> {
                        if (seat.held()) {
                            SenateProposeDialog.open(getState(), Lng.str("Remove %s from the Council", seat.member),
                                    why -> Council.requestPropose(2, at, seat.member, why));
                        } else {
                            pickReplacement(own, st, seat, at, flavour, editor, senate);
                        }
                    });
            change.setPos(x, y, 0);
            content.attach(change);
            y += 32;
        }
        wrapped(DIM, senate ? Lng.str("Progress counts on its own. Backing an agenda tells the faction we mean to meet it. A councilor whose favour runs out resigns.")
                : Lng.str("Progress counts on its own. A councilor whose favour runs out resigns. With a Senate, members can be voted onto the Council."), x, y, w, 60);
    }

    /** The styles the civics allow; the one in use is marked. */
    private void pickStyle(java.util.List<CouncilLore.Flavour> styles, CouncilLore.Flavour current) {
        List<GUIElement> items = new ArrayList<>();
        for (CouncilLore.Flavour f : styles) {
            items.add(row(Lng.str("%s  -  shaped by %s%s", CouncilLore.title(f), CouncilLore.shapedBy(f), f == current ? Lng.str("  (now)") : ""), f));
        }
        new PlayerGameDropDownInput("COUNCIL_STYLE", getState(), 520, 220, Lng.str("The Council's style"), 24,
                Lng.str("Who the Council is: its seats, voices and new councilors' names. Councilors keep their names; what they ask for stays the same."), items) {
            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void pressedOK(GUIListElement current) {
                deactivate();
                if (current != null && current.getContent() != null) {
                    Council.requestStyle(((CouncilLore.Flavour) current.getContent().getUserPointer()).name());
                }
            }
        }.activate();
    }

    /** Who takes an AI councilor's seat: one of today's candidates, or a member by Senate vote. */
    private void pickReplacement(Faction own, Council.State st, Council.Seat seat, int at, CouncilLore.Flavour flavour, boolean editor, boolean senate) {
        List<GUIElement> items = new ArrayList<>();
        if (editor) {
            for (int k = 0; k < st.pool.size(); k++) {
                Council.Seat c = st.pool.get(k);
                items.add(row(Lng.str("%s  -  %s, favour %s. Devoted: %s", CouncilLore.seat(flavour, c.archetype), c.name, c.favour, Council.perk(c.archetype)), k));
            }
        }
        if (senate) {
            items.add(row(Lng.str("Add a player... (Senate vote)"), -1));
        }
        if (items.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("Today's candidates have all been seated. New ones come tomorrow."), 0);
            return;
        }
        int cost = seat.agenda != null ? seat.agenda.refuseCost() : 0;
        new PlayerGameDropDownInput("COUNCIL_REPLACE", getState(), 760, 320, Lng.str("Replace %s", seat.name), 24,
                cost > 0 ? Lng.str("Candidates change each day. Dismissing %s leaves an agenda unfinished: it costs %s faction points, as if refused.", seat.name, cost)
                        : Lng.str("Candidates change each day. A candidate brings their own role, perk and favour."), items) {
            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void pressedOK(GUIListElement current) {
                deactivate();
                if (current == null || current.getContent() == null) {
                    return;
                }
                int k = (Integer) current.getContent().getUserPointer();
                if (k < 0) {
                    pickMember(own, st, 1, at);
                } else {
                    Council.requestReplace(at, k);
                }
            }
        }.activate();
    }

    private GUIAnchor row(String text, Object pointer) {
        GUITextOverlay t = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        t.setTextSimple(text);
        t.setPos(5, 5, 0);
        GUIAnchor a = new GUIAnchor(getState(), 740, 24);
        a.attach(t);
        a.setUserPointer(pointer);
        return a;
    }

    /** Places text by its width, measured as it draws: before that the font has not laid it out. {@code align} 0 left, 0.5 centre, 1 right. */
    private void aligned(GUITextOverlay t, float x, float y, float align) {
        GUIAnchor holder = new GUIAnchor(getState(), 1, 1) {
            @Override
            public void draw() {
                t.setPos(x - t.getMaxLineWidth() * align, y, 0);
                super.draw();
            }
        };
        holder.attach(t);
        content.attach(holder);
    }

    /** A new seat: which role, then a new councilor or a member by Senate vote. */
    private void pickRole(Faction own, Council.State st, CouncilLore.Flavour flavour, boolean senate) {
        List<GUIElement> items = new ArrayList<>();
        for (int role : Council.openRoles(st)) {
            items.add(row(Lng.str("%s  -  devoted: %s", CouncilLore.seat(flavour, role), Council.perk(role)), role));
        }
        new PlayerGameDropDownInput("COUNCIL_ROLE", getState(), 760, 320, Lng.str("A new seat"), 24,
                Lng.str("Choose the seat's role. Each seat past the first three costs %s faction points of upkeep an income turn.",
                        videogoose.betterfactions.BFConfig.institutionUpkeep()), items) {
            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void pressedOK(GUIListElement current) {
                deactivate();
                if (current != null && current.getContent() != null) {
                    pickFiller(own, st, flavour, (Integer) current.getContent().getUserPointer(), senate);
                }
            }
        }.activate();
    }

    private void pickFiller(Faction own, Council.State st, CouncilLore.Flavour flavour, int role, boolean senate) {
        List<GUIElement> items = new ArrayList<>();
        if (editor(own)) {
            items.add(row(Lng.str("A new councilor"), 0));
        }
        if (senate) {
            items.add(row(Lng.str("Add a player... (Senate vote)"), 1));
        }
        new PlayerGameDropDownInput("COUNCIL_FILLER", getState(), 420, 200, Lng.str("Who takes the seat of %s?", CouncilLore.seat(flavour, role)), 24,
                Lng.str("A new councilor takes it now. A player takes it once the Senate agrees."), items) {
            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void pressedOK(GUIListElement current) {
                deactivate();
                if (current == null || current.getContent() == null) {
                    return;
                }
                if ((Integer) current.getContent().getUserPointer() == 0) {
                    Council.requestAddSeat(role);
                } else {
                    pickMember(own, st, 0, role);
                }
            }
        }.activate();
    }

    /** Pick a member for a seat, then say why, then send it to the Senate. */
    private void pickMember(Faction own, Council.State st, int op, int seat) {
        List<GUIElement> items = new ArrayList<>();
        for (Map.Entry<String, FactionPermission> e : own.getMembersUID().entrySet()) {
            if (e.getValue().isActiveMember() && st.heldBy(e.getKey()) == null) {
                GUITextOverlay name = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
                name.setTextSimple(e.getKey());
                name.setPos(5, 4, 0);
                GUIAnchor a = new GUIAnchor(getState(), 300, 24);
                a.attach(name);
                a.setUserPointer(e.getKey());
                items.add(a);
            }
        }
        if (items.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("Every active member already holds a seat."), 0);
            return;
        }
        String place = op == 1 ? Lng.str("in place of %s", st.seats.get(seat).name)
                : Lng.str("to a new seat as %s", CouncilLore.seat(CouncilLore.of(Civics.client().of(own.getIdFaction()), st.style), seat));
        new PlayerGameDropDownInput("COUNCIL_MEMBER_PICK", getState(), 360, 300, Lng.str("Appoint to the Council"), 24,
                Lng.str("Who should the Senate appoint %s?", place), items) {
            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void pressedOK(GUIListElement current) {
                deactivate();
                if (current == null || current.getContent() == null) {
                    return;
                }
                String player = (String) current.getContent().getUserPointer();
                SenateProposeDialog.open(getState(), Lng.str("Appoint %s to the Council %s", player, place),
                        why -> Council.requestPropose(op, seat, player, why));
            }
        }.activate();
    }

    private void confirmRefuse(Council.Seat seat, Council.Agenda a) {
        int at = selected;
        new PlayerGameOkCancelInput("CONFIRM", getState(), Lng.str("Refuse %s's agenda?", seat.name),
                Lng.str("We pay %s faction points now.%s", a.refuseCost(), seat.held() ? "" : Lng.str(" Their favour falls."))) {
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
                Council.requestAction(1, at, 0);
            }
        }.activate();
    }

    private boolean editor(Faction own) {
        return videogoose.betterfactions.Perms.has(own, getState().getPlayer().getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION);
    }

    private void askName(String title, String initial, java.util.function.Consumer<String> onInput) {
        new PlayerTextInput("COUNCIL_NAME", getState(), videogoose.betterfactions.civics.FactionCivics.MAX_SENATE_NAME, title, "", initial) {
            @Override
            public String[] getCommandPrefixes() {
                return null;
            }

            @Override
            public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
                return null;
            }

            @Override
            public void onFailedTextCheck(String msg) {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean onInput(String entry) {
                onInput.accept(entry == null ? "" : entry.strip());
                return true;
            }
        }.activate();
    }

    private int line(GUITextOverlay t, int x, int y, int height) {
        t.setPos(x, y, 0);
        content.attach(t);
        return y + height;
    }

    private int wrapped(Vector4f colour, Object s, int x, int y, int w, int height) {
        GUIAnchor box = new GUIAnchor(getState(), w, height);
        GUITextOverlay t = text(getState(), 14, colour, s);
        t.autoWrapOn = box;
        t.setPos(x, y, 0);
        content.attach(t);
        return y + height;
    }

    private String factionName(int id) {
        Faction f = getState().getFactionManager().getFaction(id);
        return f != null ? f.getName() : "?";
    }

    private static Vector4f favourColour(int favour) {
        return favour >= 75 ? BLUE : favour >= 35 ? TEXT : ORANGE;
    }

    private static Vector4f accent(CouncilLore.Flavour f) {
        return switch (f) {
            case SHADOW -> new Vector4f(0.79f, 0.65f, 1.0f, 1.0f);
            case BOARD -> new Vector4f(0.91f, 0.82f, 0.48f, 1.0f);
            case JUNTA -> ORANGE;
            case COUNCIL -> BLUE;
        };
    }
}
