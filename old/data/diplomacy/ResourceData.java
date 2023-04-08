package thederpgamer.betterfactions.data.diplomacy;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;

import java.io.IOException;
import java.util.HashMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class ResourceData extends DiplomaticData {

	private HashMap<Short, Integer> resourceMap;

	public ResourceData(FactionData[] from, FactionData[] to, HashMap<Short, Integer> resourceMap) {
		super(from, to);
		this.resourceMap = resourceMap;
	}

	public ResourceData(PacketReadBuffer readBuffer) throws IOException {
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
		return "RESOURCE_DATA";
	}

	@Override
	public boolean equals(SerializationInterface data) {
		return resourceMap.equals(((ResourceData) data).resourceMap);
	}

	@Override
	public void deserialize(PacketReadBuffer readBuffer) throws IOException {
		for(int i = 0; i < readBuffer.readInt(); i ++) resourceMap.put(readBuffer.readShort(), readBuffer.readInt());
	}

	@Override
	public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeInt(resourceMap.size());
		for(Short key : resourceMap.keySet()) {
			writeBuffer.writeShort(key);
			writeBuffer.writeInt(resourceMap.get(key));
		}
	}
}
