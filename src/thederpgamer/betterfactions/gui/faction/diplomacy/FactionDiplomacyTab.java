package thederpgamer.betterfactions.gui.faction.diplomacy;

import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.gui.NewFactionPanel;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import thederpgamer.betterfactions.gui.elements.data.GUIPositioningInterface;
import thederpgamer.betterfactions.utils.FactionUtils;
import javax.vecmath.Vector2f;

/**
 * FactionDiplomacyTab.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionDiplomacyTab extends GUIContentPane {

    private GUIPositioningInterface posInterface;
    private NewFactionPanel guiPanel;
    private FactionInfoPanel infoPanel;
    private FactionActionsPanel actionsPanel;
    private FactionListPanel listPanel;
    private GUITextOverlay[] cornerPosText;

    public FactionDiplomacyTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("FACTION DIPLOMACY"));
        this.guiPanel = guiPanel;
    }

    @Override
    public void onInit() {
        super.onInit();

        setTextBoxHeightLast(270);
        addDivider(224);
        infoPanel = new FactionInfoPanel(getState());
        addNewTextBox(0, 70);
        actionsPanel = new FactionActionsPanel(getState());
        listPanel = new FactionListPanel(getState());

        infoPanel.onInit();
        actionsPanel.onInit();
        listPanel.onInit();

        if(guiPanel.isInFaction()) {
            FactionData factionData = FactionUtils.getFactionData(guiPanel.getOwnFaction());
            infoPanel.setFaction(guiPanel.getOwnFaction());
            infoPanel.setNameText(guiPanel.getOwnFaction().getName());
            infoPanel.setInfoText(factionData.getInfoString());
            infoPanel.updateLogo(factionData.getFactionLogo());
        } else {
            infoPanel.setNameText("No Faction");
        }
    }

    @Override
    public void draw() {
        super.draw();

        if(BetterFactions.getInstance().debugMode) {
            if(BetterFactions.getInstance().showDebugText) {
                for(GUITextOverlay posTextOverlay : cornerPosText) {
                    posTextOverlay.setVisibility(1);
                    posTextOverlay.draw();
                }
            } else {
                for(GUITextOverlay posTextOverlay : cornerPosText) {
                    posTextOverlay.setVisibility(2);
                    posTextOverlay.cleanUp();
                }
            }
        }
    }

    public GUIPositioningInterface getPosInterface() {
        if(posInterface == null) createPosInterface();
        return posInterface;
    }

    public void createPosInterface() {
        posInterface = new GUIPositioningInterface() {
            @Override
            public Vector2f[] getCorners() {
                Vector2f[] corners = new Vector2f[5];
                corners[0] = new Vector2f(getPos().x - (getWidth() / 2), getPos().y - (getHeight() / 2));
                corners[1] = new Vector2f(getPos().x + (getWidth() / 2), getPos().y - (getHeight() / 2));
                corners[2] = new Vector2f(getPos().x - (getWidth() / 2), getPos().y + (getHeight() / 2));
                corners[3] = new Vector2f(getPos().x + (getWidth() / 2), getPos().y + (getHeight() / 2));
                corners[4] = new Vector2f(getPos().x, getPos().y);
                return corners;
            }

            @Override
            public void createCornerPosText() {
                if(BetterFactions.getInstance().debugMode) {
                    cornerPosText = new GUITextOverlay[5];
                    for(int i = 0; i < cornerPosText.length; i ++) {
                        Vector2f corner = getCorners()[i];
                        GUITextOverlay cornerText = new GUITextOverlay(10, 10, getState());
                        cornerText.onInit();
                        cornerText.setPos(corner.x, corner.y, getPos().z);
                        attach(cornerText);
                        cornerPosText[i] = cornerText;
                    }
                }
            }
        };
        posInterface.createCornerPosText();
    }
}
