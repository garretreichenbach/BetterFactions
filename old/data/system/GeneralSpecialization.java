package thederpgamer.betterfactions.data.system;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import thederpgamer.betterfactions.data.SerializationInterface;

import java.io.IOException;

/**
 * Default system specialization.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class GeneralSpecialization implements SystemSpecialization {

	private int factionId;
	private Vector3i systemCoords;

	public GeneralSpecialization(FactionSystemData systemData) {
		initialize(systemData);
	}

	public GeneralSpecialization(PacketReadBuffer readBuffer) throws Exception {
		deserialize(readBuffer);
	}

	@Override
	public void initialize(FactionSystemData systemData) {
		this.factionId = systemData.getId();
		this.systemCoords = systemData.getSystemCoords();
	}

	@Override
	public int getId() {
		return factionId;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public boolean equals(SerializationInterface data) {
		return false;
	}

	@Override
	public void deserialize(PacketReadBuffer readBuffer) throws IOException {

	}

	@Override
	public void serialize(PacketWriteBuffer writeBuffer) throws IOException {

	}

	public static SystemSpecialization fromPacket(PacketReadBuffer readBuffer) throws IOException {
		switch(readBuffer.readString()) {
			case "INTEL":
				return new IntelSpecialization(readBuffer);
			default:
				return null;
		}
	}
}
