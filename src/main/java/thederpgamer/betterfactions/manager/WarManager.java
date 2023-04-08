package thederpgamer.betterfactions.manager;

import api.mod.config.PersistentObjectUtil;
import api.utils.other.HashList;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.diplomacy.war.WarData;
import thederpgamer.betterfactions.data.diplomacy.war.wargoal.WarGoalData;

import java.util.ArrayList;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class WarManager {

	public static HashList<WarData, WarGoalData> getWarsInvolvedIn(Faction faction) {
		HashList<WarData, WarGoalData> wars = new HashList<>();
		for(Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), WarData.class)) {
			WarData warData = (WarData) obj;
			if(warData.isInvolved(faction)) wars.put(warData, warData.getGoals(faction));
		}
		return wars;
	}

	public static ArrayList<WarData> getAllWars() {
		ArrayList<WarData> wars = new ArrayList<>();
		for(Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), WarData.class)) {
			WarData warData = (WarData) obj;
			wars.add(warData);
		}
		return wars;
	}

	public static boolean isOpposingSides(Faction from, Faction to, WarData warData) {
		if(from.getIdFaction() == to.getIdFaction()) return false;
		for(WarGoalData warGoal : warData.getGoals(from)) {
			if(warGoal.getTo().getIdFaction() == to.getIdFaction()) return true;
		}
		return false;
	}
}
