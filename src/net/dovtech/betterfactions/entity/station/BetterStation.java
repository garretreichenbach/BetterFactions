package net.dovtech.betterfactions.entity.station;

import api.entity.Station;
import api.faction.Faction;
import api.universe.Sector;
import api.universe.System;

public class BetterStation extends Station {

    private Station baseEntity;
    private Faction ownerFaction;
    private Faction controllerFaction;
    private Sector sector;
    private System system;
    private StationType stationType;
    private int level;

    public BetterStation(Station baseEntity, StationType stationType, Faction ownerFaction, Sector sector) {
        this.baseEntity = baseEntity;
        this.ownerFaction = ownerFaction;
        this.controllerFaction = ownerFaction;
        this.system = sector.getSystem();
        this.sector = sector;
        this.stationType = stationType;
        this.level = 1;
    }

    public Station getBaseEntity() {
        return baseEntity;
    }

    public Faction getOwnerFaction() {
        return ownerFaction;
    }

    public void setOwnerFaction(Faction ownerFaction) {
        this.ownerFaction = ownerFaction;
    }

    public Faction getControllerFaction() {
        return controllerFaction;
    }

    public void setControllerFaction(Faction controllerFaction) {
        this.controllerFaction = controllerFaction;
    }

    public Sector getSector() {
        return sector;
    }

    public System getSystem() {
        return system;
    }

    public StationType getStationType() {
        return stationType;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}