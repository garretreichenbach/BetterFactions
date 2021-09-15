package thederpgamer.betterfactions.gui.elements.pane;

import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButton;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;

/**
 * Improved version of GUIHorizontalButtonTablePane that allows for sub-panes to be added.
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/07/2021]
 */
public class GUIEnterableHorizontalButtonPane extends GUIHorizontalButtonTablePane {

    public boolean hide = false;

    public GUIEnterableHorizontalButtonPane(InputState state, int columns, int rows, GUIElement p) {
        super(state, columns, rows, p);
    }

    public GUIEnterableHorizontalButtonPane addSubButtonPane(final int x, final int y, GUIHorizontalArea.HButtonColor color, Object text) {
        checkSize(x, y);
        final GUIEnterableHorizontalButtonPane subPane = new GUIEnterableHorizontalButtonPane(getState(), 1, 1, null);
        final GUIHorizontalButton button = (GUIHorizontalButton) addButton(x, y, text, color, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    subPane.hide = !subPane.hide;
                    updateHeight(y, subPane.getHeight(), subPane.hide);
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
        subPane.dependend = button;
        subPane.onInit();
        button.attach(subPane);
        subPane.totalButtonWidthOffset -= button.getWidth() / 20.0f;
        subPane.setPos(button.getPos());
        subPane.getPos().x += button.getWidth() / 10.0f;
        subPane.getPos().y += button.getHeight();
        return subPane;
    }

    public GUIEnterableHorizontalButtonPane addSubButtonPane(final int x, final int y, GUIHorizontalArea.HButtonType type, Object text) {
        checkSize(x, y);
        final GUIEnterableHorizontalButtonPane subPane = new GUIEnterableHorizontalButtonPane(getState(), 1, 1, null);
        final GUIHorizontalButton button = (GUIHorizontalButton) addButton(x, y, text, type, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    subPane.hide = !subPane.hide;
                    updateHeight(y, subPane.getHeight(), subPane.hide);
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
        subPane.dependend = button;
        subPane.onInit();
        button.attach(subPane);
        subPane.totalButtonWidthOffset -= button.getWidth() / 20.0f;
        subPane.setPos(button.getPos());
        subPane.getPos().x += button.getWidth() / 10.0f;
        subPane.getPos().y += button.getHeight();
        return subPane;
    }

    private void updateHeight(int yPos, float subPaneHeight, boolean hide) {
        if(getButtons()[yPos] != null) {
            if(!hide) {
                for(int i = getButtons().length - 1; i >= yPos; i --) {
                    for(int j = 0; j < getButtons()[i].length; j ++) {
                        getButtons()[i][j].getPos().y += subPaneHeight;
                    }
                }
            } else {
                for(int i = yPos; i < getButtons().length; i ++) {
                    for(int j = 0; j < getButtons()[i].length; j ++) {
                        getButtons()[i][j].getPos().y -= subPaneHeight;
                    }
                }
            }
        }
    }

    private void checkSize(int x, int y) {
        if(getButtons() != null && getButtons().length > 0 && getButtons()[0].length > 0) {
            if(getButtons().length <= y) {
                int additionalColumns = (y + 1) - getButtons().length;
                for(int i = 0; i < additionalColumns; i ++) addColumn();
            }

            if(getButtons().length > 0 && getButtons()[0] != null && getButtons()[0].length <= x) {
                int additionalRows = (x + 1) - getButtons()[0].length;
                for(int i = 0; i < additionalRows; i ++) addRow();
            }
        }
    }
}
