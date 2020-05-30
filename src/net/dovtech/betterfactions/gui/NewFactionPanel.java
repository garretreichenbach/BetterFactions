package net.dovtech.betterfactions.gui;

import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.schine.graphicsengine.forms.gui.GUIListElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContextPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUITabbedContent;
import org.schema.schine.input.InputState;

public class NewFactionPanel extends FactionPanelNew {

    private GUIContentPane organization;

    public NewFactionPanel(InputState inputState) {
        super(inputState);
    }

    @Override
    public void recreateTabs() {
        super.recreateTabs();
        organization = this.factionPanel.addTab("ORGANIZATION");
        createOrgPanel();
    }

    private void createOrgPanel() {
        organization.setName("ORGANIZATION");
        
        GUITabbedContent organizationPanel = new GUITabbedContent(organization.getState(), organization);

        //Organization Info
        GUIContextPane orgInfo = new GUIContextPane(organization.getState(), 4.0F, 4.0F);
        GUIListElement memberFactions = new GUIListElement(organization.getState());

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
