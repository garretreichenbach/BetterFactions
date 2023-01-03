package thederpgamer.betterfactions.data.commands;

import api.common.GameCommon;
import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import thederpgamer.betterfactions.manager.FactionDiplomacyManager;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class ForceDiploCommand implements CommandInterface {

	@Override
	public String getCommand() {
		return "force_diplo";
	}

	@Override
	public String[] getAliases() {
		return new String[] {
				"/force_diplo"
		};
	}

	@Override
	public String getDescription() {
		return "Force a diplomatic action between two factions.\n" +
				"%COMMAND% <faction1> <faction2> <action> : Force a diplomatic action between two factions.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args.length != 3) return false;
		else {
			int faction1 = Integer.parseInt(args[0]);
			int faction2 = Integer.parseInt(args[1]);
			String action = args[2];
			if(GameCommon.getGameState().getFactionManager().existsFaction(faction1) && GameCommon.getGameState().getFactionManager().existsFaction(faction2)) {
				try {
					FactionDiplomacyAction.DiploActionType diplomacyAction = FactionDiplomacyAction.DiploActionType.valueOf(action.toUpperCase(Locale.ROOT));
					FactionDiplomacyManager.forceDiplomacyAction(faction1, faction2, diplomacyAction);
					return true;
				} catch(IllegalArgumentException ignored) {}
			} return false;
		}
	}

	@Override
	public void serverAction(@Nullable PlayerState sender, String[] args) {

	}

	@Override
	public StarMod getMod() {
		return BetterFactions.getInstance();
	}
}
