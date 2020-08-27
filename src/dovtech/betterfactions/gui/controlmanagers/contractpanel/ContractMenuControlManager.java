package dovtech.betterfactions.gui.controlmanagers.contractpanel;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.controller.manager.AbstractControlManager;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.camera.CameraMouseState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.forms.gui.newgui.DialogInterface;
import org.schema.schine.input.KeyEventInterface;

public class ContractMenuControlManager extends AbstractControlManager {

    private ContractMenuPanel menuPanel;

    public ContractMenuControlManager(GameClientState gameClientState) {
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
        boolean contains = false;
        if(active) {
            synchronized (getState().getController().getPlayerInputs()) {
                for(DialogInterface p : getState().getController().getPlayerInputs()) {
                    if(p instanceof ContractMenuPanel) {
                        contains = true;
                    }
                }
            }
            if(!contains) {
                    this.menuPanel = new ContractMenuPanel(getState());
                    getState().getController().getPlayerInputs().add(this.menuPanel);
            }
        } else {
            PlayerInput.lastDialougeClick = (System.currentTimeMillis());
            menuPanel.deactivate();
            synchronized (getState().getController().getPlayerInputs()) {
                for(int i = 0; i < getState().getController().getPlayerInputs().size(); i++) {
                    DialogInterface p = getState().getController().getPlayerInputs().get(i);
                    if(p instanceof ContractMenuPanel) {
                        getState().getController().getPlayerInputs().get(i).deactivate();
                        break;
                    }
                }
            }
        }

        super.onSwitch(active);
    }

    @Override
    public void update(Timer timer) {
        CameraMouseState.setGrabbed(false);
        super.update(timer);
        if(menuPanel != null) {
            menuPanel.update(timer);
        }
    }
}
