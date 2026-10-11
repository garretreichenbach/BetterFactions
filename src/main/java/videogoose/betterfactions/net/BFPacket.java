package videogoose.betterfactions.net;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.DiplomacyManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Every message the mod sends: a kind and a tag. The diplomacy types already serialize to tags, so one
 * packet class carries all of them and {@link DiplomacyManager} dispatches on the kind.
 */
public final class BFPacket extends Packet {

	public enum Kind {
		// client -> server
		PROPOSAL, FEDERATION_COMMAND, EMBLEM_UPLOAD, EMBLEM_CLEAR, EMBLEM_REQUEST, ASSETS_REQUEST,
		// server -> client
		SNAPSHOT, TREATY, OFFER, FEDERATION, OPINION, EMBLEM_HASH, EMBLEM_DATA, ASSETS
	}

	private Kind kind;
	private Tag tag;

	public BFPacket() {
	}

	public BFPacket(Kind kind, Tag tag) {
		this.kind = kind;
		this.tag = tag;
	}

	public static void toServer(Kind kind, Tag tag) {
		PacketUtil.sendPacketToServer(new BFPacket(kind, tag));
	}

	public static void toPlayer(PlayerState p, Kind kind, Tag tag) {
		PacketUtil.sendPacket(p, new BFPacket(kind, tag));
	}

	@Override
	public void readPacketData(PacketReadBuffer buf) throws IOException {
		kind = Kind.values()[buf.readByte()];
		tag = Tag.deserializeNT(new DataInputStream(new ByteArrayInputStream(buf.readByteArray())));
	}

	@Override
	public void writePacketData(PacketWriteBuffer buf) throws IOException {
		buf.writeByte((byte) kind.ordinal());
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		tag.serializeNT(new DataOutputStream(bytes));
		buf.writeByteArray(bytes.toByteArray());
	}

	@Override
	public void processPacketOnClient() {
		DiplomacyManager.client().receive(kind, tag);
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		DiplomacyManager.server().receive(sender, kind, tag);
	}
}
