package thederpgamer.betterfactions.data.diplomacy.action;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.StringTools;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyAction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.common.language.Translatable;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class FactionDiplomacyAction extends DiplomacyAction {

	public enum DiploActionType {
		ATTACK(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Attacked them");
			}
		}),
		ATTACK_ENEMY(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Attacked their enemies");
			}
		}),
		ATTACK_ALLY(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Attacked their allies");
			}
		}),
		MINING(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Mined their resources");
			}
		}),
		TERRITORY(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Violated their territory");
			}
		}),
		PEACE_OFFER(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Peace offering made");
			}
		}),
		DECLARATION_OF_WAR(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Declared war");
			}
		}),
		ALLIANCE_REQUEST(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Requested Alliance");
			}
		}),
		ALLIANCE_CANCEL(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Canceled Alliance");
			}
		}),
		TRADING_WITH_US(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Traded with them");
			}
		}),
		TRADING_WITH_ENEMY(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("Traded with their enemies");
			}
		}),
		ALLIANCE_WITH_ENEMY(new Translatable() {
			@Override
			public String getName(Enum en) {
				return Lng.str("In alliance with their enemies");
			}
		}),
		ALLIANCE_WITH_FRIEND(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("In alliance with their friends");
			}
		}),
		REJECT_NON_AGRESSION_PACT(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We rejected Non-Aggression pact with them");
			}
		}),
		REMOVE_NON_AGGRESSION_PACT(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We cancelled our Non-Aggression pact with them");
			}
		}),
		ACCEPT_NON_AGGRESSION_PACT(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We have a Non-Aggression pact with them");
			}
		}),
		ACCEPT_ALLIANCE(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We have an Alliance with them");
			}
		}),
		REJECT_ALLIANCE(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We rejected an Alliance with them");
			}
		}),
		ACCEPT_PEACE_OFFER(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We accepted their Peace offer");
			}
		}),
		REJECT_PEACE_OFFER(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We rejected their Peace offer");
			}
		}),
		ACCEPT_FEDERATION_OFFER(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We are in a federation with them");
			}
		}),
		REJECT_FEDERATION_OFFER(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("We rejected an offer to join their federation");
			}
		}),
		THREATENING(new Translatable() {
			@Override
			public String getName(Enum anEnum) {
				return Lng.str("They feel threatened by us");
			}
		});
		private final Translatable description;

		private DiploActionType(Translatable description){
			this.description = description;
		}

		public String getDescription() {
			return description.getName(this);
		}

		public static String list() {
			return StringTools.listEnum(FactionDiplomacyAction.DiploActionType.values());
		}

	}

	public DiploActionType type;

	public int counter;

	public long timeDuration;

	public Tag toTag() {
		return new Tag(Tag.Type.STRUCT, null, new Tag[]{
				new Tag(Tag.Type.BYTE, null, (byte)0),
				new Tag(Tag.Type.INT, null, counter),
				new Tag(Tag.Type.LONG, null, timeDuration),
				new Tag(Tag.Type.BYTE, null, (byte)type.ordinal()),
				FinishTag.INST,
		});
	}

	public void fromTag(Tag tag){
		Tag[] t = tag.getStruct();
		byte version = t[0].getByte();

		counter = t[1].getInt();
		timeDuration = t[2].getLong();
		type = FactionDiplomacyAction.DiploActionType.values()[t[3].getByte()];
	}


	public void fromNetwork(PacketReadBuffer packetReadBuffer) throws IOException {
		type = DiploActionType.values()[packetReadBuffer.readInt()];
		counter = packetReadBuffer.readInt();
		timeDuration = packetReadBuffer.readLong();
	}

	public void toNetwork(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeInt(type.ordinal());
		packetWriteBuffer.writeInt(counter);
		packetWriteBuffer.writeLong(timeDuration);
	}
}
