package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.common.GameClient;
import api.utils.gui.GUIInputDialog;
import api.utils.gui.GUIInputDialogPanel;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.diplomacy.DiplomaticData;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/17/2022]
 */
public class DiplomacyDialog extends GUIInputDialog {

	private final DiplomaticData data;

	public DiplomacyDialog(DiplomaticData data) {
		this.data = data;
	}

	@Override
	public DiplomaticPanel createPanel() {
		return new DiplomaticPanel(GameClient.getClientState(), data);
	}

	@Override
	public DiplomaticPanel getInputPanel() {
		return (DiplomaticPanel) super.getInputPanel();
	}

	@Override
	public void callback(GUIElement element, MouseEvent mouseEvent) {
		if(mouseEvent.pressedLeftMouse()) {
			switch(((String) element.getUserPointer()).toUpperCase()) {
				case "X":
				case "CANCEL":
					deactivate();
					break;
				case "OK":
				case "SEND":
					getInputPanel().sendMessage();
					deactivate();
					break;
			}
		}
	}

	public static class DiplomaticPanel extends GUIInputDialogPanel {

		private final DiplomaticData data;

		public DiplomaticPanel(InputState inputState, DiplomaticData data) {
			super(inputState, "DiplomaticPanel", "DIPLOMACY", "", 700, 500, data);
			this.data = data;
		}

		public void sendMessage() {
			data.send();
		}
	}
}
