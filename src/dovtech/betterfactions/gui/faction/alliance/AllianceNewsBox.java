package dovtech.betterfactions.gui.faction.alliance;

import dovtech.betterfactions.faction.diplo.alliance.AllianceNews;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class AllianceNewsBox extends ScrollableTableList<AllianceNews> {

    private ArrayList<AllianceNews> news;

    public AllianceNewsBox(InputState state, GUIElement element) {
        super(state, 150.0F, 150.0F, element);
        this.news = new ArrayList<>();
    }

    @Override
    public void initColumns() {

    }

    @Override
    protected Collection<AllianceNews> getElementList() {
        return news;
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<AllianceNews> set) {

    }
}
