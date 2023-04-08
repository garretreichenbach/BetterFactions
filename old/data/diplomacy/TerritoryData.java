package thederpgamer.betterfactions.data.diplomacy;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;

import java.io.IOException;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/17/2022]
 */
public class TerritoryData extends DiplomaticData {

	private Vector3i system;
	private String systemName;

	public TerritoryData(FactionData[] from, FactionData[] to, Vector3i system, String systemName) {
		super(from, to);
		this.system = system;
		this.systemName = systemName;
	}

	public TerritoryData(PacketReadBuffer readBuffer) throws IOException {
		super(readBuffer);
	}

	@Override
	public void send() {

	}

	@Override
	public void callback(GUIElement guiElement, MouseEvent mouseEvent) {

	}

	@Override
	public boolean isOccluded() {
		return false;
	}

	@Override
	public String getName() {
		return systemName;
	}

	@Override
	public boolean equals(SerializationInterface data) {
		return data instanceof TerritoryData && data.getId() == id;
	}

	@Override
	public void deserialize(PacketReadBuffer readBuffer) throws IOException {

	}

	@Override
	public void serialize(PacketWriteBuffer writeBuffer) throws IOException {

	}

	public Vector3i getSystem() {
		return system;
	}
}
