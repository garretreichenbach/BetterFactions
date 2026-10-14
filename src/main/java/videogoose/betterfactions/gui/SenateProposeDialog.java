package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.Senate;
import videogoose.betterfactions.diplomacy.DiplomacyProposal;

import java.util.function.Consumer;

/**
 * Shown when a member takes an act their faction's Senate votes on: says what goes to the vote and asks,
 * optionally, why. Cancelling sends nothing.
 */
public final class SenateProposeDialog extends PlayerInput {

    private static final int FULL = 480;

    private final Consumer<String> send;
    private final String info;
    private final Panel panel;

    private SenateProposeDialog(GameClientState state, String info, Consumer<String> send) {
        super(state);
        this.info = info;
        this.send = send;
        panel = new Panel(state);
    }

    public static void open(GameClientState state, String description, Consumer<String> send) {
        Faction own = state.getFaction();
        int seats = own == null ? 1 : Senate.seats(own);
        int hours = own == null ? BFConfig.senateVoteHours()
                : Civics.client().of(own.getIdFaction()).voteHours(BFConfig.senateVoteHours(), BFConfig.senateMaxVoteHours());
        new SenateProposeDialog(state,
                Lng.str("The Senate will vote on: %s\n%s senators; it carries at %s for, or closes in %s hours. You count as voting for if you sit.",
                        description, seats, seats / 2 + 1, hours), send).activate();
    }

    @Override
    public void callback(GUIElement callingGuiElement, MouseEvent event) {
        if (!event.pressedLeftMouse()) {
            return;
        }
        Object p = callingGuiElement.getUserPointer();
        if ("OK".equals(p)) {
            //read before deactivating: the panel is torn down with the dialog
            String why = panel.field.getText();
            deactivate();
            send.accept(why == null ? "" : why.trim());
        } else if ("CANCEL".equals(p) || "X".equals(p)) {
            cancel();
        }
    }

    @Override
    public GUIElement getInputPanel() {
        return panel;
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    public boolean isOccluded() {
        return false;
    }

    @Override
    public void handleMouseEvent(MouseEvent e) {
    }

    private class Panel extends GUIInputPanel {

        private GUIActivatableTextBar field;

        Panel(GameClientState state) {
            super("SENATE_PROPOSE_PANEL_V1", state, 520, 360, SenateProposeDialog.this, Lng.str("Put to the Senate"), "");
        }

        @Override
        public void onInit() {
            super.onInit();
            GameClientState s = (GameClientState) getState();

            GUITextOverlay text = new GUITextOverlay(FULL, 100, FontLibrary.FontSize.MEDIUM.getFont(), s);
            text.autoWrapOn = getContent();
            text.setTextSimple(info);
            text.setPos(6, 6, 0);

            GUITextOverlay label = new GUITextOverlay(FULL, 16, FontLibrary.FontSize.MEDIUM.getFont(), s);
            label.setTextSimple(Lng.str("Justification (optional): cannot be edited once the vote opens"));
            label.setPos(6, 120, 0);

            //a fixed-size box: the bar sizes itself to what it depends on, and would fill the dialog otherwise
            GUIAnchor box = new GUIAnchor(s, FULL, 110);
            box.setPos(6, 142, 0);
            field = new GUIActivatableTextBar(s, FontLibrary.FontSize.MEDIUM, DiplomacyProposal.MAX_JUSTIFICATION, 4,
                    Lng.str("Why should they vote for it?"), box, DiplomacyProposeDialog.TEXT, t -> t);
            field.onInit();
            //characters per line, not pixels: roughly what fits the 480px box at MEDIUM
            field.setLineWrap(54);
            box.attach(field);

            getContent().attach(text);
            getContent().attach(label);
            getContent().attach(box);
        }
    }
}
