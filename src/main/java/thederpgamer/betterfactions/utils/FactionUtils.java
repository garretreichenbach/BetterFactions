package thederpgamer.betterfactions.utils;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import api.utils.other.HashList;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacy;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacyEntity;
import thederpgamer.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import thederpgamer.betterfactions.data.diplomacy.modifier.FactionDiplomacyStaticMod;
import thederpgamer.betterfactions.data.diplomacy.war.WarData;
import thederpgamer.betterfactions.data.diplomacy.war.wargoal.WarGoalData;
import thederpgamer.betterfactions.manager.FactionDiplomacyManager;
import thederpgamer.betterfactions.manager.WarManager;

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

	public static boolean canPeace(Faction from, Faction to, String reason, WarData warData) {
		if(isAtWar(from, to)) {
			StringBuilder builder = new StringBuilder();
			boolean willing = NPCFactionUtils.isWillingToAcceptPeaceOffer(to, from, warData, builder);
			reason = builder.toString();
			if(from.isNPC()) return willing;
			else return true;
		} else return false;
	}

	public static int getPower(Faction from) {

	}

	public static ArrayList<Faction> getAllies(Faction from) {
		ArrayList<Faction> allies = new ArrayList<>();
		allies.addAll(from.getFriends());
		FactionDiplomacy diplomacy = getDiplomacy(from);
		for(FactionDiplomacyEntity entity : diplomacy.entities.values()) {
			if(entity.getDbId() != from.getIdFaction()) {
				for(FactionDiplomacyStaticMod mod : entity.getStaticMap().values()) {
					switch(mod.type) {
						case PROTECTING:
						case BEING_PROTECTED:
						case IN_FEDERATION:
						case FEDERATION_ALLY:
						case ALLIANCE:
							allies.add(GameCommon.getGameState().getFactionManager().getFaction(entity.getDbId()));
							break;
					}
				}
			}
		}
		return allies;
	}

	public static int getOpinion(Faction from, Faction to) {

	}
}
