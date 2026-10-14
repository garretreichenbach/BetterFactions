package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerGameDropDownInput;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.CivicType;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.FactionCivics;

import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.List;

import static videogoose.betterfactions.gui.InstitutionsContent.*;

/**
 * Picks the civic for a slot: the slots across the top, every civic not held as a card, and the
 * selected one's details with the button that takes it. Civics that cannot go in the selected slot
 * are dimmed but still selectable, so the detail can say why. The server checks everything again.
 *
 * <p>Fleet doctrines are not picked here; there are three, so Faction Settings shows them inline.</p>
 */
public final class CivicChooser {

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 680;
    private static final int DETAIL_WIDTH = 316;
    private static final int CARD_HEIGHT = 50;
    private static final int GAP = 8;
    private static final int COLUMNS = 3;
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;

    private CivicChooser() {
    }

    /** Kept for the old chained prompt; the picker stays open across picks now, so nothing to continue. */
    public static void civicsReceived(GameClientState state) {
    }

    /**
     * @param fill a new faction filling its slots: after a pick into an empty slot the next empty one
     *             is selected instead of closing
     */
    public static void open(GameClientState state, int slot, boolean fill) {
        Faction own = state.getFaction();
        if (own == null || slot < 0) {
            return;
        }
        new Picker(state, own.getIdFaction(), slot, fill).activate();
    }

    private static final class Picker extends PlayerGameOkCancelInput {

        private final int faction;
        private final boolean fill;
        private int slot;
        private String selected;
        private boolean availableOnly;
        private short resource;
        /** What the grid and strip were last built for; rebuilt when any of it changes. */
        private String built = "";
        private final GUIAnchor body;

        Picker(GameClientState state, int faction, int slot, boolean fill) {
            super("CIVIC_PICKER", state, WIDTH, HEIGHT, Lng.str("Choose a civic"), "");
            this.faction = faction;
            this.fill = fill;
            this.slot = slot;
            getInputPanel().setOkButton(false);
            getInputPanel().setCancelButtonText(Lng.str("CLOSE"));
            getInputPanel().onInit();
            body = new GUIAnchor(getState()) {
                @Override
                public void draw() {
                    setWidth(getInputPanel().getContent().getWidth());
                    setHeight(getInputPanel().getContent().getHeight());
                    rebuildIfChanged();
                    super.draw();
                }
            };
            getInputPanel().getContent().attach(body);
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
        }

        private FactionCivics held() {
            return Civics.client().of(faction);
        }

        /** Why {@code t} cannot go in the selected slot, unlock rules included; null when it can. */
        private String blocked(CivicType t) {
            String why = held().conflict(slot, t.getId());
            if (why != null) {
                return why;
            }
            Faction f = getState().getFactionManager().getFaction(faction);
            String locked = f == null ? null : t.locked(Civics.standing(f));
            return locked != null ? Lng.str("Locked. %s", locked) : null;
        }

        /** Every civic the slot could take or explain, held ones left out: there is nothing to say about them. */
        private List<CivicType> candidates() {
            List<CivicType> out = new ArrayList<>();
            for (CivicType t : CivicRegistry.all()) {
                if (t.getKind() == CivicType.Kind.CIVIC && !held().civics.contains(t.getId()) && !BFConfig.civicDisabled(t.getId())
                        && (!availableOnly || blocked(t) == null)) {
                    out.add(t);
                }
            }
            return out;
        }

        private void rebuildIfChanged() {
            String key = slot + "|" + selected + "|" + availableOnly + "|" + resource + "|" + Civics.client().version()
                    + "|" + (int) body.getWidth() + "x" + (int) body.getHeight();
            if (key.equals(built) || body.getWidth() < 200) {
                return;
            }
            built = key;
            body.detachAll();
            FactionCivics c = held();
            int w = (int) body.getWidth();
            int h = (int) body.getHeight();
            int left = w - DETAIL_WIDTH - 14;

            //the slots: what each holds, the selected one marked
            int n = c.slotsEarned;
            int sw = (w - GAP * (n - 1)) / Math.max(1, n);
            for (int i = 0; i < n; i++) {
                int s = i;
                String name = i < c.civics.size() ? CivicRegistry.nameOf(c.civics.get(i)) : Lng.str("empty");
                GUITextButton b = button(sw, i == slot ? Lng.str("SLOT %s - REPLACING: %s", i + 1, name) : Lng.str("SLOT %s: %s", i + 1, name),
                        i == slot ? GUITextButton.ColorPalette.OK : GUITextButton.ColorPalette.NEUTRAL,
                        //slots fill in order, so an empty slot past the next one is not offered
                        () -> {
                            if (s <= held().civics.size()) {
                                slot = s;
                            }
                        });
                b.setPos(i * (sw + GAP), 0, 0);
                body.attach(b);
            }

            List<CivicType> shown = candidates();
            if (selected == null || shown.stream().noneMatch(t -> t.getId().equals(selected))) {
                selected = shown.isEmpty() ? null : shown.get(0).getId();
            }

            GUITextButton all = button(90, Lng.str("ALL"), availableOnly ? GUITextButton.ColorPalette.TRANSPARENT : GUITextButton.ColorPalette.OK, () -> availableOnly = false);
            GUITextButton ok = button(110, Lng.str("AVAILABLE"), availableOnly ? GUITextButton.ColorPalette.OK : GUITextButton.ColorPalette.TRANSPARENT, () -> availableOnly = true);
            all.setPos(0, 36, 0);
            ok.setPos(98, 36, 0);
            body.attach(all);
            body.attach(ok);
            GUITextOverlay count = text(getState(), 14, DIM, Lng.str("%s shown", shown.size()));
            count.setPos(220, 40, 0);
            body.attach(count);

            //ponytail: no scrolling; 3 columns x 8 rows fit, which is more than the 16 there are. Scroll when mods add enough to overflow
            int cw = (left - GAP * (COLUMNS - 1)) / COLUMNS;
            for (int i = 0; i < shown.size(); i++) {
                GUIAnchor card = card(shown.get(i), cw);
                card.setPos((i % COLUMNS) * (cw + GAP), 68 + (i / COLUMNS) * (CARD_HEIGHT + GAP), 0);
                body.attach(card);
            }

            GUIAnchor detail = detail(h - 36);
            detail.setPos(left + 14, 36, 0);
            body.attach(detail);
        }

