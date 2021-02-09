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
        setContent(new GUIAncor(inputState, 240, 70));
    }

    @Override
    public void onInit() {
        super.onInit();

        factionLogo = new FactionLogoOverlay(ImageUtils.getImage(FactionUtils.defaultLogo), this, getState());
        factionLogo.onInit();

        factionLogoButton = new GUIIconButton(getState(), (int) factionLogo.getWidth(), (int) factionLogo.getHeight(), factionLogo, factionLogo);
        factionLogoButton.onInit();
        factionLogoButton.getBackgroundColor().set(0, 0, 0, 0);
        factionLogoButton.getSelectedBackgroundColor().set(0, 0, 0, 0);
        factionLogoButton.getPressedColor().set(1.0f, 1.0f, 1.0f, 0.3f);
        factionLogoButton.getSelectColor().set(1.0f, 1.0f, 1.0f, 0.3f);
        getContent().attach(factionLogoButton);

        nameOverlay = new GUITextOverlay(10, 10, getState());
        nameOverlay.onInit();
        nameOverlay.setTextSimple("");
        nameOverlay.getPos().x = 10;
        nameOverlay.getPos().y = (float) (factionLogo.getSprite().getHeight() / 4) + 15;
        nameOverlay.setWidth(factionLogo.getSprite().getWidth());
        getContent().attach(nameOverlay);

        infoOverlay = new GUITextOverlay(10, 10, getState());
        infoOverlay.onInit();
        infoOverlay.setTextSimple("");
        infoOverlay.getPos().x = 10;
        infoOverlay.getPos().y = nameOverlay.getPos().y + 15;
        infoOverlay.setWidth(factionLogo.getSprite().getWidth());
        getContent().attach(infoOverlay);
    }

    @Override
    public void draw() {
        super.draw();
        factionLogo.getSprite().getPos().x = ((float) (factionLogo.getSprite().getWidth() / 2) + (getContent().getWidth() - factionLogo.getSprite().getWidth()) / 2) * 2;
        factionLogo.getSprite().getPos().y = (float) factionLogo.getSprite().getHeight() / 2;
    }

    public void updateLogo(final String newPath) {
        ImageUtils.getImage(newPath);
        new StarRunnable() {
            @Override
            public void run() {
                factionLogo.setSprite(ImageUtils.getImage(newPath));
            }
        }.runLater(BetterFactions.getInstance(), 15);
    }

    public void setNameText(String nameText) {
        nameOverlay.setTextSimple(nameText);
        nameOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        nameOverlay.updateTextSize();
        nameOverlay.limitTextWidth = 15;
    }

    public void setNameText(String nameText, Color color) {
        nameOverlay.setTextSimple(nameText);
        nameOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        nameOverlay.setColor(color);
        nameOverlay.updateTextSize();
        nameOverlay.limitTextWidth = 15;
    }

    public void setInfoText(String newText) {
        infoOverlay.setTextSimple(newText);
        infoOverlay.setFont(FontLibrary.FontSize.SMALL.getFont());
        infoOverlay.updateTextSize();
        infoOverlay.limitTextWidth = 15;
    }

    public void setFaction(Faction faction) {
        factionLogo.setFaction(faction);
    }
}
