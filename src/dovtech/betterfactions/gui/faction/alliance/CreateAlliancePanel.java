package dovtech.betterfactions.gui.faction.alliance;

import api.common.GameClient;
import dovtech.betterfactions.BetterFactions;
import dovtech.betterfactions.faction.BetterFaction;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import dovtech.betterfactions.faction.government.AllianceGovernmentType;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.input.InputState;

import java.util.ArrayList;

public class CreateAlliancePanel extends GUIElement {

    private InputState state;
    private float width;
    private float height;
    private GUITextInput allianceNameInput;
    private GUIDropDownList governmentTypeList;
    private AllianceGovernmentType governmentType;
    private GUITextButton okButton;
    private GUITextButton cancelButton;

    public CreateAlliancePanel(InputState state) {
        super(state);
        this.state = state;
        this.width = 120.0F;
        this.height = 80.0F;
        this.allianceNameInput = new GUITextInput(80, 30, state);
        this.governmentTypeList = new GUIDropDownList(state, 50, 30, 100, new DropDownCallback() {
            @Override
            public void onSelectionChanged(GUIListElement guiListElement) {
                governmentType = AllianceGovernmentType.valueOf((String) guiListElement.getContent().getUserPointer());
            }
        });
        for(AllianceGovernmentType govType : AllianceGovernmentType.values()) {
            GUIListElement listElement = new GUIListElement(state);
            GUITextOverlay listOverlay = new GUITextOverlay(15, 5, state);
            listOverlay.setTextSimple(govType.displayName);
            listElement.setContent(listOverlay);
            governmentTypeList.add(listElement);
        }

        this.okButton = new GUITextButton(state, 85, 20, GUITextButton.ColorPalette.OK, new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                   //Todo
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });

        this.attach(allianceNameInput);
        this.attach(governmentTypeList);
        onInit();
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public void cleanUp() {

    }

    @Override
    public void draw() {
        allianceNameInput.draw();
        governmentTypeList.draw();
    }

    @Override
    public void onInit() {
        this.allianceNameInput.onInit();
        this.governmentTypeList.onInit();
    }
}
