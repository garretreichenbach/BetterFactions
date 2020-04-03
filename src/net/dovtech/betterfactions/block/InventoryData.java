package net.dovtech.betterfactions.block;

import api.inventory.Inventory;

public class InventoryData extends Data {

    private Inventory inventory;

    public InventoryData(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
