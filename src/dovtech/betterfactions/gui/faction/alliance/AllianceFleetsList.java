package dovtech.betterfactions.gui.faction.alliance;

import dovtech.betterfactions.entity.fleet.BetterFleet;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.ScrollableTableList;
import org.schema.schine.input.InputState;
import java.util.Collection;
import java.util.Set;

public class AllianceFleetsList extends ScrollableTableList<BetterFleet> {

    public AllianceFleetsList(InputState state, GUIElement element) {
        super(state, 150.0F, 150.0F, element);
    }

    @Override
    public void initColumns() {

    }

    @Override
    protected Collection<BetterFleet> getElementList() {
        return null;
    }

    @Override
    public void updateListEntries(GUIElementList guiElementList, Set<BetterFleet> set) {

    }
}
