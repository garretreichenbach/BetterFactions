package thederpgamer.betterfactions.data.diplomacy;

import api.network.PacketReadBuffer;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;

import java.io.IOException;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/17/2022]
 */
public abstract class DiplomaticData implements SerializationInterface, GUICallback {

	protected int id;
	protected FactionData[] from;
	protected FactionData[] to;

	public DiplomaticData(FactionData[] from, FactionData[] to) {
		this.from = from;
		this.to = to;
	}

	public DiplomaticData(PacketReadBuffer readBuffer) throws IOException {
		deserialize(readBuffer);
	}

	@Override
	public int getId() {
		return id;
	}

	public FactionData[] getFrom() {
		return from;
	}

	public FactionData[] getTo() {
		return to;
	}

	public abstract void send();
}
