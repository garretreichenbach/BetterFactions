package dovtech.betterfactions.gui.faction.alliance;

import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.input.InputState;

public class AllianceStatsList extends GUIElementList {

    private Alliance alliance;

    public AllianceStatsList(InputState inputState, Alliance alliance) {
        super(inputState);
        this.alliance = alliance;
    }

    public Alliance getAlliance() {
        return alliance;
    }
}
