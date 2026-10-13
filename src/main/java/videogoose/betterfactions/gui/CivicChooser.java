package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerGameDropDownInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIListElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.CivicType;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.FactionCivics;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks the civic for one slot. Civics the faction cannot hold next to the ones it has are greyed, and
 * OK on one says why. The server checks everything again.
 */
public final class CivicChooser {

    private static final int WIDTH = 720;
    /** A name and two wrapped lines of description. */
    private static final int ITEM_HEIGHT = 58;

    /** The slot to open next once the server confirms a chained pick; -1 for none. */
    private static int chainSlot = -1;

    private CivicChooser() {
    }

    /** Client: civics arrived from the server. Continues a chained first pick if this confirmed it. */
    public static void civicsReceived(GameClientState state) {
        Faction own = state == null ? null : state.getFaction();
        if (chainSlot < 0 || own == null || Civics.client().of(own.getIdFaction()).civics.size() != chainSlot) {
            return;
        }
        int slot = chainSlot;
        chainSlot = -1;
        open(state, slot, true);
    }

    /**
     * @param chain after a pick into an empty slot, open the next empty one: how a new faction fills
     *              all its slots in one go
     */
    public static void open(GameClientState state, int slot, boolean chain) {
        Faction own = state.getFaction();
        if (own == null) {
            return;
        }
        FactionCivics held = Civics.client().of(own.getIdFaction());
        boolean swap = held.isSwap(slot);

        List<GUIElement> items = new ArrayList<>();
        for (CivicType t : CivicRegistry.all()) {
            //one already held is left out rather than greyed: there is nothing to explain about it
            if (!held.civics.contains(t.getId())) {
                items.add(item(state, t, held.conflict(slot, t.getId()) != null));
            }
        }
        Object description = swap
                ? Lng.str("Replacing %s costs %s faction points, and the next swap waits %s days.",
                CivicRegistry.nameOf(held.civics.get(slot)), BFConfig.civicSwapCost(), BFConfig.civicSwapCooldownDays())
                : Lng.str("Slot %s of %s. Filling an empty slot is free; replacing a civic later costs faction points.",
                slot + 1, held.slotsEarned);

        new PlayerGameDropDownInput("CIVIC_CHOOSER", state, WIDTH, 340, Lng.str("Choose a civic"), ITEM_HEIGHT, description, items) {
            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void pressedOK(GUIListElement current) {
                if (current == null || current.getContent() == null) {
                    return;
                }
                String id = (String) current.getContent().getUserPointer();
                String why = Civics.client().of(own.getIdFaction()).conflict(slot, id);
                if (why != null) {
                    setErrorMessage(why);
                    return;
                }
                Civics.requestPick(slot, id);
                deactivate();
                //the next slot opens once the server has confirmed this one, so it greys what was just picked
                chainSlot = chain && !swap && slot + 1 < held.slotsEarned ? slot + 1 : -1;
            }
        }.activate();
    }

    private static GUIAnchor item(GameClientState state, CivicType t, boolean greyed) {
        GUIAnchor a = new GUIAnchor(state, WIDTH - 40, ITEM_HEIGHT);
        GUITextOverlay name = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium16(), state);
        name.setTextSimple(t.getDisplayName());
        name.setPos(5, 3, 0);
        GUITextOverlay desc = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium14(), state);
        desc.setTextSimple(t.getDescription());
        desc.autoWrapOn = new GUIAnchor(state, WIDTH - 80, ITEM_HEIGHT);
        desc.setPos(5, 21, 0);
        if (greyed) {
            name.setColor(0.45f, 0.45f, 0.45f, 1.0f);
            desc.setColor(0.40f, 0.40f, 0.40f, 1.0f);
        } else {
            desc.setColor(0.55f, 0.58f, 0.60f, 1.0f);
        }
        a.attach(name);
        a.attach(desc);
        a.setUserPointer(t.getId());
        return a;
    }
}
