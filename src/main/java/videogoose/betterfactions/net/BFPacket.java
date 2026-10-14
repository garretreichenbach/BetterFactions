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
		SNAPSHOT, TREATY, OFFER, FEDERATION, OPINION, EMBLEM_HASH, EMBLEM_DATA, ASSETS, MILITARY,
		// admin profiler: the request goes to the server, the report comes back
		BATTLES_REQUEST, BATTLES, BATTLE_COMMAND,
		// server -> client, to an admin watching a battle: 1 soft-loaded, 2 held for load, 0 stopped watching
		BATTLE_WATCH,
		// client -> server, then back: the player's faction's battle reports
		BATTLE_REPORTS_REQUEST, BATTLE_REPORTS,
		// client -> server, then back: one report's ships and stations, for its full report
		BATTLE_DETAIL_REQUEST, BATTLE_DETAIL,
		// client -> server: shipyard repairs and faction designs
		REPAIR_ORDER, REPAIR_CANCEL, REPAIR_QUEUE_REQUEST, DESIGNS_REQUEST, DESIGN_SET,
		// server -> client
		REPAIR_QUEUE, DESIGNS,
		// client -> server: post a fleet to intercept from a sector
		INTERCEPT_ORDER,
		// client -> server, then back: the battles being fought against one enemy right now, for anyone in the faction
		LIVE_BATTLES_REQUEST, LIVE_BATTLES,
		// server -> client: fleets whose flagship is radar jamming, hidden on the map
		JAMMED_FLEETS,
		// client -> server: put a civic in a slot; server -> client: civics, the whole store on join or one faction's change
		CIVICS_PICK, CIVICS,
		// server -> client: every faction's galaxy-wide reputation
		REPUTATION,
		// client -> server: spend faction points on another faction, by id
		GIFT, FABRICATE_CLAIM,
		// client -> server: adopt or drop an institution; cast a Senate ballot. server -> client: the faction's open Senate votes
		INSTITUTION_SET, SENATE_BALLOT, SENATE, SENATE_RULES, SENATE_LEADER,
		// server -> client: the faction's Council. client -> server: back, refuse or choose an agenda; put an appointment to the Senate
		COUNCIL, COUNCIL_ACTION, COUNCIL_PROPOSE,
		// server -> client: one new line of the diplomatic history between two factions
		HISTORY,
		// server -> client: every faction's mod faction point lines from the last income turn
		POINT_MODS,
		// server -> client, on join: the server's civics.xml
		CIVIC_TYPES
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
