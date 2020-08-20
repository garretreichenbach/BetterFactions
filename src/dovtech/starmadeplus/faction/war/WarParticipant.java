package dovtech.starmadeplus.faction.war;

import org.schema.game.common.data.player.faction.Faction;
import java.util.ArrayList;

public class WarParticipant {

    private Faction faction;
    private ArrayList<WarGoal> warGoals;
    private double warExhaustion;
    private double warParticipation;

    public WarParticipant(Faction faction, ArrayList<WarGoal> warGoals) {
        this.faction = faction;
        this.warGoals = warGoals;
        warExhaustion = 0.0;
        warParticipation = 0.0;
    }

    public Faction getFaction() {
        return faction;
    }

    public ArrayList<WarGoal> getWarGoals() {
        return warGoals;
    }

    public void addWarGoal(WarGoal warGoal) {
        this.warGoals.add(warGoal);
    }

    public void removeWarGoal(WarGoal warGoal) {
        this.warGoals.remove(warGoal);
    }

    public double getWarExhaustion() {
        return warExhaustion;
    }

    public void setWarExhaustion(double warExhaustion) {
        this.warExhaustion = warExhaustion;
    }

    public double getWarParticipation() {
        return warParticipation;
    }

    public void setWarParticipation(double warParticipation) {
        this.warParticipation = warParticipation;
    }
}
