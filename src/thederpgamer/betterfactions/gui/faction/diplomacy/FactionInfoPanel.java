package thederpgamer.betterfactions.gui.faction.diplomacy;

import org.newdawn.slick.Color;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUIIconButton;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.manager.ResourceManager;

/**
 * FactionInfoPanel.java
 * <Description>
 *
 * @since 01/30/2021
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

        factionLogo = new FactionLogoOverlay(ResourceManager.getSprite("default-logo"), this, getState());
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
        nameOverlay.getPos().y = 160;
        nameOverlay.setWidth((int) (nameOverlay.getWidth() - 4));
        attach(nameOverlay);

        infoOverlay = new GUITextOverlay(10, 10, getState());
        infoOverlay.autoWrapOn = this;
        infoOverlay.setFont(FontLibrary.FontSize.SMALL.getFont());
        infoOverlay.setTextSimple("");
        infoOverlay.onInit();
        infoOverlay.getPos().x = 10;
        infoOverlay.getPos().y = nameOverlay.getPos().y + 30;
        infoOverlay.setWidth((int) (infoOverlay.getWidth() - 4));
        attach(infoOverlay);
    }

    @Override
    public void draw() {
        super.draw();
        factionLogoButton.setImagePos((int) factionLogoButton.getPos().x + 100, (int) factionLogoButton.getPos().y + 100);
    }

    public void updateLogo(Sprite logo) {
        factionLogo.setSprite(logo);
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
}