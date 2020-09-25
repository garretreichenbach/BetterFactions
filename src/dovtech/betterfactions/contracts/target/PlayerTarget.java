package dovtech.betterfactions.contracts.target;

import api.entity.StarPlayer;
import dovtech.betterfactions.contracts.Contract;

public class PlayerTarget implements ContractTarget {

    private StarPlayer target;

    @Override
    public int getAmount() {
        return 1;
    }

    @Override
    public StarPlayer getTarget() {
        return target;
    }

    @Override
    public void setTarget(Object target) {
        this.target = (StarPlayer) target;
    }

    @Override
    public Contract.ContractType getContractType() {
        return Contract.ContractType.BOUNTY;
    }

    @Override
    public int[] getLocation() {
        return null;
    }

    @Override
    public void setLocation(int[] coords) {

    }
}
