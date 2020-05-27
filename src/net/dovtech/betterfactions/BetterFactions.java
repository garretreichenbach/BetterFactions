package net.dovtech.betterfactions;

import api.DebugFile;
import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.gui.GUIElementCreateEvent;
import api.main.GameClient;
import api.mod.StarLoader;
import api.mod.StarMod;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.game.client.view.gui.npc.GUINPCFactionsScrollableList;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;

import java.util.Set;

public class BetterFactions extends StarMod {
    static BetterFactions inst;
    public BetterFactions() {
        inst = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.modName = "BetterFactions";
        this.modAuthor = "DovTech";
        this.modVersion = "0.1.9";
        this.modDescription = "A versatile faction management and trade mod aimed at improving the game's player to player interaction.";
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
                    GUIMainWindow factionPanel = factionPanelNew.factionPanel;

                    GUIContentPane news;
                    GUIContentPane factionRelations;
                    GUIContentPane members;
                    GUIContentPane factionList;
                    GUIContentPane options;
                    GUIContentPane organization;

                    if(factionPanelNew.getOwnFaction() != null) {
                        news = factionPanel.getTabs().get(2);
                        factionRelations = factionPanel.getTabs().get(1);
                        members = factionPanel.getTabs().get(3);
                        factionList = factionPanel.getTabs().get(4);
                        options = factionPanel.getTabs().get(5);
                    } else {
                        news = factionPanel.getTabs().get(0);
                        factionRelations = factionPanel.getTabs().get(1);
                        members = factionPanel.getTabs().get(2);
                        factionList = factionPanel.getTabs().get(3);
                        options = factionPanel.getTabs().get(4);
                    }

                    //Relations
                    factionRelations.setName("FACTION RELATIONS");
                    GUINPCFactionsScrollableList npcFactionsList = (GUINPCFactionsScrollableList) factionRelations.getContent(0, 0).getChilds().get(0);
                    Set<Faction> factionsSet = (Set<Faction>) GameClient.getClientState().getFactionManager().getFactionCollection();
                    GUIElementList elementList = new GUIElementList(npcFactionsList.getState());
                    Set<NPCFaction> npcFactionsSet = null;
                    while(factionsSet.iterator().hasNext()) {
                        if(factionsSet.iterator().next() != factionPanelNew.getOwnFaction()) {
                            npcFactionsSet.add((NPCFaction) factionsSet.iterator().next());
                        }
                    }
                    npcFactionsList.updateListEntries(elementList, npcFactionsSet);

                    //ToDo:Check to see if the player's faction is part of an organization. If it isn't, the organization tab should be different.

                    //Organization
                    organization = new GUIContentPane(factionPanel.getState(), (GUIWindowInterface) factionPanel.activeInterface, "ORGANIZATION");
                    organization.setTextBoxHeightLast(270);

                    //GUIAncor organizationLogo = ;
                    //organization.setContent(0, 0, organizationLogo);

                    organization.addNewTextBox(30); //Organization Info

                    organization.addDivider(300);
                    organization.setTextBoxHeightLast(1, 48);

                    GUITabbedContent organizationPanel = new GUITabbedContent(factionPanel.getState(), organization.getContent(0, 1));
                    organizationPanel.clearTabs();

                    //Organization News
                    GUIContentPane organizationNews = new GUIContentPane(organizationPanel.getState(), (GUIWindowInterface) organizationPanel.activationInterface, "NEWS");

                    //Organization Members
                    GUIContentPane memberFactions = new GUIContentPane(organizationPanel.getState(), (GUIWindowInterface) organizationPanel.activationInterface, "MEMBER FACTIONS");

                    //Messages
                    GUIContentPane messages = new GUIContentPane(organizationPanel.getState(), (GUIWindowInterface) organizationPanel.activationInterface, "MESSAGES");

                    //Resources
                    GUIContentPane resources = new GUIContentPane(organizationPanel.getState(), (GUIWindowInterface) organizationPanel.activationInterface, "RESOURCES");

                    //Settings
                    GUIContentPane settings = new GUIContentPane(organizationPanel.getState(), (GUIWindowInterface) organizationPanel.activationInterface, "SETTINGS");

                    organizationPanel.getTabs().add(organizationNews);
                    organizationPanel.getTabs().add(memberFactions);
                    organizationPanel.getTabs().add(messages);
                    organizationPanel.getTabs().add(resources);
                    organizationPanel.getTabs().add(settings);

                    organization.setContent(0, 1, organizationPanel);

                    if(factionPanelNew.getOwnFaction() != null) {
                        factionPanel.getTabs().add(organization);
                    }
                }
            }
        });
    }
}