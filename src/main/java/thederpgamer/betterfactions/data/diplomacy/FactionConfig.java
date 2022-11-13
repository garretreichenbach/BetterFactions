package thederpgamer.betterfactions.data.diplomacy;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import org.schema.common.config.ConfigParserException;
import org.schema.common.config.ConfigurationElement;
import org.schema.common.util.CompareTools;
import org.schema.common.util.LogInterface;
import org.schema.common.util.LogInterface.LogLevel;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.world.SimpleTransformableSendableObject.EntityType;
import org.schema.game.server.controller.EntityNotFountException;
import org.schema.game.server.data.blueprintnw.BlueprintClassification;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.game.server.data.simulation.npc.diplomacy.FactionDiplomacyAction.DiploActionType;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyReaction;
import org.schema.game.server.data.simulation.npc.diplomacy.NPCDiplomacyEntity.DiplStatusType;
import org.schema.game.server.data.simulation.npc.geo.NPCEntityContingent;
import org.schema.game.server.data.simulation.npc.geo.NPCFactionPreset;
import org.schema.game.server.data.simulation.npc.geo.NPCSystemFleetManager.FleetType;
import org.schema.game.server.data.simulation.npc.geo.NPCSystemStructure;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

public class FactionConfig {

	public static final byte XML_VERSION = 0;

	public static boolean recreate = false;
	public class EntityPriority{
		private float vals[] = new float[BlueprintClassification.values().length];
		private float valsMassDist[][] = new float[BlueprintClassification.values().length][4];
		private float valsMassDistTotals[] = new float[BlueprintClassification.values().length];






		public EntityPriority() {
			super();
			Arrays.fill(vals, 1f);
			for(int i = 0; i < BlueprintClassification.values().length; i++){
				Arrays.fill(valsMassDist[i], 1f);
				calcTotalWeightDistribution(BlueprintClassification.values()[i]);
			}

		}

		private void calcTotalWeightDistribution(BlueprintClassification c) {
			valsMassDistTotals[c.ordinal()] = 0;
			for(int i = 0; i < valsMassDist[c.ordinal()].length; i++){
				valsMassDistTotals[c.ordinal()] += valsMassDist[c.ordinal()][i];
			}
		}
		private void calcAllTotalWeightDistribution() {
			for(int i = 0; i < BlueprintClassification.values().length; i++){
				calcTotalWeightDistribution(BlueprintClassification.values()[i]);
			}
		}
		public float getVal(BlueprintClassification c){
			return vals[c.ordinal()];
		}

		public float getWeight(BlueprintClassification c, float sizeFrom0To1){
			assert(valsMassDistTotals[c.ordinal()] > 0);
			float[] fs = valsMassDist[c.ordinal()];
			assert(fs.length > 0);
			assert(sizeFrom0To1 >= 0);
			int index = Math.min(fs.length-1, Math.round(sizeFrom0To1*(fs.length-1f)));
			assert(index >= 0):Math.round(sizeFrom0To1*(fs.length-1f))+"; "+fs.length+"; "+sizeFrom0To1;
			return fs[index]/valsMassDistTotals[c.ordinal()];
		}

		public void setWeight(BlueprintClassification c, float ... f){
			assert(f.length == valsMassDist[c.ordinal()].length);
			for(int i = 0; i < f.length && i < valsMassDist[c.ordinal()].length; i++){
				valsMassDist[c.ordinal()][i] = f[i];
			}
			calcTotalWeightDistribution(c);
			assert(valsMassDistTotals[c.ordinal()] > 0);
		}
		public void parse(Node parent) throws ConfigParserException {
			NodeList childNodes = parent.getChildNodes();
			for(int n = 0; n < childNodes.getLength(); n++){
				Node item = childNodes.item(n);

				if (item.getNodeType() == Node.ELEMENT_NODE) {
					if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("classificationweight")){
						NodeList cassNodes = item.getChildNodes();
						BlueprintClassification c = null;
						float[] weightDistribution = null;
						float value = -1;

						for(int i = 0; i < cassNodes.getLength(); i++){
							Node clNode = cassNodes.item(i);
							if (clNode.getNodeType() == Node.ELEMENT_NODE) {

								if(clNode.getNodeName().toLowerCase(Locale.ENGLISH).equals("massdistribution")){
									String[] split = clNode.getTextContent().split(",");
									if(split == null){
										throw new ConfigParserException(item.getNodeName() + "-> " + parent.getNodeName() + " -> " + parent.getNodeName() + ": mass distribution has to be at least one value");
									}else{
										try{
											weightDistribution = new float[split.length];
											for(int x = 0; x < split.length; x++){
												float parseFloat = Float.parseFloat(split[x].trim());
												if(parseFloat >= 0){
													weightDistribution[x] = parseFloat;
												}else{
													throw new ConfigParserException(item.getNodeName() + " -> " + item.getNodeName() + " -> " + parent.getNodeName() + ": malformed mass distribution (need to all positive values)");
												}
											}
										}catch(Exception e){
											e.printStackTrace();
											throw new ConfigParserException(item.getNodeName() + " -> " + item.getNodeName() + " -> " + parent.getNodeName() + ": malformed mass distribution (need to be at least one floating point values)");
										}
									}
									if(weightDistribution == null || weightDistribution.length == 0){
										throw new ConfigParserException(item.getNodeName() + " -> " + item.getNodeName() + " -> " + parent.getNodeName() + ": malformed mass distribution (need to be at least one floating point values)");
									}
								}

								if(clNode.getNodeName().toLowerCase(Locale.ENGLISH).equals("classification")){
									BlueprintClassification bb = null;
									try{
										bb = BlueprintClassification.valueOf(clNode.getTextContent().toUpperCase(Locale.ENGLISH));
									}catch(IllegalArgumentException e){
										e.printStackTrace();
									}
									if(bb == null && clNode.getTextContent().toUpperCase(Locale.ENGLISH).endsWith("AVENGER")){
										bb = BlueprintClassification.SCAVENGER;
									}
									if(bb != null){
										c = bb;
									}else{
										throw new ConfigParserException(item.getNodeName() + " -> " + parent.getNodeName()+ ": classification unknown: '"+clNode.getTextContent()+"'; Allowed: "+Arrays.toString(BlueprintClassification.values()));
									}
								}
								if(clNode.getNodeName().toLowerCase(Locale.ENGLISH).equals("weight")){
									try{
										float parseFloat = Float.parseFloat(clNode.getTextContent());
										if(parseFloat >= 0){
											value = parseFloat;
										}else{
											throw new ConfigParserException(item.getNodeName() + " -> " + parent.getNodeName() + ": malformed classification weight (need to a positive value)");
										}
									}catch(Exception e){
										e.printStackTrace();
										throw new ConfigParserException(item.getNodeName() + " -> " + parent.getNodeName()+": malformed classification weight  (need to be a floating point value)");
									}
								}
							}
						}
						if(c == null || value < 0f || weightDistribution == null){
							throw new ConfigParserException(item.getNodeName() + " -> " + parent.getNodeName()  + ": Invalid ClassificationWeight tag. need 'classification', 'Weight', and 'MassDistribution' sub tags");
						}else{
							vals[c.ordinal()] = value;
							valsMassDist[c.ordinal()] = weightDistribution;
						}
					}else{
						assert(false):item.getNodeName() + " -> " + parent.getNodeName() + ": Tag unknown";
						throw new ConfigParserException(item.getNodeName() + " -> " + parent.getNodeName() + ": Tag unknown");
					}
				}
			}
			calcAllTotalWeightDistribution();
		}


