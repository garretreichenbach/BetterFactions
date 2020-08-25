package dovtech.betterfactions.contracts.target;

import dovtech.betterfactions.contracts.Contract;

public interface ContractTarget {

    int getAmount();

    Object getTarget();

    void setTarget(Object target);

    Contract.ContractType getContractType();
}
