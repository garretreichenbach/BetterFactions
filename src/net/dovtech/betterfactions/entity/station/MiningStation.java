package net.dovtech.betterfactions.entity.station;

import api.element.Element;
import api.entity.Station;
import api.faction.Faction;
import api.inventory.Inventory;
import api.universe.Sector;
import net.dovtech.betterfactions.universe.ResourceZone;
import net.dovtech.betterfactions.utilities.ConfigReader;
import java.util.Map;

public class MiningStation extends BetterStation {

    private Inventory inventory;
    private ResourceZone resourceZone;
    private int miningBonus = 1;
    private Map<StationModule, Integer> modules;

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

    public Map<StationModule, Integer> getModules() {
        return modules;
    }

    public void addModule(StationModule module) {
        if(!(modules.containsKey(module))) {
            modules.put(module, 1);
        }
    }

    public void upgradeModule(StationModule module) {
        if(modules.containsKey(module) && modules.get(module) < 5) {
            int level = modules.get(module);
            modules.replace(module, level, level + 1);
        }
    }

    public void downgradeModule(StationModule module) {
        if(modules.containsKey(module) && modules.get(module) > 1) {
            int level = modules.get(module);
            modules.replace(module, level, level - 1);
        }
    }

    public void awardResources() {
        for(Element element : resourceZone.getResources().keySet()) {
            int existingAmount = 0;
            if(inventory.getContents().get(element) != 0) {
                existingAmount = inventory.getContents().get(element);
            }
            inventory.addElement(element, existingAmount + calculateResource(element));
        }
    }

    private int calculateResource(Element element) {
        int amount = resourceZone.getResources().get(element);
        double levelMultiplier = 1;
        switch(getLevel()) {
            case 1: levelMultiplier = ConfigReader.getConfig().getMiningLevel1Multiplier();
            case 2: levelMultiplier = ConfigReader.getConfig().getMiningLevel2Multiplier();
            case 3: levelMultiplier = ConfigReader.getConfig().getMiningLevel3Multiplier();
            case 4: levelMultiplier = ConfigReader.getConfig().getMiningLevel4Multiplier();
            case 5: levelMultiplier = ConfigReader.getConfig().getMiningLevel5Multiplier();
        }
        return (int) (Math.pow(amount, levelMultiplier) * miningBonus);
    }
}
