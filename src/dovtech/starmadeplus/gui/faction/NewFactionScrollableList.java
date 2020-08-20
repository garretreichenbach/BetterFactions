package dovtech.starmadeplus.gui.faction;

import org.schema.game.client.view.gui.faction.newfaction.FactionScrollableListNew;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.GUITextButton;
import org.schema.schine.input.InputState;
import java.util.Observer;
import java.util.Set;

public class NewFactionScrollableList extends FactionScrollableListNew implements Observer {
    public NewFactionScrollableList(InputState inputState, GUIElement guiElement) {
        super(inputState, guiElement);
    }

    @Override
    public void updateListEntries(GUIElementList elementList, Set<Faction> factionSet) {
        super.updateListEntries(elementList, factionSet);
        GUITextButton factionInfoButton = new GUITextButton(this.getState(), 130, 24, "FACTION INFO", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    FactionInfoWindow infoWindow = new FactionInfoWindow(NewFactionScrollableList.super.getState(), 300, 300, "FACTION INFO", NewFactionScrollableList.super.getSelectedRow().f);
                    infoWindow.draw();
                }
            }

            @Override
            public boolean isOccluded() {
                return !NewFactionScrollableList.super.isActive();
            }
        });
        super.extendedRow.expanded.get(0).attach(factionInfoButton);
    }
}
