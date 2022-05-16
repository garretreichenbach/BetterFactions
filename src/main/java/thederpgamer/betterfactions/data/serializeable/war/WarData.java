package thederpgamer.betterfactions.data.serializeable.war;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.serializeable.SerializeableData;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/15/2021]
 */
public class WarData implements SerializeableData {

    public final ConcurrentHashMap<Integer, WarParticipantData> defenders = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer, WarParticipantData> attackers = new ConcurrentHashMap<>();

    public WarData() {

    }

    public WarData(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {

    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {

    }

    public boolean isInvolved(FactionData factionData) {
        return defenders.containsKey(factionData.getFactionId()) || attackers.containsKey(factionData.getFactionId());
    }
}
