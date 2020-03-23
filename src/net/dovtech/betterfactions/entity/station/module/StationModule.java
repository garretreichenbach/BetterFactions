package net.dovtech.betterfactions.entity.station.module;

import net.dovtech.betterfactions.entity.station.BetterStation;

public class StationModule {

    private String displayName;
    private int maxLevel;
    private int[] powerUsage;

    public StationModule(String displayName, int maxLevel, int[] powerUsage) {
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.powerUsage = powerUsage;

    }

    public void activate(BetterStation station) {

    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int[] getPowerUsage() {
        return powerUsage;
    }
}