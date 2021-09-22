package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.network.packets.PacketUtil;
import api.utils.gui.GUIInputDialog;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;
import thederpgamer.betterfactions.network.client.SendFactionMessagePacket;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/16/2021]
 */
public class FactionMessageSendDialog extends GUIInputDialog {

    private final String title;
    private final Faction from;
    private final Faction to;
    private final FactionMessage.MessageType messageType;

    public FactionMessageSendDialog(String title, Faction from, Faction to, FactionMessage.MessageType messageType) {
        this.title = title;
        this.from = from;
        this.to = to;
        this.messageType = messageType;
    }

    @Override
    public FactionMessageSendPanel createPanel() {
        return new FactionMessageSendPanel(getState(), title, from, to, messageType, this);
    }

    @Override
    public FactionMessageSendPanel getInputPanel() {
        return (FactionMessageSendPanel) super.getInputPanel();
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
                    sendMessage();
                    deactivate();
                    break;
            }
        }
    }

    public String getTitleText() {
        return getInputPanel().getTitleText();
    }

    public String getMessageText() {
        return getInputPanel().getMessageText();
    }

    public void sendMessage() {
        FactionMessage message = new FactionMessage(from, to, getTitleText(), getMessageText(), messageType);
        PacketUtil.sendPacketToServer(new SendFactionMessagePacket(message));
    }
}