        private GUIAnchor card(CivicType t, int width) {
            String why = blocked(t);
            boolean on = t.getId().equals(selected);
            GUIAnchor a = new GUIAnchor(getState(), width, CARD_HEIGHT);
            a.attach(new GUIColoredRectangle(getState(), width, CARD_HEIGHT, new Vector4f(on ? CARD_HELD : CARD)));
            GUITextOverlay name = text(getState(), 16, why != null ? DIM : TEXT, t.getDisplayName());
            name.setPos(10, 7, 0);
            GUITextOverlay badge = text(getState(), 14, why != null ? ORANGE : BLUE, badge(t, why));
            badge.setPos(10, 28, 0);
            //an empty transparent button over the card is what makes all of it clickable
            GUITextButton hit = button(width, "", GUITextButton.ColorPalette.TRANSPARENT, () -> selected = t.getId());
            hit.setHeight(CARD_HEIGHT);
            a.attach(name);
            a.attach(badge);
            a.attach(hit);
            return a;
        }

        /** The card's one-line status: why it cannot be taken, shortest first. */
        private String badge(CivicType t, String why) {
            if (why == null) {
                return t.choosesResource() ? Lng.str("PICK A RESOURCE") : Lng.str("AVAILABLE");
            }
            FactionCivics c = held();
            for (int i = 0; i < c.civics.size(); i++) {
                CivicType other = CivicRegistry.get(c.civics.get(i));
                if (i != slot && other != null && t.conflictsWith(other)) {
                    return Lng.str("CONFLICTS WITH %s", other.getDisplayName().toUpperCase());
                }
            }
            for (String need : t.getRequires()) {
                int at = c.civics.indexOf(need);
                if (at < 0 || at == slot) {
                    return Lng.str("NEEDS %s", CivicRegistry.nameOf(need).toUpperCase());
                }
            }
            return c.conflict(slot, t.getId()) == null ? Lng.str("LOCKED") : Lng.str("BLOCKED");
        }

