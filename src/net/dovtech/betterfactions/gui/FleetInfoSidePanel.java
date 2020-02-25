package net.dovtech.betterfactions.gui;

import api.entity.Fleet;
import api.entity.Player;
import api.entity.Ship;
import java.util.ArrayList;

public class FleetInfoSidePanel {

    private Player player;
    private Fleet fleet = player.getCurrentEntity().getFleet();

    public void draw() {
        ArrayList<Ship> ships = getShips();
        for(int i = 0; i <= ships.size() - 1; i ++) {
            drawShipStats(i, ships.get(i));
        }
    }

    private void drawShipStats(int num, Ship ship) {
        double powerUsage = ship.getActiveReactor().getCurrentUsage();
        double shieldCap = ship.getShields().get(0).getCapacity();
        double shieldMaxCap = ship.getShields().get(0).getMaxCapacity();
        double speed = ship.getSpeed();
    }

    private ArrayList<Ship> getShips() {
        ArrayList<Ship> ships = null;
        for(int i = 0; i <= fleet.getFleetMembers().size() - 1; i ++) {
            if(i < 10) {
                ships.set(i, fleet.getFleetMembers().get(i));
            } else {
                break;
            }
        }
        return ships;
    }
}
