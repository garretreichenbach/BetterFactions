package net.dovtech.betterfactions.block;

import net.dovtech.betterfactions.Global;
import org.schema.game.common.data.element.ElementInformation;

public class LongRangeTransmitter {

    private ElementInformation blockInfo;
    private TransmitterLink transmitter;

    public LongRangeTransmitter() {
        blockInfo.setFullName("Long Range Transmitter");
        if(Boolean.parseBoolean(Global.getConfigValue("Transmitter-Distance-Limited"))) {
            int transmitterDistance = Integer.parseInt(Global.getConfigValue("Transmitter-Max-Distance"));
            blockInfo.setDescription("Can transmit logic signals to another linked transmitter up to " + transmitterDistance + " sectors away. Use with a Data Feeder to transmit data instead.");
        } else if(!Boolean.parseBoolean(Global.getConfigValue("Transmitter-Distance-Limited"))) {
            blockInfo.setDescription("Can transmit logic signals to another linked transmitter across long distances. Use with a Data Feeder to transmit data instead.");
        } else {
            blockInfo.setDescription("[ERROR] CONFIGURATION ERROR DETECTED: 'Transmitter-Distance-Limited' SHOULD BE EITHER TRUE OR FALSE. PLEASE NOTIFY AN ADMIN!");
        }
        blockInfo.setArmorValue(1);
        blockInfo.setMaxHitPointsE(1);
        blockInfo.setBuildIconNum(0);
        blockInfo.setCanActivate(true);
        blockInfo.setShoppable(true);
        blockInfo.setPrice(5000);
        blockInfo.setAnimated(false);
        blockInfo.setEnterable(false);
        blockInfo.setProducedInFactory(1);
        blockInfo.setDoor(false);
    }

    public ElementInformation getBlockInfo() {
        return blockInfo;
    }

    public void TransmitLogic(TransmitterLink destination, boolean state) {

    }

    public void TransmitData(TransmitterLink destination, Data data) {

    }
}
