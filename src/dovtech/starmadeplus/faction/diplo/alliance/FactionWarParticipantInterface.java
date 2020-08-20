package dovtech.starmadeplus.faction.diplo.alliance;

import dovtech.starmadeplus.faction.diplo.alliance.assistance.FactionAssistance;
import dovtech.starmadeplus.faction.diplo.relations.FactionMessage;

public interface FactionWarParticipantInterface {
    void requestAssistance(FactionAssistance[] assistance);
    void allianceMessage(FactionMessage message);
}
