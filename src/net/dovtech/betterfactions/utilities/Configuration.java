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
    gasMiningLevel1Multiplier: 1.025
    gasMiningLevel2Multiplier: 1.05
    gasMiningLevel3Multiplier: 1.07
    gasMiningLevel4Multiplier: 1.1
    gasMiningLevel5Multiplier: 1.15
    resourceZoneMaxSize: 10
    resourceZoneMinSize: 5
    resourceZoneDensity: 5
    resourceZoneMaxCap: 15000000
    resourceZoneMinCap: 3000000
    resourceZoneRegenMultiplier: 0.007
     */

    private double gasMiningLevel1Multiplier;
    private double gasMiningLevel2Multiplier;
    private double gasMiningLevel3Multiplier;
    private double gasMiningLevel4Multiplier;
    private double gasMiningLevel5Multiplier;
    private int resourceZoneMaxSize;
    private int resourceZoneMinSize;
    private int resourceZoneDensity;
    private int resourceZoneMaxCap;
    private int resourceZoneMinCap;
    private double resourceZoneRegenMultiplier;

    public double getGasMiningLevel1Multiplier() {
        return gasMiningLevel1Multiplier;
    }

    public double getGasMiningLevel2Multiplier() {
        return gasMiningLevel2Multiplier;
    }

    public double getGasMiningLevel3Multiplier() {
        return gasMiningLevel3Multiplier;
    }

    public double getGasMiningLevel4Multiplier() {
        return gasMiningLevel4Multiplier;
    }

    public double getGasMiningLevel5Multiplier() {
        return gasMiningLevel5Multiplier;
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