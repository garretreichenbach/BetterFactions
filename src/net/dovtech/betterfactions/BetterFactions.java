package net.dovtech.betterfactions;

import api.DebugFile;
import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.gui.FactionPanelGUICreateEvent;
import api.listener.events.gui.GUIElementCreateEvent;
import api.main.GameClient;
import api.mod.StarLoader;
import api.mod.StarMod;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.game.client.view.gui.npc.GUINPCFactionsScrollableList;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import org.schema.schine.graphicsengine.forms.AbstractSceneNode;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BetterFactions extends StarMod {
    static BetterFactions inst;
    public BetterFactions() {
        inst = this;
    }
    private boolean debug;

    @Override
    public void onGameStart() {
        setModName("BetterFactions");
        setModVersion("0.1.10");
        setModDescription("A versatile mod aimed at improving player faction interaction.");
        setModAuthor("TheDerpGamer");
        debug = true;
    }

    @Override
    public void onEnable() {
        DebugFile.log("Enabled", this);

        //Faction GUI
        StarLoader.registerListener(FactionPanelGUICreateEvent.class, new Listener() {
            @Override
            public void onEvent(Event e) {
                FactionPanelGUICreateEvent event = (FactionPanelGUICreateEvent) e;
                FactionPanelNew factionPanelNew = event.getFactionPanel();
                GUIMainWindow factionPanel = event.getPanelMenu();

                if(debug) {
                    DebugFile.log("[DEBUG]: FactionPanelGUICreateEvent fired!");
                }

                GUIContentPane news = null;
                GUIContentPane factionRelations = null;
                GUIContentPane factionNews = null;
                GUIContentPane members = null;
                GUIContentPane factionList = null;
                GUIContentPane options = null;

                for(GUIContentPane contentPane : factionPanel.getTabs()) {
                    if(contentPane.getTabNameText().getText().contains("NEWS")) {
                        news = contentPane;
                    } else if(contentPane.getTabNameText().getText().contains("NPC DIPLOMACY")) {
                        factionRelations = contentPane;
                    } else if(contentPane.getTabNameText().getText().contains("FACTION NEWS")) {
                        factionNews = contentPane;
                    } else if(contentPane.getTabNameText().getText().contains("MEMBERS")) {
                        members = contentPane;
                    } else if(contentPane.getTabNameText().getText().contains("LIST")) {
                        factionList = contentPane;
                    } else if(contentPane.getTabNameText().getText().contains("OPTIONS")) {
                        options = contentPane;
                    }
                    if(debug) {
                        DebugFile.log("[DEBUG]: " + contentPane.getTabNameText().getText().toString() + " tab created");
                    }
                }

                factionRelations.setName("FACTION RELATIONS");
                factionList.setTabName("FACTION LIST");
                //Todo:StarMade doesn't like casting from a player faction to an NPC one. Maybe look into creating new NPC factions and adding them as placeholders for player ones?

                /*
                GUINPCFactionsScrollableList npcFactionsList = (GUINPCFactionsScrollableList) factionRelations.getContent(0, 0).getChilds().get(0);
                Collection<Faction> factions = GameClient.getClientState().getFactionManager().getFactionCollection();
                GUIElementList elementList = new GUIElementList(npcFactionsList.getState());
                Set<NPCFaction> npcFactionsSet = new HashSet<NPCFaction>();
                for(Faction faction : factions) {
                    npcFactionsSet.add((NPCFaction) faction);
                    if(debug) {
                        DebugFile.log("[DEBUG]: Added faction " + faction.getName() + " to the relations list" );
                    }
                }
                npcFactionsList.updateListEntries(elementList, npcFactionsSet);
                 */


                //ToDo:Check to see if the player's faction is part of an organization. If it isn't, the organization tab should be different.

                //Organization
                GUIContentPane organization = new GUIContentPane(factionPanel.getState(), factionPanel, "ORGANIZATION");
                organization.setName("ORGANIZATION");
                organization.setTabName("ORGANIZATION");


                //GUIAncor organizationLogo = ;
                //organization.setContent(0, 0, organizationLogo);

                organization.addNewTextBox(30); //Organization Info

                organization.addDivider(300);

                /*

                GUITabbedContent organizationPanel = (GUITabbedContent) factionPanel.clone();
                organizationPanel.clearTabs();

                GUIContentPane paneBase = (GUIContentPane) news.clone();

                //Organization News
                GUIContentPane organizationNews = (GUIContentPane) paneBase.clone();
                organizationNews.setTabName("ORGANIZATION NEWS");

                //Organization Members
                GUIContentPane memberFactions = (GUIContentPane) paneBase.clone();
                memberFactions.setTabName("MEMBER FACTIONS");

                //Messages
                GUIContentPane messages = (GUIContentPane) paneBase.clone();
                messages.setTabName("MESSAGES");

                //Resources
                GUIContentPane resources = (GUIContentPane) paneBase.clone();
                resources.setTabName("RESOURCES");

                //Settings
                GUIContentPane settings = (GUIContentPane) paneBase.clone();
                settings.setTabName("ORGANIZATION SETTINGS");

                organizationPanel.getTabs().add(organizationNews);
                organizationPanel.getTabs().add(memberFactions);
                organizationPanel.getTabs().add(messages);
                organizationPanel.getTabs().add(resources);
                organizationPanel.getTabs().add(settings);
                organization.setContent(1, organizationPanel);
                 */

                factionPanel.clearTabs();
                if(factionPanelNew.getOwnFaction() != null) {
                    if(debug) {
                        DebugFile.log("[DEBUG]: Player is in a faction" );
                    }
                    factionPanel.getTabs().add(0, news);
                    factionPanel.getTabs().add(1, factionRelations);
                    factionPanel.getTabs().add(2, factionNews);
                    factionPanel.getTabs().add(3, members);
                    factionPanel.getTabs().add(4, factionList);
                    factionPanel.getTabs().add(5, organization);
                    factionPanel.getTabs().add(6, options);

                } else {
                    if(debug) {
                        DebugFile.log("[DEBUG]: Player is not in a faction" );
                    }
                    factionPanel.getTabs().add(0, news);
                    factionPanel.getTabs().add(1, factionRelations);
                    factionPanel.getTabs().add(2, factionList);
                    factionPanel.getTabs().add(3, organization);
                    factionPanel.getTabs().add(4, options);
                }

                if(debug) {
                    for(int i = 0; i < factionPanel.getTabs().size(); i ++) {
                        DebugFile.log("[DEBUG]: Added tab " + factionPanel.getTabs().get(i).getTabName().toString() + " to the faction menu at position " + i);
                    }
                }
                factionPanel.draw();
                if(debug) {
                    DebugFile.log("[DEBUG]: Redrew the faction menu");
                }
            }
        });
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
}