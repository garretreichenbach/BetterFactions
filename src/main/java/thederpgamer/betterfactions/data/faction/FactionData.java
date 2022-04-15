package thederpgamer.betterfactions.data.faction;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.betterfactions.data.SerializationInterface;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class FactionData implements SerializationInterface {

	public static final int FP = 0;
	public static final int INFLUENCE = 1;
	public static final int TERRITORY = 2;
	public static final int ECONOMIC = 3;
	public static final int MILITARY = 4;
	public static final int DIPLOMATIC = 5;

	private int id;
	private String name;
	private final int[] score = new int[6];

	public FactionData(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public FactionData(Faction faction) {
		this.id = faction.getIdFaction();
		this.name = faction.getName();
		this.score[0] = (int) faction.factionPoints;
	}

	public FactionData(PacketReadBuffer readBuffer) throws IOException {
		deserialize(readBuffer);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(SerializationInterface data) {
		return data instanceof FactionData && data.getId() == getId() && data.getName().equals(getName());
	}

	@Override
	public void deserialize(PacketReadBuffer readBuffer) throws IOException {
		id = readBuffer.readInt();
		name = readBuffer.readString();
		for(int i = 0; i < score.length; i ++) score[i] = readBuffer.readInt();
	}

	@Override
	public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeInt(id);
		writeBuffer.writeString(name);
		for(int i : score) writeBuffer.writeInt(i);
	}
}