        private GUIAnchor detail(int height) {
            GUIAnchor d = new GUIAnchor(getState(), DETAIL_WIDTH, height);
            d.attach(new GUIColoredRectangle(getState(), DETAIL_WIDTH, height, new Vector4f(CARD)));
            CivicType t = selected == null ? null : CivicRegistry.get(selected);
            if (t == null) {
                GUITextOverlay none = text(getState(), 14, DIM, Lng.str("Every civic is held or filtered out."));
                none.setPos(12, 12, 0);
                d.attach(none);
                return d;
            }
            FactionCivics c = held();
            String why = blocked(t);
            long now = System.currentTimeMillis();
            long cooldownMs = BFConfig.civicSwapCooldownDays() * DAY_MS;
            int cost = c.cost(slot, now, videogoose.betterfactions.civics.Council.ownCost(videogoose.betterfactions.civics.Council.REFORM, BFConfig.civicSwapCost()), BFConfig.civicRushCost(), cooldownMs);
            long days = c.cooldownDays(now, cooldownMs);
            boolean swap = c.isSwap(slot);
            if (why == null && swap && days > 0 && BFConfig.civicRushCost() <= 0) {
                why = Lng.str("You can swap a civic again in %s days.", days);
            }
            boolean needsResource = why == null && t.choosesResource() && !Civics.isResource(resource);

            GUITextOverlay name = text(getState(), 16, TEXT, t.getDisplayName());
            name.setPos(12, 10, 0);
            GUIAnchor wrap = new GUIAnchor(getState(), DETAIL_WIDTH - 24, height);
            GUITextOverlay desc = text(getState(), 14, TEXT, t.getDescription() + "\n\n" + rules(t));
            desc.autoWrapOn = wrap;
            desc.setPos(12, 34, 0);
            d.attach(name);
            d.attach(desc);

            int y = height - 34;
            GUITextButton take = button(DETAIL_WIDTH - 24,
                    why != null ? Lng.str("CANNOT TAKE") : needsResource ? Lng.str("CHOOSE A RESOURCE FIRST")
                            : cost > 0 ? Lng.str("REPLACE FOR %s FP", cost) : Lng.str("TAKE IT"),
                    why != null || needsResource ? GUITextButton.ColorPalette.TRANSPARENT : GUITextButton.ColorPalette.OK,
                    why != null || needsResource ? () -> { } : () -> take(t));
            take.setPos(12, y, 0);
            d.attach(take);
            if (t.choosesResource()) {
                y -= 30;
                GUITextButton res = button(DETAIL_WIDTH - 24,
                        Civics.isResource(resource) ? Lng.str("RESOURCE: %s", ElementKeyMap.getInfo(resource).getName()) : Lng.str("CHOOSE A RESOURCE"),
                        GUITextButton.ColorPalette.NEUTRAL, this::chooseResource);
                res.setPos(12, y, 0);
                d.attach(res);
            }
            String verdict = why != null ? why
                    : !swap ? Lng.str("Fills slot %s for free.", slot + 1)
                    : days > 0 ? Lng.str("The swap cooldown has %s days left. Replacing %s now costs %s faction points.", days, CivicRegistry.nameOf(c.at(slot)), cost)
                    : Lng.str("Replaces %s for %s faction points. The next swap waits %s days.", CivicRegistry.nameOf(c.at(slot)), cost, BFConfig.civicSwapCooldownDays());
            if (why == null && swap) {
                for (String dep : c.dependants(slot)) {
                    verdict += " " + Lng.str("%s is dropped with it.", CivicRegistry.nameOf(dep));
                }
            }
            GUIAnchor verdictWrap = new GUIAnchor(getState(), DETAIL_WIDTH - 24, 60);
            GUITextOverlay v = text(getState(), 14, why != null ? ORANGE : DIM, verdict);
            v.autoWrapOn = verdictWrap;
            v.onInit();
            v.updateTextSize();
            v.setPos(12, y - v.getTextHeight() - 6, 0);
            d.attach(v);
            return d;
        }

        /** What it cannot be held with and what it needs, both ways round. */
        private static String rules(CivicType t) {
            StringBuilder b = new StringBuilder();
            for (CivicType o : CivicRegistry.all()) {
                if (o != t && t.conflictsWith(o)) {
                    b.append(Lng.str("Cannot be held with %s.", o.getDisplayName())).append('\n');
                }
            }
            for (String need : t.getRequires()) {
                b.append(Lng.str("Needs %s in another slot.", CivicRegistry.nameOf(need))).append('\n');
            }
            for (CivicType.Unlock u : t.getUnlocks()) {
                b.append(Lng.str("Unlocks at: %s", u.describe())).append('\n');
            }
            return b.toString();
        }

        private void take(CivicType t) {
            boolean wasEmpty = !held().isSwap(slot);
            Civics.requestPick(slot, t.getId(), resource);
            //a new faction carries on to its next empty slot once the server confirms; the grid follows
            //the civics version, so it greys what was just picked by itself
            if (fill && wasEmpty && slot + 1 < held().slotsEarned) {
                slot++;
                selected = null;
                resource = 0;
            } else {
                deactivate();
            }
        }

        private void chooseResource() {
            List<GUIElement> items = new ArrayList<>();
            for (short r : ElementKeyMap.resources) {
                if (Civics.isResource(r) && ElementKeyMap.isValidType(r)) {
                    GUITextOverlay name = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), getState());
                    name.setTextSimple(ElementKeyMap.getInfo(r).getName());
                    name.setPos(5, 4, 0);
                    GUIAnchor a = new GUIAnchor(getState(), 400, 24);
                    a.attach(name);
                    a.setUserPointer(r);
                    items.add(a);
                }
            }
            new PlayerGameDropDownInput("CIVIC_RESOURCE_CHOOSER", getState(), 440, 300, Lng.str("Choose a resource"), 24,
                    Lng.str("%s specialises in one resource.", CivicRegistry.nameOf(selected)), items) {
                @Override
                public void onDeactivate() {
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }

                @Override
                public void pressedOK(GUIListElement current) {
                    if (current != null && current.getContent() != null) {
                        resource = (Short) current.getContent().getUserPointer();
                    }
                    deactivate();
                }
            }.activate();
        }

        private GUITextButton button(int width, Object text, GUITextButton.ColorPalette palette, Runnable onPress) {
            return new GUITextButton(getState(), width, 24, palette, text, new GUICallback() {
                @Override
                public void callback(GUIElement callingGuiElement, MouseEvent event) {
                    if (event.pressedLeftMouse()) {
                        onPress.run();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return !getState().getController().getPlayerInputs().isEmpty()
                            && getState().getController().getPlayerInputs().get(getState().getController().getPlayerInputs().size() - 1) != Picker.this;
                }
            });
        }
    }
}
