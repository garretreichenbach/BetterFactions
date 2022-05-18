package thederpgamer.betterfactions.data.diplomacy;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRelationship;
import thederpgamer.betterfactions.manager.data.FactionRelationshipManager;
import thederpgamer.betterfactions.utils.NameUtils;

import java.io.IOException;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/17/2022]
 */
public class WarGoalData extends DiplomaticData {

	public enum WarGoalType {
		FORCE_CONCESSION(new DiplomaticRequirement() {
			@Override
			public boolean meetsRequirements(FactionData[] from, FactionData[] to, Object... args) {
				for(FactionData fromData : from) {
					for(FactionData toData : to) {
						FactionRelationship relationship = FactionRelationshipManager.instance.getRelationship(fromData, toData);
						for(FactionRelationship.Relationship relation : relationship.getRelations()) {
							switch(relation.getRelationType()) {
								case FEDERATION_MEMBERS:
								case FEDERATION_ALLY:
								case SELF:
								case ALLIANCE:
								case SUBJECT:
								case LEND_LEASE:
								case NON_AGGRESSION_PACT:
								case GUARANTEED_INDEPENDENCE:
									return false;
							}
						}
					}
				}
				return true;
			}

			@Override
			public String getRequirementsDisplay(FactionData[] from, FactionData[] to, Object... args) {
				StringBuilder builder = new StringBuilder();
				if(args[0] instanceof DiplomaticData) {

				}
				return builder.toString();
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "War of " + NameUtils.getSideName(to) + " concession";
			}
		}),
		TERRITORY(new DiplomaticRequirement() {
			@Override
			public boolean meetsRequirements(FactionData[] from, FactionData[] to, Object... args) {
				for(FactionData fromData : from) {
					for(FactionData toData : to) {
						FactionRelationship relationship = FactionRelationshipManager.instance.getRelationship(fromData, toData);
						for(FactionRelationship.Relationship relation : relationship.getRelations()) {
							switch(relation.getRelationType()) {
								case FEDERATION_MEMBERS:
								case FEDERATION_ALLY:
								case SELF:
								case ALLIANCE:
								case SUBJECT:
								case LEND_LEASE:
								case NON_AGGRESSION_PACT:
								case GUARANTEED_INDEPENDENCE:
									return false;
							}
						}
					}
				}
				return true;
			}

			@Override
			public String getRequirementsDisplay(FactionData[] from, FactionData[] to, Object... args) {
				return null;
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "War of " + NameUtils.getSideName(from) + " aggression for " + ((TerritoryData) args[0]).getName() + " system";
			}
		}),
		RESOURCES(new DiplomaticRequirement() {
			@Override
			public boolean meetsRequirements(FactionData[] from, FactionData[] to, Object... args) {
				return false;
			}

			@Override
			public String getRequirementsDisplay(FactionData[] from, FactionData[] to, Object... args) {
				return null;
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return null;
			}
		}),
		SUBJUGATION(),
		INDEPENDENCE(),
		DEFENDING_ALLY(),
		INTERVENTION(),
		COALITION(),
		GREAT_WAR();

		public final DiplomaticRequirement requirements;

		WarGoalType(DiplomaticRequirement requirements) {
			this.requirements = requirements;
		}
	}

	private WarGoalType type;

	public WarGoalData(FactionData[] from, FactionData[] to, WarGoalType type) {
		super(from, to);
		this.type = type;
	}

	public WarGoalData(PacketReadBuffer readBuffer) throws IOException {
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
		return null;
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

	public WarGoalType getType() {
		return type;
	}
}
