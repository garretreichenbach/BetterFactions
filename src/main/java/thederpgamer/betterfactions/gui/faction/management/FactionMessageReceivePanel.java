package thederpgamer.betterfactions.gui.faction.management;

import api.common.GameCommon;
import api.network.packets.PacketUtil;
import api.utils.gui.GUIInputDialogPanel;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;
import thederpgamer.betterfactions.network.client.ModifyFactionMessagePacket;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/22/2021]
 */
public class FactionMessageReceivePanel extends GUIInputDialogPanel {

    private FactionMessage message;
    private GUITextOverlay messageOverlay;
    private GUIActivatableTextBar titleTextBar;
    private GUIActivatableTextBar messageTextBar;

    public FactionMessageReceivePanel(InputState inputState, GUICallback guiCallback) {
        super(inputState, "FactionMessageReceivePanel", "FACTION MESSAGE", "", 500, 300, guiCallback);
        setOkButtonText("OK");
        setCancelButton(false);
    }

    public void markRead() {
        if(message != null) {
            message.read = true;
            PacketUtil.sendPacketToServer(new ModifyFactionMessagePacket(message, FactionMessage.MARK_READ));
        }
    }

    public void createPanel(FactionMessage message) {
        this.message = message;
        (messageOverlay = new GUITextOverlay(30, 30, getState())).onInit();
        messageOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        String name = GameCommon.getGameState().getFactionManager().getFactionName(message.fromId);
        if(message.fromId == FactionManager.TRAIDING_GUILD_ID) name = "Trading Guild";
        messageOverlay.setTextSimple(message.messageType.getDisplayFormatted() + " from " + name);
        ((GUIDialogWindow) background).getMainContentPane().getContent(0).attach(messageOverlay);
        ((GUIDialogWindow) background).getMainContentPane().setTextBoxHeightLast(30);

        ((GUIDialogWindow) background).getMainContentPane().addNewTextBox(26);
        titleTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 80, 1, "Title", ((GUIDialogWindow) background).getMainContentPane().getContent(1), new FactionMessageTextCallback(), new FactionMessageTextChangedCallback()) {
            @Override
            public void callback(GUIElement element, MouseEvent mouseEvent) {
                //Don't allow editing
            }
        };
        titleTextBar.onInit();
        titleTextBar.setTextWithoutCallback(message.title);
        ((GUIDialogWindow) background).getMainContentPane().getContent(0, 1).attach(titleTextBar);
        ((GUIDialogWindow) background).getMainContentPane().setTextBoxHeightLast(28);

        ((GUIDialogWindow) background).getMainContentPane().addNewTextBox(0, 100);
        messageTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 500, 10, "Message", ((GUIDialogWindow) background).getMainContentPane().getContent(2), new FactionMessageTextCallback(), new FactionMessageTextChangedCallback()) {
            @Override
            public void callback(GUIElement element, MouseEvent mouseEvent) {
                //Don't allow editing
            }
        };
        messageTextBar.onInit();
        messageTextBar.setTextWithoutCallback(message.message);
        ((GUIDialogWindow) background).getMainContentPane().getContent(0, 2).attach(messageTextBar);
    }

    private static class FactionMessageTextCallback implements TextCallback {
        @Override
        public String[] getCommandPrefixes() {
            return null;
        }

        @Override
        public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
            return "";
        }

        @Override
        public void onFailedTextCheck(String msg) {

        }

        @Override
        public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {

        }

        @Override
        public void newLine() {

        }
    }

    private static class FactionMessageTextChangedCallback implements OnInputChangedCallback {
        @Override
        public String onInputChanged(String t) {
            return t;
        }
    }
}
