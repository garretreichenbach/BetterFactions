package dovtech.betterfactions.gui.faction;

import dovtech.betterfactions.BetterFactions;
import dovtech.betterfactions.gui.faction.alliance.AllianceInfoBox;
import dovtech.betterfactions.gui.faction.alliance.AllianceResourcesBox;
import dovtech.betterfactions.gui.faction.alliance.AlliancesScrollableList;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.input.InputState;

public class NoFactionPanel extends FactionPanelNew {

    private InputState inputState;
    private GUIContentPane allianceTab;
    private GUIContentPane warsTab;

    public NoFactionPanel(InputState inputState) {
        super(inputState);
        this.inputState = inputState;
    }

    @Override
    public void recreateTabs() {
        super.recreateTabs();

        //BetterFactions.getInstance().testFunction();

        modTabs();
        //createAllianceTab();
        //createWarsTab();
    }

    private void modTabs() {
        for(GUIContentPane tab : factionPanel.getTabs()) {
            if(tab.getTabName().toString().contains("NPC DIPLOMACY")) {
                tab.setName("FACTION DIPLOMACY");
                tab.setTabName("FACTION DIPLOMACY");
            } else if(tab.getTabName().toString().contains("LIST")) {
                tab.setName("FACTION LIST");
                tab.setTabName("FACTION LIST");
            }
        }
    }

    private void createAllianceTab() {
        allianceTab = this.factionPanel.addTab("ALLIANCE");
        allianceTab.setName("ALLIANCE");
        allianceTab.setTextBoxHeightLast(300);
        allianceTab.addDivider(300);
        allianceTab.addNewTextBox(0, 70);
        allianceTab.addNewTextBox(1, 70);

        AllianceInfoBox allianceInfoBox = new AllianceInfoBox(allianceTab.getState(), 130, 70);
        allianceTab.getContent(0, 0).attach(allianceInfoBox);

        AllianceResourcesBox allianceResourcesBox = new AllianceResourcesBox(allianceTab.getState());
        allianceTab.getContent(1, 1).attach(allianceResourcesBox);

        AlliancesScrollableList alliancesScrollableList = new AlliancesScrollableList(allianceTab.getState(), 150, 100, allianceTab.getContent(1, 0));
        alliancesScrollableList.onInit();
        allianceTab.getContent(1, 0).attach(alliancesScrollableList);
    }

    private void createWarsTab() {
        warsTab = this.factionPanel.addTab("WARS");
        warsTab.setName("WARS");
        warsTab.setTextBoxHeightLast(300);
    }
}
