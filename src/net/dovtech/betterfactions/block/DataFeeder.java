package net.dovtech.betterfactions.block;

import api.element.block.Block;
import api.element.block.Blocks;
import api.inventory.Inventory;
import api.inventory.InventoryType;
import org.schema.game.common.controller.elements.sensor.SensorElementManager;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.world.SegmentData;

public class DataFeeder {

    private ElementInformation blockInfo;
    private Block linkedBlock;
    private int[] linkableBlockIDs;
    private Data linkedBlockData;

    public DataFeeder() {
        blockInfo.setFullName("Resource Manager");
        blockInfo.setDescription("A highly configurable block that manages and sorts connected storage blocks, factories, and shipyards. Can be used with the Data Feeder and Long Range Transmitter to request resources/production over long distances.");
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

    public void setLinkedBlock(Block block) {
        linkedBlock = block;
    }

    public void unlinkBlock() {
        linkedBlock = null;
    }

    public Data getData() {
        updateData();
        return linkedBlockData;
    }

    private void updateData() {
        if(linkedBlock.getType() == Blocks.STORAGE) {
            SegmentData segmentData = linkedBlock.getInternalSegmentPiece().getSegment().getSegmentData();
            SensorElementManager sensorElementManager = new SensorElementManager(linkedBlock.getInternalSegmentPiece().getSegmentController());
            Inventory blockInventory = new Inventory(sensorElementManager.getManagerContainer().getInventory(linkedBlock.getLocation()), InventoryType.STORAGE_BOX);
            linkedBlockData = new InventoryData(blockInventory);
        }
    }
}