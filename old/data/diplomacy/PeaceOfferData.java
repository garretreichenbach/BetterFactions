package thederpgamer.betterfactions.data.diplomacy;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;

import java.io.IOException;

/**
 * Data object for diplomatic peace offers.
 * <p>These usually come with offers or demands that the other party needs to accept for peace to be made.</p>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/17/2022]
 */
public class PeaceOfferData extends DiplomaticData {

	private WarData warData;

	public PeaceOfferData(FactionData[] from, FactionData[] to, WarData warData) {
		super(from, to);
		this.warData = warData;
	}

	public PeaceOfferData(PacketReadBuffer readBuffer) throws IOException {
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
		return "PEACE_OFFER";
	}

	@Override
	public boolean equals(SerializationInterface data) {
		return data instanceof PeaceOfferData && data.getId() == id;
	}

	@Override
	public void deserialize(PacketReadBuffer readBuffer) throws IOException {

	}

	@Override
	public void serialize(PacketWriteBuffer writeBuffer) throws IOException {

	}
}
