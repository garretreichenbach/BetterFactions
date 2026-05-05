package videogoose.betterfactions.gui.faction.diplomacy.war;

import api.common.GameClient;
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
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.data.persistent.federation.PeaceOfferMessage;
import videogoose.betterfactions.data.serializeable.DiplomaticData;
import videogoose.betterfactions.data.serializeable.DiplomaticData.DiplomaticDataType;
import videogoose.betterfactions.data.serializeable.PeaceOfferData;
import videogoose.betterfactions.data.serializeable.war.WarData;
import videogoose.betterfactions.manager.FactionManager;
import videogoose.betterfactions.network.client.SendFactionMessagePacket;

import java.util.ArrayList;

/**
 * Stellaris-style peace deal negotiation panel.
 * Players can add demands and offers, constrained by war score.
 */
public class PeaceDealPanel extends GUIInputDialogPanel {

    private WarData warData;
    private Faction from;
    private Faction to;
    private boolean isAttacker;
    private final ArrayList<DiplomaticData> selectedDemands = new ArrayList<>();

    private GUIElementList demandList;
    private GUIElementList selectedList;
    private GUITextOverlay warScoreOverlay;
    private GUIActivatableTextBar messageBar;

    public PeaceDealPanel(InputState inputState, GUICallback guiCallback) {
        super(inputState, "PeaceDealPanel", "PEACE DEAL", "", 600, 400, guiCallback);
        setOkButtonText("SEND OFFER");
    }

    public void createPanel(WarData warData) {
        this.warData = warData;
        Faction playerFaction = FactionManager.getFaction(GameClient.getClientPlayerState());
        if (playerFaction == null || !warData.isInvolved(playerFaction.getIdFaction())) return;

        // Determine sides
        isAttacker = warData.attackers.containsKey(playerFaction.getIdFaction());
        this.from = playerFaction;

        // Find opponent leader
        if (isAttacker) {
            this.to = warData.getDefenderLeaderFaction();
        } else {
            this.to = warData.getAttackerLeaderFaction();
        }
        if (to == null) return;

        GUIDialogWindow dialog = (GUIDialogWindow) background;
        GUIContentPane contentPane = dialog.getMainContentPane();

        // Header: war score display
        contentPane.setTextBoxHeightLast(40);
        warScoreOverlay = new GUITextOverlay(30, 30, getState());
        warScoreOverlay.onInit();
        warScoreOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        updateWarScoreDisplay();
        contentPane.getContent(0).attach(warScoreOverlay);

        // Split: available demands (left) | selected demands (right)
        contentPane.addDivider((int) ((contentPane.getWidth() - 28) / 2));

        // Left: available demand types
        contentPane.addNewTextBox(0, 250);
        GUITextOverlay availableHeader = new GUITextOverlay(10, 10, getState());
        availableHeader.onInit();
        availableHeader.setFont(FontLibrary.FontSize.SMALL.getFont());
        availableHeader.setTextSimple("Available Demands:");
        contentPane.getContent(0, 1).attach(availableHeader);

        contentPane.addNewTextBox(0, 200);
        demandList = new GUIElementList(getState());
        demandList.onInit();
        populateAvailableDemands();
        contentPane.getContent(0, 2).attach(demandList);

        // Right: selected demands
        contentPane.addNewTextBox(1, 250);
        GUITextOverlay selectedHeader = new GUITextOverlay(10, 10, getState());
        selectedHeader.onInit();
        selectedHeader.setFont(FontLibrary.FontSize.SMALL.getFont());
        selectedHeader.setTextSimple("Selected Terms:");
        contentPane.getContent(1, 0).attach(selectedHeader);

        contentPane.addNewTextBox(1, 200);
        selectedList = new GUIElementList(getState());
        selectedList.onInit();
        contentPane.getContent(1, 1).attach(selectedList);

        // Bottom: message input
        contentPane.addNewTextBox(50);
        messageBar = new GUIActivatableTextBar(
            getState(), FontLibrary.FontSize.SMALL, 520, 3, "Message (optional)",
            contentPane.getContent(contentPane.getContentCount() - 1),
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
        contentPane.getContent(contentPane.getContentCount() - 1).attach(messageBar);
    }

    private void populateAvailableDemands() {
        demandList.clear();
        for (DiplomaticDataType type : DiplomaticDataType.values()) {
            boolean canSelect = isAttacker ? type.selectableByAttacker : type.selectableByDefender;
            if (!canSelect) continue;

            // Don't show WHITE_PEACE and STATUS_QUO alongside demands
            if (type == DiplomaticDataType.WHITE_PEACE || type == DiplomaticDataType.STATUS_QUO) {
                // Always show these as options
            } else if (!selectedDemands.isEmpty()) {
                // If white peace or status quo is selected, don't show other options
                if (selectedDemands.stream().anyMatch(d ->
                    d.type == DiplomaticDataType.WHITE_PEACE || d.type == DiplomaticDataType.STATUS_QUO)) {
                    continue;
                }
            }

            GUITextOverlay label = new GUITextOverlay(10, 10, getState());
            label.onInit();
            label.setFont(FontLibrary.FontSize.SMALL.getFont());
            String costStr = String.format(" (%.0f%% war score)", type.warScoreCost * 100);
            label.setTextSimple("+ " + type.display + costStr);
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
            demandList.add(element);
        }
    }

    private void addDemand(DiplomaticDataType type) {
        // White peace / status quo are exclusive
        if (type == DiplomaticDataType.WHITE_PEACE || type == DiplomaticDataType.STATUS_QUO) {
            selectedDemands.clear();
        } else {
            // Remove white peace/status quo if adding a real demand
            selectedDemands.removeIf(d -> d.type == DiplomaticDataType.WHITE_PEACE || d.type == DiplomaticDataType.STATUS_QUO);
        }

        DiplomaticData data = new DiplomaticData(type);
        //TODO: For territory/credits/resources, open a sub-dialog to specify values (Phase 2.3 polish)
        selectedDemands.add(data);
        refreshSelectedList();
        populateAvailableDemands();
        updateWarScoreDisplay();
    }

    private void removeDemand(DiplomaticData data) {
        selectedDemands.remove(data);
        refreshSelectedList();
        populateAvailableDemands();
        updateWarScoreDisplay();
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

    private void updateWarScoreDisplay() {
        float ourScore = warData.getTotalProgress(from.getIdFaction());
        float theirScore = to != null ? warData.getTotalProgress(to.getIdFaction()) : 0;
        float totalCost = 0;
        for (DiplomaticData d : selectedDemands) totalCost += d.getWarScoreCost();

        String scoreText = String.format("War Score: %.0f%% vs %.0f%%  |  Demand Cost: %.0f%%",
            ourScore * 100, theirScore * 100, totalCost * 100);
        if (warScoreOverlay != null) warScoreOverlay.setTextSimple(scoreText);
    }

    public void sendMessage() {
        if (from == null || to == null) return;
        if (selectedDemands.isEmpty()) {
            // Default to white peace if nothing selected
            selectedDemands.add(new DiplomaticData(DiplomaticDataType.WHITE_PEACE));
        }
        String title = from.getName() + " proposes peace to " + to.getName();
        String msg = messageBar != null ? messageBar.getText() : "";
        PeaceOfferData offerData = new PeaceOfferData(from.getIdFaction(), to.getIdFaction(), selectedDemands);
        PeaceOfferMessage message = new PeaceOfferMessage(from, to, title, offerData);
        message.message = msg;
        PacketUtil.sendPacketToServer(new SendFactionMessagePacket(message));
    }
}
