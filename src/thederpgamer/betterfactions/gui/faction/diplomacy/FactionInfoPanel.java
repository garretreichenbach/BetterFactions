package thederpgamer.betterfactions.gui.faction.diplomacy;

import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.utils.ImageUtils;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIOverlay;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;

import java.util.Objects;

/**
 * FactionInfoPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionInfoPanel extends GUIInnerTextbox {

    private GUIAncor anchor;
    private GUIOverlay factionLogo;
    private GUITextOverlay nameOverlay;
    private GUITextOverlay infoOverlay;

    public FactionInfoPanel(InputState inputState, GUIAncor anchor) {
        super(inputState);
        this.anchor = anchor;
    }

    @Override
    public void onInit() {
        super.onInit();

        factionLogo = new GUIOverlay(Objects.requireNonNull(ImageUtils.getImage(BetterFactions.getInstance().defaultLogo)), getState());
        factionLogo.onInit();
        factionLogo.setInside(true);
        factionLogo.orientate(ORIENTATION_HORIZONTAL_MIDDLE | ORIENTATION_TOP);
        anchor.attach(factionLogo);

        nameOverlay = new GUITextOverlay((int) getWidth() - 8, (int) getHeight() - 8, getState());
        nameOverlay.onInit();
        nameOverlay.setInside(true);
        nameOverlay.orientate(ORIENTATION_HORIZONTAL_MIDDLE | ORIENTATION_VERTICAL_MIDDLE);
        anchor.attach(nameOverlay);

        infoOverlay = new GUITextOverlay((int) getWidth() - 8, (int) getHeight() - 8, getState());
        infoOverlay.onInit();
        infoOverlay.setInside(true);
        infoOverlay.orientate(ORIENTATION_HORIZONTAL_MIDDLE | ORIENTATION_VERTICAL_MIDDLE);
        infoOverlay.getPos().y -= nameOverlay.getHeight() + 4;
        anchor.attach(infoOverlay);
    }

    public void updateLogo(String newPath) {
        factionLogo.setSprite(ImageUtils.getImage(newPath));
    }

    public void setNameText(String nameText) {
        nameOverlay.setTextSimple(nameText);
        nameOverlay.setFont(FontLibrary.FontSize.BIG.getFont());
    }

    public void setInfoText(String newText) {
        infoOverlay.setTextSimple(newText);
        infoOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
    }
}
