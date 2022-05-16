package thederpgamer.betterfactions.data.serializeable.war;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.serializeable.SerializeableData;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/18/2021]
 */
public class WarParticipantData implements SerializeableData {

    public int factionId;
    public WarGoalData warGoal;

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {

    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {

    }
}
