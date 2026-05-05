package videogoose.betterfactions.gui.faction.diplomacy.war;

import api.network.packets.PacketUtil;
import api.utils.gui.GUIInputDialogPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.data.persistent.federation.FactionMessage;
import videogoose.betterfactions.data.serializeable.war.WarGoalData;
import videogoose.betterfactions.network.client.SendFactionMessagePacket;

/**
 * Panel for selecting a war goal and declaring war on a faction.
 */
public class WarDeclarationPanel extends GUIInputDialogPanel {

    private Faction from;
    private Faction to;
    private WarGoalData.WarGoalType selectedWarGoal;
    private GUIActivatableTextBar messageBar;
    private GUITextOverlay headerOverlay;

    public WarDeclarationPanel(InputState inputState, GUICallback guiCallback) {
        super(inputState, "WarDeclarationPanel", "DECLARE WAR", "", 500, 350, guiCallback);
        setOkButtonText("DECLARE");
    }

    public void createPanel(Faction from, Faction to) {
        this.from = from;
        this.to = to;
        this.selectedWarGoal = WarGoalData.WarGoalType.SHOW_SUPERIORITY; // Default

        GUIDialogWindow dialog = (GUIDialogWindow) background;

        // Header
        headerOverlay = new GUITextOverlay(30, 30, getState());
        headerOverlay.onInit();
        headerOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        headerOverlay.setTextSimple("Declare war on " + to.getName());
        dialog.getMainContentPane().getContent(0).attach(headerOverlay);
        dialog.getMainContentPane().setTextBoxHeightLast(30);

        // War goal selection
        dialog.getMainContentPane().addNewTextBox(200);
        GUIElementList goalList = new GUIElementList(getState());
        goalList.onInit();

        for (WarGoalData.WarGoalType goalType : WarGoalData.WarGoalType.values()) {
            if (!goalType.selectable) continue;
            GUITextOverlay label = new GUITextOverlay(10, 10, getState());
            label.onInit();
            label.setFont(FontLibrary.FontSize.SMALL.getFont());
            String prefix = goalType == selectedWarGoal ? "> " : "  ";
            label.setTextSimple(prefix + goalType.displayName);
            label.setMouseUpdateEnabled(true);

            GUIListElement element = new GUIListElement(label, getState());
            element.onInit();
            element.setUserPointer(goalType);
            element.setMouseUpdateEnabled(true);
            element.setCallback(new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        selectedWarGoal = goalType;
                        getState().getController().queueUIAudio("0022_menu_ui - select 2");
                        updateGoalSelection(goalList);
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            });
            goalList.add(element);
        }
        dialog.getMainContentPane().getContent(1).attach(goalList);

        // Message input
        dialog.getMainContentPane().addNewTextBox(60);
        messageBar = new GUIActivatableTextBar(
            getState(), FontLibrary.FontSize.SMALL, 420, 3, "Message (optional)",
            dialog.getMainContentPane().getContent(2),
            new OnInputChangedCallback() {
                @Override
                public String onInputChanged(String s) { return s; }
            },
            new TextCallback() {
                @Override
                public String[] getCommandPrefixes() { return null; }
                @Override
                public String handleAutoComplete(String s, TextCallback cb, String prefix) throws PrefixNotFoundException { return null; }
                @Override
                public void onFailedTextCheck(String msg) {}
                @Override
                public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {}
                @Override
                public void newLine() {}
            }
        );
        messageBar.onInit();
        dialog.getMainContentPane().getContent(2).attach(messageBar);
    }

    private void updateGoalSelection(GUIElementList goalList) {
        for (GUIElement child : goalList.getChilds()) {
            if (child instanceof GUIListElement listElement) {
                WarGoalData.WarGoalType goalType = (WarGoalData.WarGoalType) listElement.getUserPointer();
                GUITextOverlay label = (GUITextOverlay) listElement.getContent();
                String prefix = goalType == selectedWarGoal ? "> " : "  ";
                label.setTextSimple(prefix + goalType.displayName);
            }
        }
    }

    public void declareWar() {
        if (from == null || to == null || selectedWarGoal == null) return;
        String msg = messageBar != null ? messageBar.getText() : "";
        String title = from.getName() + " declares war on " + to.getName() + " (" + selectedWarGoal.displayName + ")";
        // Encode war goal type in message metadata using a prefix
        String fullMessage = "[WAR_GOAL:" + selectedWarGoal.name() + "]" + msg;
        FactionMessage message = new FactionMessage(from, to, title, fullMessage, FactionMessage.MessageType.DECLARE_WAR);
        PacketUtil.sendPacketToServer(new SendFactionMessagePacket(message));
    }

    public WarGoalData.WarGoalType getSelectedWarGoal() {
        return selectedWarGoal;
    }
}
