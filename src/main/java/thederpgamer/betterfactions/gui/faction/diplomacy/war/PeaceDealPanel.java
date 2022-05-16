package thederpgamer.betterfactions.gui.faction.diplomacy.war;

import api.utils.gui.GUIInputDialogPanel;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.old.diplomacy.peace.PeaceOfferData;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/15/2021]
 */
public class PeaceDealPanel extends GUIInputDialogPanel {

    //private WarData warData;
    private PeaceOfferData peaceOfferData;

    private GUIActivatableTextBar messageBar;
    private GUIElementList leftSide;
    private GUIElementList rightSide;


    public PeaceDealPanel(InputState inputState, GUICallback guiCallback) {
        super(inputState, "PeaceDealPanel", "OFFER PEACE", "", 500, 300, guiCallback);
        setOkButtonText("SEND");
    }

	public void sendMessage() {
        //Todo:
	}

    /*
    public void createPanel(WarData warData) {
        this.warData = warData;
        FactionData playerFactionData = FactionManagerOld.getPlayerFactionData(GameClient.getClientPlayerState().getDisplayName());
        if(playerFactionData != null && warData.isInvolved(playerFactionData)) {
            GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();

            { //Message Input
                contentPane.addNewTextBox(0, 30);
                (messageBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, 420, 8, "", contentPane.getContent(0), new MessageTextCallback(), new MessageTextChangedCallback())).onInit();
                contentPane.getContent(0).attach(messageBar);
            }

            { //Current Demands / Offers
                contentPane.addNewTextBox(1, (int) ((contentPane.getHeight() - 28) / 3));
            }

            { //Participants
                contentPane.addDivider((int) ((contentPane.getWidth() - 28) / 2));

                (leftSide = new GUIElementList(getState())).onInit();
                (rightSide = new GUIElementList(getState())).onInit();

                if(warData.defenders.containsKey(playerFactionData.getFactionId())) { //Defender
                    FactionData warLeader = FactionDiplomacyUtils.getDefenderLeader(warData);
                    for(WarParticipantData participantData : warData.defenders.values()) {
                        switch(participantData.warGoal.warGoalType) {

                        }
                    }
                } else if(warData.attackers.containsKey(playerFactionData.getFactionId())) { //Attacker
                    FactionData warLeader = FactionDiplomacyUtils.getAttackerLeader(warData);
                    WarParticipantData participantData = warData.attackers.get(playerFactionData.getFactionId());
                    if(warLeader.equals(playerFactionData)) {

                    } else {

                    }
                }
            }
        }

        /*
        (warList = new GUIElementList(getState())).onInit();
        for(Map.Entry<String, WarData> entry : ClientCacheManager.factionWars.entrySet()) {
            if(entry.getValue().isInvolved(playerFaction)) {
                GUITextOverlay textOverlay = new GUITextOverlay(50, 12, getState());
                textOverlay.onInit();
                textOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
                textOverlay.setTextSimple(entry.getKey());
                textOverlay.setUserPointer(entry.getValue());
                textOverlay.setMouseUpdateEnabled(true);
                GUIListElement element = new GUIListElement(textOverlay, getState());
                element.onInit();
                element.setUserPointer(entry.getValue());
                element.setMouseUpdateEnabled(true);
                element.setCallback(new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {

                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                });
                warList.add(element);
            }
        }
        contentPane.getContent(0).attach(warList);



         */
        /*
        (messageOverlay = new GUITextOverlay(30, 30, getState())).onInit();
        messageOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        String name = to.getDisplayName();
        if(to.getIdFaction() == FactionManagerOld.TRAIDING_GUILD_ID) name = "Trading Guild";
        messageOverlay.setTextSimple("Peace offer to " + name);
        ((GUIDialogWindow) background).getMainContentPane().getContent(0).attach(messageOverlay);
        ((GUIDialogWindow) background).getMainContentPane().setTextBoxHeightLast(30);

        ((GUIDialogWindow) background).getMainContentPane().addNewTextBox(26);
        (titleTextBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 80, 1, "Title", ((GUIDialogWindow) background).getMainContentPane().getContent(1), new FactionMessageTextCallback(), new FactionMessageTextChangedCallback())).onInit();
        ((GUIDialogWindow) background).getMainContentPane().getContent(0, 1).attach(titleTextBar);
        ((GUIDialogWindow) background).getMainContentPane().setTextBoxHeightLast(28);

    }

    private GUIListElement createOverlay(DiplomaticData diplomaticData, GUICallback callback) {
        GUITextOverlay textOverlay = new GUITextOverlay(50, 12, getState());
        textOverlay.onInit();
        textOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        //textOverlay.setTextSimple(diplomaticData.display);
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
       // FactionMessage message = new PeaceOfferMessage(from.getFaction(), to.getFaction(), getTitleText(), new PeaceOfferData(from, to, dataList));
        //PacketUtil.sendPacketToServer(new SendFactionMessagePacket(message));
    }

    //public String getTitleText() {
        //return titleTextBar.getText();
  //  }

    private class MessageTextChangedCallback implements OnInputChangedCallback {

        @Override
        public String onInputChanged(String s) {
            return s;
        }
    }

    private class MessageTextCallback implements TextCallback {
        @Override
        public String[] getCommandPrefixes() {
            return null;
        }

        @Override
        public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
            return null;
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
    */
}
