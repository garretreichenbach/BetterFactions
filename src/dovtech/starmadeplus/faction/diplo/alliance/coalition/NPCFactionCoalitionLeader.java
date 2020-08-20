package dovtech.starmadeplus.faction.diplo.alliance.coalition;

import dovtech.starmadeplus.faction.diplo.relations.FactionMessage;
import dovtech.starmadeplus.faction.diplo.alliance.FactionWarParticipantInterface;
import dovtech.starmadeplus.faction.diplo.alliance.assistance.FactionAssistance;
import org.schema.game.common.data.player.faction.Faction;

public class NPCFactionCoalitionLeader implements FactionWarParticipantInterface, FactionCoalitionLeaderInterface {

    private Coalition coalition;
    private Faction leader;

    public NPCFactionCoalitionLeader(Coalition coalition) {
        this.coalition = coalition;
        this.leader = this.coalition.getCoalitionLeader();
    }

    @Override
    public void requestAssistance(FactionAssistance[] assistance) {

    }

    @Override
    public void allianceMessage(FactionMessage message) {

    }

    @Override
    public void inviteToCoalition(Faction faction) {
        
    }
}
