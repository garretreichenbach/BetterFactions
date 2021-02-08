package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.utils.StarRunnable;
import org.newdawn.slick.Color;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIIconButton;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.ImageUtils;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIInnerTextbox;
import org.schema.schine.input.InputState;

/**
 * FactionInfoPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionInfoPanel extends GUIInnerTextbox {

    private GUIIconButton factionLogoButton;
    private FactionLogoOverlay factionLogo;
    private GUITextOverlay nameOverlay;
    private GUITextOverlay infoOverlay;

    public FactionInfoPanel(InputState inputState, GUIAncor anchor) {
        super(inputState);
        anchor.attach(this);
    }

    @Override
    public void onInit() {
        super.onInit();

        factionLogo = new FactionLogoOverlay(ImageUtils.getImage(FactionUtils.defaultLogo), this, getState());
        factionLogo.onInit();
        factionLogo.getPos().x = (factionLogo.getWidth() / 2) + (factionLogo.getWidth() / 18);
        factionLogo.getPos().y = 135;
        factionLogo.orientateInsideFrame();

        factionLogoButton = new GUIIconButton(getState(), (int) factionLogo.getWidth(), (int) factionLogo.getHeight(), factionLogo, factionLogo);
        factionLogoButton.onInit();
        factionLogoButton.getBackgroundColor().set(0, 0, 0, 0);
        factionLogoButton.getSelectedBackgroundColor().set(0, 0, 0, 0);
        factionLogoButton.getPressedColor().set(1.0f, 1.0f, 1.0f, 0.3f);
        factionLogoButton.getSelectColor().set(1.0f, 1.0f, 1.0f, 0.3f);
        getContent().attach(factionLogoButton);

        nameOverlay = new GUITextOverlay(10, 10, getState());
        nameOverlay.onInit();
        nameOverlay.limitTextWidth = (int) factionLogo.getWidth();
        nameOverlay.setLimitTextDraw((int) factionLogo.getWidth());
        nameOverlay.getPos().x = 10;
        nameOverlay.getPos().y = factionLogo.getPos().y + 50;
        getContent().attach(nameOverlay);

        infoOverlay = new GUITextOverlay(10, 10, getState());
        infoOverlay.onInit();
        infoOverlay.limitTextWidth = (int) factionLogo.getWidth();
        infoOverlay.setLimitTextDraw((int) factionLogo.getWidth());
        infoOverlay.getPos().x = 10;
        infoOverlay.getPos().y = nameOverlay.getPos().y + 10;
        getContent().attach(infoOverlay);
    }

    @Override
    public void draw() {
        super.draw();
        factionLogo.getSprite().getPos().x = (float) ((factionLogo.getSprite().getWidth() / 2) + (factionLogo.getSprite().getWidth() / 18));
        factionLogo.getSprite().getPos().y = 135;
    }

    public void updateLogo(final String newPath) {
        ImageUtils.getImage(newPath);
        new StarRunnable() {
            @Override
            public void run() {
                factionLogo.setSprite(ImageUtils.getImage(newPath));
                factionLogo.getSprite().getPos().x = (float) ((factionLogo.getSprite().getWidth() / 2) + (factionLogo.getSprite().getWidth() / 18));
                factionLogo.getSprite().getPos().y = 135;
            }
        }.runLater(BetterFactions.getInstance(), 15);
    }

    public void setNameText(String nameText) {
        nameOverlay.setTextSimple(nameText);
        nameOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        nameOverlay.limitTextWidth = (int) factionLogo.getWidth();
        nameOverlay.setLimitTextDraw((int) factionLogo.getWidth());
    }

    public void setNameText(String nameText, Color color) {
        nameOverlay.setTextSimple(nameText);
        nameOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        nameOverlay.setColor(color);
        nameOverlay.limitTextWidth = (int) factionLogo.getWidth();
        nameOverlay.setLimitTextDraw((int) factionLogo.getWidth());
    }

    public void setInfoText(String newText) {
        infoOverlay.setTextSimple(newText);
        infoOverlay.setFont(FontLibrary.FontSize.SMALL.getFont());
        infoOverlay.limitTextWidth = (int) factionLogo.getWidth();
        infoOverlay.setLimitTextDraw((int) factionLogo.getWidth());
    }

    public void setFaction(Faction faction) {
        factionLogo.setFaction(faction);
    }
}
