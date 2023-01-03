package thederpgamer.betterfactions.data.diplomacy.war;

import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.diplomacy.war.wargoal.WarGoalData;
import thederpgamer.betterfactions.manager.UpdateManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class WarData {

	private final long id;
	private String name;
	private final HashMap<WarGoalData, Float> attackers = new HashMap<>();
	private final HashMap<WarGoalData, Float> defenders = new HashMap<>();

	public WarData(String name) {
		this.id = System.currentTimeMillis();
		this.name = name;
	}

	@Override
	public boolean equals(Object object) {
		if(object instanceof WarData) return ((WarData) object).getId() == id;
		else return false;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HashMap<WarGoalData, Float> getAttackers() {
		return attackers;
	}

	public HashMap<WarGoalData, Float> getDefenders() {
		return defenders;
	}

	public void addAttacker(WarGoalData warGoalData, float score) {
		attackers.put(warGoalData, score);
		sendUpdate();
	}

	public void addDefender(WarGoalData warGoalData, float score) {
		defenders.put(warGoalData, score);
	}

	public void removeAttacker(WarGoalData warGoalData) {
		attackers.remove(warGoalData);
	}

	public void removeDefender(WarGoalData warGoalData) {
		defenders.remove(warGoalData);
	}

	public boolean isInvolved(Faction faction) {
		for(WarGoalData warGoalData : attackers.keySet()) {
			if(warGoalData.getFaction().equals(faction)) return true;
		}
		for(WarGoalData warGoalData : defenders.keySet()) {
			if(warGoalData.getFaction().equals(faction)) return true;
		}
		return false;
	}

	public ArrayList<WarGoalData> getGoals(Faction faction) {
		ArrayList<WarGoalData> goals = new ArrayList<>();
		for(WarGoalData warGoalData : attackers.keySet()) {
			if(warGoalData.getFaction().equals(faction)) goals.add(warGoalData);
		}
		for(WarGoalData warGoalData : defenders.keySet()) {
			if(warGoalData.getFaction().equals(faction)) goals.add(warGoalData);
		}
		return goals;
	}

	public void sendUpdate() {
		UpdateManager.sendUpdate(UpdateManager.UpdateType.UPDATE_WAR_DATA, this);
	}
}
