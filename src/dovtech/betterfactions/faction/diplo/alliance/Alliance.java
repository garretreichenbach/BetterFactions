package dovtech.betterfactions.faction.diplo.alliance;

import dovtech.betterfactions.faction.BetterFaction;
import dovtech.betterfactions.faction.government.AllianceGovernmentType;
import org.newdawn.slick.Image;
import java.util.ArrayList;
import java.util.UUID;

public class Alliance {

    private ArrayList<BetterFaction> members;
    private String name;
    private AllianceGovernmentType governmentType;
    private Image logo;
    private String allianceID;
    private String description;

    public Alliance(String name, AllianceGovernmentType governmentType) {
        this.members = new ArrayList<>();
        this.name = name;
        this.governmentType = governmentType;
        this.logo = null;
        this.allianceID = UUID.randomUUID().toString();
        this.description = getDefaultDescription();
    }

    private String getDefaultDescription() {
        switch(governmentType) {
            case EMPIRE:
                return "An empire of states under a singular rule.";
            case FEDERATION:
                return "A collection of states working together to protect themselves and their interests.";
            case CONFEDERATION:
                return "A collection of states with a centralized government made up of officials from each member faction.";
            case MEGA_CORPORATION:
                return "A collection of states run by a large corporate conglomerate with major business ties across the galaxy.";
            case DEFENSIVE_ALLIANCE:
                return "A group of factions ready to defend themselves and each other in a defensive conflict.";
            case MILITARY_PACT:
                return "A group of factions ready to support each other in any sort of offensive or defensive conflict.";
        }
        return "No Description";
    }

    public ArrayList<BetterFaction> getMembers() {
        return members;
    }

    public void setMembers(ArrayList<BetterFaction> members) {
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AllianceGovernmentType getGovernmentType() {
        return governmentType;
    }

    public void setGovernmentType(AllianceGovernmentType governmentType) {
        this.governmentType = governmentType;
    }

    public Image getLogo() {
        return logo;
    }

    public void setLogo(Image logo) {
        this.logo = logo;
    }

    public String getAllianceID() {
        return allianceID;
    }

    public void setAllianceID(String allianceID) {
        this.allianceID = allianceID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
