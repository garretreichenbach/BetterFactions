package org.schema.schine.graphicsengine.forms.gui.newgui;

import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.font.FontLibrary.FontSize;
import org.schema.schine.graphicsengine.forms.gui.ColoredInterface;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.input.InputState;
import java.util.Arrays;

/**
 * Modified version of GUIHorizontalButtonTablePane.
 *
 * @author Schema, TheDerpGamer
 * @since 07/28/2021
 */
public class GUIHorizontalButtonTablePane extends GUIElement {

    private static final int buttonHeight = 25;
    private static final float titleHeight = 32;
    public GUIElement dependend;
    public GUIActiveInterface activeInterface;
    public int totalButtonWidthOffset;
    private int columns;
    private int rows;
    private GUIAbstractHorizontalArea[][] buttons;
    private String title;
    private GUITextOverlay tOverlay;
    private int titleWidth;
    //INSERTED CODE
    public boolean hide;
    //

    public GUIHorizontalButtonTablePane(InputState state, int columns, int rows, GUIElement p) {
        this(state, columns, rows, null, p);
    }

    public GUIHorizontalButtonTablePane(InputState state, int columns, int rows, String title, GUIElement p) {
        super(state);
        this.columns = columns;
        this.rows = rows;
        this.dependend = p;
        this.title = title;
        this.hide = false;
    }

