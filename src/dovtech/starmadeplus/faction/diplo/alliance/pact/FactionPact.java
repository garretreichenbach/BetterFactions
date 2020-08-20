package dovtech.starmadeplus.faction.diplo.alliance.pact;

import dovtech.starmadeplus.faction.government.PactGovernmentType;
import dovtech.starmadeplus.gui.faction.FactionLogo;
import org.schema.game.common.data.player.faction.Faction;
import java.util.ArrayList;

public class FactionPact {

    private ArrayList<Faction> members;
    private String name;
    private PactGovernmentType governmentType;
    private FactionLogo logo;
    private String pactID;

    public FactionPact(String name, PactGovernmentType governmentType) {
        this.members = new ArrayList<>();
        this.name = name;
        this.governmentType = governmentType;
        this.logo = null;
        this.pactID = null;
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

    public PactGovernmentType getGovernmentType() {
        return governmentType;
    }

    public void setGovernmentType(PactGovernmentType governmentType) {
        this.governmentType = governmentType;
    }

    public FactionLogo getLogo() {
        return logo;
    }

    public void setLogo(FactionLogo logo) {
        this.logo = logo;
    }

    public String getPactID() {
        return pactID;
    }

    public void setPactID(String pactID) {
        this.pactID = pactID;
    }
}
