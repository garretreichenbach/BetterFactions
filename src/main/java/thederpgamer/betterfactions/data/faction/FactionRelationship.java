package thederpgamer.betterfactions.data.faction;

import api.common.GameClient;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.manager.data.FactionDataManager;
import thederpgamer.betterfactions.utils.NetworkUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/12/2022]
 */
public class FactionRelationship implements SerializationInterface {

	private static final String errMsg = "If you're reading this, a catastrophic error has occurred and you should notify an admin ASAP!";

	private FactionData self;
	private FactionData other;
	private final ArrayList<Relationship> relations = new ArrayList<>();

	public FactionRelationship(FactionData self, FactionData other) {
		this.self = self;
		this.other = other;
	}

	public FactionRelationship(PacketReadBuffer readBuffer) throws IOException {
		deserialize(readBuffer);
	}

	public FactionData getSelf() {
		return self;
	}

	public FactionData getOther() {
		return other;
	}

	public ArrayList<Relationship> getRelations() {
		return relations;
	}

	public boolean isClientOwnFaction() {
		return NetworkUtils.onClient() && FactionDataManager.instance.getPlayerFaction(GameClient.getClientPlayerState()).equals(self);
	}

	@Override
	public int getId() {
		return self.getId() | other.getId();
	}

	@Override
	public String getName() {
		return self.getName() + "_" + other.getName();
	}

	@Override
	public boolean equals(SerializationInterface data) {
		return data instanceof FactionRelationship && data.getId() == getId();
	}

	@Override
	public void deserialize(PacketReadBuffer readBuffer) throws IOException {
		self = FactionDataManager.instance.getFactionData(readBuffer.readInt());
		other = FactionDataManager.instance.getFactionData(readBuffer.readInt());
		int size = readBuffer.readInt();
		for(int i = 0; i < size; i ++) relations.add(new Relationship(readBuffer));
	}

	@Override
	public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
		writeBuffer.writeInt(self.getId());
		writeBuffer.writeInt(other.getId());
		writeBuffer.writeInt(relations.size());
		for(Relationship relation : relations) relation.serialize(writeBuffer);
	}

	public static class Relationship implements SerializationInterface {

		private FactionData faction1;
		private FactionData faction2;
		private RelationType relationType;
		private float opinionModifier;

		public Relationship(FactionData faction1, FactionData faction2, RelationType relationType, float opinionModifier) {
			this.faction1 = faction1;
			this.faction2 = faction2;
			this.relationType = relationType;
			this.opinionModifier = opinionModifier;
		}

		public Relationship(PacketReadBuffer readBuffer) throws IOException {
			deserialize(readBuffer);
		}

		@Override
		public int getId() {
			return faction1.getId() | faction2.getId();
		}

		@Override
		public String getName() {
			return faction1.getName() + "_" + relationType.name + "_" + faction2.getName();
		}

		@Override
		public boolean equals(SerializationInterface data) {
			return data instanceof FactionRelationship && ;
		}

		@Override
		public void deserialize(PacketReadBuffer readBuffer) throws IOException {
			faction1 = FactionDataManager.instance.getFactionData(readBuffer.readInt());
			faction2 = FactionDataManager.instance.getFactionData(readBuffer.readInt());
			relationType = RelationType.values()[readBuffer.readInt()];
			opinionModifier = readBuffer.readFloat();
		}

		@Override
		public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
			writeBuffer.writeInt(faction1.getId());
			writeBuffer.writeInt(faction2.getId());
			writeBuffer.writeInt(relationType.ordinal());
			writeBuffer.writeFloat(opinionModifier);
		}

		public FactionData getFaction1() {
			return faction1;
		}

		public FactionData getFaction2() {
			return faction2;
		}

		public RelationType getRelationType() {
			return relationType;
		}

		public float getOpinionModifier() {
			return opinionModifier;
		}
	}

	public enum RelationType {
		SELF("Self", errMsg, errMsg), //This should never be visible in-game
		NEUTRAL("Neutral", "%s is neutral to us.", "%s is neutral to %s."),
		NON_AGGRESSION_PACT("Non-Aggression Pact", "%s has a non-aggression pact with us."),
		//TRADE_PACT("", ""),
		LEND_LEASE("Lend-Lease", ""),
		GUARANTEED_INDEPENDENCE("Guaranteed Independence", ""),
		ALLIANCE("Allied", ""),
		FEDERATION_MEMBERS("Federation Members", ""),
		FEDERATION_ALLY("Aligned with Federation", ""),
		ENEMY("Enemy", ""),
		AT_WAR("At war", ""),
		TRIBUTARY("Tributary", ""),
		PAYING_TRIBUTE("Paying Tribute"),
		SUBJECT("Subject", ""),
		MASTER("Master", "");

		public final String name;
		public final String relationSelf;
		public final String relationOther;


		RelationType(String name, String relationSelf, String relationOther) {
			this.name = name;
			this.relationSelf = relationSelf;
			this.relationOther = relationOther;
		}
	}
}