		public void appendXML(Document config, Element configElementNode) {

			Comment comment = config.createComment(
					"Classification Weight: A weighted distribution of blueprint classes. "
							+ "This will normalize on existing classes. "
							+ "Example if we have available FIGHTER(weight 3), "
							+ "MINING(weight 1), TRADING(weight 5), "
							+ "then with 9 spawned ships, there would be 3 fighters, "
							+ "1 miner, and 5 traders. "
							+ "Same happens with mass distribution of "
							+ "every class from index 0 (lightest ship) to 15 (heaviest ship), "
							+ "so if you set 0,0,0,0,0,0,0,0,0,0,0,0,0,0,1, "
							+ "only the biggest mass ships will be spawned (but nothing else). "
							+ "if you set 1,0,0,0,0,0,0,0,0,0,0,0,0,0,1, the smalled "
							+ "and biggest mass ships will spawn equally (but nothing inbetween)");
			configElementNode.appendChild(comment);

			for(BlueprintClassification c : BlueprintClassification.values()){



				Element classWeigthNode = config.createElement("ClassificationWeight");



				Element bbName = config.createElement("Classification");
				Element bbWeight = config.createElement("Weight");

				bbName.setTextContent(c.name());
				bbWeight.setTextContent(String.valueOf(vals[c.ordinal()]));

				classWeigthNode.appendChild(bbName);
				classWeigthNode.appendChild(bbWeight);


				Element massDistNode = config.createElement("MassDistribution");
				StringBuffer ms = new StringBuffer();
				for(int i = 0; i < valsMassDist[c.ordinal()].length; i++){
					ms.append(valsMassDist[c.ordinal()][i]);
					if(i < valsMassDist[c.ordinal()].length-1){
						ms.append(", ");
					}
				}
				massDistNode.setTextContent(ms.toString());
				classWeigthNode.appendChild(massDistNode);

				configElementNode.appendChild(classWeigthNode);
			}

		}
	}
	@ConfigurationElement(name = "Outer")
	public EntityPriority outer = new EntityPriority();

	@ConfigurationElement(name = "Inner")
	public EntityPriority inner = new EntityPriority();

	@ConfigurationElement(name = "BaseCredits")
	public int baseCredits = 10000;

	@ConfigurationElement(name = "RandomCredits")
	public int randomCredits = 1000000;



	@ConfigurationElement(name = "HomeSystemEntitiesStart", description = "count of entities in home system. the amount of other systems adapts depending on distance and fill")
	private int homeSystemEntitiesStart = 25;

	@ConfigurationElement(name = "ShipCountModifier", description ="Must be at least 1. Changes the HomeSystemEntitiesStart for ships")
	private float shipCountModifier = 1;

	@ConfigurationElement(name = "StationCountModifier", description ="Must be at least 1. Changes the HomeSystemEntitiesStart for stations")
	private float stationCountModifier = 1;

	@ConfigurationElement(name = "FactionName")
	private String factionName = "TestFaction";

	public int[] baseTypeCount = new int[ElementKeyMap.highestType+1];

	public int[] randomTypeCount = new int[ElementKeyMap.highestType+1];

	@ConfigurationElement(name = "DefaultBaseTypeCountRaw", description="for initial inventory filling (raw stuff)")
	public int defaultBaseTypeCountRaw = 3;
	@ConfigurationElement(name = "DefaultRandomTypeCountRaw", description="for initial inventory filling (raw stuff)")
	public int defaultRandomTypeCountRaw = 10;

	@ConfigurationElement(name = "DefaultBaseTypeCountLevel1", description="for initial inventory filling (basic factory stuff)")
	public int defaultBaseTypeCountLevel1 = 2;
	@ConfigurationElement(name = "DefaultRandomTypeCountLevel1", description="for initial inventory filling (basic factory stuff)")
	public int defaultRandomTypeCountLevel1 = 6;

	@ConfigurationElement(name = "DefaultBaseTypeCountLevel2", description="for initial inventory filling (std factory stuff)")
	public int defaultBaseTypeCountLevel2 = 2;
	@ConfigurationElement(name = "DefaultRandomTypeCountLevel2", description="for initial inventory filling (std factory stuff)")
	public int defaultRandomTypeCountLevel2 = 4;

	@ConfigurationElement(name = "DefaultBaseTypeCountLevel3", description="for initial inventory filling (adv factory stuff)")
	public int defaultBaseTypeCountLevel3 = 1;
	@ConfigurationElement(name = "DefaultRandomTypeCountLevel3", description="for initial inventory filling (adv factory stuff)")
	public int defaultRandomTypeCountLevel3 = 5;

	/** normalized **/
	public final Short2FloatOpenHashMap typeWeightsWithProduction = new Short2FloatOpenHashMap();

	/** normalized **/
	public final Short2FloatOpenHashMap typeWeights = new Short2FloatOpenHashMap();


	@ConfigurationElement(name = "MinimumNormalizationModifier", description="When creating basic demand "
			+ "from blueprints. This is used to set a minimum weight, "
			+ "so that smaller ships become similary important than bigger things "
			+ "(since an npc faction is going to use higher numbers of smaller ships mostly)")
	private float minimumNormalizationModifier = 0.1f;


	private long totalElements;


	/** levels filled until entity count falloff**/
	@ConfigurationElement(name = "LevelsFilled", description="levels (distance from homebase) filled until entity count "
			+ "falloff. The higher, the further away systems will have the same amount of entities as the home system. "
			+ "Use to make more defensive slow expending factions, since they need take longer to assemble resources to"
			+ "expand as they are building much more stuff in the already taken territory.")
	private int levelsFilled = 2;

	private BlueprintTypeAvailability bta = new BlueprintTypeAvailability();

	@ConfigurationElement(name = "ProductionRawConversion", description="How many materials (in 0..1%) to use in production turn")
	public float productionRawConversion = 0.5f;

	@ConfigurationElement(name = "DemandAdditionalPerSystemMultiplier", description="Adds percentage of demand per system to total demand")
	public float demandMultPerSystem = 0.01f;

	@ConfigurationElement(name = "MinDemandPerSystem", description="Minimum demand for every resource per system")
	public int minDemandPerSystem = 500;

	@ConfigurationElement(name = "TradeDemandMult", description="Demand multiplier for trading")
	public float tradeDemandMult = 3;

	@ConfigurationElement(name = "ExpensionDemandMult", description="Demand multiplier for faction to grow")
	public float expensionDemandMult = 0.2f;

	@ConfigurationElement(name = "ExpensionDemandConsumeMult", description="Demand consumed on actual expansion")
	public float expensionDemandConsumeMult = 0.75f;

	@ConfigurationElement(name = "TradeBuyLowerPercentage", description="minimum Inventory/Demand percentage on which to start buying")
	public float tradeBuyLowerPercentage = 0.0f;

	@ConfigurationElement(name = "TradeBuyUpperPercentage", description="maximum Inventory/Demand percentage on which to stop buying")
	public float tradeBuyUpperPercentage = 0.6f;

	@ConfigurationElement(name = "TradeBuyLowerPricePercentage", description="Price to buy at on TradeBuyUpperPercentage")
	public float tradeBuyLowerPricePercentage = 0.95f;

	@ConfigurationElement(name = "TradeBuyUpperPricePercentage", description="Price to buy at on TradeBuyLowerPercentage")
	public float tradeBuyUpperPricePercentage = 1.10f;

	@ConfigurationElement(name = "TradeSellLowerPercentage", description="maximum Inventory/Demand percentage on which to stop selling")
	public float tradeSellLowerPercentage = 0.4f;

	@ConfigurationElement(name = "TradeSellUpperPercentage", description="minimum Inventory/Demand percentage on which to start selling")
	public float tradeSellUpperPercentage = 1.0f;

	@ConfigurationElement(name = "TradeSellLowerPricePercentage", description="Price to buy at on TradeSellUpperPercentage")
	public float tradeSellLowerPricePercentage = 0.9f;

	@ConfigurationElement(name = "TradeSellUpperPricePercentage", description="Price to sell at on TradeSellLowerPercentage")
	public float tradeSellUpperPricePercentage = 1.05f;

	@ConfigurationElement(name = "TradePricePerSystem", description="Trade Value: additional price per system (transport cost)")
	public float tradePricePerSystem = 0.005f;

	@ConfigurationElement(name = "MaxOwnCreditsToUseForTrade", description="Credits to use to buy stuff")
	public float maxOwnCreditsToUseForTrade = 0.35f;

	@ConfigurationElement(name = "MaxOtherCreditsToUseForTrade", description="Credits of trading partner to use for selling")
	public float maxOtherCreditsToUseForTrade = 0.75f;

	private NPCFactionPreset preset;

	@ConfigurationElement(name = "AbandonSystemOnStatus", description = "Abanond system when status (lost resources) are percentage of contingent")
	public double abandonSystemOnStatus = 0.25;

	@ConfigurationElement(name = "AbandonSystemOnStatusAfterResupply", description = "Abanond system when status (lost resources) are percentage of contingent. This is checked after resupply")
	public double abandonSystemOnStatusAfterResupply = 0.60;

	@ConfigurationElement(name = "MinimumResourceMining", description = "Minimum mining factor (for raw resources)")
	public float minimumResourceMining = 0.01f;

	@ConfigurationElement(name = "ResourcesPerMinerScore", description = "Multiplies score of all "
			+ "available Miners in a System with their mining score. Then that is "
			+ "multiplied with the mining factor (between 0 and 1 depending on "
			+ "resource richness of the system)")
	public float resourcesPerMinerScore = 100.0f;

	@ConfigurationElement(name = "MinedResourcesAddedFromMisc", description = "Percentage of mined misc materials (flowers, stone, etc) to be added to mined resources (indication for mined-out system)")
	public float minedResourcesAddedFromMisc = 0.1f;


	@ConfigurationElement(name = "ResourceAvaililityMultiplicator", description = "How much resources is richness worth. Basically the simulated amount of resources in a system in combination with richness")
	public float resourceAvaililityMultiplicator = 1000f;

	@ConfigurationElement(name = "BasicMiningScorePerSystem", description = "Basic mining score per system")
	public double basicMiningScorePerSystem = 300;

	@ConfigurationElement(name = "MiningStationMiningScore", description = "A mining station's mining score")
	public double miningStationMiningScore = 10000;


	@ConfigurationElement(name = "ResourcesConsumeStep", description = "Step size to consume per turn (added per turn until sum is 1)")
	public float resourcesConsumeStep = 0.05f;

	@ConfigurationElement(name = "ResourcesConsumedPerDistance", description = "Step size to consume per turn (added per turn until sum is 1)")
	public float resourcesConsumedPerDistance = 0.2f;


	@ConfigurationElement(name = "ReplenishResourceRate", description = "Percentage of Minable Resources in system replenished per turn")
	public float replenishResourceRate = 0.02f;

	@ConfigurationElement(name = "MinimumMinableResources", description = "Minimum Resource percentage in system on which faction will do mining")
	public float minimumMinableResources = 0.1f;

	@ConfigurationElement(name = "InitialGrowBaseDefault", description = "(only used if setting in spawn config is -1) Fixed minimum initial grown systems (additional to home system)")
	public int initialGrowBaseDefault = 0;

	@ConfigurationElement(name = "InitialGrowAddedDefaultRandom", description = "(only used if setting in spawn config is -1) Randomly added initial grown systems (additional to home system and minimum fixed amount)")
	public int initialGrowAddedDefaultRandom = 5;

	@ConfigurationElement(name = "TimeBetweenTurnsMS", description = "Time between faction turns in milliseconds")
	public long timeBetweenTurnsMS = 60000*15;


	@ConfigurationElement(name = "Diplomacy")
	public FactionDiplomacyConfig diplomacy = new FactionDiplomacyConfig();

	@ConfigurationElement(name = "DiplomacyStatusCheckDelay", description = "Check for status effects (war, etc)")
	public long diplomacyStatusCheckDelay = 60000;

	@ConfigurationElement(name = "DiplomacyTurnEffectDelay", description = "Time for turn effects to apply to points")
	public long diplomacyTurnEffectDelay = 60000;

	@ConfigurationElement(name = "DiplomacyTurnEffectChangeDelay", description = "Time for turn effects to change (falloff or get stronger depending on direction)")
	public long diplomacyTurnEffectChangeDelay = 60000;

	@ConfigurationElement(name = "FleetClasses")
	public final FleetClasses fleetClasses = new FleetClasses();

	@ConfigurationElement(name = "DiplomacyStartPoints")
	public int diplomacyStartPoints = 0;

	@ConfigurationElement(name = "DiplomacyMaxPoints")
	public int diplomacyMaxPoints = 5000;

	@ConfigurationElement(name = "DiplomacyMinPoints")
	public int diplomacyMinPoints = -5000;

	@ConfigurationElement(name = "MinimumTradeValue")
	public int minimumTradeValue = 10;

	@ConfigurationElement(name = "AmountOfBlocksToTradeMax")
	public int amountOfBlocksToTradeMax = 50000;

	@ConfigurationElement(name = "ProductionMultiplier")
	public int productionMultiplier = 1000;

	@ConfigurationElement(name = "ProductionStepts")
	public int productionStepts = 10;

	@ConfigurationElement(name = "DoesAbandonSystems", description="Should faction ever lose any system")
	public boolean doesAbandonSystems = true;

	@ConfigurationElement(name = "DoesAbandonHome", description="Should faction ever lose its home system (DoesAbandonSystems must be true for this to be considered)")
	public boolean doesAbandonHome  = false;

	@ConfigurationElement(name = "MinDefendFleetCount", description="Minimum fleets per system")
	public int minDefendFleetCount = 1;

	@ConfigurationElement(name = "MaxDefendFleetCount", description="Maximum fleets per system")
	public int maxDefendFleetCount = 5;

	@ConfigurationElement(name = "DefendFleetsPerStation", description="Fleets per station")
	public float defendFleetsPerStation = 0.3f;


	@ConfigurationElement(name = "MaxMiningFleetCount", description="Maximum fleets per system")
	public int maxMiningFleetCount = 5;

	@ConfigurationElement(name = "MiningShipsPerFleet", description="Amount of mining contingent to put in a mining fleet (no mining fleets without any contingent)")
	public float miningShipsPerFleet = 5f;


	@ConfigurationElement(name = "CanAttackStations", description="")
	public boolean canAttackStations = true;

	@ConfigurationElement(name = "CanAttackShips", description="")
	public boolean canAttackShips = true;

	@ConfigurationElement(name = "DamageTaken", description="")
	public float damageTaken = 1f;

	@ConfigurationElement(name = "DamageGiven", description="")
	public float damageGiven = 1f;

	@ConfigurationElement(name = "MinScavenginRange", description="Range in systems")
	public float minScavenginRange = 7.0f;

	@ConfigurationElement(name = "ScavengingRangePerFactionLevel", description="(level is max distance of territory in one dimension)")
	public float scavengingRangePerFactionLevel = 2.0f;

	@ConfigurationElement(name = "ScvengingFleetsPerTurn", description="Amount of sectors to target with scavengin fleets (sectors where an attack happened in range)")
	public int scvengingFleetsPerTurn = 3;

	@ConfigurationElement(name = "DefendFleetsToSendToAttacker", description="Percent of defend fleets of total defend fleets available to send to sector in which attack happens. This is only called once every 5 minutes per sector. Already close fleets are not considered")
	public float defendFleetsToSendToAttacker = 0.5f;

	@ConfigurationElement(name = "MinDefendFleetsToSendToAttacker", description="Minimum fleets to send to an attack in the system")
	public int minDefendFleetsToSendToAttacker = 1;

	@ConfigurationElement(name = "TradingMinDefenseShips", description="minimum defense/attack ships (if existing in the fleet type) to send on a trading mission")
	public int tradingMinDefenseShips = 1;

	@ConfigurationElement(name = "TradingCargoMassVersusDefenseMass", description="defense/attack ships to send compared to mass of cargo (more cargo, more ships) to send on a trding mission")
	public double tradingCargoMassVersusDefenseMass = 0.75;

	@ConfigurationElement(name = "TradingMaxDefenseShipsPerCargo", description="maximum defense/attack ships per cargo ship to send on a trding mission")
	public int tradingMaxDefenseShipsPerCargo = 5;


	private static byte VERSION = 0;

	public FactionConfig(){
		typeWeightsWithProduction.defaultReturnValue(0.0f);
	}
	public void initialize(){
		bta.create();
	}

	public void getWeightedContingent(int level, int maxLevel, float totalLevelFill, NPCEntityContingent out) throws EntityNotFountException{
		getWeightedContingent(level, maxLevel, totalLevelFill, EntityType.SHIP, Math.max(1f, shipCountModifier), out);
		getWeightedContingent(level, maxLevel, totalLevelFill, EntityType.SPACE_STATION, Math.max(1f, stationCountModifier), out);
	}
	private void getWeightedContingent(int level, int maxLevel, float totalLevelFill, EntityType t, float mult, NPCEntityContingent out) throws EntityNotFountException{
		Object2IntOpenHashMap<String> outerCounts = new Object2IntOpenHashMap<String>();
		Object2IntOpenHashMap<String> innerCounts = new Object2IntOpenHashMap<String>();

		assert(homeSystemEntitiesStart > 0);

		int homeSystemShips = (int) (homeSystemEntitiesStart * mult * totalLevelFill);
		getWeightedEntityCount(level, homeSystemShips, inner, t, innerCounts);
		getWeightedEntityCount(level, homeSystemShips, outer, t, outerCounts);

		float weight = Math.min(1.0f, NPCSystemStructure.getLevelWeight(level) * levelsFilled);

		float lvlWeight = (float)level / (float)maxLevel;
		for(String bb : outerCounts.keySet()){
			int innerAmount = innerCounts.getInt(bb);
			int outerAmount = outerCounts.getInt(bb);

			float diff = outerAmount - innerAmount;

			int amountInterpolWeighted = Math.max(1, (int) ((innerAmount + (diff * lvlWeight)) * weight));


			BlueprintEntry blueprint = getPreset().blueprintController.getBlueprint(bb);

			out.add(bb, blueprint.getEntityType().type, (float)blueprint.getMass(), blueprint.getClassification(), amountInterpolWeighted, blueprint.getElementCountMapWithChilds());
		}

	}
	private void getWeightedEntityCount(int level, int homeCount, EntityPriority a, EntityType t, Object2IntOpenHashMap<String> bbMap){

		assert(bta.created):"Faction config not initialized";
		float totalClassDist = 0;
		for(Entry<BlueprintClassification, BlueprintTypeAvailability.BlueprintTypeScale> e : bta.mp.entrySet()){
			if(e.getKey().type == t){
				float classificationDist = a.getVal(e.getKey());
				totalClassDist += classificationDist;
			}
		}

		for(Entry<BlueprintClassification, BlueprintTypeAvailability.BlueprintTypeScale> e : bta.mp.entrySet()){
			if(e.getKey().type == t){
				float classificationDist = a.getVal(e.getKey());
				float classWeight = classificationDist/totalClassDist;

				int shipCount = (int) (classWeight * homeCount);

				for(BlueprintTypeAvailability.BlueprintTypeScale.BBScale s : e.getValue().entries){

					float totalScale = 0;
					for(BlueprintTypeAvailability.BlueprintTypeScale.BBScale m : e.getValue().entries){
						totalScale += a.getWeight(e.getKey(), m.normalizedScale);
					}

					float dNorm = a.getWeight(e.getKey(), s.normalizedScale) / totalScale;
					int amountShips = (int) (dNorm * shipCount);

					bbMap.add(s.name, amountShips);
				}
			}
		}
	}
	public static void main(String[] sdf) throws IllegalArgumentException, IllegalAccessException, ConfigParserException, IOException, SAXException, ParserConfigurationException{
		recreate = true;
		FactionConfig c = new FactionConfig();
		writeDocument(new File("./data/npcFactions/npcConfigDefault.xml"), c);

		File dir = new File("./data/npcFactions/");

		File[] listFiles = dir.listFiles();
		for(File dd : listFiles){
			if(dd.isDirectory()){
				File file = new File(dd, "npcConfig.xml");
				if(file.exists()){
					System.err.println("WRITING TESTING "+file.getAbsolutePath());
					NPCFactionPreset preset = NPCFactionPreset.readFromDirOnlyConf(dd.getAbsolutePath());
					FactionConfig conf = new FactionConfig();
					conf.setPreset(preset);
				}else{
					System.err.println("ERROR NO NPC CONFIG FOUND "+file.getAbsolutePath());
				}
			}
		}
	}
	public class BlueprintTypeAvailability{
		final Object2ObjectOpenHashMap<BlueprintClassification, BlueprintTypeScale> mp = new Object2ObjectOpenHashMap<BlueprintClassification, BlueprintTypeScale>();
		private boolean created;
		private class BlueprintTypeScale{
			private List<BBScale> entries = new ObjectArrayList<BBScale>();
			private int totalMass;
			private class BBScale implements Comparable<BBScale>{
				String name;
				float mass;
				float normalizedScale;
				@Override
				public int compareTo(BBScale o) {
					return CompareTools.compare(normalizedScale, o.normalizedScale);
				}


			}
			void add(String name, float mass){
				BBScale v = new BBScale();
				v.name = name;
				v.mass = mass;
				entries.add(v);
			}
			public void calcNormal() {
				totalMass = 0;
				for(BBScale s : entries){
					totalMass += s.mass;
				}
				for(BBScale s : entries){
					s.normalizedScale = s.mass / totalMass;
				}

				Collections.sort(entries);
			}
		}

		void create(){

			if(!created){
				assert(getPreset() != null);
				assert(getPreset().blueprintController != null);
				List<BlueprintEntry> readBluePrints = getPreset().blueprintController.readBluePrints();

				for(BlueprintEntry b : readBluePrints){
					BlueprintClassification c = b.getClassification();
					BlueprintTypeScale s = mp.get(c);
					if(s == null){
						s = new BlueprintTypeScale();
						mp.put(c, s);
					}
					s.add(b.getName(), (float)b.getMass());
				}

				for(BlueprintTypeScale e : mp.values()){
					e.calcNormal();
				}
				created = true;
			}
		}
	}
	public String getSystemStationBlueprintName() {
		List<BlueprintEntry> readBluePrints = getPreset().blueprintController.readBluePrints();
		for(BlueprintEntry e : readBluePrints){
			if(e.getType().type == EntityType.SPACE_STATION){
				return e.getName();
			}
		}
		return null;
	}
	public void generate(LogInterface log){
		createWeightTableFromBlueprints();



		for(short type : ElementKeyMap.keySet){
			log.log(String.format("WEIGHT          : %-40s %f", ElementKeyMap.toString(type), typeWeights.get(type)), LogLevel.DEBUG);
			log.log(String.format("WEIGHT WITH PROD: %-40s %f", ElementKeyMap.toString(type), typeWeightsWithProduction.get(type)), LogLevel.DEBUG);
			//log.log("WEIGHT: "+ElementKeyMap.toString(type)+" -> "+typeWeights.get(type), LogLevel.DEBUG);
			//log.log("WEIGHT WITH PROD: "+ElementKeyMap.toString(type)+" -> "+typeWeightsWithProduction.get(type), LogLevel.DEBUG);

			ElementInformation info = ElementKeyMap.getInfoFast(type);

			if(info.isOre() || info.isCapsule() || (info.isShoppable() && info.getConsistence().isEmpty())){
				baseTypeCount[type] = defaultBaseTypeCountRaw;
				randomTypeCount[type] = defaultRandomTypeCountRaw;
			}else{
				int defbase = 0;
				int defrand = 1;
				switch(info.getProducedInFactoryType()){
					case(ElementKeyMap.FACTORY_CAPSULE_ASSEMBLER_ID): defbase = defaultBaseTypeCountRaw; defrand = defaultRandomTypeCountRaw; break;
					case(ElementKeyMap.FACTORY_MICRO_ASSEMBLER_ID): defbase = defaultBaseTypeCountRaw; defrand = defaultRandomTypeCountRaw; break;
					case(ElementKeyMap.FACTORY_BASIC_ID):defbase = defaultBaseTypeCountLevel1; defrand = defaultRandomTypeCountLevel1; break;
					case(ElementKeyMap.FACTORY_STANDARD_ID): defbase = defaultBaseTypeCountLevel2; defrand = defaultRandomTypeCountLevel2; break;
					case(ElementKeyMap.FACTORY_ADVANCED_ID): defbase = defaultBaseTypeCountLevel3; defrand = defaultRandomTypeCountLevel3; break;
				}
//				assert(defrand > 0);
				baseTypeCount[type] = defbase;
				randomTypeCount[type] = defrand;
				//log.log("BASE INV AMOUNTS: "+ElementKeyMap.toString(type)+" -> "+defbase+" (Rand: "+defrand+")", LogLevel.DEBUG);
			}
		}
	}

	private void createWeightTableFromBlueprints() {
		List<BlueprintEntry> prints = getPreset().blueprintController.readBluePrints();


		long maxAmount = 0;
		//get biggest blueprint to normalize amounts of others on
		for(BlueprintEntry entry : prints){
			maxAmount = Math.max(maxAmount, entry.getElementCountMapWithChilds().getTotalAmount());
		}
		ElementCountMap total = new ElementCountMap();
		for(BlueprintEntry entry : prints){
			double weight = Math.max(minimumNormalizationModifier, (double)entry.getElementCountMapWithChilds().getTotalAmount() / (double)maxAmount);
			total.add(entry.getElementCountMapWithChilds(), weight);
		}
		this.totalElements = total.getTotalAmount();
		total.getWeights(typeWeights);
		total.spikeWithProduction();
		total.getWeights(typeWeightsWithProduction);


	}
	public int getDemandAmount(short type){
		long val = Math.min(Integer.MAX_VALUE, (long) ((double)typeWeights.get(type)* (double)this.totalElements *homeSystemEntitiesStart));
		return (int)val;
	}
	public int getInitialAmount(short type, Random random, long seed) {
		random.setSeed(seed*234234222+type);

		if(ElementKeyMap.isOre(type) || ElementKeyMap.isShard(type)){
			return 10000+random.nextInt(10000);
		}

		return (int) ((double)(baseTypeCount[type] + (randomTypeCount[type] > 0 ? random.nextInt(randomTypeCount[type]) : 0) ) * getWeightedAmountWithProduction(type));
	}
	public int getWeightedAmountWithProduction(short type){
		long val = Math.min(Integer.MAX_VALUE, (long) ((double)typeWeightsWithProduction.get(type)* (double)this.totalElements * homeSystemEntitiesStart));
		return (int)val;
	}
	public void getWeightedDemand(ElementCountMap out, float weight){
		for(Entry<Short, Float> e : typeWeights.entrySet()){
			out.inc(e.getKey(), (int)(getWeightedAmountWithProduction(e.getKey()) * weight));
		}
	}


	public int getBuyPrice(float filling, long defaultPrice) {
		return getPrice(filling, defaultPrice,
				tradeBuyLowerPercentage,
				tradeBuyUpperPercentage,
				tradeBuyLowerPricePercentage,
				tradeBuyUpperPricePercentage,
				true);
	}
	public int getSellPrice(float filling, long defaultPrice) {
		return getPrice(filling, defaultPrice,
				tradeSellLowerPercentage,
				tradeSellUpperPercentage,
				tradeSellLowerPricePercentage,
				tradeSellUpperPricePercentage,
				false);
	}
	public int getPrice(float filling, long defaultPrice,
	                    float tradeLower,
	                    float tradeUpper,
	                    float tradeLowerPrice,
	                    float tradeUpperPrice, boolean buyPrice) {

		int price = 0;
		if((filling >= tradeLower || buyPrice) && (filling <= tradeUpper || !buyPrice) ){
			float cappedFilling;
			if(buyPrice){
				/*
				 * cap the filling for buying, so we will pay only as much as
				 * the lower limit suggests, even if we have less
				 */
				cappedFilling = Math.max(tradeLower, filling);
			}else{
				/*
				 * cap the filling for selling, so we will set the price only as
				 * much as the upper limit suggests, even if we have more
				 */
				cappedFilling = Math.min(tradeUpper, filling);
			}
			float dist = tradeUpper - tradeLower;
			float percFilled = (cappedFilling - tradeLower) / dist;

			float priceSpan = tradeUpperPrice - tradeLowerPrice;

			float pricePerc = tradeUpperPrice - (priceSpan * percFilled);

			price = (int)(Math.round((double)defaultPrice * (double)pricePerc));

		}

		return price;
	}


	public NPCFactionPreset getPreset() {
		return preset;
	}


	public void setPreset(NPCFactionPreset preset) throws IllegalArgumentException, IllegalAccessException, ConfigParserException {
		this.preset = preset;
		try{
			parse(preset.doc);
		}catch(RuntimeException e){
			throw new ConfigParserException("EXCEPTION WHEN PARSING FACTION CONFIG FOR: "+preset.factionPresetName, e);
		}
	}


	/*
	public void fromTagStructure(Tag tag, NPCFactionPresetManager npcFactionPresetManager) throws IllegalArgumentException, IllegalAccessException, ConfigParserException {
		Tag[] t = tag.getStruct();
		byte b = t[0].getByte();

		String nm = t[1].getString();

		for(NPCFactionPreset p : npcFactionPresetManager.getNpcPresets()){
			if(p.factionPresetName.equals(nm)){
				setPreset(p);
				break;
			}
		}
		if(preset == null){
			setPreset(npcFactionPresetManager.getRandomLeastUsedPreset(Universe.getRandom().nextLong(), nm.hashCode()));
		}
	}


	public Tag toTagStructure() {
		return new Tag(Type.STRUCT, null, new Tag[]{
				new Tag(Type.BYTE, null, VERSION),
				new Tag(Type.STRING, null, preset.factionPresetName),
				new Tag(Type.FINISH, null,null)})
				;
	}
	*/
	public void create(Document config) throws DOMException, IllegalArgumentException, IllegalAccessException{
		Element root = config.createElement("FactionConfig");
		config.appendChild(root);


		Element verNode = config.createElement("Version");
		verNode.setTextContent(String.valueOf(XML_VERSION));

		Comment comment = config
				.createComment("autocreated");
		verNode.appendChild(comment);


		root.appendChild(verNode);

		Element configNode = config.createElement("Config");
		root.appendChild(configNode);

		Field[] fields = getClass().getDeclaredFields();
		for (Field f : fields) {
			f.setAccessible(true);

			ConfigurationElement annotation = f.getAnnotation(ConfigurationElement.class);

			if (annotation != null) {

				Element configElementNode = config.createElement(annotation.name());

				if (f.getType().equals(FleetClasses.class)) {
					((FleetClasses) f.get(this)).appendXML(config, configElementNode);
				}else if (f.getType().equals(FactionDiplomacyConfig.class)) {
					((FactionDiplomacyConfig) f.get(this)).appendXML(config, configElementNode);
				}else if (f.getType().equals(EntityPriority.class)) {
					((EntityPriority) f.get(this)).appendXML(config, configElementNode);
				}else{
					configElementNode.setTextContent(f.get(this).toString());
				}

				if(annotation.description() != null && annotation.description().trim().length() > 0){
					Comment desc = config
							.createComment(annotation.description());
					configElementNode.appendChild(desc);
				}
				configNode.appendChild(configElementNode);
			}
		}

	}
	public static File writeDocument(File file, FactionConfig config) {
		try {
			// ///////////////////////////
			// Creating an empty XML Document

			DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
			Document doc = docBuilder.newDocument();


			config.create(doc);

			doc.setXmlVersion("1.0");

			// create a comment and put it in the root element

			// ///////////////
			// Output the XML

			// set up a transformer
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			// create string from xml tree
			StringWriter sw = new StringWriter();
			StreamResult result = new StreamResult(file);
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
			// String xmlString = sw.toString();

			// print xml
			// System.out.println("Here's the xml:\n\n" + xmlString);
			return file;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public void parse(Document config) throws IllegalArgumentException, IllegalAccessException, ConfigParserException {
		org.w3c.dom.Element root = config.getDocumentElement();
		NodeList childNodesTop = root.getChildNodes();

		Field[] fields = getClass().getDeclaredFields();

		boolean foundTop = false;
		ObjectOpenHashSet<Field> loaded = new ObjectOpenHashSet<Field>();
		byte version = 0;
		for (int j = 0; j < childNodesTop.getLength(); j++) {
			Node itemTop = childNodesTop.item(j);

			if (itemTop.getNodeType() == Node.ELEMENT_NODE && itemTop.getNodeName().toLowerCase(Locale.ENGLISH).equals("version")) {

				try{
					version = Byte.parseByte(itemTop.getTextContent());
				}catch(Exception e){
					e.printStackTrace();
					throw new ConfigParserException("malformed version in xml", e);
				}

			}else if (itemTop.getNodeType() == Node.ELEMENT_NODE && itemTop.getNodeName().toLowerCase(Locale.ENGLISH).equals(getTag().toLowerCase(Locale.ENGLISH))) {
				NodeList childNodesIn = itemTop.getChildNodes();
				foundTop = true;
				for (int k = 0; k < childNodesIn.getLength(); k++) {

					Node item = childNodesIn.item(k);
					if (item.getNodeType() == Node.ELEMENT_NODE) {
						boolean searching = false;
						boolean found = false;
						for (Field f : fields) {
							f.setAccessible(true);
							ConfigurationElement annotation = f.getAnnotation(ConfigurationElement.class);

							if (annotation != null) {
								searching = true;
								if (annotation.name().toLowerCase(Locale.ENGLISH).equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
									try {
										if (f.getType() == Boolean.TYPE) {
											f.setBoolean(this, Boolean.parseBoolean(item.getTextContent()));
											found = true;
										} else if (f.getType() == Integer.TYPE) {
											f.setInt(this, Integer.parseInt(item.getTextContent()));
											found = true;
										} else if (f.getType() == Short.TYPE) {
											f.setShort(this, Short.parseShort(item.getTextContent()));
											found = true;
										} else if (f.getType() == Byte.TYPE) {
											f.setByte(this, Byte.parseByte(item.getTextContent()));
											found = true;
										} else if (f.getType() == Float.TYPE) {
											f.setFloat(this, Float.parseFloat(item.getTextContent()));
											found = true;
										} else if (f.getType() == Double.TYPE) {
											f.setDouble(this, Double.parseDouble(item.getTextContent()));
											found = true;
										} else if (f.getType() == Long.TYPE) {
											f.setLong(this, Long.parseLong(item.getTextContent()));
											found = true;
										} else if (f.getType().equals(EntityPriority.class)) {
											EntityPriority ePrio = (EntityPriority) f.get(this);
											ePrio.parse(item);
											found = true;
										} else if (f.getType().equals(FactionDiplomacyConfig.class)) {
											FactionDiplomacyConfig ePrio = (FactionDiplomacyConfig) f.get(this);
											ePrio.parse(item);
											found = ePrio.version == FactionDiplomacyConfig.VERSION;
											if(!found){
												ePrio.reactions.clear();
												ePrio.addDefaultActions();
											}
										} else if (f.getType().equals(FleetClasses.class)) {
											FleetClasses ePrio = (FleetClasses) f.get(this);
											ePrio.parse(item);
											found = true;
										} else if (f.getType().equals(String.class)) {
											String desc = item.getTextContent();
											desc = desc.replaceAll("\\r\\n|\\r|\\n", "");
											desc = desc.replaceAll("\\\\n", "\n");
											desc = desc.replaceAll("\\\\r", "\r");
											desc = desc.replaceAll("\\\\t", "\t");
											f.set(this, desc);
											found = true;
										} else {
											throw new ConfigParserException("Cannot parse field: " + f.getName() + "; " + f.getType());
										}
									} catch (NumberFormatException e) {
										throw new ConfigParserException("Cannot parse field: " + f.getName() + "; " + f.getType() + "; with " + item.getTextContent(), e);
									}
								}
								if (found) {
									loaded.add(f);
									break;
								}
							}
						}
						if (searching && !found) {
//							assert(false):itemTop.getNodeName() + " -> " + item.getNodeName()  + ": No appropriate field found for tag: " + item.getNodeName();
							try{
								throw new ConfigParserException(itemTop.getNodeName() + " -> " + item.getNodeName()  + ": No appropriate field found for tag: " + item.getNodeName()+"\nVALUE WILL BE OMITTED");
							}catch(ConfigParserException e){
								e.printStackTrace();
							}
						}

					}
				}

			}
		}

		if (!foundTop) {
			throw new ConfigParserException("Block module Tag \"" + getTag() + "\" not found in block behavior configuation. Please create it (case insensitive)");
		}
		Annotation[] annotations = getClass().getAnnotations();
		for (Field f : fields) {
			f.setAccessible(true);
			ConfigurationElement annotation = f.getAnnotation(ConfigurationElement.class);

			if (annotation != null && !loaded.contains(f)) {

				try{
					throw new ConfigParserException("virtual field " + f.getName() + " (" + annotation.name() + ") not found. Please define a tag \"" + annotation.name() + "\" inside  of \"" + getTag() + "\"");
				}catch(Exception e){
					e.printStackTrace();
				}
				System.err.println("CATCHED EXCEPTION. WRITING MISSING TAGS TO "+getPreset().confFile.getAbsolutePath());
				writeDocument(getPreset().confFile, this);
				break;
			}
		}


	}
	private String getTag() {
		return "Config";
	}

	public boolean existsAction(DiploActionType action) {
		return diplomacy.get(action) != null;
	}
	public boolean existsStatus(DiplStatusType status) {
		return diplomacy.get(status) != null;
	}
	public int getDiplomacyUpperLimit(DiploActionType action) {
		return diplomacy.get(action).upperLimit;
	}

	public int getDiplomacyLowerLimit(DiploActionType action) {
		return diplomacy.get(action).lowerLimit;
	}
	public int getDiplomacyValue(DiploActionType action) {
		return diplomacy.get(action).value;
	}
	public int getDiplomacyValue(DiplStatusType action) {
		return diplomacy.get(action).value;
	}
	public DiplomacyReaction getDiplomacyActionRequired(DiploActionType action) {
		return  diplomacy.get(action).reaction;
	}
	public int getDiplomacyExistingActionModifier(DiploActionType action) {
		return diplomacy.get(action).existingModifier;
	}
	public int getDiplomacyNonExistingActionModifier(DiploActionType action) {
		return diplomacy.get(action).nonExistingModifier;
	}
	public long getDiplomacyTurnTimeout(DiploActionType action) {
		return (long) ((double)diplomacy.get(action).turnsActionDuration * (double)timeBetweenTurnsMS);
	}
	public long getDiplomacyStaticTimpout(DiplStatusType action) {
		return (long) ((double)diplomacy.get(action).staticTimeoutTurns * (double)timeBetweenTurnsMS);
	}
	public BlueprintClassification[] getFleetClasses(FleetType type) {
		return fleetClasses.classes.get(type);
	}
	public class FleetClasses{
		Object2ObjectOpenHashMap<FleetType, BlueprintClassification[]> classes = new Object2ObjectOpenHashMap<FleetType, BlueprintClassification[]>();
		public FleetClasses(){
			for(FleetType t : FleetType.values()){
				switch(t){
					case DEFENDING:
						classes.put(t, new BlueprintClassification[]{BlueprintClassification.NONE, BlueprintClassification.DEFENSE, BlueprintClassification.ATTACK});
						break;
					default:
						classes.put(t, new BlueprintClassification[]{BlueprintClassification.NONE});
						break;
				}

			}
		}
		public void appendXML(Document doc, Node parent){
			for(FleetType t : FleetType.values()){
				BlueprintClassification[] bb = classes.get(t);

				Node flt = doc.createElement("FleetClass");
				Node typ = doc.createElement("Type");
				Node clss = doc.createElement("Classes");


				flt.appendChild(doc.createComment("Blueprint classes going to be added to a fleet of this class in looping order"));
				flt.appendChild(typ);
				flt.appendChild(clss);

				typ.setTextContent(t.name());

				StringBuffer b = new StringBuffer();
				for(int i = 0; i < bb.length; i++){
					b.append(bb[i].name());
					if(i < bb.length-1){
						b.append(", ");
					}
				}
				clss.setTextContent(b.toString());

				parent.appendChild(flt);

			}
		}

		public void parse(Node parent) throws ConfigParserException{
			NodeList childs = parent.getChildNodes();
			for(int i = 0; i < childs.getLength(); i++){
				Node node = childs.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE &&
						node.getNodeName().toLowerCase(Locale.ENGLISH).equals("fleetclass")) {
					NodeList childNodes = node.getChildNodes();
					FleetType type = null;
					BlueprintClassification[] classes = null;
					for(int c = 0; c < childNodes.getLength(); c++){
						Node item = childNodes.item(c);
						if (item.getNodeType() == Node.ELEMENT_NODE &&
								item.getNodeName().toLowerCase(Locale.ENGLISH).equals("type")) {

							type = FleetType.valueOf(item.getTextContent());
						}else if (item.getNodeType() == Node.ELEMENT_NODE &&
								item.getNodeName().toLowerCase(Locale.ENGLISH).equals("classes")) {

							String[] split = item.getTextContent().split(",");
							if(split.length > 0){


								classes = new BlueprintClassification[split.length];

								for(int g = 0; g < split.length; g++){
									classes[g] = BlueprintClassification.valueOf(split[g].trim().toUpperCase(Locale.ENGLISH));
									if(classes[g] == null){
										throw new ConfigParserException("Blueprint Classification unknown: "+split[i]+"; "+item.getNodeName()+"; "+node.getNodeName());
									}
								}
							}else{
								throw new ConfigParserException("Blueprint Classification Classes Empty for fleet type; "+item.getNodeName()+"; "+node.getNodeName()+"; Content: "+item.getTextContent()+"; parse: "+Arrays.toString(split));
							}

						}
					}
					if(type == null){
						throw new ConfigParserException("Missing 'Type' node; "+node.getNodeName()+"; "+node.getParentNode().getNodeName());
					}
					if(classes == null){
						throw new ConfigParserException("Missing 'Classes' node; "+node.getNodeName()+"; "+node.getParentNode().getNodeName());
					}
					this.classes.put(type, classes);
				}
			}

		}

	}
	public List<FactionDiplomacyReaction> getDiplomacyReactions() {
		return diplomacy.reactions;
	}
	public long getDiplomacyActionTimeout(DiploActionType type) {
		if(diplomacy.actionTimeoutMap.containsKey(type)){
			return diplomacy.actionTimeoutMap.getLong(type);
		}
		return diplomacyTurnEffectChangeDelay*3;
	}


}