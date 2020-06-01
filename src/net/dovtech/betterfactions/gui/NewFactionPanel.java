package net.dovtech.betterfactions.gui;

import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContextPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITabbedContent;
import org.schema.schine.input.InputState;

public class NewFactionPanel extends FactionPanelNew {

    private GUIContentPane newsTab;
    private GUIContentPane factionRelations;
    private GUIContentPane factionNews;
    private GUIContentPane members;
    private GUIContentPane factionList;
    private GUIContentPane options;
    private GUIContentPane organization;

    public NewFactionPanel(InputState inputState) {
        super(inputState);
    }

    @Override
    public void recreateTabs() {
        super.recreateTabs();
        getTabs();
        modifyFactionRelations();
        modifyFactionList();
        //createOrgPanel();
    }

    private void getTabs() {
        for(GUIContentPane tab : factionPanel.getTabs()) {
            if(tab.getTabName().toString().contains("NEWS")) {
                newsTab = tab;
            } else if(tab.getTabName().toString().contains("NPC DIPLOMACY")) {
                factionRelations = tab;
            } else if(tab.getTabName().toString().contains("FACTION NEWS")) {
                factionNews = tab;
            } else if(tab.getTabName().toString().contains("MEMBERS")) {
                members = tab;
            } else if(tab.getTabName().toString().contains("LIST")) {
                factionList = tab;
            } else if(tab.getTabName().toString().contains("OPTIONS")) {
                options = tab;
            }
        }
    }

    private void modifyFactionRelations() {
        factionRelations.setTabName("FACTION RELATIONS");
    }

    private void modifyFactionList() {
        factionList.setTabName("FACTION LIST");
    }

    private void createOrgPanel() {

        organization = this.factionPanel.addTab("ORGANIZATION");

        organization.setName("ORGANIZATION");

        GUITabbedContent organizationPanel = new GUITabbedContent(organization.getState(), organization);

        //Organization Info
        GUIContextPane orgInfo = new GUIContextPane(organization.getState(), 4.0F, 4.0F);
        GUIElementList memberFactions = new GUIElementList(organization.getState());

        //Organization News
        GUIContentPane organizationNews = new GUIContentPane(organizationPanel.getState(), organizationPanel, "ORGANIZATION NEWS");
        organizationNews.setName("ORGANIZATION NEWS");

        //Messages
        GUIContentPane messages = new GUIContentPane(organizationPanel.getState(), organizationPanel, "MESSAGES");
        messages.setName("MESSAGES");

        //Resources
        GUIContentPane resources = new GUIContentPane(organizationPanel.getState(), organizationPanel, "RESOURCES");
        resources.setName("RESOURCES");

        //Settings
        GUIContentPane settings = new GUIContentPane(organizationPanel.getState(), organizationPanel, "SETTINGS");
        settings.setName("SETTINGS");

        organizationPanel.getTabs().add(organizationNews);
        organizationPanel.getTabs().add(messages);
        organizationPanel.getTabs().add(resources);
        organizationPanel.getTabs().add(settings);
        organization.addDivider(30);
        organization.addNewTextBox(0, 15);
        organization.getContent(0).attach(orgInfo);
        organization.getContent(0, 1).attach(memberFactions);
        organization.getContent(1).attach(organizationPanel);
    }
}
