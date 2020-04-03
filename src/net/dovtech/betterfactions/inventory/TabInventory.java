package net.dovtech.betterfactions.inventory;

import api.inventory.Block;
import api.inventory.Inventory;
import java.util.Map;

public class TabInventory {

    private Map<Integer, Inventory> inventories;
    private Block container;

    public TabInventory(Block container) {
        this.container = container;
    }

    public TabInventory() {
        this.container = null;
    }

    public Inventory getInventory(int tab) {
        return inventories.get(tab);
    }
}
