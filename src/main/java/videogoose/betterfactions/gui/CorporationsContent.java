package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerGameDropDownInput;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.controller.PlayerTextInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.Corporations;

import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static videogoose.betterfactions.gui.InstitutionsContent.*;

/**
 * Governance › Corporations: the faction's corporations as cards on the left, the selected one's books,
 * HQ and stations on the right. Rebuilt when the server sends them again or another card is picked.
 */
public class CorporationsContent extends GUIAnchor {

    private static final int LIST_W = 300;
    private static final int CARD_H = 74;
    private static final int DETAIL_X = LIST_W + 20;
    private static final int DETAIL_W = 640;
    private static final int WIDTH = DETAIL_X + DETAIL_W + 10;
    private static final int HEIGHT = 660;
    private static final Vector4f RULE = new Vector4f(0.17f, 0.24f, 0.32f, 1.0f);
    private static final Vector4f GOLD = new Vector4f(0.91f, 0.82f, 0.48f, 1.0f);

    private final GUIAnchor content;
    private int selected;
    private int builtVersion = -1;
    private int builtCivics = -1;
    private int builtSelected = -1;

    public CorporationsContent(InputState state, GUIActiveInterface active) {
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
        if (Corporations.client().version() != builtVersion || Civics.client().version() != builtCivics || selected != builtSelected) {
            builtVersion = Corporations.client().version();
            builtCivics = Civics.client().version();
            builtSelected = selected;
            content.detachAll();
            build();
        }
        super.draw();
    }

    private void build() {
        Faction own = getState().getFaction();
        if (own == null) {
            return;
        }
        Corporations.State st = Corporations.client().of(own.getIdFaction());
        List<Corporations.Corp> corps = st == null ? List.of() : st.corps;
        boolean editor = editor(own);
        boolean chains = Civics.client().of(own.getIdFaction()).civics.contains(CivicRegistry.CORPORATE_CHAINS);

        int y = 6;
        y = line(text(getState(), 16, TEXT, Lng.str("CORPORATIONS  -  %s CHARTERED", corps.size())), 8, y, 24);
        if (chains) {
            y = wrapped(GOLD, Lng.str("Corporate Chains: one more free charter, dividends +15%."), 8, y, LIST_W - 8, 22);
        }
        selected = Math.max(0, Math.min(selected, corps.size() - 1));
        builtSelected = selected;
        for (int i = 0; i < corps.size(); i++) {
            card(corps.get(i), i, 4, y);
            y += CARD_H + 6;
        }
        GUITextButton charter = FactionSettingsContent.button(getState(), LIST_W - 4, Lng.str("+ CHARTER A CORPORATION"),
                GUITextButton.ColorPalette.NEUTRAL, () -> editor && corps.size() < Corporations.MAX_CORPS, this::pickType);
        charter.setPos(4, y, 0);
        content.attach(charter);
        y += 34;
        int free = Corporations.freeCharters(chains);
        wrapped(DIM, Lng.str("%s free. Each further charter adds %s FP of institution upkeep an income turn. Chartering needs the Edit ranks permission.",
                free == 1 ? Lng.str("One charter is") : Lng.str("%s charters are", free), BFConfig.institutionUpkeep()), 8, y, LIST_W - 8, 60);

        if (corps.isEmpty()) {
            wrapped(DIM, Lng.str("No corporations yet. Charter one, then give it stations with shops: it takes %s%% of every sale there into its treasury, and pays part of each income turn's takings to the faction as faction points.",
                    BFConfig.corporationCutPercent()), DETAIL_X + 16, 40, DETAIL_W - 32, 80);
            return;
        }
        detail(own, st, corps.get(selected), editor);
    }

