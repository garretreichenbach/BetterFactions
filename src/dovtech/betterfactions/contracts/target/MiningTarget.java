package dovtech.betterfactions.contracts.target;

import api.element.inventory.ItemStack;
import dovtech.betterfactions.contracts.Contract;

public class MiningTarget implements ContractTarget {

    private ItemStack target;

    @Override
    public int getAmount() {
        return target.getAmount();
    }

    @Override
    public ItemStack getTarget() {
        return target;
    }

    @Override
    public void setTarget(Object target) {
        this.target = (ItemStack) target;
    }

    @Override
    public Contract.ContractType getContractType() {
        return Contract.ContractType.MINING;
    }
}
