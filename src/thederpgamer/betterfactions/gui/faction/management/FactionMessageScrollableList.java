package thederpgamer.betterfactions.gui.faction.management;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.persistent.faction.FactionMember;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;
import thederpgamer.betterfactions.gui.faction.diplomacy.FactionMessageSendDialog;
import thederpgamer.betterfactions.manager.FactionManager;
import thederpgamer.betterfactions.manager.LogManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/22/2021]
 */
public class FactionMessageScrollableList extends ScrollableTableList<FactionMessage> {

    public FactionMessageScrollableList(InputState inputState, GUIAncor anchor) {
        super(inputState, 100, 100, anchor);
        anchor.attach(this);
        ((GameClientState) inputState).getFactionManager().addObserver(this);
    }

    public void redrawList() {
        flagDirty();
        handleDirty();
    }

    @Override
    public ArrayList<FactionMessage> getElementList() {
        ArrayList<FactionMessage> messageList = new ArrayList<>();
        try {
            messageList.addAll(FactionManager.getPlayerFactionData(GameClient.getClientPlayerState().getName()).getInbox());
        } catch(Exception exception) {
            LogManager.logException("Encountered an exception while trying to fetch faction message inbox", exception);
        }
        return messageList;
    }

    @Override
    public void initColumns() {
        addColumn("Title", 15.0f, new Comparator<FactionMessage>() {
            public int compare(FactionMessage o1, FactionMessage o2) {
                return o1.title.compareTo(o2.title);
            }
        });

        addColumn("Type", 12.0f, new Comparator<FactionMessage>() {
            public int compare(FactionMessage o1, FactionMessage o2) {
                return o1.messageType.compareTo(o2.messageType);
            }
        });

        addColumn("From", 10.0f, new Comparator<FactionMessage>() {
            public int compare(FactionMessage o1, FactionMessage o2) {
                String o1Name = GameCommon.getGameState().getFactionManager().getFactionName(o1.fromId);
                String o2Name = GameCommon.getGameState().getFactionManager().getFactionName(o2.fromId);
                return o1Name.compareTo(o2Name);
            }
        });

        addColumn("Date", 8.5f, new Comparator<FactionMessage>() {
            @Override
            public int compare(FactionMessage o1, FactionMessage o2) {
                return Long.compare(o1.date, o2.date);
            }
        });

        addTextFilter(new GUIListFilterText<FactionMessage>() {
            public boolean isOk(String s, FactionMessage message) {
                String fromName = GameCommon.getGameState().getFactionManager().getFactionName(message.fromId);
                return fromName.toLowerCase().contains(s.toLowerCase());
            }
        }, "SENDER", ControllerElement.FilterRowStyle.LEFT);

        addDropdownFilter(new GUIListFilterDropdown<FactionMessage, FactionMessage.MessageType>() {
            @Override
            public boolean isOk(FactionMessage.MessageType messageType, FactionMessage factionMessage) {
                if(messageType.equals(FactionMessage.MessageType.ALL)) return true;
                else return messageType.equals(factionMessage.messageType);
            }
        }, new CreateGUIElementInterface<FactionMessage.MessageType>() {
            @Override
            public GUIElement create(FactionMessage.MessageType messageType) {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(messageType.display);
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(messageType.display);
                anchor.attach(dropDown);
                return anchor;
            }

            @Override
            public GUIElement createNeutral() {
                return null;
            }
        }, ControllerElement.FilterRowStyle.RIGHT);

        activeSortColumnIndex = 3;
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<FactionMessage> set) {
        guiElementList.deleteObservers();
        guiElementList.addObserver(this);
        FactionMember playerFactionMember = FactionManager.getPlayerFactionMember(GameClient.getClientPlayerState().getName());
        assert playerFactionMember != null;
        for(FactionMessage message : set) {
            GUITextOverlayTable titleTextElement;
            (titleTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(message.title);
            GUIClippedRow titleRowElement;
            (titleRowElement = new GUIClippedRow(this.getState())).attach(titleTextElement);

            FactionMessageListRow row = new FactionMessageListRow(getState(), message, titleRowElement);
            if(playerFactionMember.hasPermission("manage.messages.[ANY]")) {
                GUIAncor anchor = new GUIAncor(getState(), 300, 28.0f);
                anchor.attach(redrawButtonPane(message, playerFactionMember, anchor));
                row.expanded = new GUIElementList(getState());
                row.expanded.add(new GUIListElement(anchor, getState()));
                row.expanded.attach(anchor);
            }
            row.onInit();
            guiElementList.addWithoutUpdate(row);
        }
        guiElementList.updateDim();
    }

    private GUIHorizontalButtonTablePane redrawButtonPane(final FactionMessage message, final FactionMember playerFactionMember, GUIAncor anchor) {
        GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 0, 1, anchor);
        buttonPane.onInit();
        final FactionData factionData = FactionManager.getFactionData(message.fromId);
        int buttonIndex = 0;
        if(playerFactionMember.hasPermission("manage.messages.mark_read")) {
            buttonPane.addColumn();
            buttonPane.addButton(buttonIndex, 0, "MARK AS READ", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        message.read = true;
                        //Todo: Send packet to mark message as read
                        redrawList();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return message.read;
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return !message.read;
                }
            });
            buttonIndex ++;
        }

        if(playerFactionMember.hasPermission("manage.messages.delete")) {
            buttonPane.addColumn();
            buttonPane.addButton(buttonIndex, 0, "DELETE", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        //Todo: Send packet to delete message
                        redrawList();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return true;
                }
            });
            buttonIndex ++;
        }

        if(playerFactionMember.hasPermission("manage.messages.reply")) {
            buttonPane.addColumn();
            buttonPane.addButton(buttonIndex, 0, "REPLY", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        (new FactionMessageSendDialog("REPLY TO MESSAGE", playerFactionMember.getFactionData().getFaction(), factionData.getFaction(), FactionMessage.MessageType.REPLY)).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return false;
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return true;
                }
            });
            buttonIndex ++;
        }
        return buttonPane;
    }

    public class FactionMessageListRow extends ScrollableTableList<FactionMessage>.Row {

        public FactionMessageListRow(InputState inputState, FactionMessage factionMessage, GUIElement... guiElements) {
            super(inputState, factionMessage, guiElements);
            this.highlightSelect = true;
            this.highlightSelectSimple = true;
            this.setAllwaysOneSelected(true);
        }
    }
}