    private void card(Corporations.Corp c, int i, int x, int y) {
        boolean on = i == selected;
        GUIColoredRectangle bg = new GUIColoredRectangle(getState(), LIST_W - 4, CARD_H, new Vector4f(on ? CARD_HELD : CARD));
        bg.setPos(x, y, 0);
        content.attach(bg);
        if (on) {
            GUIColoredRectangle bar = new GUIColoredRectangle(getState(), LIST_W - 4, 2, new Vector4f(BLUE));
            bar.setPos(x, y, 0);
            content.attach(bar);
        }
        line(text(getState(), 14, colour(c.type), c.type.title().toUpperCase(java.util.Locale.ROOT)), x + 10, y + 7, 0);
        line(text(getState(), 16, TEXT, c.name), x + 10, y + 25, 0);
        line(text(getState(), 14, DIM, Lng.str("CEO %s", c.ceoName())), x + 10, y + 49, 0);
        GUITextOverlay fp = text(getState(), 14, BLUE, Lng.str("+%s FP last turn", c.lastDividend));
        aligned(fp, x + LIST_W - 14, y + 49, 1);
        GUITextButton pick = new GUITextButton(getState(), LIST_W - 4, CARD_H, GUITextButton.ColorPalette.TRANSPARENT, "", new GUICallback() {
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

    private void detail(Faction own, Corporations.State st, Corporations.Corp c, boolean editor) {
        int at = selected;
        boolean runs = editor || c.ceo.equalsIgnoreCase(getState().getPlayer().getName());
        boolean senate = Civics.client().holds(own.getIdFaction(), CivicRegistry.SENATE);
        int x = DETAIL_X;
        int y = 4;
        int w = DETAIL_W;
        GUIColoredRectangle bg = new GUIColoredRectangle(getState(), w, 266, new Vector4f(CARD));
        bg.setPos(x, y, 0);
        content.attach(bg);
        x += 16;
        y += 12;
        w -= 32;
        line(text(getState(), 14, colour(c.type), c.type.title().toUpperCase(java.util.Locale.ROOT)), x, y, 0);
        GUITextButton rename = FactionSettingsContent.button(getState(), 80, Lng.str("RENAME"), GUITextButton.ColorPalette.NEUTRAL,
                () -> runs, () -> askName(Lng.str("Rename %s", c.name), c.name, Corporations.MAX_NAME, name -> Corporations.request(2, at, 0, name)));
        rename.setPos(x + w - 80, y, 0);
        content.attach(rename);
        //blank goes back to the type's own words
        GUITextButton describe = FactionSettingsContent.button(getState(), 110, Lng.str("DESCRIPTION"), GUITextButton.ColorPalette.NEUTRAL,
                () -> runs, () -> askName(Lng.str("Describe %s (blank for the default)", c.name), c.description, Corporations.MAX_DESCRIPTION,
                        d -> Corporations.request(8, at, 0, d)));
        describe.setPos(x + w - 196, y, 0);
        content.attach(describe);
        y += 18;
        y = line(text(getState(), 16, TEXT, c.name), x, y, 22);
        y = wrapped(DIM, c.describe(), x, y, w, 52);

        //who runs it
        GUITextOverlay ceo = text(getState(), 14, TEXT, c.memberCeo() ? Lng.str("CEO %s  (member)", c.ceo) : Lng.str("CEO %s  (NPC)", c.npcCeo));
        ceo.setPos(x, y, 0);
        content.attach(ceo);
        GUITextButton appoint = FactionSettingsContent.button(getState(), 190,
                c.memberCeo() ? Lng.str("BACK TO AN NPC CEO") : senate ? Lng.str("APPOINT A MEMBER (VOTE)") : Lng.str("APPOINT A MEMBER"),
                GUITextButton.ColorPalette.NEUTRAL, () -> c.memberCeo() ? editor : senate || editor,
                () -> {
                    if (c.memberCeo()) {
                        Corporations.request(7, at, 0, "");
                    } else {
                        pickCeo(own, st, at, c, senate);
                    }
                });
        appoint.setPos(x + w - 190, y - 4, 0);
        content.attach(appoint);
        //a member CEO carries their own name; the NPC who stands in keeps theirs while a member runs it
        GUITextButton renameCeo = FactionSettingsContent.button(getState(), 110, Lng.str("RENAME CEO"), GUITextButton.ColorPalette.NEUTRAL,
                () -> runs, () -> askName(Lng.str("Rename %s", c.npcCeo), c.npcCeo, Corporations.MAX_NAME, name -> Corporations.request(9, at, 0, name)));
        renameCeo.setPos(x + w - 306, y - 4, 0);
        if (!c.memberCeo()) {
            content.attach(renameCeo);
        }
        y += 30;

        //the books
        int cell = (w - 24) / 4;
        stat(x, y, cell, Lng.str("TREASURY"), money(c.treasury) + " cr", TEXT, 0);
        stat(x, y, cell, Lng.str("REVENUE, LAST TURN"), money(c.lastRevenue) + " cr", TEXT, 1);
        stat(x, y, cell, Lng.str("THIS TURN SO FAR"), money(c.revenue) + " cr", TEXT, 2);
        stat(x, y, cell, Lng.str("DIVIDEND, LAST TURN"), "+" + c.lastDividend + " FP", BLUE, 3);
        y += 44;
        GUITextOverlay rate = text(getState(), 14, TEXT, Lng.str("Dividend rate: %s%% of each turn's revenue", c.rate));
        rate.setPos(x, y + 6, 0);
        content.attach(rate);
        GUITextButton down = FactionSettingsContent.button(getState(), 40, "-10", GUITextButton.ColorPalette.NEUTRAL,
                () -> runs && c.rate > 0, () -> Corporations.request(3, at, c.rate - 10, ""));
        down.setPos(x + w - 90, y + 2, 0);
        GUITextButton up = FactionSettingsContent.button(getState(), 40, "+10", GUITextButton.ColorPalette.NEUTRAL,
                () -> runs && c.rate < 100, () -> Corporations.request(3, at, c.rate + 10, ""));
        up.setPos(x + w - 40, y + 2, 0);
        content.attach(down);
        content.attach(up);
        y += 30;
        wrapped(DIM, Lng.str("Paid from the treasury at %s credits a faction point, at most %s FP a turn. The rest stays in the treasury.",
                money(BFConfig.corporationCreditsPerPoint()), BFConfig.corporationDividendCap() < 0 ? "-" : BFConfig.corporationDividendCap()), x, y, w, 36);

        //HQ
        x = DETAIL_X;
        y = 282;
        w = DETAIL_W;
        GUIColoredRectangle hqBg = new GUIColoredRectangle(getState(), w, 92, new Vector4f(CARD));
        hqBg.setPos(x, y, 0);
        content.attach(hqBg);
        x += 16;
        w -= 32;
        y += 10;
        line(text(getState(), 14, c.hq != null ? colour(c.type) : DIM, Lng.str("CORPORATE HQ")), x, y, 0);
        GUITextButton seat = FactionSettingsContent.button(getState(), 120, c.hq != null ? Lng.str("MOVE HQ...") : Lng.str("SET HQ..."),
                GUITextButton.ColorPalette.NEUTRAL, () -> runs && !st.stations.isEmpty(), () -> pickHq(st, at, c));
        seat.setPos(x + w - 120, y - 2, 0);
        content.attach(seat);
        y += 20;
        if (c.hq != null) {
            y = line(text(getState(), 14, TEXT, new Object() {
                @Override
                public String toString() {
                    long left = Corporations.HQ_SETTLE_MS - (System.currentTimeMillis() - c.hqSince);
                    return left > 0 ? Lng.str("%s  -  sector %s  -  settling in, %s left", c.hq.name, c.hq.sector, DiplomacyTreatyList.roughly(left))
                            : Lng.str("%s  -  sector %s", c.hq.name, c.hq.sector);
                }
            }), x, y, 20);
        } else {
            y = line(text(getState(), 14, DIM, Lng.str("No HQ. Seat it at one of our stations or planets; one corporate HQ fits in a sector.")), x, y, 20);
        }
        wrapped(DIM, Lng.str("Stations it runs within 3 systems of its HQ earn 10% more. %s", c.type.hqBonus()), x, y, w, 36);

        //stations
        x = DETAIL_X;
        y = 388;
        w = DETAIL_W;
        line(text(getState(), 14, DIM, Lng.str("STATIONS AND PLANETS IT RUNS  (%s of %s with franchises)", c.holdingCount(), Corporations.MAX_HOLDINGS)), x + 4, y, 0);
        GUITextButton assign = FactionSettingsContent.button(getState(), 200, Lng.str("ASSIGN A STATION OR PLANET..."),
                GUITextButton.ColorPalette.NEUTRAL, () -> runs && c.holdingCount() < Corporations.MAX_HOLDINGS, () -> pickStation(st, at));
        assign.setPos(x + w - 200, y - 4, 0);
        content.attach(assign);
        y += 26;
        if (c.holdings.isEmpty()) {
            y = wrapped(DIM, Lng.str("None yet. A station or planet earns once it has a shop players buy from: %s%% of each sale goes to the treasury.",
                    BFConfig.corporationCutPercent()), x + 4, y, w - 8, 40);
        }
        for (Corporations.Holding h : c.holdings) {
            GUIColoredRectangle row = new GUIColoredRectangle(getState(), w, 30, new Vector4f(CARD));
            row.setPos(x, y, 0);
            content.attach(row);
            boolean near = c.hq != null && Corporations.near(h.sector, c.hq.sector);
            line(text(getState(), 14, TEXT, h.name), x + 10, y + 7, 0);
            line(text(getState(), 14, near ? BLUE : DIM, near ? Lng.str("sector %s  -  near the HQ", h.sector) : Lng.str("sector %s", h.sector)), x + 260, y + 7, 0);
            GUITextButton release = FactionSettingsContent.button(getState(), 90, Lng.str("RELEASE"), GUITextButton.ColorPalette.HOSTILE,
                    () -> runs, () -> Corporations.request(5, at, 0, h.uid));
            release.setPos(x + w - 96, y + 3, 0);
            content.attach(release);
            y += 34;
        }
        //franchises on Trading Guild shops
        y += 14;
        line(text(getState(), 14, DIM, Lng.str("FRANCHISES ON TRADING GUILD SHOPS  (%s)", c.franchises.size())), x + 4, y, 0);
        GUITextButton buy = FactionSettingsContent.button(getState(), 200, Lng.str("FRANCHISE A GUILD SHOP..."),
                GUITextButton.ColorPalette.NEUTRAL, () -> runs && c.holdingCount() < Corporations.MAX_HOLDINGS, () -> pickFranchise(st, at, c));
        buy.setPos(x + w - 200, y - 4, 0);
        content.attach(buy);
        y += 26;
        if (c.franchises.isEmpty()) {
            y = wrapped(DIM, Lng.str("None yet. A franchise takes %s%% of every sale at a Guild shop, less the Guild's %s%% royalty. The shop stays the Guild's; build your own station to keep every credit.",
                    BFConfig.franchiseCutPercent(), BFConfig.franchiseRoyaltyPercent()), x + 4, y, w - 8, 40);
        }
        for (Corporations.Holding h : c.franchises) {
            GUIColoredRectangle row = new GUIColoredRectangle(getState(), w, 30, new Vector4f(CARD));
            row.setPos(x, y, 0);
            content.attach(row);
            boolean near = c.hq != null && Corporations.near(h.sector, c.hq.sector);
            line(text(getState(), 14, GOLD, h.name), x + 10, y + 7, 0);
            line(text(getState(), 14, near ? BLUE : DIM, near ? Lng.str("sector %s  -  near the HQ", h.sector) : Lng.str("sector %s", h.sector)), x + 260, y + 7, 0);
            GUITextButton drop = FactionSettingsContent.button(getState(), 90, Lng.str("GIVE UP"), GUITextButton.ColorPalette.HOSTILE,
                    () -> runs, () -> confirmGiveUp(c, h, at));
            drop.setPos(x + w - 96, y + 3, 0);
            content.attach(drop);
            y += 34;
        }
        y += 10;
        GUITextButton dissolve = FactionSettingsContent.button(getState(), 160, Lng.str("DISSOLVE"), GUITextButton.ColorPalette.HOSTILE,
                () -> editor, () -> confirmDissolve(c, at));
        dissolve.setPos(x + w - 160, y, 0);
        content.attach(dissolve);
        content.setHeight(Math.max(HEIGHT, y + 40));
    }

    /** One of four figures across the books, in column {@code col}. */
    private void stat(int x, int y, int cell, String label, String value, Vector4f colour, int col) {
        int cx = x + col * (cell + 8);
        GUIColoredRectangle bg = new GUIColoredRectangle(getState(), cell, 40, new Vector4f(CARD_HELD));
        bg.setPos(cx, y, 0);
        content.attach(bg);
        line(text(getState(), 14, DIM, label), cx + 8, y + 4, 0);
        line(text(getState(), 16, colour, value), cx + 8, y + 20, 0);
    }

    private void pickType() {
        List<GUIElement> items = new ArrayList<>();
        for (Corporations.Type t : Corporations.Type.values()) {
            items.add(row(Lng.str("%s  -  %s", t.title(), t.description()), t));
        }
        new PlayerGameDropDownInput("CORP_TYPE", getState(), 860, 300, Lng.str("Charter a corporation"), 24,
                Lng.str("Its line of business is fixed at charter. An NPC CEO runs it until a member is appointed."), items) {
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
                    Corporations.request(0, -1, ((Corporations.Type) current.getContent().getUserPointer()).ordinal(), "");
                }
            }
        }.activate();
    }

    /** The faction's stations no corporation runs yet. */
    private void pickStation(Corporations.State st, int at) {
        List<GUIElement> items = new ArrayList<>();
        for (Corporations.Holding h : st.stations) {
            if (st.runs(h.uid) == null) {
                items.add(row(Lng.str("%s  -  sector %s", h.name, h.sector), h.uid));
            }
        }
        if (items.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("Every station and planet of ours is already run by a corporation."), 0);
            return;
        }
        drop("CORP_STATION", Lng.str("Assign a station or planet"), Lng.str("It takes %s%% of every sale at its shops.", BFConfig.corporationCutPercent()),
                items, uid -> Corporations.request(4, at, 0, (String) uid));
    }

