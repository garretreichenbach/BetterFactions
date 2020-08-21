package dovtech.betterfactions.faction.diplo.alliance;

import dovtech.betterfactions.faction.diplo.alliance.assistance.FactionAssistance;
import dovtech.betterfactions.faction.diplo.relations.FactionMessage;

public interface FactionWarParticipantInterface {
    void requestAssistance(FactionAssistance[] assistance);
    void allianceMessage(FactionMessage message);
}
