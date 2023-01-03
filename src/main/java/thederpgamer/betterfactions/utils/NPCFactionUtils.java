package thederpgamer.betterfactions.utils;

import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import thederpgamer.betterfactions.manager.WarManager;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class NPCFactionUtils {

	public static boolean isWillingToDoAction(Faction from, Faction to, FactionDiplomacyAction.DiploActionType actionType) {
		switch(actionType) {
			case ATTACK:
				if(FactionUtils.hasValidWarGoal(from, to)) {
					//Compare faction power between two factions, and include allies that would be willing to help
					//If the faction power is greater than or equal to the power of the target faction and their allies, then return true
					//Otherwise return false
					int power = FactionUtils.getPower(from);
					int targetPower = FactionUtils.getPower(to);
					for(Faction ally : FactionUtils.getAllies(from)) {
						int allyPower = FactionUtils.getPower(ally);
						int difference = allyPower - targetPower;
						//If the ally has a valid war goal, isn't involved in any current wars, and has a relative power strength to the target no less than -30 they are probably willing to join
						if(FactionUtils.hasValidWarGoal(from, ally) && WarManager.getWarsInvolvedIn(ally).isEmpty() && difference >= -30) power += allyPower;
					}
				} else return false;
			default:
				return false;
		}
	}

	public static boolean isWillingToAssistInWar(Faction from, Faction ally, Faction target) {
		int targetPower = FactionUtils.getPower(target);
		int allyPower = FactionUtils.getPower(ally);
		int difference = allyPower - targetPower;
		return FactionUtils.getOpinion(from, ally) >= 30 && FactionUtils.hasValidWarGoal(ally, target) && WarManager.getWarsInvolvedIn(ally).isEmpty() && difference >= -30;
	}
}
