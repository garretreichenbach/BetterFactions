package dovtech.betterfactions.gui.controlmanagers.contractpanel;

import dovtech.betterfactions.gui.contracts.PlayerContractsScrollableList;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.input.InputState;

public class ContractMenuPanelNew extends GUIMainWindow implements GUIActiveInterface {

    private ContractMenuPanel managerPanel;
    private GUIContentPane contractsPane;
    private GUIContentPane historyPane;
    private boolean init;

    public ContractMenuPanelNew(InputState state, ContractMenuPanel managerPanel) {
        super(state, 800, 500, "PLAYERCONTRACTSPANEL");
        this.managerPanel = managerPanel;
    }

    @Override
    public void cleanUp() {
        contractsPane.cleanUp();
        historyPane.cleanUp();
    }

    @Override
    public void draw() {
        if(!init) {
            onInit();

        }
        super.draw();
    }

    @Override
    public void update(Timer timer) {
    }

    @Override
    public void onInit() {
        super.onInit();

        recreateTabs();
        orientate(ORIENTATION_HORIZONTAL_MIDDLE | ORIENTATION_VERTICAL_MIDDLE);

        init = true;
    }

    @Override
    public GameClientState getState() {
        return ((GameClientState) super.getState());
    }

    public void recreateTabs() {
        clearTabs();

        contractsPane = addTab("CONTRACTS");
        contractsPane.setName("CONTRACTS");
        historyPane = addTab("HISTORY");
        historyPane.setName("HISTORY");

        createContractsPane();
        createHistoryPane();
    }

    private void createContractsPane() {
        contractsPane.setTextBoxHeightLast(300);
        PlayerContractsScrollableList playerContractsList = new PlayerContractsScrollableList(super.getState(), 500, 300, contractsPane.getContent(0));
        playerContractsList.onInit();
        contractsPane.getContent(0).attach(playerContractsList);
    }

    private void createHistoryPane() {
        historyPane.setTextBoxHeightLast(300);
        //Todo
    }
}
