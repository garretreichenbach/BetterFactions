package thederpgamer.betterfactions.gui.elements.buttons;

import org.schema.schine.graphicsengine.forms.AbstractSceneNode;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIScrollablePanel;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIExpandableButton;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButton;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;
import java.lang.reflect.Field;

/**
 * ExpandableButton.java
 * <Description>
 * ==================================================
 * Created 02/07/2021
 * @author TheDerpGamer
 */
public class ExpandableButton extends GUIElement {

    private GUIExpandableButton expandableButton;
    private GUIScrollablePanel expandedPanel;
    private boolean active;

    public ExpandableButton(InputState inputState, GUIInnerTextbox textBox, String displayText, GUIHorizontalArea.HButtonColor color) {
        super(inputState);
        active = false;
        expandedPanel = new GUIScrollablePanel(textBox.getWidth(), textBox.getHeight(), textBox, inputState);
        expandableButton = new GUIExpandableButton(inputState, textBox, displayText, displayText, createActivationCallback(), expandedPanel, false);
        GUIHorizontalButton button = getHorizontalButton();
        button.setColor(color);
        setHorizontalButton(button);
    }

    public ExpandableButton(InputState inputState, GUIInnerTextbox textBox, String displayText, GUIHorizontalArea.HButtonType type) {
        super(inputState);
        active = false;
        expandedPanel = new GUIScrollablePanel(textBox.getWidth() - 2, textBox.getHeight() - 2, textBox, inputState);
        expandableButton = new GUIExpandableButton(inputState, textBox, displayText, displayText, createActivationCallback(), expandedPanel, false);
        GUIHorizontalButton button = getHorizontalButton();
        button.setType(type);
        setHorizontalButton(button);
    }

    @Override
    public float getWidth() {
        return expandedPanel.getWidth();
    }

    @Override
    public float getHeight() {
        return expandedPanel.getHeight();
    }

    @Override
    public void cleanUp() {
        expandableButton.cleanUp();
        expandedPanel.cleanUp();
        try {
            for (AbstractSceneNode element : childs) element.cleanUp();
        } catch (Exception ignored) { }
    }

    @Override
    public void draw() {
        expandableButton.draw();
        if(expandedPanel.isActive()) expandedPanel.draw();
    }

    @Override
    public void onInit() {
        expandableButton.onInit();
    }

    public GUIHorizontalButton getHorizontalButton() {
        try {
            Field buttonField = expandableButton.getClass().getDeclaredField("button");
            buttonField.setAccessible(true);
            return (GUIHorizontalButton) buttonField.get(expandableButton);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setHorizontalButton(GUIHorizontalButton button) {
        try {
            Field buttonField = expandableButton.getClass().getDeclaredField("button");
            buttonField.setAccessible(true);
            buttonField.set(expandableButton, button);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public GUIExpandableButton getExpandableButton() {
        return expandableButton;
    }

    public GUIScrollablePanel getExpandedPanel() {
        return expandedPanel;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private GUIActivationCallback createActivationCallback() {
        return new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return active;
            }
        };
    }
}