    @Override
    public void cleanUp() {
        if(buttons != null){
            for(GUIAbstractHorizontalArea[] r : buttons){
                if(r != null){
                    for(GUIAbstractHorizontalArea b : r){
                        if(b != null){
                            b.cleanUp();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void draw() {
        if(!hide) {
            GlUtil.glPushMatrix();
            transform();
            if (title != null) {
                tOverlay.setPos((int) (getWidth() / 2 - titleWidth / 2), 4, 0);
                tOverlay.draw();
                GlUtil.translateModelview(0, titleHeight, 0);
            }

            int wPart = (int) (getWidth() / columns);
            for (int x = 0; x < columns; x++) {
                for (int y = 0; y < rows; y++) {
                    GUIAbstractHorizontalArea button = buttons[y][x];
                    if (button != null) {

                        button.setPos(x * wPart, y * buttonHeight, 0);

                        if (x == columns - 1 && (columns * wPart + ((button.spacingButtonIndexX - 1) * wPart)) != (int) getWidth()) {
                            button.setWidth(wPart + ((int) getWidth() - (columns * wPart + ((button.spacingButtonIndexX - 1) * wPart))));
                        } else {
                            button.setWidth(wPart*button.spacingButtonIndexX);
                        }
                        button.draw();
                    }
                }
            }

            GlUtil.glPopMatrix();
        } else cleanUp();
    }

    @Override
    public void onInit() {
        buttons = new GUIAbstractHorizontalArea[rows][columns];

        if (title != null) {
            this.tOverlay = new GUITextOverlay(10, 10, FontLibrary.getBlenderProMedium20(), getState());
            tOverlay.setTextSimple(title);
            tOverlay.onInit();
            this.titleWidth = tOverlay.getFont().getWidth(title);
        }
    }

    public GUIHorizontalArea addButton(int x, int y, Object text, final GUIHorizontalArea.HButtonColor type, GUICallback callback, final GUIActivationCallback actCallback) {
        checkSize(x, y);
        buttons[y][x] = new GUIHorizontalButton(getState(), type, text, callback, activeInterface, actCallback);
        return (GUIHorizontalButton)buttons[y][x];
    }
    public GUIHorizontalArea addButton(int x, int y, Object text, final GUIHorizontalArea.HButtonType type, GUICallback callback, final GUIActivationCallback actCallback) {
        checkSize(x, y);
        buttons[y][x] = new GUIHorizontalButton(getState(), type, text, callback, activeInterface, actCallback);
        return (GUIHorizontalButton)buttons[y][x];
    }
    public GUIHorizontalText addText(int x, int y, Object text, FontSize size, int orientation) {
        return addText(x, y, text, size, null, orientation);
    }
    public GUIHorizontalText addText(int x, int y, Object text, int orientation) {
        return addText(x, y, text, FontSize.MEDIUM, null, orientation);
    }
    public GUIHorizontalText addText(int x, int y, Object text) {
        return addText(x, y, text, FontSize.MEDIUM, null, ORIENTATION_HORIZONTAL_MIDDLE);
    }
    public GUIHorizontalText addText(int x, int y, Object text, FontSize size, ColoredInterface colorIface, int orientation) {
        checkSize(x, y);
        buttons[y][x] = new GUIHorizontalText(getState(), text, size, colorIface);
        ((GUIHorizontalText)buttons[y][x]).setAlign(orientation);
        return (GUIHorizontalText)buttons[y][x];
    }

    public void addButton(GUIAbstractHorizontalArea guiHorizontalButton, int x, int y) {
        checkSize(x, y);
        guiHorizontalButton.activeInterface = activeInterface;
        buttons[y][x] = guiHorizontalButton;
    }

    //INSERTED CODE
    public GUIHorizontalButtonTablePane addSubButtonPane(final int x, final int y, GUIHorizontalArea.HButtonColor color, Object text) {
        checkSize(x, y);
        final GUIHorizontalButtonTablePane subPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, null);
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
        subPane.totalButtonWidthOffset -= button.getWidth() / 20.0f;
        subPane.setPos(button.getPos());
        subPane.getPos().x += button.getWidth() / 10.0f;
        subPane.getPos().y += button.getHeight();
        return subPane;
    }

    public GUIHorizontalButtonTablePane addSubButtonPane(final int x, final int y, GUIHorizontalArea.HButtonType type, Object text) {
        checkSize(x, y);
        final GUIHorizontalButtonTablePane subPane = new GUIHorizontalButtonTablePane(getState(), 1, 1, null);
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
        subPane.totalButtonWidthOffset -= button.getWidth() / 20.0f;
        subPane.setPos(button.getPos());
        subPane.getPos().x += button.getWidth() / 10.0f;
        subPane.getPos().y += button.getHeight();
        return subPane;
    }
    //

    @Override
    public float getHeight() {
        return rows * buttonHeight + (title != null ? titleHeight : 0);
    }

    @Override
    public float getWidth() {
        return dependend.getWidth() + totalButtonWidthOffset;
    }

    /**
     * @return the buttons
     */
    public GUIAbstractHorizontalArea[][] getButtons() {
        return buttons;
    }

    public void setButtonSpacing(int x, int y, int spacing) {
        buttons[y][x].spacingButtonIndexX = spacing;
    }

    //INSERTED CODE
    private void updateHeight(int yPos, float subPaneHeight, boolean hide) {
        if(buttons[yPos] != null) {
            if(!hide) {
                for(int i = buttons.length - 1; i >= yPos; i --) {
                    for(int j = 0; j < buttons[i].length; j ++) {
                        buttons[i][j].getPos().y += subPaneHeight;
                    }
                }
            } else {
                for(int i = yPos; i < buttons.length; i ++) {
                    for(int j = 0; j < buttons[i].length; j ++) {
                        buttons[i][j].getPos().y -= subPaneHeight;
                    }
                }
            }
        }
    }

    public void addColumns(int amount) {
        if(buttons != null) buttons = Arrays.copyOf(buttons, buttons.length + amount);
        else buttons = new GUIAbstractHorizontalArea[amount][1];
        columns += amount;
    }

    public void addRows(int amount) {
        if(buttons == null || buttons.length <= 0) addColumns(1);
        else {
            if(buttons[0] != null) buttons[0] = Arrays.copyOf(buttons[0], buttons[0].length + amount);
            else buttons[0] = new GUIAbstractHorizontalArea[amount];
        }
        rows += amount;
    }

    private void checkSize(int x, int y) {
        if(buttons != null) {
            if(buttons.length <= y) addColumns((y + 1) - buttons.length);
            if(buttons.length > 0 && buttons[0] != null && buttons[0].length <= x) addRows((x + 1) - buttons[0].length);

        } else buttons = new GUIAbstractHorizontalArea[y][x];
    }
    //
}