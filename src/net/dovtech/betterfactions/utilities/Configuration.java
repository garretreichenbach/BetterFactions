package net.dovtech.betterfactions.utilities;

public class Configuration {

    /* BetterFactionsConfig.yml
    !!net.dovtech.betterfactions.Configuration
    # BetterFactions Config File
    #
    # Factions
    #
    # Stations
    # Mining Stations
    miningLevel1Multiplier: 1.025
    miningLevel2Multiplier: 1.05
    miningLevel3Multiplier: 1.07
    miningLevel4Multiplier: 1.1
    miningLevel5Multiplier: 1.15
    resourceZoneMaxSize: 10
    resourceZoneMinSize: 5
    resourceZoneDensity: 5
    resourceZoneMaxCap: 15000000
    resourceZoneMinCap: 3000000
    resourceZoneRegenMultiplier: 0.007
     */

    private double miningLevel1Multiplier;
    private double miningLevel2Multiplier;
    private double miningLevel3Multiplier;
    private double miningLevel4Multiplier;
    private double miningLevel5Multiplier;
    private int resourceZoneMaxSize;
    private int resourceZoneMinSize;
    private int resourceZoneDensity;
    private int resourceZoneMaxCap;
    private int resourceZoneMinCap;
    private double resourceZoneRegenMultiplier;

    public double getMiningLevel1Multiplier() {
        return miningLevel1Multiplier;
    }

    public double getMiningLevel2Multiplier() {
        return miningLevel2Multiplier;
    }

    public double getMiningLevel3Multiplier() {
        return miningLevel3Multiplier;
    }

    public double getMiningLevel4Multiplier() {
        return miningLevel4Multiplier;
    }

    public double getMiningLevel5Multiplier() {
        return miningLevel5Multiplier;
    }

    public int getResourceZoneMaxSize() {
        return resourceZoneMaxSize;
    }

    public int getResourceZoneMinSize() {
        return resourceZoneMinSize;
    }

    public int getResourceZoneDensity() {
        return resourceZoneDensity;
    }

    public int getResourceZoneMaxCap() {
        return resourceZoneMaxCap;
    }

    public int getResourceZoneMinCap() {
        return resourceZoneMinCap;
    }

    public double getResourceZoneRegenMultiplier() {
        return resourceZoneRegenMultiplier;
    }
}