package net.dovtech.betterfactions.gui;

import api.entity.Fleet;
import api.entity.Player;
import api.entity.Ship;
import java.util.ArrayList;

public class FleetInfoSidePanel {

    private Player player;
    private Fleet fleet;

    public void draw() {
        ArrayList<Ship> ships = fleet.getMembers();
        for(int i = 0; i <= ships.size() - 1; i ++) {
            drawShipStats(i, ships.get(i));
        }
    }

    private void drawShipStats(int num, Ship ship) {
        double currentUsage = ship.getCurrentReactor().getCurrentUsage();
        double totalRegen = ship.getCurrentReactor().getRegen();
        double shieldCap = ship.getShield(0).getCurrentShields();
        double shieldMaxCap = ship.getShield(0).getMaxCapacity();
        double speed = ship.getSpeed();
        
    }
}
