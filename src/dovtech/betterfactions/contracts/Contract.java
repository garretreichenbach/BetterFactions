package dovtech.betterfactions.contracts;

import api.entity.StarPlayer;
import dovtech.betterfactions.contracts.target.ContractTarget;
import org.schema.game.common.data.player.faction.Faction;
import java.util.ArrayList;

public class Contract {

    private String name;
    private Faction contractor;
    private ContractType contractType;
    private int reward;
    private ContractTarget[] target;
    private ArrayList<StarPlayer> claimants;
    private String uid;

    public Contract(Faction contractor, String name, ContractType contractType, int reward, String uid, ContractTarget... target) {
        this.name = name;
        this.contractor = contractor;
        this.contractType = contractType;
        this.reward = reward;
        this.target = target;
        this.claimants = new ArrayList<>();
        this.uid = uid;
    }

    public ArrayList<StarPlayer> getClaimants() {
        return claimants;
    }

    public void setClaimants(ArrayList<StarPlayer> claimants) {
        this.claimants = claimants;
    }

    public String getName() {
        return name;
    }

    public Faction getContractor() {
        return contractor;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public int getReward() {
        return reward;
    }

    public ContractTarget[] getTarget() {
        return target;
    }

    public String getUid() {
        return uid;
    }

    public enum ContractType {
        ALL("All"),
        BOUNTY("Bounty"),
        CARGO_ESCORT("Cargo Escort"),
        MINING("Mining"),
        PRODUCTION("Production");

        public String displayName;

        ContractType(String displayName) {
            this.displayName = displayName;
        }
    }
}
