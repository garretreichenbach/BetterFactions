package videogoose.betterfactions.utils;

import api.common.GameCommon;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.data.serializeable.war.WarData;
import videogoose.betterfactions.data.serializeable.war.WarParticipantData;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [03/06/2022]
 */
public class FactionDiplomacyUtils {

	public static Faction getAttackerLeader(WarData warData) {
		for(WarParticipantData participantData : warData.attackers.values()) {
			if(participantData.warGoal.warGoalType.warLeader) {
				return GameCommon.getGameState().getFactionManager().getFaction(participantData.factionId);
			}
		}
		return null;
	}

	public static Faction getDefenderLeader(WarData warData) {
		for(WarParticipantData participantData : warData.defenders.values()) {
			if(participantData.warGoal.warGoalType.warLeader) {
				return GameCommon.getGameState().getFactionManager().getFaction(participantData.factionId);
			}
		}
		return null;
	}
}
