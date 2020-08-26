package dovtech.betterfactions.contracts;

import dovtech.betterfactions.contracts.target.CargoTarget;
import dovtech.betterfactions.contracts.target.MiningTarget;
import dovtech.betterfactions.contracts.target.PlayerTarget;
import dovtech.betterfactions.contracts.target.ProductionTarget;
import java.io.Serializable;

public class ContractData implements Serializable {

    public String display;
    public int contractType;
    public int reward;
    public int contractorID;
    public int[] location;
    public String[] targetData;
    public String uid;

    public ContractData(Contract contract) {
        this.display = contract.getName();
        this.uid = contract.getUid();
        this.reward = contract.getReward();
        this.contractorID = contract.getContractor().getInternalFaction().getIdFaction();
        switch(contract.getContractType()) {
            case BOUNTY:
                contractType = 0;
                location = null;
                PlayerTarget playerTarget = (PlayerTarget) contract.getTarget()[0];
                targetData = new String[] { playerTarget.getTarget().getInternalPlayer().getName() + "" };
                break;
            case CARGO_ESCORT:
                contractType = 1;
                CargoTarget[] cargoTarget = new CargoTarget[contract.getTarget().length];
                for(int t = 0; t < cargoTarget.length; t ++) {
                    cargoTarget[t] = (CargoTarget) contract.getTarget()[t];
                }
                location = cargoTarget[0].getLocation();
                int itemStacksCargo = cargoTarget.length;
                targetData = new String[itemStacksCargo];
                for(int i = 0; i < itemStacksCargo; i ++) {
                    targetData[i] = cargoTarget[i].getTarget().getId() + " : " + cargoTarget[i].getTarget().getAmount();
                }
                break;
            case MINING:
                contractType = 2;
                MiningTarget[] miningTarget = new MiningTarget[contract.getTarget().length];
                for(int t = 0; t < miningTarget.length; t ++) {
                    miningTarget[t] = (MiningTarget) contract.getTarget()[t];
                }
                location = null;
                int itemStacksMining = miningTarget.length;
                targetData = new String[itemStacksMining];
                for(int i = 0; i < itemStacksMining; i ++) {
                    targetData[i] = miningTarget[i].getTarget().getId() + " : " + miningTarget[i].getTarget().getAmount();
                }
                break;
            case PRODUCTION:
                contractType = 3;
                ProductionTarget[] productionTarget = new ProductionTarget[contract.getTarget().length];
                for(int t = 0; t < productionTarget.length; t ++) {
                    productionTarget[t] = (ProductionTarget) contract.getTarget()[t];
                }
                location = null;
                int itemStacksProduction = productionTarget.length;
                targetData = new String[itemStacksProduction];
                for(int i = 0; i < itemStacksProduction; i ++) {
                    targetData[i] = productionTarget[i].getTarget().getId() + " : " + productionTarget[i].getTarget().getAmount();
                }
                break;
        }
    }
}