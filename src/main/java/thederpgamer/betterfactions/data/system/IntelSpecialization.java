package thederpgamer.betterfactions.data.system;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.SerializationInterface;

import java.io.IOException;

/**
 * Intel specialization that gives the faction the ability to spy on neighboring systems.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class IntelSpecialization implements SystemSpecialization {

	public IntelSpecialization(PacketReadBuffer readBuffer) throws IOException {
		deserialize(readBuffer);
	}

	@Override
	public int getId() {
		return 0;
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

	@Override
	public void initialize(FactionSystemData systemData) {

	}
}