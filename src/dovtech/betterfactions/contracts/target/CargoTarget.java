package dovtech.betterfactions.contracts.target;

import api.element.inventory.ItemStack;
import api.universe.StarSector;
import api.universe.StarUniverse;
import dovtech.betterfactions.contracts.Contract;

public class CargoTarget implements ContractTarget {

    private ItemStack target;
    private StarSector location;

    @Override
    public int getAmount() {
        return -1;
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
        return Contract.ContractType.CARGO_ESCORT;
    }

    @Override
    public int[] getLocation() {
        return new int[] {location.getCoordinates().x, location.getCoordinates().y, location.getCoordinates().z};
    }

    @Override
    public void setLocation(int[] coords) {
        location = StarUniverse.getUniverse().getSector(coords[0], coords[1], coords[2]);
    }

}
