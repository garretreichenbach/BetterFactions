package thederpgamer.betterfactions.data.diplomacy;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRelationship;
import thederpgamer.betterfactions.manager.ConfigManager;
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
					DiplomaticData diplomaticData = (DiplomaticData) args[0];
					builder.append("The following factions must not be in any of the following relationships with each other:\n");
					for(FactionData fromData : diplomaticData.getFrom()) {
						for(FactionData toData : diplomaticData.getTo()) {
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
										builder.append(relation.getRelationType().name()).append(", ");
										break;
								}
							}
						}
					}
					builder.delete(builder.length() - 2, builder.length());
				}
				return builder.toString();
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "War of " + NameUtils.getSideName(to) + " concession";
			}
		}, new DiplomaticModifier() {
			@Override
			public String getModifierDisplay(FactionData[] from, FactionData[] to, Object... args) {
				float aggressionMod = ConfigManager.getDiploConfig().getConfigurableFloat("concession-war-aggression-modifier", 0.5f);
				float exhaustionMod = ConfigManager.getDiploConfig().getConfigurableFloat("concession-war-exhaustion-modifier", 0.5f);
				return "Attackers:\n +" + aggressionMod + "% aggression score per day\n" +
						" +" + exhaustionMod + "% exhaustion score per day\n" +
						"Defenders:\n +" + exhaustionMod + "% exhaustion score per day";
			}

			@Override
			public void applyModifier(FactionData[] from, FactionData[] to, Object... args) {
				WarData warData = (WarData) args[0];
				float aggressionMod = ConfigManager.getDiploConfig().getConfigurableFloat("concession-war-aggression-modifier", 0.5f);
				float exhaustionMod = ConfigManager.getDiploConfig().getConfigurableFloat("concession-war-exhaustion-modifier", 0.5f);
				for(FactionData factionData : warData.getFrom()) {
					factionData.addAggressionScore(aggressionMod * warData.getWarDuration() / 86400000f);
					factionData.addExhaustionScore(exhaustionMod * warData.getWarDuration() / 86400000f);
				}
				for(FactionData factionData : warData.getTo()) factionData.addExhaustionScore(exhaustionMod * warData.getWarDuration() / 86400000f);
				//Todo: This updated data needs to be saved and sent to clients
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
				return "The following factions must not be in any of the following relationships with each other:\n" +
						"FEDERATION_MEMBERS, FEDERATION_ALLY, SELF, ALLIANCE, SUBJECT, LEND_LEASE, NON_AGGRESSION_PACT, GUARANTEED_INDEPENDENCE";
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "War of " + NameUtils.getSideName(from) + " aggression for " + ((TerritoryData) args[0]).getName() + " system";
			}
		}),
		RESOURCES(new DiplomaticRequirement() {
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
				return "The following factions must not be in any of the following relationships with each other:\n" +
						"FEDERATION_MEMBERS, FEDERATION_ALLY, SELF, ALLIANCE, SUBJECT, LEND_LEASE, NON_AGGRESSION_PACT, GUARANTEED_INDEPENDENCE";
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "War of " + NameUtils.getSideName(from) + " aggression for " + to[0].getNameAdj() + " resources";
			}
		}),
		SUBJUGATION(new DiplomaticRequirement() {
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
				return "The following factions must not be in any of the following relationships with each other:\n" +
						"FEDERATION_MEMBERS, FEDERATION_ALLY, SELF, ALLIANCE, SUBJECT, LEND_LEASE, NON_AGGRESSION_PACT, GUARANTEED_INDEPENDENCE";
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "War of " + to[0].getNameAdj() + " subjugation";
			}
		}),
		INDEPENDENCE(new DiplomaticRequirement() {
			@Override
			public boolean meetsRequirements(FactionData[] from, FactionData[] to, Object... args) {
				for(FactionData fromData : from) {
					for(FactionData toData : to) {
						FactionRelationship relationship = FactionRelationshipManager.instance.getRelationship(fromData, toData);
						for(FactionRelationship.Relationship relation : relationship.getRelations()) {
							if(relation.getRelationType() == FactionRelationship.RelationType.SUBJECT) return true;
						}
					}
				}
				return false;
			}

			@Override
			public String getRequirementsDisplay(FactionData[] from, FactionData[] to, Object... args) {
				return from[0].getName() + " must be a subject of " + to[0].getName();
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "War of " + from[0].getNameAdj() + " independence";
			}
		}),
		DEFENDING_ALLY(new DiplomaticRequirement() {
			@Override
			public boolean meetsRequirements(FactionData[] from, FactionData[] to, Object... args) {
				for(FactionData fromData : from) {
					for(FactionData toData : to) {
						FactionRelationship relationship = FactionRelationshipManager.instance.getRelationship(fromData, toData);
						for(FactionRelationship.Relationship relation : relationship.getRelations()) {
							switch(relation.getRelationType()) {
								case FEDERATION_MEMBERS:
								case FEDERATION_ALLY:
								case ALLIANCE:
								case SUBJECT:
								case GUARANTEED_INDEPENDENCE:
									return true;
							}
						}
					}
				}
				return false;
			}

			@Override
			public String getRequirementsDisplay(FactionData[] from, FactionData[] to, Object... args) {
				return "The following factions must be in one of the following relationships with each other:\n" +
						"FEDERATION_MEMBERS, FEDERATION_ALLY, ALLIANCE, SUBJECT, GUARANTEED_INDEPENDENCE";
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "[DEFAULT]"; //Use name of existing war
			}
		}),
		INTERVENTION(new DiplomaticRequirement() {
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
				//To faction must have an aggression score of at least 100 and actively be at war to be able to intervene
				return to[0].getAggressionScore() >= 100 && to[0].inOffensiveWar();
			}

			@Override
			public String getRequirementsDisplay(FactionData[] from, FactionData[] to, Object... args) {
				return "The following factions must not be in any of the following relationships with each other:\n" +
						"FEDERATION_MEMBERS, FEDERATION_ALLY, SELF, ALLIANCE, SUBJECT, LEND_LEASE, NON_AGGRESSION_PACT, GUARANTEED_INDEPENDENCE\n" +
						"and the target faction must have an aggression score of at least 100 and be actively at war";
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "[DEFAULT]"; //Use name of existing war
			}
		}),
		COALITION(new DiplomaticRequirement() {
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
				//To faction must have an aggression score of at least 200
				return to[0].getAggressionScore() >= 200;
			}

			@Override
			public String getRequirementsDisplay(FactionData[] from, FactionData[] to, Object... args) {
				return "The following factions must not be in any of the following relationships with each other:\n" +
						"FEDERATION_MEMBERS, FEDERATION_ALLY, SELF, ALLIANCE, SUBJECT, LEND_LEASE, NON_AGGRESSION_PACT, GUARANTEED_INDEPENDENCE\n" +
						"and the target faction must have an aggression score of at least 200";
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "War of the " + from[0].getNameAdj() + " coalition";
			}
		}),
		GREAT_WAR(new DiplomaticRequirement() {
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
				return "The following factions must not be in any of the following relationships with each other:\n" +
						"FEDERATION_MEMBERS, FEDERATION_ALLY, SELF, ALLIANCE, SUBJECT, LEND_LEASE, NON_AGGRESSION_PACT, GUARANTEED_INDEPENDENCE";
			}

			@Override
			public String getWarName(FactionData[] from, FactionData[] to, Object... args) {
				return "Galactic Great War";
			}
		});

		public final DiplomaticRequirement requirements;
		public final DiplomaticModifier[] modifiers;

		WarGoalType(DiplomaticRequirement requirements, DiplomaticModifier... modifiers) {
			this.requirements = requirements;
			this.modifiers = modifiers;
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
