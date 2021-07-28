package thederpgamer.betterfactions.gui.elements.overlay;

import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIHorizontalArea;
import org.schema.schine.input.InputState;

/**
 * GUIListElementOverlayButton
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/18/2021
 */
public class GUIListElementOverlayButton extends GUIHorizontalArea {

    private GUITextOverlay textOverlay;

    public GUIListElementOverlayButton(InputState inputState, HButtonColor buttonColor, String text, int width, GUICallback callback) {
        super(inputState, buttonColor, width);
        text = text.toUpperCase().trim();
        actCallback = new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return true;
            }
        };
        textOverlay = new GUITextOverlay(FontLibrary.FontSize.MEDIUM.getFont().getWidth(text), FontLibrary.FontSize.MEDIUM.getFont().getHeight(text), getState());
        textOverlay.setTextSimple(text);
        textOverlay.onInit();
        setCallback(callback);
        setMouseUpdateEnabled(true);
        GUIScrollablePanel lr = new GUIScrollablePanel(getWidth(), getHeight(), this, getState());
        lr.setScrollable(0);
        lr.setLeftRightClipOnly = true;
        lr.setContent(textOverlay);
        attach(lr);
        setUserPointer(text);
    }

    public GUIListElementOverlayButton(InputState inputState, HButtonType buttonType, String text, int width, GUICallback callback) {
        super(inputState, buttonType, width);
        text = text.toUpperCase().trim();
        actCallback = new GUIActivationCallback() {
            @Override
            public boolean isVisible(InputState inputState) {
                return true;
            }

            @Override
            public boolean isActive(InputState inputState) {
                return true;
            }
        };
        textOverlay = new GUITextOverlay(FontLibrary.FontSize.MEDIUM.getFont().getWidth(text), FontLibrary.FontSize.MEDIUM.getFont().getHeight(text), getState());
        textOverlay.setTextSimple(text);
        textOverlay.onInit();
        setCallback(callback);
        setMouseUpdateEnabled(true);
        GUIScrollablePanel lr = new GUIScrollablePanel(getWidth(), getHeight(), this, getState());
        lr.setScrollable(0);
        lr.setLeftRightClipOnly = true;
        lr.setContent(textOverlay);
        attach(lr);
        setUserPointer(text);
    }

    public String getText() {
        StringBuilder builder = new StringBuilder();
        for(Object object : textOverlay.getText()) builder.append(object);
        return builder.toString();
    }
}
