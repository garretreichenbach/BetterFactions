package thederpgamer.betterfactions.utils;

import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.serializeable.war.WarData;
import thederpgamer.betterfactions.data.serializeable.war.WarParticipantData;
import thederpgamer.betterfactions.manager.FactionManager;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [03/06/2022]
 */
public class FactionDiplomacyUtils {

	public static FactionData getAttackerLeader(WarData warData) {
		for(WarParticipantData participantData : warData.attackers.values()) {
			if(participantData.warGoal.warGoalType.warLeader) return FactionManager.getFactionData(participantData.factionId);
		}
		return null;
	}

	public static FactionData getDefenderLeader(WarData warData) {
		for(WarParticipantData participantData : warData.defenders.values()) {
			if(participantData.warGoal.warGoalType.warLeader) return FactionManager.getFactionData(participantData.factionId);
		}
		return null;
	}
}
