package dovtech.betterfactions.gui.faction;

import dovtech.betterfactions.gui.faction.alliance.AllianceInfoBox;
import dovtech.betterfactions.gui.faction.alliance.AllianceResourcesBox;
import dovtech.betterfactions.gui.faction.alliance.AlliancesScrollableList;
import org.schema.game.client.view.gui.faction.newfaction.FactionPanelNew;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.input.InputState;

public class NewFactionPanel extends FactionPanelNew {

    private InputState inputState;
    private GUIContentPane alliance;

    public NewFactionPanel(InputState inputState) {
        super(inputState);
        this.inputState = inputState;
    }

    @Override
    public void recreateTabs() {
        super.recreateTabs();
        modTabs();
        createAlliancePanel();
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

    private void createAlliancePanel() {

        alliance = this.factionPanel.addTab("ALLIANCE");
        alliance.setName("ALLIANCE");
        alliance.setTextBoxHeightLast(300);
        alliance.addDivider(300);
        alliance.addNewTextBox(0, 70);
        alliance.addNewTextBox(1, 70);

        AllianceInfoBox allianceInfoBox = new AllianceInfoBox(alliance.getState());
        alliance.getContent(0, 0).attach(allianceInfoBox);

        AllianceResourcesBox allianceResourcesBox = new AllianceResourcesBox(alliance.getState());
        alliance.getContent(1, 1).attach(allianceResourcesBox);

        AlliancesScrollableList alliancesScrollableList = new AlliancesScrollableList(alliance.getState(), 150, 100, alliance.getContent(1, 0));
        alliancesScrollableList.onInit();
        alliance.getContent(1, 0).attach(alliancesScrollableList);
    }
}
