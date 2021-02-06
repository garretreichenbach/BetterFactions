package thederpgamer.betterfactions.gui.elements;

import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIScrollablePanel;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIExpandableButton;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButton;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;
import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * VerticalButtonPane.java
 * <Description>
 * ==================================================
 * Created 02/06/2021
 * @author TheDerpGamer
 */
public class VerticalButtonPane extends GUIInnerTextbox {

    private GUIHorizontalButton[] buttons;
    private final int spacing;

    public VerticalButtonPane(InputState inputState, int spacing) {
        super(inputState);
        this.spacing = spacing;
    }

    public GUIHorizontalButton addButton(String text, GUIHorizontalArea.HButtonType type, GUICallback callback) {
        expandSize();
        GUIHorizontalButton button = new GUIHorizontalButton(getState(), type, text, callback, activationInterface, null);
        button.onInit();
        getContent().attach(button);
        button.setWidth(getContent().getWidth() - (float) (spacing / 2));
        button.setPos( (float) spacing, (float) spacing + (button.getHeight() + (float) spacing) * (float) buttons.length - 1, 0.0F);
        buttons[buttons.length - 1] = button;
        return button;
    }

    public GUIHorizontalButton addButton(String text, GUIHorizontalArea.HButtonColor color, GUICallback callback) {
        expandSize();
        GUIHorizontalButton button = new GUIHorizontalButton(getState(), color, text, callback, activationInterface, null);
        button.onInit();
        getContent().attach(button);
        button.setWidth(getContent().getWidth() - (float) (spacing / 2));
        button.setPos( (float) spacing, (float) spacing + (button.getHeight() + (float) spacing) * (float) buttons.length - 1, 0.0F);
        buttons[buttons.length - 1] = button;
        return button;
    }

    public GUIExpandableButton addExpandableButton(String text, GUIHorizontalArea.HButtonType type, float expandedHeight) {
        expandSize();
        final GUIScrollablePanel expandedPanel = new GUIScrollablePanel(getContent().getWidth() - (float) (spacing / 2), expandedHeight, getState());
        GUIActivationCallback activationCallback = new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return expandedPanel.isActive();
            }
        };
        GUIExpandableButton button = new GUIExpandableButton(getState(), this, text, text, activationCallback, expandedPanel, false);
        button.onInit();
        getContent().attach(button);
        button.setPos( (float) spacing, (float) spacing + (button.getHeight() + (float) spacing) * (float) buttons.length - 1, 0.0F);
        try {
            Field buttonElementField = button.getClass().getDeclaredField("button");
            buttonElementField.setAccessible(true);
            GUIHorizontalButton buttonElement = (GUIHorizontalButton) buttonElementField.get(button);
            buttonElement.setType(type);
            buttonElementField.set(button, buttonElement);
            buttons[buttons.length - 1] = buttonElement;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return button;
    }

    public GUIExpandableButton addExpandableButton(String text, GUIHorizontalArea.HButtonColor color, float expandedHeight) {
        expandSize();
        final GUIScrollablePanel expandedPanel = new GUIScrollablePanel(getContent().getWidth() - (float) (spacing / 2), expandedHeight, getState());
        GUIActivationCallback activationCallback = new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return expandedPanel.isActive();
            }
        };
        GUIExpandableButton button = new GUIExpandableButton(getState(), this, text, text, activationCallback, expandedPanel, false);
        button.onInit();
        getContent().attach(button);
        button.setPos( (float) spacing, (float) spacing + (button.getHeight() + (float) spacing) * (float) buttons.length - 1, 0.0F);
        try {
            Field buttonElementField = button.getClass().getDeclaredField("button");
            buttonElementField.setAccessible(true);
            GUIHorizontalButton buttonElement = (GUIHorizontalButton) buttonElementField.get(button);
            buttonElement.setColor(color);
            buttonElementField.set(button, buttonElement);
            buttons[buttons.length - 1] = buttonElement;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return button;
    }

    private void expandSize() {
        if(buttons == null) {
            buttons = new GUIHorizontalButton[1];
        } else {
            buttons = Arrays.copyOf(buttons, buttons.length + 1);
        }
    }
}
