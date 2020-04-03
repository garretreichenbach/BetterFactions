package net.dovtech.betterfactions.block;

import api.element.block.Block;
import api.element.block.Blocks;
import api.entity.Fleet;
import api.entity.Ship;
import api.inventory.ItemStack;
import api.listener.events.block.BlockActivateEvent;
import api.universe.Sector;
import org.schema.game.common.data.element.ElementInformation;
import java.util.ArrayList;

public class FleetManager {

    private ElementInformation blockInfo;
    private FleetManagerMode mode;
    private Sector targetSector;
    private int patrolRadius;
    private Fleet registeredFleet;
    private ArrayList<Block> linkedRails;

    public FleetManager() {
        blockInfo.setFullName("Fleet Manager");
        blockInfo.setDescription("Manages and deploys player fleets that have been transferred to it's control. Missions can be activated manually or through logic.");
        blockInfo.setArmorValue(1);
        blockInfo.setMaxHitPointsE(1);
        blockInfo.setBuildIconNum(0);
        blockInfo.setCanActivate(true);
        blockInfo.setShoppable(true);
        blockInfo.setPrice(3000);
        blockInfo.setAnimated(false);
        blockInfo.setEnterable(false);
        blockInfo.setProducedInFactory(1);
        blockInfo.setDoor(false);
    }

    public ElementInformation getBlockInfo() {
        return blockInfo;
    }

    public FleetManagerMode getMode() {
        return mode;
    }

    public void setMode(FleetManagerMode mode) {
        this.mode = mode;
    }

    public void onBlockActivate(BlockActivateEvent blockActivateEvent) {
        if(checkFleet()) {
            activateMission();
        }
    }

    public void onPlayerActivate(PlayerActivateEvent playerActivateEvent) {

    }

    private boolean checkFleet() {
        //Todo: Check if registered Fleet is docked at linked rail modules and is valid for current mission

        boolean check = true;

        if(mode == FleetManagerMode.PATROL && patrolRadius > 0 && patrolRadius <= 10) {

        } else if(mode == FleetManagerMode.MINE) {
            for(Ship ship : registeredFleet.getMembers()) {
                if(!checkForBlocks(ship, new int[] { Blocks.CARGO_SPACE_0.getId(), Blocks.STORAGE.getId(), Blocks.SALVAGE_COMPUTER.getId(), Blocks.SALVAGE_MODULE.getId() })) {
                    displayMessage("[ERROR]: One or more of the ships in the fleet is missing some required blocks for the mission!");
                    check = false;
                    break;
                }
                if(!checkDockedatLink(ship)) {
                    displayMessage("[ERROR]: One or more of the ships in the fleet is not docked to a linked rail module!");
                    check = false;
                    break;
                }
            }
        } else if(mode == FleetManagerMode.TRANSPORT) {

        } else if(mode == FleetManagerMode.TRADE) {

        } else if(mode == FleetManagerMode.ATTACK || mode == FleetManagerMode.DEFEND) {

        }
        return check;
    }

    private boolean checkForBlocks(Ship ship, int[] blockIDs) {
        for(int i : blockIDs) {
            if(ship.getBlockAmount(new ItemStack((short) blockIDs[i])) == 0) {
                return false;
            }
        }
        return true;
    }

    private boolean checkDockedatLink(Ship ship) {
        for(Block block : linkedRails) {
            if(ship.getDockLocation().compareTo(block.getLocation()) <= 3) {
                return true;
            }
        }
        return false;
    }

    private void displayMessage(String string) {

    }

    public void activateMission() {

    }
}
