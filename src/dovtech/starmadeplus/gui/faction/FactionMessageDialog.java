package dovtech.starmadeplus.gui.faction;

import dovtech.starmadeplus.faction.diplo.relations.FactionMessage;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

public class FactionMessageDialog extends GUIInputPanel {

    private FactionMessage message;
    private GUIActivatableTextBar subjectTextBar;
    private GUIActivatableTextBar messageTextBar;

    public FactionMessageDialog(InputState state, GUICallback callback, Faction from, Faction to, FactionMessage.MessageType messageType) {
        super("FactionMessagePanelDialog", state, 750, 320, callback, "Send Message", "");
        this.message = new FactionMessage(from, to, messageType);
        setOkButtonText("SEND");
    }

    @Override
    public void onInit() {
        super.onInit();
        subjectTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 100, 1, "SUBJECT", ((GUIDialogWindow) background).getMainContentPane().getContent(0), new DefaultTextCallback(), new DefaultTextChangedCallback());
        subjectTextBar.onInit();
        subjectTextBar.setText(message.getSubject());
        ((GUIDialogWindow) background).getMainContentPane().getContent(0).attach(subjectTextBar);
        ((GUIDialogWindow) background).getMainContentPane().setTextBoxHeightLast(52);
        ((GUIDialogWindow) background).getMainContentPane().addNewTextBox(1);

        messageTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 500, 10, "MESSAGE", ((GUIDialogWindow) background).getMainContentPane().getContent(1), new DefaultTextCallback(), new DefaultTextChangedCallback());
        messageTextBar.onInit();
        messageTextBar.setText(message.getMessage());
        ((GUIDialogWindow) background).getMainContentPane().getContent(1).attach(messageTextBar);
    }

    public void sendMessage() {
        //Todo: Send Message
    }

    public class DefaultTextChangedCallback implements OnInputChangedCallback {
        @Override
        public String onInputChanged(String t) {
            return t;
        }
    }

    public class DefaultTextCallback implements TextCallback {
        @Override
        public String[] getCommandPrefixes() {
            return null;
        }

        @Override
        public String handleAutoComplete(String s, TextCallback callback,
                                         String prefix) throws PrefixNotFoundException {
            return getState().onAutoComplete(s, callback, "#");
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
}
