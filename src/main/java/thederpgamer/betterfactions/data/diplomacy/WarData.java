package thederpgamer.betterfactions.data.diplomacy;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
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
public class WarData extends DiplomaticData {

	private WarGoalData[] warGoalData;

	public WarData(FactionData[] from, FactionData[] to, WarGoalData... warGoalData) {
		super(from, to);
		this.warGoalData = warGoalData;
	}

	public WarData(PacketReadBuffer readBuffer) throws IOException {
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
		return NameUtils.generateWarName(from, to, this);
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

	public WarGoalData[] getWarGoalData() {
		return warGoalData;
	}
}
