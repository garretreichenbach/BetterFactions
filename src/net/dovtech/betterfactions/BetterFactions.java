package net.dovtech.betterfactions;

import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.gui.ControlManagerActivateEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import net.dovtech.betterfactions.faction.Organization;
import net.dovtech.betterfactions.gui.NewFactionPanel;
import net.dovtech.betterfactions.gui.NewFactionScrollableList;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.game.common.data.player.faction.Faction;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BetterFactions extends StarMod {
    static BetterFactions inst;
    public BetterFactions() {
        inst = this;
    }
    private static Map<Faction, Organization> factions = new HashMap<Faction, Organization>();

    @Override
    public void onGameStart() {
        setModName("BetterFactions");
        setModVersion("0.1.14");
        setModDescription("A versatile mod aimed at improving player faction interaction.");
        setModAuthor("TheDerpGamer");
    }

    @Override
    public void onEnable() {
        //Faction GUI
        StarLoader.registerListener(ControlManagerActivateEvent.class, new Listener() {
            @Override
            public void onEvent(Event e) {
                ControlManagerActivateEvent event = ((ControlManagerActivateEvent) e);
                PlayerPanel playerPanel = GameClientState.instance.getWorldDrawer().getGuiDrawer().getPlayerPanel();
                playerPanel.onInit();
                try {
                    Field panel = PlayerPanel.class.getDeclaredField("factionPanelNew");
                    panel.setAccessible(true);
                    FactionPanelNew p = (FactionPanelNew) panel.get(playerPanel);
                    if(!(p instanceof NewFactionPanel)) {
                        GameClientState state = playerPanel.getState();
                        panel.set(playerPanel, new NewFactionPanel(state));

                        NewFactionScrollableList fList = new NewFactionScrollableList(p.getState(), p);
                        Field fListField = FactionPanelNew.class.getDeclaredField("fList");
                        fListField.setAccessible(true);
                        fListField.set(p, fList);
                    }
                } catch (NoSuchFieldException ex) {
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public static Organization getOrg(Faction faction) {
        if(factions.get(faction) != null)  {
            return factions.get(faction);
        } else {
            return null;
        }
    }

    public static void addFactionToOrg(Faction faction, Organization org) {
        factions.put(faction, org);
    }

    public static void removeFactionFromOrg(Faction faction) {
        factions.put(faction, null);
    }
}