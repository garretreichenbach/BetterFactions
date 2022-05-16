package thederpgamer.betterfactions.data.faction;

import api.common.GameClient;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.StringTools;
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
			return data instanceof FactionRelationship && data.getId() == getId();
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
		SELF("Self", errMsg, errMsg, errMsg), //This should never be visible in-game
		NEUTRAL("Neutral", "%s is neutral to us.", "We are neutral to %s.", "%s is neutral to %s."),
		NON_AGGRESSION_PACT("Non-Aggression Pact", "%s has a non-aggression pact with us.", "We have a non-aggression pact with %s.", "%s has a non-aggression pact with %s."),
		//TRADE_PACT("", ""),
		LEND_LEASE("Lend-Lease", "%s is providing material support for our war efforts.", "We are providing material support for %s's war efforts.", "%s is providing material support for %s's war efforts."),
		GUARANTEED_INDEPENDENCE("Guaranteed Independence", "%s has guaranteed our independence.", "We are guaranteeing the independence of %s.", "%s has their independence guaranteed by %s."),
		ALLIANCE("Allied", "%s is allied to us.", "We are allied to %s.", "%s is allied to %s."),
		FEDERATION_MEMBERS("Federation Members", "%s is in a federation with us.", "We are in a federation with %s.", "%s is in a federation with %s."),
		FEDERATION_ALLY("Aligned with Federation", "%s is aligned with our federation.", "We are aligned with %s's federation.", "%s is aligned with %s's federation."),
		ENEMY("Enemy", "%s considers us an enemy.", "We consider %s an enemy.", "%s considers %s an enemy."),
		AT_WAR("At war", "%s is at war with us.", "We are at war with %s.", "%s is at war with %s."),
		TRIBUTARY("Tributary", "%s is paying tribute to us.", "We are paying tribute to %s.", "%s is paying tribute to %s."),
		SUBJECT("Subject", "%s is our subject.", "We are a subject of %s.", "%s is a subject of %s.");

		public static final int RELATION_TO_SELF = 0;
		public static final int RELATION_FROM_SELF = 1;
		public static final int RELATION_TO_OTHER = 2;

		public final String name;
		private final String[] relations;

		RelationType(String name, String otherToSelf, String selfToOther, String otherToOther) {
			this.name = name;
			this.relations = new String[] {otherToSelf, selfToOther, otherToOther};
		}

		/**
		 * Returns a formatted relation String between the player's faction and another faction.
		 * @param type The relation direction type
		 * @param other The other faction
		 * @return A formatted String of the relation
		 */
		public String getRelation(int type, FactionData other) {
			return StringTools.format(relations[type], other.getName());
		}

		/**
		 * Returns a formatted relation String between two factions.
		 * @param type The relation direction type
		 * @param factionTo The to faction
		 * @param factionFrom The from faction
		 * @return A formatted String of the relation
		 */
		public String getRelationTo(int type, FactionData factionTo, FactionData factionFrom) {
			return StringTools.format(relations[type], factionTo.getName(), factionFrom.getName());
		}
	}
}
