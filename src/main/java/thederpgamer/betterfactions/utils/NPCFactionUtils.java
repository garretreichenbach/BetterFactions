package thederpgamer.betterfactions.utils;

import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.diplomacy.war.WarData;
import thederpgamer.betterfactions.data.diplomacy.war.wargoal.WarGoalData;
import thederpgamer.betterfactions.manager.WarManager;

import java.util.ArrayList;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class NPCFactionUtils {

	public static boolean isWillingToAcceptPeaceOffer(Faction from, Faction to, WarData warData, StringBuilder reason) {
		if(warData.isInvolved(from) && warData.isInvolved(to)) {
			if(WarManager.isOpposingSides(from, to, warData)) {
				//If both factions are on opposite sides of the war, check the war goal progress for each
				float fromProgress = warData.getTotalProgress(from);
				float toProgress = warData.getTotalProgress(to);
				float difference = fromProgress - toProgress;
				float exhaustion = warData.getTotalExhaustion(to);
				float acceptance = 0.0f;

				acceptance += difference;
				acceptance += exhaustion;
				boolean willing = acceptance > 0.0f;
				if(willing) reason.append("Will Accept:\n");
				else reason.append("Will Not Accept:\n");

				if(exhaustion >= 50.0f) reason.append(" + ").append(to).append(" has high war exhaustion.\n");
				else if(exhaustion >= 30.0f) reason.append(" + ").append(to).append(" has moderate war exhaustion.\n");
				else if(exhaustion >= 15.0f) reason.append(" - ").append(to).append(" has low war exhaustion.\n");

				if(difference >= 65) reason.append(" + ").append(to.getName()).append(" is losing by ").append(difference).append("%");
				else if(difference >= 30) reason.append(" + ").append(to.getName()).append(" is losing by ").append(difference).append("%");
				else if(difference >= 15) reason.append(" - ").append(to.getName()).append(" is losing by ").append(difference).append("%");
				else if(difference >= 5) reason.append(" - ").append(to.getName()).append(" is losing by ").append(difference).append("%");
				else reason.append(" - ").append(to.getName()).append(" is winning by ").append(difference).append("%");
				return willing;
			} else {
				//Otherwise, check the current peace offer demands and how many of them meet the demands put forth by the NPC faction.
				//Each demand is worth a certain amount of point, and if the total amount of points in the peace offer is at least half of the points in the NPC faction's demands, the NPC faction will accept the peace offer.
				ArrayList<WarGoalData> npcGoals = warData.getGoals(to);
				float npcPoints = 0.0f;
				for(WarGoalData npcGoal : npcGoals) npcPoints += npcGoal.getScore();
				ArrayList<WarGoalData> demanded = warData.getGoals(from);
				float demandedPoints = 0.0f;
				for(WarGoalData demand : demanded) demandedPoints += demand.getScore();
				float acceptance = demandedPoints / npcPoints;
				boolean willing = acceptance >= 0.5f;
				if(willing) reason.append("Will Accept:\n + Most of ").append(to.getName()).append("'s demands are met.\n");
				else reason.append("Will Not Accept:\n - Most of ").append(to.getName()).append("'s demands are not met.\n");
				return willing;
			}
		} else return false;
	}

	public static boolean isWillingToAssistInWar(Faction from, Faction ally, Faction target, String reason) {
		int targetPower = FactionUtils.getPower(target);
		int allyPower = FactionUtils.getPower(ally);
		int difference = allyPower - targetPower;
		return FactionUtils.getOpinion(from, ally) >= 30 && FactionUtils.hasValidWarGoal(ally, target) && WarManager.getWarsInvolvedIn(ally).isEmpty() && difference >= -30;
	}
}
