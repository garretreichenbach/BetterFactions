package dovtech.betterfactions.controller;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.controller.manager.AbstractControlManager;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.input.KeyEventInterface;

public class ContractsMenuControlManager extends AbstractControlManager {

    private ContractManagerPanel managerPanel;

    public ContractsMenuControlManager(GameClientState gameClientState) {
        super(gameClientState);
    }

    @Override
    public void handleKeyEvent(KeyEventInterface e) {
        super.handleKeyEvent(e);
    }

    @Override
    public void handleMouseEvent(MouseEvent e) {
        super.handleMouseEvent(e);
        PlayerInput.lastDialougeClick = (System.currentTimeMillis());
    }

    @Override
    public void onSwitch(boolean active) {
        if(active) {
            this.managerPanel = new ContractManagerPanel(getState());
            getState().getController().getPlayerInputs().add(this.managerPanel);
        } else {
            PlayerInput.lastDialougeClick = (System.currentTimeMillis());
            managerPanel.deactivate();
        }

        super.onSwitch(active);
    }
}
