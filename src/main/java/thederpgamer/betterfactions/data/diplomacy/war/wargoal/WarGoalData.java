package thederpgamer.betterfactions.data.diplomacy.war.wargoal;

import api.common.GameCommon;
import org.schema.game.common.data.player.faction.Faction;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class WarGoalData {

	public enum WarGoalType {
		TAKE_CLAIMED_SYSTEM("Take Claimed System", "Take a claimed system from the enemy faction.", 1.5f, 1.75f),
		PUPPET("Puppet", "Create a puppet government out of some or all of the enemy faction's territory.", 1.8f, 2.7f),
		DISMANTLE("Dismantle", "Free some of the enemy faction's territory from their control as independents.", 1.75f, 2.65f),
		ASSIST_ALLY("Assist Ally", "Assist an ally in a war against the enemy faction.", 1.15f, 1.25f),
		INTERVENTION("Intervention", "Intervene in a war between two or more other factions.", 1.2f, 1.3f),
		DEMAND_CONCESSIONS("Demand Concessions", "Demand concessions from the enemy faction.", 1.35f, 1.7f);

		public final String display;
		public final String description;
		public final float costModifierNPC;
		public final float costModifierPlayer;

		WarGoalType(String display, String description, float costModifierNPC, float costModifierPlayer) {
			this.display = display;
			this.description = description;
			this.costModifierNPC = costModifierNPC;
			this.costModifierPlayer = costModifierPlayer;
		}

		public static WarGoalType getWarGoalType(String warGoalType) {
			for(WarGoalType type : WarGoalType.values()) {
				if(type.name().equalsIgnoreCase(warGoalType)) return type;
			}
			return null;
		}
	}

	private final WarGoalType warGoalType;
	private final int from;
	private final int to;
	private final float score;
	private boolean demanded;

	public WarGoalData(WarGoalType warGoalType, Faction from, Faction to, float score) {
		this.warGoalType = warGoalType;
		this.from = from.getIdFaction();
		this.to = to.getIdFaction();
		this.score = score;
		this.demanded = false;
	}

	public WarGoalType getWarGoalType() {
		return warGoalType;
	}

	public Faction getFrom() {
		return GameCommon.getGameState().getFactionManager().getFaction(from);
	}

	public Faction getTo() {
		return GameCommon.getGameState().getFactionManager().getFaction(to);
	}

	public float getScore() {
		return score;
	}

	public void setDemanded(boolean demanded) {
		this.demanded = demanded;
	}
}
