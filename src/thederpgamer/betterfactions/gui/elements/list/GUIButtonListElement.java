package thederpgamer.betterfactions.gui.elements.list;

import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIListElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalButton;
import org.schema.schine.input.InputState;

/**
 * GUIButtonListElement.java
 * <Description>
 * ==================================================
 * Created 02/07/2021
 * @author TheDerpGamer
 */
public class GUIButtonListElement extends GUIListElement {

    private GUIHorizontalButton button;
    private String text;
    private GUIHorizontalArea.HButtonColor color;
    private GUIHorizontalArea.HButtonType type;
    private GUICallback callback;

    public GUIButtonListElement(InputState inputState, GUIHorizontalArea.HButtonColor color, String text, GUICallback callback) {
        super(inputState);
        this.text = text;
        this.color = color;
        this.type = null;
        this.callback = callback;
    }

    public GUIButtonListElement(InputState inputState, GUIHorizontalArea.HButtonType type, String text, GUICallback callback) {
        super(inputState);
        this.text = text;
        this.color = null;
        this.type = type;
        this.callback = callback;
    }

    @Override
    public void onInit() {
        super.onInit();
        if(color != null) {
            button = new GUIHorizontalButton(getState(), color, text, callback, getActiveInterface(), null);
        } else if(type != null) {
            button = new GUIHorizontalButton(getState(), type, text, callback, getActiveInterface(), null);
        }

        if(button != null) {
            button.onInit();
            setContent(button);
            button.setWidth(238);
        }
    }

    public void setButtonWidth(float buttonWidth) {
        button.setWidth(buttonWidth);
    }

    private GUIActiveInterface getActiveInterface() {
        return new GUIActiveInterface() {
            @Override
            public boolean isActive() {
                return true;
            }
        };
    }
}
