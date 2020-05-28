package net.dovtech.betterfactions;

import api.DebugFile;
import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.gui.GUIElementCreateEvent;
import api.main.GameClient;
import api.mod.StarLoader;
import api.mod.StarMod;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.game.client.view.gui.npc.GUINPCFactionsScrollableList;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BetterFactions extends StarMod {
    static BetterFactions inst;
    public BetterFactions() {
        inst = this;
    }

    @Override
    public void onGameStart() {
        setModName("BetterFactions");
        setModVersion("0.1.10");
        setModDescription("A versatile mod aimed at improving player faction interaction.");
        setModAuthor("TheDerpGamer");
    }

    @Override
    public void onEnable() {
        DebugFile.log("Enabled", this);
        registerListeners();
    }

    public void onBlockConfigLoad(BlockConfig config) {

        /*
        ShipMarket shipMarket = new ShipMarket();
        ElementInformation shipMarketInfo = shipMarket.getBlockInfo();
        FactoryResource[] shipMarketRecipe = {
                new FactoryResource(1, Blocks.SHOP_MODULE.getId()),
                new FactoryResource(1, Blocks.FACTION_MODULE.getId())
        };
        BlockConfig.addRecipe(shipMarketInfo, FactoryType.ADVANCED, 5, shipMarketRecipe);
        config.add(shipMarketInfo);

         */
    }

    public void registerListeners() {
        //Faction GUI
        StarLoader.registerListener(GUIElementCreateEvent.class, new Listener() {
            @Override
            public void onEvent(Event e) {
                GUIElementCreateEvent event = (GUIElementCreateEvent) e;
                if(event.getGUIElement() instanceof FactionPanelNew) {
                    FactionPanelNew factionPanelNew = (FactionPanelNew) event.getGUIElement();
                    factionPanelNew.onInit();
                    GUIMainWindow factionPanel = factionPanelNew.factionPanel;

                    GUIContentPane news = null;
                    GUIContentPane factionRelations = null;
                    GUIContentPane factionNews = null;
                    GUIContentPane members = null;
                    GUIContentPane factionList = null;
                    GUIContentPane options = null;
                    GUIContentPane organization;

                    for(GUIContentPane contentPane : factionPanel.getTabs()) {
                        if(contentPane.getTabName().equals("NEWS")) {
                            news = contentPane;
                        } else if(contentPane.getTabName().equals("NPC DIPLOMACY")) {
                            factionRelations = contentPane;
                        } else if(contentPane.getTabName().equals("FACTION NEWS")) {
                            factionNews = contentPane;
                        } else if(contentPane.getTabName().equals("MEMBERS")) {
                            members = contentPane;
                        } else if(contentPane.getTabName().equals("LIST")) {
                            factionList = contentPane;
                        } else if(contentPane.getTabName().equals("OPTIONS")) {
                            options = contentPane;
                        }
                    }

                    factionRelations.setName("FACTION RELATIONS");
                    factionList.setTabName("FACTION LIST");
                    GUINPCFactionsScrollableList npcFactionsList = (GUINPCFactionsScrollableList) factionRelations.getContent(0, 0).getChilds().get(0);
                    Collection<Faction> factions = GameClient.getClientState().getFactionManager().getFactionCollection();
                    GUIElementList elementList = new GUIElementList(npcFactionsList.getState());
                    Set<NPCFaction> npcFactionsSet = new HashSet<NPCFaction>();
                    for(Faction faction : factions) {
                        npcFactionsSet.add((NPCFaction) faction);
                    }
                    npcFactionsList.updateListEntries(elementList, npcFactionsSet);

                    //ToDo:Check to see if the player's faction is part of an organization. If it isn't, the organization tab should be different.

                    //Organization
                    organization = new GUIContentPane(factionPanel.getState(), factionPanel, "ORGANIZATION");

                    //GUIAncor organizationLogo = ;
                    //organization.setContent(0, 0, organizationLogo);

                    organization.addNewTextBox(30); //Organization Info

                    organization.addDivider(300);

                    /*
                    GUITabbedContent organizationPanel = new GUITabbedContent(factionPanel.getState(), organization.getContent(1));
                    organizationPanel.clearTabs();

                    //Organization News
                    GUIContentPane organizationNews = new GUIContentPane(organizationPanel.getState(), organizationPanel, "NEWS");

                    //Organization Members
                    GUIContentPane memberFactions = new GUIContentPane(organizationPanel.getState(), organizationPanel, "MEMBER FACTIONS");

                    //Messages
                    GUIContentPane messages = new GUIContentPane(organizationPanel.getState(), organizationPanel, "MESSAGES");

                    //Resources
                    GUIContentPane resources = new GUIContentPane(organizationPanel.getState(), organizationPanel, "RESOURCES");

                    //Settings
                    GUIContentPane settings = new GUIContentPane(organizationPanel.getState(), organizationPanel, "SETTINGS");

                    organizationPanel.getTabs().add(organizationNews);
                    organizationPanel.getTabs().add(memberFactions);
                    organizationPanel.getTabs().add(messages);
                    organizationPanel.getTabs().add(resources);
                    organizationPanel.getTabs().add(settings);

                    organization.setContent(0, 1, organizationPanel);
                    */

                    factionPanel.clearTabs();
                    factionPanel.getTabs().add(news);
                    factionPanel.getTabs().add(factionRelations);
                    if(factionPanelNew.getOwnFaction() != null) {
                        factionPanel.getTabs().add(factionNews);
                        factionPanel.getTabs().add(members);
                        factionPanel.getTabs().add(organization);
                    }
                    factionPanel.getTabs().add(factionList);
                    factionPanel.getTabs().add(options);
                    factionPanel.draw();
                }
            }
        });
    }
}