package dovtech.starmadeplus.faction.war;

import org.schema.game.common.data.player.faction.Faction;
import java.util.ArrayList;
import java.util.Arrays;

public class WarGoal {

    private Faction faction;
    private Faction target;
    private WarGoalType warGoalType;
    private ArrayList<Object> demands;

    public WarGoal(Faction faction, Faction target, WarGoalType warGoalType, Object[] demands) {
        this.faction = faction;
        this.target = target;
        this.demands = new ArrayList<>(Arrays.asList(demands));
        this.warGoalType = warGoalType;
    }

    public Faction getFaction() {
        return faction;
    }

    public Faction getTarget() {
        return target;
    }

    public WarGoalType getWarGoalType() {
        return warGoalType;
    }

    public Object[] getDemands() {
        return demands.toArray();
    }

    public void addDemand(Object demand) {
        demands.add(demand);
    }

    public void removeDemand(Object demand) {
        demands.remove(demand);
    }

    public enum WarGoalType {
        COALITION,
        TERRITORY,
        DEFEAT,
        RIVALRY,
        CREDITS,
        RESOURCES,
        DIPLOMATIC_INCIDENT
    }
}
