package net.dovtech.betterfactions.entity.fleet;

import api.entity.Fleet;
import api.entity.Player;
import api.entity.Ship;
import api.universe.Sector;
import net.dovtech.betterfactions.faction.BetterFaction;
import java.util.ArrayList;

public class BetterFleet {

    private Fleet baseFleet;
    private String name;
    private boolean factionOwned = false;
    private boolean playerOwned = true;
    private BetterFaction ownerFaction = null;
    private Player owner;
    private ArrayList<Ship> fleetMembers;
    private Sector currentSector;

    public BetterFleet(Fleet baseFleet, String name, Player owner) {
        this.baseFleet = baseFleet;
        this.name = name;
        this.owner = owner;
    }

    public Fleet getBaseFleet() {
        return baseFleet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isFactionOwned() {
        return factionOwned;
    }

    public boolean isPlayerOwned() {
        return playerOwned;
    }

    public BetterFaction getOwnerFaction() {
        return ownerFaction;
    }

    public void setOwnerFaction(BetterFaction ownerFaction) {
        this.ownerFaction = ownerFaction;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public void transferOwnershipToFaction(BetterFaction faction) {
        if(playerOwned) {
            playerOwned = false;
            factionOwned = true;
            ownerFaction = faction;
            owner = null;
        }
    }

    public void transferOwnerShipToPlayer(Player player) {
        if(factionOwned) {
            playerOwned = true;
            factionOwned = false;
            ownerFaction = null;
            owner = player;
        }
    }
}
