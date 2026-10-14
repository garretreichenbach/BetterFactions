package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerBlockTypeDropdownInputNew;
import org.schema.game.client.controller.PlayerGameDropDownInput;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIBlockSprite;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.meta.MetaObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import videogoose.betterfactions.civics.Contracts;
import videogoose.betterfactions.civics.Corporations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static videogoose.betterfactions.gui.InstitutionsContent.*;

/**
 * Posting a contract, in one dialog: the kind, then what it asks (a player for a bounty; a block, how many and
 * where for goods), the reward and how long it runs. Blocks come from the engine's own searchable picker.
 * Checked here for what can be, and again on the server.
 */
public final class ContractPostDialog extends PlayerGameOkCancelInput {

    private static final int[] DAYS = {1, 3, 5, 7};
    private static final int LABEL_W = 130;
    private static final int FIELD_X = LABEL_W + 10;

    private final Faction own;
    private final Corporations.State st;
    private final Corporations.Corp corp;
    private final int at;
    private Contracts.Type type = Contracts.Type.BOUNTY;
    private String target = "";
    private short item = -1;
    private Corporations.Holding destination;
    private Corporations.Holding origin;
    private int days = 3;
    private GUIActivatableTextBar count;
    private GUIActivatableTextBar reward;

    public ContractPostDialog(GameClientState state, Faction own, Corporations.State st, Corporations.Corp corp, int at) {
        super("CONTRACT_POST", state, 640, 470, Lng.str("Post a job for %s", corp.name), "");
        this.own = own;
        this.st = st;
        this.corp = corp;
        this.at = at;
        getInputPanel().setOkButtonText(Lng.str("POST"));
        getInputPanel().onInit();
        build(getInputPanel().getContent());
    }

    private boolean goods() {
        return type == Contracts.Type.HAUL || type == Contracts.Type.SUPPLY;
    }

    private void build(GUIAnchor box) {
        int y = 4;
        label(box, Lng.str("KIND"), y);
        int x = FIELD_X;
        for (Contracts.Type t : new Contracts.Type[]{Contracts.Type.BOUNTY, Contracts.Type.HAUL, Contracts.Type.SUPPLY}) {
            GUITextButton b = toggle(110, t.title().toUpperCase(java.util.Locale.ROOT), () -> type == t, () -> type = t);
            b.setPos(x, y - 2, 0);
            box.attach(b);
            x += 116;
        }
        y += 30;
        wrapped(box, new Object() {
            @Override
            public String toString() {
                return Lng.str("Taken by a %s corporation of another faction.", type.takenBy().title());
            }
        }, FIELD_X, y, 460, 20);
        y += 26;

        //a bounty: who
        int by = y;
        GUIAnchor bounty = shown(box, () -> type == Contracts.Type.BOUNTY);
        label(bounty, Lng.str("TARGET"), by);
        picker(bounty, by, () -> target.isEmpty() ? Lng.str("CHOOSE A PLAYER...") : target, this::pickTarget);

        //goods: what, how many, where
        GUIAnchor goodsBox = shown(box, this::goods);
        label(goodsBox, Lng.str("BLOCK"), y);
        GUIAnchor icon = new GUIAnchor(getState(), 32, 32) {
            private short shown = -1;
            private GUIBlockSprite sprite;

            @Override
            public void draw() {
                if (item != shown) {
                    shown = item;
                    if (sprite != null) {
                        detach(sprite);
                    }
                    sprite = null;
                    if (item > 0) {
                        sprite = new GUIBlockSprite(getState(), item);
                        sprite.getScale().set(0.5f, 0.5f, 0.5f);
                        attach(sprite);
                    }
                }
                super.draw();
            }
        };
        icon.setPos(FIELD_X, y - 6, 0);
        goodsBox.attach(icon);
        button(goodsBox, FIELD_X + 40, y - 2, 290, () -> item > 0 ? ElementKeyMap.getInfo(item).getName() : Lng.str("CHOOSE A BLOCK..."),
                this::pickBlock);
        y += 34;
        label(goodsBox, Lng.str("HOW MANY"), y);
        count = bar(goodsBox, y, 12, Lng.str("e.g. 5000 or 20k"));
        y += 34;
        label(goodsBox, Lng.str("DELIVER TO"), y);
        picker(goodsBox, y, () -> destination == null ? Lng.str("CHOOSE A STATION...") : destination.name + "  " + destination.sector, () -> pickStation(h -> destination = h));
        y += 34;
        GUIAnchor haulBox = shown(goodsBox, () -> type == Contracts.Type.HAUL);
        label(haulBox, Lng.str("PICK UP AT"), y);
        picker(haulBox, y, () -> origin == null ? Lng.str("CHOOSE A STATION...") : origin.name + "  " + origin.sector, () -> pickStation(h -> origin = h));
        y += 34;

        //the terms
        y = Math.max(y, by + 34);
        label(box, Lng.str("REWARD"), y);
        reward = bar(box, y, 16, Lng.str("credits, e.g. 5m"));
        y += 34;
        label(box, Lng.str("RUNS FOR"), y);
        x = FIELD_X;
        for (int d : DAYS) {
            GUITextButton b = toggle(70, Lng.str("%s DAYS", d), () -> days == d, () -> days = d);
            b.setPos(x, y - 2, 0);
            box.attach(b);
            x += 76;
        }
        y += 40;
        wrapped(box, new Object() {
            @Override
            public String toString() {
                return summary();
            }
        }, 4, y, 600, 80);
    }

