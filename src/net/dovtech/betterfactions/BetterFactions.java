package net.dovtech.betterfactions;

import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.gui.FactionPanelGUICreateEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import net.dovtech.betterfactions.faction.Organization;
import net.dovtech.betterfactions.gui.NewFactionPanel;
import org.schema.game.common.data.player.faction.Faction;
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
        setModVersion("0.1.12");
        setModDescription("A versatile mod aimed at improving player faction interaction.");
        setModAuthor("TheDerpGamer");
    }

    @Override
    public void onEnable() {
        //Faction GUI
        StarLoader.registerListener(FactionPanelGUICreateEvent.class, new Listener() {
            @Override
            public void onEvent(Event e) {
                FactionPanelGUICreateEvent event = (FactionPanelGUICreateEvent) e;
                NewFactionPanel factionPanel = new NewFactionPanel(event.getInputState());
                factionPanel.recreateTabs();
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