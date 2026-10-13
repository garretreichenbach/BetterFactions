package videogoose.betterfactions.gui;

import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.InputChecker;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.Senate;
import videogoose.betterfactions.diplomacy.DiplomacyProposal;

import java.util.function.Consumer;

/**
 * Shown when a member takes an act their faction's Senate votes on: says what goes to the vote and asks,
 * optionally, why. Cancelling sends nothing.
 */
public final class SenateProposeDialog {

    private SenateProposeDialog() {
    }

    public static void open(GameClientState state, String description, Consumer<String> send) {
        Faction own = state.getFaction();
        int seats = own == null ? 1 : Senate.seats(own);
        PlayerTextAreaInput t = new PlayerTextAreaInput("SENATE_PROPOSE", state, DiplomacyProposal.MAX_JUSTIFICATION, 4,
                Lng.str("Put to the Senate"),
                Lng.str("The Senate will vote on: %s\n%s senators; it carries at %s for, or closes in %s hours. You count as voting for if you sit.\n\nJustification (optional): why should they vote for it? It cannot be edited once the vote opens.",
                        description, seats, seats / 2 + 1, own == null ? BFConfig.senateVoteHours()
                                : videogoose.betterfactions.civics.Civics.client().of(own.getIdFaction()).voteHours(BFConfig.senateVoteHours(), BFConfig.senateMaxVoteHours())), "") {
            @Override
            public String[] getCommandPrefixes() {
                return null;
            }

            @Override
            public String handleAutoComplete(String s, TextCallback callback, String prefix) throws PrefixNotFoundException {
                return null;
            }

            @Override
            public void onFailedTextCheck(String msg) {
            }

            @Override
            public boolean isOccluded() {
                return false;
            }

            @Override
            public void onDeactivate() {
            }

            @Override
            public boolean onInput(String entry) {
                send.accept(entry == null ? "" : entry.trim());
                return true;
            }
        };
        t.activate();
        t.setInputChecker(new InputChecker() {
            @Override
            public boolean check(String entry, TextCallback callback) {
                return true;
            }
        });
    }
}
