package videogoose.betterfactions.gui.faction.diplomacy;

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
import videogoose.betterfactions.data.serializeable.DiplomaticData;
import videogoose.betterfactions.data.serializeable.DiplomaticData.DiplomaticDataType;
import videogoose.betterfactions.network.client.SendFactionMessagePacket;

import java.util.ArrayList;

/**
 * Panel for composing non-hostile demands to another faction.
 * Demands can request territory, credits, resources, or diplomatic changes
 * without declaring war. Rejected demands generate a casus belli.
 */
public class DemandPanel extends GUIInputDialogPanel {

    private Faction from;
    private Faction to;
    private final ArrayList<DiplomaticData> selectedDemands = new ArrayList<>();

    private GUIElementList demandTypeList;
    private GUIElementList selectedList;
    private GUIActivatableTextBar messageBar;

    public DemandPanel(InputState inputState, GUICallback guiCallback) {
        super(inputState, "DemandPanel", "SEND DEMAND", "", 550, 350, guiCallback);
        setOkButtonText("SEND");
    }

    public void createPanel(Faction from, Faction to) {
        this.from = from;
        this.to = to;

        GUIDialogWindow dialog = (GUIDialogWindow) background;

        // Header
        GUITextOverlay header = new GUITextOverlay(30, 30, getState());
        header.onInit();
        header.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        header.setTextSimple("Demand from " + from.getName() + " to " + to.getName());
        dialog.getMainContentPane().getContent(0).attach(header);
        dialog.getMainContentPane().setTextBoxHeightLast(30);

        // Split: demand types (left) | selected (right)
        dialog.getMainContentPane().addDivider((int) ((dialog.getMainContentPane().getWidth() - 28) / 2));

        // Left: available demand types
        dialog.getMainContentPane().addNewTextBox(0, 200);
        demandTypeList = new GUIElementList(getState());
        demandTypeList.onInit();
        populateDemandTypes();
        dialog.getMainContentPane().getContent(0, 1).attach(demandTypeList);

        // Right: selected demands
        dialog.getMainContentPane().addNewTextBox(1, 200);
        selectedList = new GUIElementList(getState());
        selectedList.onInit();
        dialog.getMainContentPane().getContent(1, 0).attach(selectedList);

        // Message input
        dialog.getMainContentPane().addNewTextBox(60);
        messageBar = new GUIActivatableTextBar(
            getState(), FontLibrary.FontSize.SMALL, 480, 3, "Message (optional)",
            dialog.getMainContentPane().getContent(dialog.getMainContentPane().getContentCount() - 1),
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
        dialog.getMainContentPane().getContent(dialog.getMainContentPane().getContentCount() - 1).attach(messageBar);
    }

    private void populateDemandTypes() {
        demandTypeList.clear();
        // Only show demand types (not offers — those are for peace deals)
        DiplomaticDataType[] demandTypes = {
            DiplomaticDataType.DEMAND_TERRITORY,
            DiplomaticDataType.DEMAND_CREDITS,
            DiplomaticDataType.DEMAND_RESOURCES,
            DiplomaticDataType.DEMAND_DIPLO
        };

        for (DiplomaticDataType type : demandTypes) {
            GUITextOverlay label = new GUITextOverlay(10, 10, getState());
            label.onInit();
            label.setFont(FontLibrary.FontSize.SMALL.getFont());
            label.setTextSimple("+ " + type.display);
            label.setMouseUpdateEnabled(true);

            GUIListElement element = new GUIListElement(label, getState());
            element.onInit();
            element.setUserPointer(type);
            element.setMouseUpdateEnabled(true);
            element.setCallback(new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        addDemand(type);
                        getState().getController().queueUIAudio("0022_menu_ui - select 2");
                    }
                }

                @Override
                public boolean isOccluded() { return false; }
            });
            demandTypeList.add(element);
        }
    }

    private void addDemand(DiplomaticDataType type) {
        DiplomaticData data = new DiplomaticData(type);
        //TODO: For territory/credits/resources, open a sub-dialog to specify values
        selectedDemands.add(data);
        refreshSelectedList();
    }

    private void removeDemand(DiplomaticData data) {
        selectedDemands.remove(data);
        refreshSelectedList();
    }

    private void refreshSelectedList() {
        selectedList.clear();
        for (DiplomaticData data : selectedDemands) {
            GUITextOverlay label = new GUITextOverlay(10, 10, getState());
            label.onInit();
            label.setFont(FontLibrary.FontSize.SMALL.getFont());
            label.setTextSimple("x " + data.toString());
            label.setMouseUpdateEnabled(true);

            GUIListElement element = new GUIListElement(label, getState());
            element.onInit();
            element.setUserPointer(data);
            element.setMouseUpdateEnabled(true);
            element.setCallback(new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if (mouseEvent.pressedLeftMouse()) {
                        removeDemand(data);
                        getState().getController().queueUIAudio("0022_menu_ui - select 2");
                    }
                }

                @Override
                public boolean isOccluded() { return false; }
            });
            selectedList.add(element);
        }
    }

    public void sendDemand() {
        if (from == null || to == null || selectedDemands.isEmpty()) return;
        String title = from.getName() + " demands concessions from " + to.getName();
        String msg = messageBar != null ? messageBar.getText() : "";

        // Encode demand types in message for server processing
        StringBuilder demandDesc = new StringBuilder();
        for (DiplomaticData d : selectedDemands) {
            if (!demandDesc.isEmpty()) demandDesc.append(", ");
            demandDesc.append(d.type.display);
        }
        String fullMessage = "[DEMANDS:" + demandDesc + "]" + msg;

        FactionMessage message = new FactionMessage(from, to, title, fullMessage, FactionMessage.MessageType.DEMAND_CONCESSION);
        PacketUtil.sendPacketToServer(new SendFactionMessagePacket(message));
    }
}