    /** What posting it would hold, and what taking it would ask, as the fields stand. */
    private String summary() {
        long r = CorporationsContent.parseMoney(reward.getText());
        String held = r > 0 ? Lng.str("Posting holds %s credits of %s's %s.", CorporationsContent.money(r), corp.name, CorporationsContent.money(corp.treasury))
                : Lng.str("%s's treasury holds %s credits.", corp.name, CorporationsContent.money(corp.treasury));
        if (type == Contracts.Type.HAUL && item > 0) {
            long n = Math.max(0, CorporationsContent.parseMoney(count.getText()));
            held += " " + Lng.str("A hauler must hold %s credits against the goods.", CorporationsContent.money(ElementKeyMap.getInfo(item).getPrice(false) * n));
        }
        return held + " " + Lng.str("If it lapses unfinished it comes back less the posting fee; withdrawn before anyone takes it, all of it.");
    }

    // ---- pickers

    private void pickTarget() {
        List<GUIElement> items = new ArrayList<>();
        for (PlayerState p : getState().getOnlinePlayersLowerCaseMap().values()) {
            if (p.getFactionId() != own.getIdFaction()) {
                Faction theirs = getState().getFactionManager().getFaction(p.getFactionId());
                items.add(row(theirs != null ? p.getName() + "  -  " + theirs.getName() : p.getName(), p.getName()));
            }
        }
        if (items.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("Nobody of another faction is online to put a bounty on."), 0);
            return;
        }
        drop(Lng.str("Bounty on whom?"), items, p -> target = (String) p);
    }

    private void pickStation(Consumer<Corporations.Holding> onPick) {
        List<GUIElement> items = new ArrayList<>();
        for (Corporations.Holding h : st.stations) {
            //planets take no deliveries
            if (!h.uid.startsWith("CORE_")) {
                items.add(row(h.name + "  -  " + Lng.str("sector %s", h.sector), h));
            }
        }
        if (items.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("We have no stations."), 0);
            return;
        }
        drop(Lng.str("Which station?"), items, h -> onPick.accept((Corporations.Holding) h));
    }

    private void pickBlock() {
        new PlayerBlockTypeDropdownInputNew("CONTRACT_BLOCK", getState(), Lng.str("Which block?"), 0, 0, false) {
            @Override
            protected boolean includeInfo(ElementInformation info) {
                return !info.isDeprecated();
            }

            @Override
            public void onOk(ElementInformation info) {
                item = info.getId();
            }

            @Override
            public void onOkMeta(MetaObject object) {
            }

            @Override
            public void onAdditionalElementOk(Object userPointer) {
            }
        }.activate();
    }

    private void drop(String title, List<GUIElement> items, Consumer<Object> onPick) {
        new PlayerGameDropDownInput("CONTRACT_PICK", getState(), 520, 300, title, 24, "", items) {
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

    // ---- posting

    @Override
    public void pressedOK() {
        long r = CorporationsContent.parseMoney(reward.getText());
        String why = r < Contracts.MIN_REWARD ? Lng.str("A job pays at least %s credits.", CorporationsContent.money(Contracts.MIN_REWARD))
                : r > corp.treasury ? Lng.str("%s's treasury cannot cover %s credits.", corp.name, CorporationsContent.money(r)) : null;
        long n = goods() ? CorporationsContent.parseMoney(count.getText()) : 0;
        if (why == null && type == Contracts.Type.BOUNTY && target.isEmpty()) {
            why = Lng.str("Choose who the bounty is on.");
        } else if (why == null && goods()) {
            why = item <= 0 ? Lng.str("Choose a block.")
                    : n < 1 || n > Contracts.MAX_GOODS ? Lng.str("How many: between 1 and %s.", Contracts.MAX_GOODS)
                    : destination == null ? Lng.str("Choose where the goods go.")
                    : type == Contracts.Type.HAUL && (origin == null || origin.uid.equals(destination.uid)) ? Lng.str("Choose another station to pick the goods up at.")
                    : null;
        }
        if (why != null) {
            //the dialog stays open to fix it
            getState().getController().popupAlertTextMessage(why, 0);
            return;
        }
        deactivate();
        if (type == Contracts.Type.BOUNTY) {
            Contracts.requestBounty(at, target, r, days);
        } else {
            Contracts.requestGoods(at, type, item, (int) n, r, days, destination.uid, origin != null ? origin.uid : "");
        }
    }

    @Override
    public boolean isOccluded() {
        return false;
    }

    @Override
    public void onDeactivate() {
    }

    // ---- layout

    /** An anchor over the whole box that draws, and so takes clicks, only while {@code when} holds. */
    private GUIAnchor shown(GUIAnchor parent, BooleanSupplier when) {
        GUIAnchor a = new GUIAnchor(getState(), 1, 1) {
            @Override
            public void draw() {
                if (when.getAsBoolean()) {
                    super.draw();
                }
            }
        };
        parent.attach(a);
        return a;
    }

    private void label(GUIAnchor parent, String s, int y) {
        GUITextOverlay t = text(getState(), 14, DIM, s);
        t.setPos(4, y + 4, 0);
        parent.attach(t);
    }

    private GUIActivatableTextBar bar(GUIAnchor parent, int y, int max, String hint) {
        //a fixed-size box: the bar sizes itself to what it depends on, and would fill the dialog otherwise
        GUIAnchor holder = new GUIAnchor(getState(), 200, 28);
        holder.setPos(FIELD_X, y - 2, 0);
        GUIActivatableTextBar b = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, max, 1, hint, holder, DiplomacyProposeDialog.TEXT, t -> t);
        b.onInit();
        holder.attach(b);
        parent.attach(holder);
        return b;
    }

    private void picker(GUIAnchor parent, int y, java.util.function.Supplier<String> text, Runnable onPress) {
        button(parent, FIELD_X, y - 2, 330, text, onPress);
    }

    private GUITextButton button(GUIAnchor parent, int x, int y, int w, java.util.function.Supplier<String> text, Runnable onPress) {
        GUITextButton b = new GUITextButton(getState(), w, 24, GUITextButton.ColorPalette.NEUTRAL, new Object() {
            @Override
            public String toString() {
                return text.get();
            }
        }, new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if (event.pressedLeftMouse()) {
                    onPress.run();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });
        b.setPos(x, y, 0);
        parent.attach(b);
        return b;
    }

    private GUIAnchor wrapped(GUIAnchor parent, Object s, int x, int y, int w, int h) {
        GUIAnchor wrap = new GUIAnchor(getState(), w, h);
        GUITextOverlay t = text(getState(), 14, DIM, s);
        t.autoWrapOn = wrap;
        t.setPos(x, y, 0);
        parent.attach(t);
        return wrap;
    }

    private GUIAnchor row(String text, Object pointer) {
        GUITextOverlay t = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), getState());
        t.setTextSimple(text);
        t.setPos(5, 5, 0);
        GUIAnchor a = new GUIAnchor(getState(), 500, 24);
        a.attach(t);
        a.setUserPointer(pointer);
        return a;
    }

    /** A button that shows whether its option is the one chosen. */
    private GUITextButton toggle(int width, Object label, BooleanSupplier on, Runnable pick) {
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
}
