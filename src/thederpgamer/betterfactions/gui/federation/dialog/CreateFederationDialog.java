package thederpgamer.betterfactions.gui.federation.dialog;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerGameTextInput;
import org.schema.schine.common.InputChecker;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.manager.FederationManager;
import java.util.regex.Pattern;

/**
 * CreateFederationDialog.java
 * <Description>
 * ==================================================
 * Created 02/08/2021
 * @author TheDerpGamer
 */
public class CreateFederationDialog extends PlayerGameTextInput {

    private final int minNameLength = 1;
    private final int maxNameLength = 60;
    private final FactionData from;
    private final FactionData to;

    public CreateFederationDialog(FactionData from, FactionData to, String descriptionString) {
        super("CreateFederationDialog", GameClient.getClientState(), 420, 210, 23, Lng.str("Create Federation"), descriptionString, null);
        this.from = from;
        this.to = to;
        this.setInputChecker(new InputChecker() {
            public boolean check(String input, TextCallback textCallback) {
                if(input.length() >= minNameLength && input.length() <= maxNameLength) {
                    if(Pattern.matches("[a-zA-Z0-9 _-]+", input)) {
                        if(!FederationManager.federationExists(input)) {
                            return true;
                        } else {
                            textCallback.onFailedTextCheck(input + " is the name of an already existing federation!");
                        }
                    } else {
                        textCallback.onFailedTextCheck("Please only alphanumeric (and space, _, -)!");
                    }
                } else {
                    textCallback.onFailedTextCheck("Please enter name between " + minNameLength + " and " + maxNameLength + " characters long!");
                }
                return false;
            }
        });
    }

    @Override
    public void onDeactivate() {

    }

    @Override
    public boolean onInput(String s) {
        FederationManager.createNewFederation(s, from, to);
        return true;
    }

    @Override
    public String[] getCommandPrefixes() {
        return new String[0];
    }

    @Override
    public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
        return s;
    }

    @Override
    public void onFailedTextCheck(String s) {
        setErrorMessage(Lng.str(s));
    }
}
