package net.dovtech.betterfactions;

import api.DebugFile;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.block.FactoryType;
import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.gui.FactionPanelGUICreateEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import net.dovtech.betterfactions.block.ResourceLinker;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;

public class BetterFactions extends StarMod {
    static BetterFactions inst;
    public BetterFactions() {
        inst = this;
    }
    private boolean debug;
    private boolean orgPanelDrawn;
    private GUIContentPane organization;

    @Override
    public void onGameStart() {
        setModName("BetterFactions");
        setModVersion("0.1.11");
        setModDescription("A versatile mod aimed at improving player faction interaction.");
        setModAuthor("TheDerpGamer");
        orgPanelDrawn = false;
        debug = true;
    }

    @Override
    public void onEnable() {

        //Faction GUI
        StarLoader.registerListener(FactionPanelGUICreateEvent.class, new Listener() {
            @Override
            public void onEvent(Event e) {
                FactionPanelGUICreateEvent event = (FactionPanelGUICreateEvent) e;
                FactionPanelNew factionPanelNew = event.getFactionPanel();
                GUIMainWindow factionPanel = event.getPanelMenu();
                boolean inFaction = factionPanelNew.getOwnFaction() != null;
                int selectedTab = factionPanel.getSelectedTab();
                if(debug) DebugFile.log("[DEBUG]: FactionPanelGUICreateEvent fired!");

                if(inFaction && factionPanel.getTabs().size() == 6) createOrganizationPane(factionPanel);

                factionPanelNew.factionPanel = factionPanel;

                /*
                if(inFaction && selectedTab == 6 && !orgPanelDrawn) {
                    organization.draw();
                    organization.drawAttached();
                    orgPanelDrawn = true;
                    if(debug) DebugFile.log("[DEBUG]: Drew Organization panel");
                }
                 */
            }
        });
    }

    private void createOrganizationPane(GUIMainWindow factionPanel) {
        organization = new GUIContentPane(factionPanel.getState(), factionPanel, "ORGANIZATION");
        organization.setName("ORGANIZATION");

        GUITabbedContent organizationPanel = new GUITabbedContent(organization.getState(), organization);

        //Organization Info
        GUIContentPane info = new GUIContentPane(organization.getState(), organizationPanel, "ORGANIZATION INFO");
        info.setName("ORGANIZATION INFO");

        //Organization News
        GUIContentPane organizationNews = new GUIContentPane(organization.getState(), organizationPanel, "ORGANIZATION NEWS");
        organizationNews.setName("ORGANIZATION NEWS");

        //Messages
        GUIContentPane messages = new GUIContentPane(organization.getState(), organizationPanel, "MESSAGES");
        messages.setName("MESSAGES");

        //Resources
        GUIContentPane resources = new GUIContentPane(organization.getState(), organizationPanel, "RESOURCES");
        resources.setName("RESOURCES");

        //Settings
        GUIContentPane settings = new GUIContentPane(organization.getState(), organizationPanel, "SETTINGS");
        settings.setName("SETTINGS");

        organizationPanel.getTabs().add(info);
        organizationPanel.getTabs().add(organizationNews);
        organizationPanel.getTabs().add(messages);
        organizationPanel.getTabs().add(resources);
        organizationPanel.getTabs().add(settings);
        organization.addNewTextBox(0);
        organization.setContent(0, organizationPanel);
        factionPanel.addTab(organization);
        if(debug) DebugFile.log("[DEBUG]: Organization panel created");
    }

    public void onBlockConfigLoad(BlockConfig config) {

        //Resource Linker
        ResourceLinker resourceLinker = new ResourceLinker();
        ElementInformation resourceLinkerInfo = resourceLinker.getElementInformation();
        FactoryResource[] resourceLinkerRecipe = {
                new FactoryResource(1, Blocks.STORAGE.getId()),
                new FactoryResource(1, Blocks.FACTION_MODULE.getId()),
                new FactoryResource(300, Blocks.ALLOYED_METAL_MESH.getId())
        };
        BlockConfig.addRecipe(resourceLinkerInfo, FactoryType.STANDARD, 5, resourceLinkerRecipe);
        config.add(resourceLinkerInfo);
    }
}