    /** Any of the faction's stations; sectors holding another corporation's HQ are refused by the server, which knows every faction's. */
    private void pickHq(Corporations.State st, int at, Corporations.Corp c) {
        List<GUIElement> items = new ArrayList<>();
        for (Corporations.Holding h : st.stations) {
            boolean here = c.hq != null && c.hq.uid.equals(h.uid);
            Corporations.Corp ours = null;
            for (Corporations.Corp o : st.corps) {
                if (o != c && o.hq != null && o.hq.sector.equals(h.sector)) {
                    ours = o;
                }
            }
            Corporations.Corp runner = st.runs(h.uid);
            String note = here ? Lng.str("  (current HQ)") : ours != null ? Lng.str("  (sector holds %s's HQ)", ours.name)
                    : runner != null && runner != c ? Lng.str("  (run by %s)", runner.name) : "";
            items.add(row(Lng.str("%s  -  sector %s%s", h.name, h.sector, note), h.uid));
        }
        drop("CORP_HQ", Lng.str("Seat the HQ of %s", c.name),
                Lng.str("The HQ is one of the stations it runs, taken on if it is not yet. One corporate HQ fits in a sector, any faction's. A new HQ gives nothing for its first 2 days."),
                items, uid -> Corporations.request(6, at, 0, (String) uid));
    }

