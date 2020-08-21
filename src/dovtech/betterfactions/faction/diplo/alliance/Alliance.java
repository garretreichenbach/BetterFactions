package dovtech.betterfactions.faction.diplo.alliance;

import dovtech.betterfactions.faction.government.AllianceGovernmentType;
import org.newdawn.slick.Image;
import org.schema.game.common.data.player.faction.Faction;
import java.util.ArrayList;

public class Alliance {

    private ArrayList<Faction> members;
    private String name;
    private AllianceGovernmentType governmentType;
    private Image logo;
    private String allianceID;

    public Alliance(String name, AllianceGovernmentType governmentType) {
        this.members = new ArrayList<>();
        this.name = name;
        this.governmentType = governmentType;
        this.logo = null;
        this.allianceID = null;
    }

    public ArrayList<Faction> getMembers() {
        return members;
    }

    public void setMembers(ArrayList<Faction> members) {
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

    public void setAllianceID(String pactID) {
        this.allianceID = pactID;
    }
}
