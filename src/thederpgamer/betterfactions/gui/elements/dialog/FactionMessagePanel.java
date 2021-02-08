package thederpgamer.betterfactions.gui.elements.dialog;

import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

/**
 * FactionMessagePanel.java
 * <Description>
 * ==================================================
 * Created 02/08/2021
 * @author TheDerpGamer
 */
public class FactionMessagePanel extends GUIInputPanel {

    private String title;
    private Faction from;
    private Faction to;

    private GUIActivatableTextBar titleTextBar;
    private GUIActivatableTextBar messageTextBar;

    public FactionMessagePanel(InputState inputState, GUICallback callback, String title, Faction from, Faction to) {
        super("FactionMessagePanel", inputState, 750, 450, callback, title, "");
        this.title = title;
        this.from = from;
        this.to = to;
        setOkButtonText(Lng.str("SEND"));
        onInit();
    }

    @Override
    public void onInit() {
        super.onInit();

        titleTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 80, 1, Lng.str("TITLE"), ((GUIDialogWindow) background).getMainContentPane().getContent(0), new DefaultTextCallback(), new DefaultTextChangedCallback());
        titleTextBar.onInit();
        titleTextBar.setTextWithoutCallback(Lng.str(title));
        ((GUIDialogWindow) background).getMainContentPane().getContent(0).attach(titleTextBar);

        ((GUIDialogWindow) background).getMainContentPane().setTextBoxHeightLast(30);
        ((GUIDialogWindow) background).getMainContentPane().addNewTextBox(1);

        messageTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 420, 8, Lng.str("MESSAGE"), ((GUIDialogWindow) background).getMainContentPane().getContent(1), new DefaultTextCallback(), new DefaultTextChangedCallback());
        messageTextBar.onInit();
        ((GUIDialogWindow) background).getMainContentPane().getContent(1).attach(messageTextBar);
    }

    public boolean isValid() {
        return !titleTextBar.getText().equals("");
    }

    public void sendMessage() {
        //Todo
    }

    private class DefaultTextChangedCallback implements OnInputChangedCallback {
        @Override
        public String onInputChanged(String t) {
            return t;
        }
    }

    private class DefaultTextCallback implements TextCallback {
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