    private void pickCeo(Faction own, Corporations.State st, int at, Corporations.Corp c, boolean senate) {
        List<GUIElement> items = new ArrayList<>();
        for (Map.Entry<String, FactionPermission> e : own.getMembersUID().entrySet()) {
            boolean runsOne = st.corps.stream().anyMatch(o -> o.ceo.equalsIgnoreCase(e.getKey()));
            if (e.getValue().isActiveMember() && !runsOne) {
                items.add(row(e.getKey(), e.getKey()));
            }
        }
        if (items.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("Every active member already runs a corporation."), 0);
            return;
        }
        drop("CORP_CEO", Lng.str("CEO of %s", c.name),
                senate ? Lng.str("Who should the Senate appoint?") : Lng.str("Who runs it? They set its dividend, stations and HQ."), items, player -> {
                    if (senate) {
                        SenateProposeDialog.open(getState(), Lng.str("Appoint %s CEO of %s", player, c.name),
                                why -> Corporations.requestAppoint(at, (String) player, why));
                    } else {
                        Corporations.request(7, at, 0, (String) player);
                    }
                });
    }

    /** The Guild shops near us nobody holds, with what each would cost this corporation. */
    private void pickFranchise(Corporations.State st, int at, Corporations.Corp c) {
        long now = System.currentTimeMillis();
        Faction own = getState().getFaction();
        List<GUIElement> items = new ArrayList<>();
        for (Corporations.Offer o : st.market) {
            long fee = Corporations.feeFor(c, o, now);
            String from = own == null ? "" : Lng.str(", %s systems away", Corporations.systems(own.getHomeSector(), o.shop.sector));
            items.add(row(Lng.str("%s  -  sector %s%s  -  sales lately %s cr  -  fee %s cr%s", o.shop.name, o.shop.sector, from,
                    money(o.traffic), money(fee), fee > c.treasury ? Lng.str("  (treasury short)") : ""), o));
        }
        if (items.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("No Trading Guild shop near us is free to franchise."), 0);
            return;
        }
        drop("CORP_FRANCHISE", Lng.str("Franchise a Guild shop for %s", c.name),
                Lng.str("Treasury: %s credits. The nearest Guild shops nobody holds; busier shops cost more. Paid from the treasury.", money(c.treasury)),
                items, o -> confirmFranchise(c, (Corporations.Offer) o, at));
    }

    private void confirmFranchise(Corporations.Corp c, Corporations.Offer o, int at) {
        long fee = Corporations.feeFor(c, o, System.currentTimeMillis());
        new PlayerGameOkCancelInput("CONFIRM", getState(), Lng.str("Franchise %s?", o.shop.name),
                Lng.str("%s pays %s credits from its treasury. It takes %s%% of every sale there, less the Guild's %s%% royalty. If the shop is destroyed, the fee is lost.",
                        c.name, money(fee), BFConfig.franchiseCutPercent(), BFConfig.franchiseRoyaltyPercent())) {
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
                Corporations.request(10, at, 0, o.shop.uid);
            }
        }.activate();
    }

    private void confirmGiveUp(Corporations.Corp c, Corporations.Holding h, int at) {
        new PlayerGameOkCancelInput("CONFIRM", getState(), Lng.str("Give up the franchise on %s?", h.name),
                Lng.str("%s stops earning there, and the fee is not returned.", c.name)) {
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
                Corporations.request(11, at, 0, h.uid);
            }
        }.activate();
    }

    private void confirmDissolve(Corporations.Corp c, int at) {
        new PlayerGameOkCancelInput("CONFIRM", getState(), Lng.str("Dissolve %s?", c.name),
                Lng.str("Its treasury of %s credits is lost, and its stations are the faction's own again.", money(c.treasury))) {
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
                Corporations.request(1, at, 0, "");
            }
        }.activate();
    }

    private void drop(String id, String title, String description, List<GUIElement> items, Consumer<Object> onPick) {
        new PlayerGameDropDownInput(id, getState(), 620, 300, title, 24, description, items) {
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
                    onPick.accept(current.getContent().getUserPointer());
                }
            }
        }.activate();
    }

    private GUIAnchor row(String text, Object pointer) {
        GUITextOverlay t = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        t.setTextSimple(text);
        t.setPos(5, 5, 0);
        GUIAnchor a = new GUIAnchor(getState(), 840, 24);
        a.attach(t);
        a.setUserPointer(pointer);
        return a;
    }

    private void askName(String title, String initial, int max, Consumer<String> onInput) {
        new PlayerTextInput("CORP_NAME", getState(), max, title, "", initial) {
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

    private boolean editor(Faction own) {
        return videogoose.betterfactions.Perms.has(own, getState().getPlayer().getName(), FactionPermission.PermType.FACTION_EDIT_PERMISSION);
    }

    /** Places text by its width, measured as it draws. {@code align} 0 left, 1 right. */
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

    /** Credits as StarMade's sums run: millions and billions shortened. */
    static String money(long v) {
        return v >= 1_000_000_000L ? String.format(java.util.Locale.ROOT, "%.2fB", v / 1e9)
                : v >= 1_000_000L ? String.format(java.util.Locale.ROOT, "%.1fM", v / 1e6)
                : String.format(java.util.Locale.ROOT, "%,d", v);
    }

    private static Vector4f colour(Corporations.Type t) {
        return switch (t) {
            case LOGISTICS -> BLUE;
            case CONSUMER_GOODS -> new Vector4f(0.56f, 0.84f, 0.66f, 1.0f);
            case RESOURCES -> GOLD;
            case SHIPYARDS -> ORANGE;
            case SCRAPPERS -> new Vector4f(0.73f, 0.78f, 0.84f, 1.0f);
            case MERCENARIES -> new Vector4f(0.79f, 0.65f, 1.0f, 1.0f);
        };
    }
}
