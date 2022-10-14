package thederpgamer.betterfactions.data.system;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.utils.NameUtils;

import java.io.IOException;

/**
 * Contains faction related data for a controlled system.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class FactionSystemData implements SerializationInterface {

	private int factionId;
	private Vector3i systemCoords;
	private SystemSpecialization specialization;

	/**
	 * Constructor for creating a new faction system instance.
	 *
	 * @param factionData The faction data for the faction that controls the system.
	 * @param systemCoords The coordinates of the system.
	 */
	public FactionSystemData(FactionData factionData, Vector3i systemCoords) {
		this.factionId = factionData.getId();
		this.systemCoords = systemCoords;
		this.specialization = new GeneralSpecialization(this);
	}

	/**
	 * Used only for networking.
	 */
	public FactionSystemData(PacketReadBuffer readBuffer) throws Exception {
		deserialize(readBuffer);
	}

	@Override
	public int getId() {
		return factionId;
	}

	@Override
	public String getName() {
		return "FactionSystemData_" + factionId + "_" + systemCoords.toString();
	}

	@Override
	public boolean equals(SerializationInterface data) {
		return data instanceof FactionSystemData && ((FactionSystemData) data).factionId == factionId && ((FactionSystemData) data).systemCoords.equals(systemCoords);
	}

	@Override
	public void deserialize(PacketReadBuffer readBuffer) throws IOException {
		factionId = readBuffer.readInt();
		systemCoords = readBuffer.readVector();
		specialization = GeneralSpecialization.fromPacket(readBuffer);
	}

	@Override
	public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeInt(factionId);
		writeBuffer.writeVector(systemCoords);
	}

	public Vector3i getSystemCoords() {
		return systemCoords;
	}

	public SystemSpecialization getSpecialization() {
		return specialization;
	}

	public void setSpecialization(SystemSpecialization specialization) {
		this.specialization = specialization;
	}

	public String getSystemName() {
		return NameUtils.getSystemName(systemCoords);
	}

	public void setSystemName(String systemName) {
		NameUtils.setSystemName(systemCoords, systemName);
	}
}
