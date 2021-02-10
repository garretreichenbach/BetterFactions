package thederpgamer.betterfactions.gui.faction.news;

import thederpgamer.betterfactions.gui.NewFactionPanel;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;

/**
 * FactionNewsTab.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionNewsTab extends GUIContentPane {

    private NewFactionPanel guiPanel;
    private FactionNewsScrollableList newsList;

    public FactionNewsTab(InputState state, GUIWindowInterface window, NewFactionPanel guiPanel) {
        super(state, window, Lng.str("NEWS"));
        this.guiPanel = guiPanel;
    }

    @Override
    public void onInit() {
        super.onInit();
        (newsList = new FactionNewsScrollableList(getState(), getContent(0), this)).onInit();
        getContent(0).attach(newsList);
    }
}
