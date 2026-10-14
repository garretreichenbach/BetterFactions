package videogoose.betterfactions.gui;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUICheckBoxTextPair;
import videogoose.betterfactions.civics.Corporations;
import videogoose.betterfactions.fleet.TradeFleetOrders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static videogoose.betterfactions.gui.InstitutionsContent.*;

/**
 * "Raid Trade Routes", in one dialog: the sectors whose systems the fleet patrols, and whose deliveries it hits,
 * nearest the fleet first. Naming factions raids them without a war; naming none raids anyone who is not us
 * or a friend.
 */
public final class RaidOrderDialog extends PlayerGameOkCancelInput {

    private final Fleet fleet;
    private final Set<Integer> targets = new HashSet<>();
    private GUIActivatableTextBar sectors;
    private boolean miners;

    public RaidOrderDialog(GameClientState state, Fleet fleet) {
        super("RAID_ORDER", state, 600, 560, Lng.str("Raid trade routes with %s", fleet.getName()), "");
        this.fleet = fleet;
        getInputPanel().setOkButtonText(Lng.str("RAID"));
        getInputPanel().onInit();
        build(getInputPanel().getContent());
    }

    private Vector3i at() {
        return fleet.getFlagShip() != null ? fleet.getFlagShip().getSector() : new Vector3i();
    }

    private void build(GUIAnchor box) {
        GUITextOverlay label = text(getState(), 14, DIM, Lng.str("SECTORS TO PATROL, ONE IN EACH SYSTEM, IN ORDER (UP TO %s)", TradeFleetOrders.MAX_ROUTE));
        label.setPos(4, 4, 0);
        box.attach(label);
        GUIAnchor holder = new GUIAnchor(getState(), 560, 28);
        holder.setPos(4, 24, 0);
        sectors = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 300, 1, Lng.str("e.g. 10, 20, 111; 30, 4, 5"), holder, DiplomacyProposeDialog.TEXT, t -> t);
        sectors.onInit();
        Vector3i here = at();
        sectors.setText(here.x + ", " + here.y + ", " + here.z);
        holder.attach(sectors);
        box.attach(holder);

        GUICheckBoxTextPair mine = new GUICheckBoxTextPair(getState(), Lng.str("Mining fleets too  (theirs, in these systems, under the same rules)"), 520, 24) {
            @Override
            public boolean isActivated() {
                return miners;
            }

            @Override
            public void activate() {
                miners = true;
            }

            @Override
            public void deactivate() {
                miners = false;
            }
        };
        mine.onInit();
        mine.setPos(4, 58, 0);
        box.attach(mine);

        GUITextOverlay who = text(getState(), 14, DIM, Lng.str("WHOSE FLEETS  (NEAREST FIRST; NONE TICKED: ANYONE NOT US OR A FRIEND)"));
        who.setPos(4, 92, 0);
        box.attach(who);
        GUIAnchor listBox = new GUIAnchor(getState(), 560, 312);
        listBox.setPos(4, 114, 0);
        GUIElementList list = new GUIElementList(getState());
        for (Faction f : candidates()) {
            int id = f.getIdFaction();
            boolean home = hasHome(f);
            String where = home ? Lng.str("%s systems away", Corporations.systems(here, f.getHomeSector())) : Lng.str("no known home");
            String war = getState().getFactionManager().isEnemy(getState().getPlayer().getFactionId(), id) ? Lng.str("  -  at war") : "";
            GUICheckBoxTextPair cb = new GUICheckBoxTextPair(getState(), f.getName() + "  -  " + where + war, 520, 24) {
                @Override
                public boolean isActivated() {
                    return targets.contains(id);
                }

                @Override
                public void activate() {
                    targets.add(id);
                }

                @Override
                public void deactivate() {
                    targets.remove(id);
                }
            };
            list.add(new GUIListElement(cb, cb, getState()));
        }
        GUIScrollablePanel scroll = new GUIScrollablePanel(560, 312, listBox, getState());
        scroll.setContent(list);
        scroll.onInit();
        listBox.attach(scroll);
        box.attach(listBox);
    }

    /** Factions whose deliveries are worth raiding: real polities, not us, nearest the fleet first. */
    private List<Faction> candidates() {
        int own = getState().getPlayer().getFactionId();
        Vector3i here = at();
        List<Faction> out = new ArrayList<>();
        for (Faction f : getState().getFactionManager().getFactionCollection()) {
            int id = f.getIdFaction();
            if (id != own && id != 0 && id != FactionManager.PIRATES_ID && id != FactionManager.TRAIDING_GUILD_ID && f.isShowInHub()) {
                out.add(f);
            }
        }
        out.sort(Comparator.<Faction>comparingInt(f -> hasHome(f) ? Corporations.systems(here, f.getHomeSector()) : Integer.MAX_VALUE)
                .thenComparing(Faction::getName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static boolean hasHome(Faction f) {
        return f.getHomebaseUID() != null && !f.getHomebaseUID().isEmpty() && f.getHomeSector() != null;
    }

    @Override
    public void pressedOK() {
        List<Vector3i> route = TradeFleetOrders.parseSectors(sectors.getText());
        if (route.isEmpty()) {
            getState().getController().popupAlertTextMessage(Lng.str("No sectors in that: give three numbers for each."), 0);
            return;
        }
        deactivate();
        TradeFleetOrders.requestRaid(fleet, route.subList(0, Math.min(route.size(), TradeFleetOrders.MAX_ROUTE)), targets, miners);
    }

    @Override
    public boolean isOccluded() {
        return false;
    }

    @Override
    public void onDeactivate() {
    }
}
