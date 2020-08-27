package dovtech.betterfactions.gui.controlmanagers.viewclaimantspanel;

import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.gui.contracts.ContractClaimantsScrollableList;
import dovtech.betterfactions.gui.contracts.PlayerContractsScrollableList;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.input.InputState;

public class ClaimantsMenuPanelNew extends GUIMainWindow implements GUIActiveInterface {

    private ClaimantsMenuPanel managerPanel;
    private GUIContentPane claimantsPane;
    private boolean init;
    private Contract contract;

    public ClaimantsMenuPanelNew(InputState state, ClaimantsMenuPanel managerPanel, Contract contract) {
        super(state, 800, 500, "CONTRACTCLAIMANTSPANEL");
        this.managerPanel = managerPanel;
        this.contract = contract;
    }

    @Override
    public void cleanUp() {
        claimantsPane.cleanUp();
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

        claimantsPane = addTab("CLAIMANTS");

        createClaimantsPane();
    }

    private void createClaimantsPane() {
        claimantsPane.setTextBoxHeightLast(300);
        ContractClaimantsScrollableList contractClaimantsList = new ContractClaimantsScrollableList(super.getState(), 500, 300, claimantsPane.getContent(0), contract);
        contractClaimantsList.onInit();
        claimantsPane.getContent(0).attach(contractClaimantsList);
    }
}
