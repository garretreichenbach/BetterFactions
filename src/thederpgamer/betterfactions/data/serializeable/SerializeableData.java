package thederpgamer.betterfactions.data.serializeable;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/14/2021]
 */
public interface SerializeableData {

    void deserialize(PacketReadBuffer readBuffer) throws IOException;
    void serialize(PacketWriteBuffer writeBuffer) throws IOException;
}
