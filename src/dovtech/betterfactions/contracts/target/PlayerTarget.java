package dovtech.betterfactions.contracts.target;

import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.faction.BetterPlayer;

public class PlayerTarget implements ContractTarget {

    private BetterPlayer target;

    @Override
    public int getAmount() {
        return 1;
    }

    @Override
    public BetterPlayer getTarget() {
        return target;
    }

    @Override
    public void setTarget(Object target) {
        this.target = (BetterPlayer) target;
    }

    @Override
    public Contract.ContractType getContractType() {
        return Contract.ContractType.BOUNTY;
    }
}
