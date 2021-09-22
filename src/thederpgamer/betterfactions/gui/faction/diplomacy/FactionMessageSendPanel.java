package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.utils.gui.GUIInputDialogPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/16/2021]
 */
public class FactionMessageSendPanel extends GUIInputDialogPanel {

    private final Faction from;
    private final Faction to;
    private final FactionMessage.MessageType messageType;
    private GUITextOverlay messageOverlay;
    private GUIActivatableTextBar titleTextBar;
    private GUIActivatableTextBar messageTextBar;

    public FactionMessageSendPanel(InputState inputState, String title, Faction from, Faction to, FactionMessage.MessageType messageType, GUICallback guiCallback) {
        super(inputState, "FactionMessageSendPanel", title, "", 500, 300, guiCallback);
        this.from = from;
        this.to = to;
        this.messageType = messageType;
        setOkButtonText("SEND");
    }

    @Override
    public void onInit() {
        (messageOverlay = new GUITextOverlay(30, 30, getState())).onInit();
        messageOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        messageOverlay.setTextSimple(messageType.getDisplayFormatted() + " to " + to.getName() + ":");
        ((GUIDialogWindow) background).getMainContentPane().getContent(0).attach(messageOverlay);

        ((GUIDialogWindow) background).getMainContentPane().addNewTextBox(0, 1);
        (titleTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 80, 1, "Title", ((GUIDialogWindow) background).getMainContentPane().getContent(1), new FactionMessageTextCallback(), new FactionMessageTextChangedCallback())).onInit();
        ((GUIDialogWindow) background).getMainContentPane().getContent(1).attach(titleTextBar);
        ((GUIDialogWindow) background).getMainContentPane().setTextBoxHeightLast(26);

        ((GUIDialogWindow) background).getMainContentPane().addNewTextBox(1, 1);
        (messageTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 420, 8, "Message", ((GUIDialogWindow) background).getMainContentPane().getContent(2), new FactionMessageTextCallback(), new FactionMessageTextChangedCallback())).onInit();
        ((GUIDialogWindow) background).getMainContentPane().getContent(2).attach(messageTextBar);
    }

    public String getTitleText() {
        return titleTextBar.getText();
    }

    public String getMessageText() {
        return messageTextBar.getText();
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