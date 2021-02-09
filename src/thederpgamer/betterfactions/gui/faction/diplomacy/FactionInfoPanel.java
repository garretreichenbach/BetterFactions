package thederpgamer.betterfactions.gui.faction.diplomacy;

import api.utils.StarRunnable;
import org.newdawn.slick.Color;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIIconButton;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.other.Vector2i;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.ImageUtils;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.input.InputState;

/**
 * FactionInfoPanel.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionInfoPanel extends GUIAncor {

    private GUIIconButton factionLogoButton;
    private FactionLogoOverlay factionLogo;
    private GUITextOverlay nameOverlay;
    private GUITextOverlay infoOverlay;

    public FactionInfoPanel(InputState inputState) {
        super(inputState);
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
        attach(factionLogoButton);

        nameOverlay = new GUITextOverlay(10, 10, getState());
        nameOverlay.autoWrapOn = this;
        nameOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        nameOverlay.setTextSimple("No Faction");
        nameOverlay.onInit();
        nameOverlay.getPos().x = 10;
        nameOverlay.getPos().y = 30;
        nameOverlay.setWidth((int) (nameOverlay.getWidth() - 4));
        attach(nameOverlay);

        infoOverlay = new GUITextOverlay(10, 10, getState());
        infoOverlay.autoWrapOn = this;
        infoOverlay.setFont(FontLibrary.FontSize.SMALL.getFont());
        infoOverlay.setTextSimple("");
        infoOverlay.onInit();
        infoOverlay.getPos().x = 10;
        infoOverlay.getPos().y = nameOverlay.getPos().y + 10;
        infoOverlay.setWidth((int) (infoOverlay.getWidth() - 4));
        attach(infoOverlay);
    }

    @Override
    public void draw() {
        super.draw();
        factionLogoButton.setImagePos(getLogoPos().x, getLogoPos().y);
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
        nameOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        nameOverlay.setTextSimple(nameText);
    }

    public void setNameText(String nameText, Color color) {
        nameOverlay.setFont(FontLibrary.FontSize.MEDIUM.getFont());
        nameOverlay.setColor(color);
        nameOverlay.setTextSimple(nameText);
    }

    public void setInfoText(String newText) {
        infoOverlay.setFont(FontLibrary.FontSize.SMALL.getFont());
        infoOverlay.setTextSimple(newText);
    }

    public void setFaction(Faction faction) {
        factionLogo.setFaction(faction);
    }

    private Vector2i getLogoPos() {
        int logoX = (int) ((factionLogo.getSprite().getWidth() / 2) + (getWidth() - factionLogo.getSprite().getWidth()));
        int logoY = (factionLogo.getSprite().getHeight() / 2) + 10;
        return new Vector2i(logoX, logoY);
    }
}