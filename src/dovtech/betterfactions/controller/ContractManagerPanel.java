package dovtech.betterfactions.controller;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;

public class ContractManagerPanel extends PlayerInput implements GUIActiveInterface {

    private ContractManagerPanelNew panel;

    public ContractManagerPanel(GameClientState gameClientState) {
        super(gameClientState);
        this.panel = new ContractManagerPanelNew(gameClientState, this);

        this.panel.setCloseCallback(new GUICallback() {
            @Override
            public void callback(GUIElement callingGuiElement, MouseEvent event) {
                if(event.pressedLeftMouse()) {
                    deactivate();
                }
            }

            @Override
            public boolean isOccluded() {
                return !isActive();
            }


        });

        this.panel.reset();
        this.panel.activeInterface = this;
    }

    @Override
    public void onDeactivate() {
        panel.cleanUp();
    }

    @Override
    public GUIElement getInputPanel() {
        return panel;
    }

    @Override
    public void update(Timer timer) {
        if(panel != null) {
            panel.update(timer);
        }
    }

    @Override
    public void handleMouseEvent(MouseEvent mouseEvent) {

    }

    @Override
    public boolean isActive() {
        return getState().getController().getPlayerInputs().isEmpty() || getState().getController().getPlayerInputs().get(getState().getController().getPlayerInputs().size() - 1).getInputPanel() == panel;
    }
}
