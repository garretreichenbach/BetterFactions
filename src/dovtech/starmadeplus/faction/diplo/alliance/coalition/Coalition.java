package dovtech.starmadeplus.faction.diplo.alliance.coalition;

import dovtech.starmadeplus.faction.war.FactionWar;
import dovtech.starmadeplus.faction.war.WarGoal;
import dovtech.starmadeplus.faction.war.WarParticipant;
import org.schema.game.common.data.player.faction.Faction;
import java.util.ArrayList;

public class Coalition {

    private boolean npcCoalition = false;
    private Faction coalitionTarget;
    private Faction coalitionLeader;
    private ArrayList<Faction> coalitionMembers;
    private FactionWar war;

    public Coalition(Faction coalitionTarget, Faction coalitionLeader, FactionWar war) {
        if(coalitionLeader.isNPC()) npcCoalition = true;
        this.coalitionTarget = coalitionTarget;
        this.coalitionLeader = coalitionLeader;
        this.war = war;
        coalitionMembers = new ArrayList<>();
        coalitionMembers.add(coalitionLeader);
        for(WarParticipant warParticipant : war.getAttackers()) {
            WarGoal coalitionGoal = new WarGoal(warParticipant.getFaction(), this.coalitionTarget, WarGoal.WarGoalType.COALITION, new Object[] {});
            warParticipant.addWarGoal(coalitionGoal);
        }
    }

    public boolean isNpcCoalition() {
        return npcCoalition;
    }

    public Faction getCoalitionTarget() {
        return coalitionTarget;
    }

    public Faction getCoalitionLeader() {
        return coalitionLeader;
    }

    public void setCoalitionLeader(Faction coalitionLeader) {
        this.coalitionLeader = coalitionLeader;
    }

    public ArrayList<Faction> getCoalitionMembers() {
        return coalitionMembers;
    }

    public void addMember(Faction member) {
        coalitionMembers.add(member);
    }

    public void removeMember(Faction member) {
        coalitionMembers.remove(member);
    }

    public FactionWar getWar() {
        return war;
    }
}