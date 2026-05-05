package videogoose.betterfactions.gui.faction.diplomacy.war;

import api.common.GameClient;
import api.network.packets.PacketUtil;
import api.utils.gui.GUIInputDialogPanel;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;
import videogoose.betterfactions.data.persistent.faction.FactionData;
import videogoose.betterfactions.data.persistent.federation.FactionMessage;
import videogoose.betterfactions.data.persistent.federation.PeaceOfferMessage;
import videogoose.betterfactions.data.serializeable.DiplomaticData;
import videogoose.betterfactions.data.serializeable.PeaceOfferData;
import videogoose.betterfactions.data.serializeable.war.WarData;
import videogoose.betterfactions.manager.FactionManager;
import videogoose.betterfactions.network.client.SendFactionMessagePacket;
import videogoose.betterfactions.utils.FactionDiplomacyUtils;

import java.util.ArrayList;

/**
 * Panel for composing peace deal offers during a war.
 */
public class PeaceDealPanel extends GUIInputDialogPanel {

    private WarData warData;
    private FactionData from;
    private FactionData to;
    private final ArrayList<DiplomaticData> dataList = new ArrayList<>();

    private GUIActivatableTextBar messageBar;
    private GUIElementList leftSide;
    private GUIElementList rightSide;

    public PeaceDealPanel(InputState inputState, GUICallback guiCallback) {
        super(inputState, "PeaceDealPanel", "OFFER PEACE", "", 500, 300, guiCallback);
        setOkButtonText("SEND");
    }

    public void createPanel(WarData warData) {
        this.warData = warData;
        FactionData playerFactionData = FactionManager.getPlayerFactionData(GameClient.getClientPlayerState().getName());
        if (playerFactionData == null || !warData.isInvolved(playerFactionData)) return;

        // Determine from/to based on which side the player is on
        if (warData.defenders.containsKey(playerFactionData.getFactionId())) {
            this.from = playerFactionData;
            this.to = FactionDiplomacyUtils.getAttackerLeader(warData);
        } else if (warData.attackers.containsKey(playerFactionData.getFactionId())) {
            this.from = playerFactionData;
            this.to = FactionDiplomacyUtils.getDefenderLeader(warData);
        } else {
            return;
        }

        GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();

        // Message input
        contentPane.addNewTextBox(0, 30);
        messageBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 420, 8, "Peace terms...", contentPane.getContent(0), new MessageTextChangedCallback(), new MessageTextCallback());
        messageBar.onInit();
        contentPane.getContent(0).attach(messageBar);

        // Current demands/offers
        contentPane.addNewTextBox(1, (int) ((contentPane.getHeight() - 28) / 3));

        // Participants
        contentPane.addDivider((int) ((contentPane.getWidth() - 28) / 2));
        (leftSide = new GUIElementList(getState())).onInit();
        (rightSide = new GUIElementList(getState())).onInit();

        //TODO: Populate left/right sides with participant war goals and demands (Phase 2.3)
    }

    private GUIListElement createOverlay(DiplomaticData diplomaticData, GUICallback callback) {
        GUITextOverlay textOverlay = new GUITextOverlay(50, 12, getState());
        textOverlay.onInit();
        textOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        textOverlay.setTextSimple(diplomaticData.display);
        textOverlay.setUserPointer(diplomaticData.toString());
        textOverlay.setMouseUpdateEnabled(true);
        GUIListElement element = new GUIListElement(textOverlay, getState());
        element.onInit();
        element.setUserPointer(diplomaticData.toString());
        element.setMouseUpdateEnabled(true);
        element.setCallback(callback);
        return element;
    }

    public void sendMessage() {
        if (from == null || to == null) return;
        String title = messageBar != null ? messageBar.getText() : "Peace Offer";
        FactionMessage message = new PeaceOfferMessage(
            from.getFaction(), to.getFaction(), title,
            new PeaceOfferData(from, to, dataList)
        );
        PacketUtil.sendPacketToServer(new SendFactionMessagePacket(message));
    }

    private class MessageTextChangedCallback implements OnInputChangedCallback {
        @Override
        public String onInputChanged(String s) {
            return s;
        }
    }

    private class MessageTextCallback implements TextCallback {
        @Override
        public String[] getCommandPrefixes() { return null; }

        @Override
        public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
            return null;
        }

        @Override
        public void onFailedTextCheck(String msg) {}

        @Override
        public void onTextEnter(String entry, boolean send, boolean onAutoComplete) {}

        @Override
        public void newLine() {}
    }
}
