package net.dovtech.betterfactions.block;

import org.schema.game.common.data.element.ElementInformation;

public class ResourceManager {

    private ElementInformation blockInfo;

    public ResourceManager() {
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
}
