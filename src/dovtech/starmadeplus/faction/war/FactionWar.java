package dovtech.starmadeplus.faction.war;

import java.util.ArrayList;

public class FactionWar {

    private ArrayList<WarParticipant> attackers;
    private ArrayList<WarParticipant> defenders;

    public FactionWar(WarParticipant attacker, WarParticipant defender) {
        this.attackers = new ArrayList<>();
        this.defenders = new ArrayList<>();
        this.attackers.add(attacker);
        this.defenders.add(defender);
    }

    public FactionWar(ArrayList<WarParticipant> attackers, ArrayList<WarParticipant> defenders) {
        this.attackers = attackers;
        this.defenders = defenders;
    }

    public ArrayList<WarParticipant> getAttackers() {
        return attackers;
    }

    public ArrayList<WarParticipant> getDefenders() {
        return defenders;
    }
}