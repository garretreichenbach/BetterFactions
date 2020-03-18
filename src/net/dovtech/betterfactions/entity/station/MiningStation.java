package net.dovtech.betterfactions.entity.station;

import api.entity.Station;
import api.faction.Faction;
import api.inventory.Inventory;
import api.universe.Sector;
import net.dovtech.betterfactions.universe.Resource;
import net.dovtech.betterfactions.universe.ResourceZone;
import net.dovtech.betterfactions.utilities.ConfigReader;

import java.util.Map;

public class MiningStation extends BetterStation {

    private Inventory inventory;
    private ResourceZone resourceZone;
    private int miningBonus;
    private double overcharge;

    public MiningStation(Station baseEntity, Faction ownerFaction, Sector sector) {
        super(baseEntity, StationType.MINING, ownerFaction, sector);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public ResourceZone getResourceZone() {
        return resourceZone;
    }

    public int getMiningBonus() {
        return miningBonus;
    }

    public void setMiningBonus(int miningBonus) {
        this.miningBonus = miningBonus;
    }

    public double getOvercharge() {
        return overcharge;
    }

    public void setOvercharge(double overcharge) {
        this.overcharge = overcharge;
    }

    public void awardResources() {
        Map<Resource, Integer> resourcesToAdd = null;
        for(Resource resource : resourceZone.getResources().keySet()) {
            resourcesToAdd.put(resource, calculateResource(resource));
        }
    }

    private int calculateResource(Resource resource) {
        double levelMultiplier = 1;
        switch(getLevel()) {
            case 1: levelMultiplier = ConfigReader.getMiningLevelMultiplier(1);
            case 2: levelMultiplier = 1.5;
            case 3: levelMultiplier =
        }

        return amount;
    }
}
