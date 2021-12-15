package thederpgamer.betterfactions.gui.faction.management;

import api.network.packets.PacketUtil;
import api.utils.gui.GUIInputDialog;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;
import thederpgamer.betterfactions.network.client.ModifyFactionMessagePacket;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/22/2021]
 */
public class FactionMessageReceiveDialog extends GUIInputDialog {

    private final FactionMessage message;

    public FactionMessageReceiveDialog(FactionMessage message) {
        this.message = message;
    }

    @Override
    public FactionMessageReceivePanel createPanel() {
        return new FactionMessageReceivePanel(getState(), this);
    }

    @Override
    public FactionMessageReceivePanel getInputPanel() {
        return (FactionMessageReceivePanel) super.getInputPanel();
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
                    getInputPanel().markRead();
                    deactivate();
                    break;
            }
        }
    }
}
