package dovtech.starmadeplus.gui.faction;

import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIResizableGrabbableWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.config.WindowPaletteInterface;
import org.schema.schine.input.InputState;

public class FactionInfoWindow extends GUIResizableGrabbableWindow implements GUICallback {

    private Faction selectedFaction;

    public FactionInfoWindow(InputState inputState, int i, int i1, String s, Faction selectedFaction) {
        super(inputState, i, i1, s);
        this.selectedFaction = selectedFaction;
    }

    @Override
    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {

    }

    @Override
    public boolean isOccluded() {
        return false;
    }

    @Override
    public WindowPaletteInterface getWindowPalette() {
        return null;
    }

    @Override
    protected int getMinWidth() {
        return 0;
    }

    @Override
    protected int getMinHeight() {
        return 0;
    }

    @Override
    public int getInnerCornerDistX() {
        return 0;
    }

    @Override
    public int getInnerCornerTopDistY() {
        return 0;
    }

    @Override
    public int getInnerCornerBottomDistY() {
        return 0;
    }

    @Override
    public int getInnerHeigth() {
        return 0;
    }

    @Override
    public int getInnerWidth() {
        return 0;
    }

    @Override
    public int getInnerOffsetX() {
        return 0;
    }

    @Override
    public int getInnerOffsetY() {
        return 0;
    }

    @Override
    public int getInset() {
        return 0;
    }

    @Override
    public int getTopDist() {
        return 0;
    }

    @Override
    public void cleanUp() {

    }

    @Override
    public void draw() {

    }
}
