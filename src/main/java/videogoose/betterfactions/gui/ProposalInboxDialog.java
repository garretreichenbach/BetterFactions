package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.KeyEventInterface;

public class ProposalInboxDialog extends PlayerInput {

	private ProposalInboxPanel panel;

	public ProposalInboxDialog(GameClientState state) {
		super(state);

		panel = new ProposalInboxPanel(state, this);
		panel.setOkButton(false);
	}

	@Override
	public void callback(GUIElement callingGuiElement, MouseEvent event) {
		if (event.pressedLeftMouse()) {
			if (callingGuiElement.getUserPointer().equals("OK")) {
				cancel();
			} else if (callingGuiElement.getUserPointer().equals("CANCEL") || callingGuiElement.getUserPointer().equals("X")) {
				cancel();
			}
		}
	}

	@Override
	public void handleKeyEvent(KeyEventInterface e) {
		super.handleKeyEvent(e);
		// TODO Auto-generated method stub

	}

	@Override
	public GUIElement getInputPanel() {
		return panel;
	}

	@Override
	public void onDeactivate() {
		getState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager()
				.getFactionControlManager().hinderInteraction(500);
		getState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager()
				.getFactionControlManager().suspend(false);
	}

	@Override
	public boolean isOccluded() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void handleMouseEvent(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}
