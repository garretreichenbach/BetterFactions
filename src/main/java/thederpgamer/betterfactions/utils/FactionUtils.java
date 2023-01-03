package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import api.utils.other.HashList;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacy;
import thederpgamer.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import thederpgamer.betterfactions.data.diplomacy.war.wargoal.WarGoalData;
import thederpgamer.betterfactions.manager.FactionDiplomacyManager;

import java.util.ArrayList;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionUtils {

	private static ModSkeleton getInstance() {
		return BetterFactions.getInstance().getSkeleton();
	}

	public static FactionDiplomacy getDiplomacy(Faction faction) {
		return FactionDiplomacyManager.getDiplomacy(faction.getIdFaction());
	}

	public static FactionRelation getRelation(Faction from, Faction to) {
		return new FactionRelation(from.getIdFaction(), to.getIdFaction(), GameCommon.getGameState().getFactionManager().getRelation(from.getIdFaction(), to.getIdFaction()).code);
	}

	public static ArrayList<WarGoalData> getWarGoalsOn(Faction from, Faction to) {
		ArrayList<WarGoalData> warGoals = new ArrayList<>();
		for(Object obj : PersistentObjectUtil.getObjects(getInstance(), WarGoalData.class)) {
			WarGoalData warGoal = (WarGoalData) obj;
			if(warGoal.getFrom().getIdFaction() == from.getIdFaction() && warGoal.getTo().getIdFaction() == to.getIdFaction()) warGoals.add(warGoal);
		}
		return warGoals;
	}

	public static HashList<Faction, WarGoalData> getAllWarGoals(Faction from) {
		HashList<Faction, WarGoalData> warGoals = new HashList<>();
		for(Object obj : PersistentObjectUtil.getObjects(getInstance(), WarGoalData.class)) {
			WarGoalData warGoal = (WarGoalData) obj;
			if(warGoal.getFrom().getIdFaction() == from.getIdFaction()) warGoals.add(warGoal.getTo(), warGoal);
		}
		return warGoals;
	}

	public static boolean hasValidWarGoal(Faction from, Faction to) {
		return getWarGoalsOn(from, to).size() > 0;
	}

	public static boolean isAtWar(Faction from, Faction to) {
		return GameCommon.getGameState().getFactionManager().isEnemy(from.getIdFaction(), to.getIdFaction());
	}

	public static boolean canAttack(Faction from, Faction to) {
		if(hasValidWarGoal(from, to)) {
			FactionRelation relation = getRelation(from, to);
			return relation.getRelation().equals(FactionRelation.RType.NEUTRAL);
		} else return false;
	}

	public static boolean canPeace(Faction from, Faction to) {
		if(isAtWar(from, to)) {
			if(from.isNPC()) return NPCFactionUtils.isWillingToDoAction(to, from, FactionDiplomacyAction.DiploActionType.ACCEPT_PEACE_OFFER);
			else return true;
		} else return false;
	}
}
