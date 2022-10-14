package thederpgamer.betterfactions.data;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/15/2022]
 */
public interface SerializationInterface {

	int getId();

	String getName();

	boolean equals(SerializationInterface data);

	void deserialize(PacketReadBuffer readBuffer) throws IOException;

	void serialize(PacketWriteBuffer writeBuffer) throws IOException;
}
