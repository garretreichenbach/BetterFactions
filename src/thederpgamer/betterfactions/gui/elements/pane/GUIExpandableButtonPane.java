package thederpgamer.betterfactions.gui.elements.pane;

import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;

/**
 * Enhanced version of GUITopBar.ExpandedButton.
 *
 * @version 1.0 - [09/14/2021]
 * @author TheDerpGamer, Schema (original)
 */
public class GUIExpandableButtonPane extends GUIHorizontalButtonTablePane {

    private boolean expanded;

    public GUIExpandableButtonPane(InputState inputState, int columns, int rows, GUIElement depend) {
        super(inputState, columns, rows, depend);
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void expandSize(int x, int y) {
        if(getButtons().length == 0 || getButtons()[0] == null) addRow();
        if(y >= getButtons().length) {
            for(int i = 0; i <= y - getButtons().length; i ++) addRow();
        }

        if(getButtons().length > 0 && getButtons()[0] != null && x >= getButtons()[0].length) {
            for(int i = 0; i <= x - getButtons()[0].length; i ++) addColumn();
        }
    }

    public GUIHorizontalArea addExpandedButton(int x, int y, Object text, GUIHorizontalArea.HButtonColor color, GUICallback callback, final GUIActivationHighlightCallback activationCallback) {
        final CallbackBlocker blockingCallback = new CallbackBlocker(callback);
        expandSize(x, y);
        return addButton(x, y, text, color, blockingCallback, new GUIActivationHighlightCallback() {

            @Override
            public boolean isVisible(InputState inputState) {
                return activationCallback.isVisible(inputState) && expanded;
            }

            @Override
            public boolean isHighlighted(InputState inputState) {
                return activationCallback.isHighlighted(inputState);
            }

            @Override
            public boolean isActive(InputState inputState) {
                return activationCallback.isActive(inputState);
            }
        });
    }

    public GUIHorizontalArea addExpandedButton(int x, int y, Object text, GUIHorizontalArea.HButtonType type, GUICallback callback, final GUIActivationHighlightCallback activationCallback) {
        final CallbackBlocker blockingCallback = new CallbackBlocker(callback);
        expandSize(x, y);
        return addButton(x, y, text, type, blockingCallback, new GUIActivationHighlightCallback() {

            @Override
            public boolean isVisible(InputState inputState) {
                return activationCallback.isVisible(inputState) && expanded;
            }

            @Override
            public boolean isHighlighted(InputState inputState) {
                return activationCallback.isHighlighted(inputState);
            }

            @Override
            public boolean isActive(InputState inputState) {
                return activationCallback.isActive(inputState);
            }
        });
    }

    public void setMainButton(int x, final int y, int heightOffset, Object text, GUIHorizontalArea.HButtonColor color, final GUIHorizontalButtonTablePane parent, final GUIActivationCallback activationCallback) {
        if(y >= parent.getButtons().length) {
            if(parent.getButtons().length == 0 || parent.getButtons()[0] == null) parent.addRow();
            for(int i = 0; i <= y - parent.getButtons().length; i ++) parent.addRow();
        }

        if(parent.getButtons().length > 0 && parent.getButtons()[0] != null && x >= parent.getButtons()[0].length) {
            for(int i = 0; i <= x - parent.getButtons()[0].length; i ++) parent.addColumn();
        }

        parent.addButton(x, y, text, color, new GUICallback() {

            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    expanded = !expanded;
                    handleExpanded(parent, y);
                }
            }
            @Override
            public boolean isOccluded() {
                return false;
            }
        }, new GUIActivationHighlightCallback() {

            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return activationCallback.isActive(inputState);
            }

            @Override
            public boolean isHighlighted(InputState inputState) {
                return expanded;
            }
        });
        dependend = parent.getButtons()[y][x];
        setPos(0, 25 + heightOffset, 0);
        parent.getButtons()[y][x].attach(this);
    }

    public void setMainButton(int x, final int y, int heightOffset, Object text, GUIHorizontalArea.HButtonType type, final GUIHorizontalButtonTablePane parent, final GUIActivationCallback activationCallback) {
        if(y >= parent.getButtons().length) {
            if(parent.getButtons().length == 0 || parent.getButtons()[0] == null) parent.addRow();
            for(int i = 0; i < y - parent.getButtons().length; i ++) parent.addRow();
        }

        if(parent.getButtons().length > 0 && parent.getButtons()[0] != null && x >= parent.getButtons()[0].length) {
            for(int i = 0; i < x - parent.getButtons()[0].length; i ++) parent.addColumn();
        }

        parent.addButton(x, y, text, type, new GUICallback() {

            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    expanded = !expanded;
                    handleExpanded(parent, y);
                }
            }
            @Override
            public boolean isOccluded() {
                return false;
            }
        }, new GUIActivationHighlightCallback() {

            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return activationCallback.isActive(inputState);
            }

            @Override
            public boolean isHighlighted(InputState inputState) {
                return expanded;
            }
        });
        dependend = parent.getButtons()[y][x];
        setPos(0, 25 + heightOffset, 0);
        parent.getButtons()[y][x].attach(this);
    }

    private void handleExpanded(GUIHorizontalButtonTablePane parent, int index) {
        for(int i = index; i <= parent.getButtons()[0].length; i ++) {
            if(expanded) parent.getButtons()[0][i].getPos().y += 26 * getButtons()[0].length;
            else parent.getButtons()[0][i].getPos().y -= 26 * getButtons()[0].length;
        }
    }

    private static class CallbackBlocker implements GUICallbackBlocking {

        private final GUICallback guiCallback;

        public CallbackBlocker(GUICallback guiCallback) {
            this.guiCallback = guiCallback;
        }
        @Override
        public boolean isOccluded() {
            return guiCallback.isOccluded();
        }

        @Override
        public void callback(GUIElement callingGuiElement, MouseEvent mouseEvent) {
            guiCallback.callback(callingGuiElement, mouseEvent);
        }

        @Override
        public boolean isBlocking() {
            return true;
        }

        @Override
        public void onBlockedCallbackExecuted() {

        }

        @Override
        public void onBlockedCallbackNotExecuted(boolean anyOtherBlockedCallbacksExecuted) {

        }
    }
}