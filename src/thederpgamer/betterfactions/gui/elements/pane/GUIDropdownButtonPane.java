package thederpgamer.betterfactions.gui.elements.pane;

import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDropdownBackground;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButtonTablePane;
import org.schema.schine.input.InputState;

/**
 * GUIDropdownButtonPane
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/16/2021
 */
public class GUIDropdownButtonPane extends GUIDropdownBackground {

    private boolean initialized;
    private int buttonIndex;
    private GUIHorizontalButtonTablePane buttonPane;

    public GUIDropdownButtonPane(InputState inputState, int width, int height) {
        super(inputState, width, height);
        initialized = false;
        buttonIndex = -1;
        buttonPane = new GUIHorizontalButtonTablePane(inputState, 1, 1, this);
    }

    public GUIHorizontalButtonTablePane getButtonPane() {
        return buttonPane;
    }

    public GUIHorizontalArea addButton(String label, GUIHorizontalArea.HButtonType type, GUICallback callback) {
        buttonIndex ++;
        return(buttonPane.addButton(0, buttonIndex, label, type, callback, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return true;
            }
        }));
    }

    public GUIHorizontalArea addButton(String label, GUIHorizontalArea.HButtonColor color, GUICallback callback) {
        buttonIndex ++;
        return(buttonPane.addButton(0, buttonIndex, label, color, callback, new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return true;
            }
        }));
    }

    public void toggleDropdown() {
        if(!initialized) onInit();
        if(isInvisible()) {
            setHeight(buttonPane.getHeight() + 2);
            setVisibility(1);
        } else {
            setHeight(0);
            setVisibility(2);
        }
    }

    public boolean isToggled() {
        return !isInvisible();
    }

    @Override
    public void onInit() {
        super.onInit();
        initialized = true;
        buttonPane.onInit();
        attach(buttonPane);
    }

    @Override
    public float getWidth() {
        return buttonPane.getWidth() + 2;
    }

    @Override
    public float getHeight() {
        return buttonPane.getHeight() + 2;
    }
}
