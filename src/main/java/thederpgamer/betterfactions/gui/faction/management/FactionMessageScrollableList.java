package thederpgamer.betterfactions.gui.faction.management;

import api.common.GameClient;
import api.common.GameCommon;
import api.network.packets.PacketUtil;
import org.apache.commons.lang3.text.WordUtils;
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
import thederpgamer.betterfactions.network.client.ModifyFactionMessagePacket;
import thederpgamer.betterfactions.utils.DateUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/**
 * <Description>
 *
 * @version 1.0 - [09/22/2021]
 * @author TheDerpGamer
 */
public class FactionMessageScrollableList extends ScrollableTableList<FactionMessage> {

    private GUIAncor anchor;
    private FactionManagementTab managementTab;

    public FactionMessageScrollableList(InputState inputState, GUIAncor anchor, FactionManagementTab managementTab) {
        super(inputState, 100, 100, anchor);
        this.anchor = anchor;
        this.managementTab = managementTab;
        anchor.attach(this);
        ((GameClientState) inputState).getFactionManager().addObserver(this);
    }

    public void redrawList() {
        clear();
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

        addColumn("Type", 10.0f, new Comparator<FactionMessage>() {
            public int compare(FactionMessage o1, FactionMessage o2) {
                return o1.messageType.compareTo(o2.messageType);
            }
        });

        addColumn("From", 12.0f, new Comparator<FactionMessage>() {
            public int compare(FactionMessage o1, FactionMessage o2) {
                String o1Name = GameCommon.getGameState().getFactionManager().getFactionName(o1.fromId);
                String o2Name = GameCommon.getGameState().getFactionManager().getFactionName(o2.fromId);
                return o1Name.compareTo(o2Name);
            }
        });

        addColumn("Date", 10.0f, new Comparator<FactionMessage>() {
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

        addDropdownFilter(new GUIListFilterDropdown<FactionMessage, FactionMessage.MessageCategory>(FactionMessage.MessageCategory.values()) {
            @Override
            public boolean isOk(FactionMessage.MessageCategory messageType, FactionMessage factionMessage) {
                if(messageType == FactionMessage.MessageCategory.ALL) return true;
                else if(messageType == FactionMessage.MessageCategory.READ) return factionMessage.read;
                else if(messageType == FactionMessage.MessageCategory.UNREAD) return !factionMessage.read;
                else return factionMessage.messageType.category == messageType;
            }
        }, new CreateGUIElementInterface<FactionMessage.MessageCategory>() {
            @Override
            public GUIElement create(FactionMessage.MessageCategory messageType) {
                GUIAncor anchor = new GUIAncor(getState(), 10.0F, 24.0F);
                GUITextOverlayTableDropDown dropDown;
                (dropDown = new GUITextOverlayTableDropDown(10, 10, getState())).setTextSimple(messageType.name());
                dropDown.setPos(4.0F, 4.0F, 0.0F);
                anchor.setUserPointer(messageType);
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
            try {
                if(message != null) {
                    GUITextOverlayTable titleTextElement;
                    (titleTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(message.title);
                    GUIClippedRow titleRowElement;
                    (titleRowElement = new GUIClippedRow(this.getState())).attach(titleTextElement);

                    GUITextOverlayTable typeTextElement;
                    (typeTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(WordUtils.capitalize(message.messageType.name().toLowerCase()));
                    GUIClippedRow typeRowElement;
                    (typeRowElement = new GUIClippedRow(this.getState())).attach(typeTextElement);

                    GUITextOverlayTable fromTextElement;
                    (fromTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(GameCommon.getGameState().getFactionManager().getFactionName(message.fromId));
                    GUIClippedRow fromRowElement;
                    (fromRowElement = new GUIClippedRow(this.getState())).attach(fromTextElement);

                    GUITextOverlayTable dateTextElement;
                    (dateTextElement = new GUITextOverlayTable(10, 10, this.getState())).setTextSimple(DateUtils.getDateFormatted(message.date));
                    GUIClippedRow dateRowElement;
                    (dateRowElement = new GUIClippedRow(this.getState())).attach(dateTextElement);

                    FactionMessageListRow row = new FactionMessageListRow(getState(), message, titleRowElement, typeRowElement, fromRowElement, dateRowElement);
                    if(playerFactionMember.hasPermission("manage.messages.[ANY]")) {
                        GUIAncor anchor = new GUIAncor(getState(), this.anchor.getWidth() - 28.0f, 28.0f);
                        anchor.attach(redrawButtonPane(message, playerFactionMember, anchor));
                        row.expanded = new GUIElementList(getState());
                        row.expanded.add(new GUIListElement(anchor, getState()));
                        row.expanded.attach(anchor);
                    }
                    row.onInit();
                    guiElementList.addWithoutUpdate(row);
                }
            } catch(Exception exception) {
                exception.printStackTrace();
            }
        }
        guiElementList.updateDim();
    }

    private GUIHorizontalButtonTablePane redrawButtonPane(final FactionMessage message, final FactionMember playerFactionMember, GUIAncor anchor) {
        GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 0, 1, anchor);
        buttonPane.onInit();
        final FactionData factionData = FactionManager.getFactionData(message.fromId);
        int buttonIndex = 0;
        if(playerFactionMember.hasPermission("manage.messages.view")) {
            buttonPane.addColumn();
            buttonPane.addButton(buttonIndex, 0, "VIEW", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        FactionMessageReceiveDialog dialog = new FactionMessageReceiveDialog(message);
                        dialog.getInputPanel().createPanel(message);
                        dialog.activate();
                        redrawList();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return !getState().getController().getPlayerInputs().isEmpty();
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return getState().getController().getPlayerInputs().isEmpty();
                }
            });
            buttonIndex ++;
        }

        if(playerFactionMember.hasPermission("manage.messages.mark_read")) {
            buttonPane.addColumn();
            String buttonName = (message.read) ? "MARK AS UNREAD" : "MARK AS READ";
            buttonPane.addButton(buttonIndex, 0, buttonName, GUIHorizontalArea.HButtonColor.PINK, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        message.read = !message.read;
                        if(message.read) PacketUtil.sendPacketToServer(new ModifyFactionMessagePacket(message, FactionMessage.MARK_READ));
                        else PacketUtil.sendPacketToServer(new ModifyFactionMessagePacket(message, FactionMessage.MARK_UNREAD));
                        redrawList();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return !getState().getController().getPlayerInputs().isEmpty();
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return getState().getController().getPlayerInputs().isEmpty();
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
                        PacketUtil.sendPacketToServer(new ModifyFactionMessagePacket(message, FactionMessage.DELETE));
                        redrawList();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return !getState().getController().getPlayerInputs().isEmpty();
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return getState().getController().getPlayerInputs().isEmpty();
                }
            });
            buttonIndex ++;
        }

        if(playerFactionMember.hasPermission("manage.messages.reply")) {
            buttonPane.addColumn();
            buttonPane.addButton(buttonIndex, 0, "REPLY", GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        FactionMessageSendDialog dialog = new FactionMessageSendDialog();
                        dialog.getInputPanel().createPanel(playerFactionMember.getFactionData().getFaction(), factionData.getFaction(), FactionMessage.MessageType.REPLY);
                        dialog.activate();
                        //(new FactionMessageSendDialog("REPLY TO MESSAGE", playerFactionMember.getFactionData().getFaction(), factionData.getFaction(), FactionMessage.MessageType.REPLY)).activate();
                    }
                }

                @Override
                public boolean isOccluded() {
                    return !getState().getController().getPlayerInputs().isEmpty();
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return getState().getController().getPlayerInputs().isEmpty();
                }
            });
            buttonIndex ++;

            if(message.acceptButtonText.equals("ACCEPT")) {
                buttonPane.addColumn();
                buttonPane.addButton(buttonIndex, 0, message.acceptButtonText, GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if(mouseEvent.pressedLeftMouse()) {
                            message.read = true;
                            //Todo: Accept message offer
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return !getState().getController().getPlayerInputs().isEmpty();
                    }
                }, new GUIActivationCallback() {
                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return getState().getController().getPlayerInputs().isEmpty();
                    }
                });
                buttonIndex ++;
            }
        }

        if(message.denyButtonText.equals("DENY")) {
            buttonPane.addColumn();
            buttonPane.addButton(buttonIndex, 0, message.denyButtonText, GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
                @Override
                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                    if(mouseEvent.pressedLeftMouse()) {
                        message.read = true;
                        //Todo: Deny message offer
                    }
                }

                @Override
                public boolean isOccluded() {
                    return !getState().getController().getPlayerInputs().isEmpty();
                }
            }, new GUIActivationCallback() {
                @Override
                public boolean isVisible(InputState inputState) {
                    return true;
                }

                @Override
                public boolean isActive(InputState inputState) {
                    return getState().getController().getPlayerInputs().isEmpty();
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
