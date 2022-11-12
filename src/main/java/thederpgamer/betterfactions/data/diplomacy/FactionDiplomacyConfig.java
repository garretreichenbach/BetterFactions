package thederpgamer.betterfactions.data.diplomacy;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.config.ConfigParserException;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyAction.DiplActionType;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyCondition;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyCondition.ConditionType;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyConditionGroup;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyConditionGroup.ConditionMod;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyReaction;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyReaction.ConditionReaction;
import org.schema.game.server.data.simulation.npc.diplomacy.NPCDiplomacyEntity.DiplStatusType;
import org.w3c.dom.*;

import java.util.List;
import java.util.Locale;

public class FactionDiplomacyConfig {
	public static final byte VERSION = 2;
	public final List<FactionDiplomacyReaction> reactions = new ObjectArrayList<>();
	public final Object2LongOpenHashMap<DiplActionType> actionTimeoutMap = new Object2LongOpenHashMap<DiplActionType>();
	private final Int2ObjectOpenHashMap<DiplomacyConfigElement> map = new Int2ObjectOpenHashMap<DiplomacyConfigElement>();
	public byte version = 0;

	public FactionDiplomacyConfig() {
		for(DiplActionType t : DiplActionType.values()) actionTimeoutMap.put(t, 60000 * 5);
		for(DiplActionType t : DiplActionType.values()) {
			DiplomacyConfigElement c = new DiplomacyConfigElement();
			c.actionType = t;
			switch(t) {
				case ALLIANCE_WITH_ENEMY:
					c.upperLimit = - 10;
					c.lowerLimit = 0;
					c.value = - 10;
					break;
				case ALLIANCE_CANCEL:
					c.upperLimit = - 10;
					c.lowerLimit = 0;
					c.value = - 10;
					break;
				case ALLIANCE_REQUEST:
					c.upperLimit = 0;
					c.lowerLimit = 0;
				{
					DiplomacyReaction dr = new DiplomacyReaction(- 1);
					dr.reaction = ConditionReaction.ACCEPT_ALLIANCE_OFFER;
					dr.condition = new DiplomacyConditionGroup();
					dr.condition.mod = ConditionMod.AND;
					{
						DiplomacyCondition r = new DiplomacyCondition();
						r.type = ConditionType.RAW_POINTS;
						r.argumentValue = 1000;
						dr.condition.conditions.add(r);
					}
					{
						DiplomacyCondition r = new DiplomacyCondition();
						r.type = ConditionType.STATUS_PERSISTED;
						r.argumentStatus = DiplStatusType.NON_AGGRESSION;
						r.argumentValue = 30 * 60 * 1000; //30 min
						dr.condition.conditions.add(r);
					}
					c.reaction = dr;
				}
				break;
				case ATTACK:
					c.upperLimit = - 500;
					c.lowerLimit = 0;
					c.value = - 50;
					break;
				case ATTACK_ENEMY:
					c.upperLimit = 500;
					c.lowerLimit = 0;
					c.value = 50;
					break;
				case DECLARATION_OF_WAR:
					c.upperLimit = - 1000;
					c.lowerLimit = 0;
					c.value = - 1000;
					break;
				case MINING:
					c.upperLimit = - 30;
					c.lowerLimit = 0;
					c.value = - 30;
					break;
				case PEACE_OFFER:
					c.upperLimit = 0;
					c.lowerLimit = 0;
				{
					DiplomacyReaction dr = new DiplomacyReaction(- 1);
					dr.reaction = ConditionReaction.ACCEPT_PEACE_OFFER;
					dr.condition = new DiplomacyConditionGroup();
					dr.condition.mod = ConditionMod.AND;
					{
						DiplomacyCondition r = new DiplomacyCondition();
						r.type = ConditionType.RAW_POINTS;
						r.argumentValue = - 500;
						dr.condition.conditions.add(r);
					}
					{
						DiplomacyCondition r = new DiplomacyCondition();
						r.type = ConditionType.STATUS_PERSISTED;
						r.argumentStatus = DiplStatusType.NON_AGGRESSION;
						r.argumentValue = 30 * 60 * 1000; //30 min
						dr.condition.conditions.add(r);
					}
					c.reaction = dr;
				}

				break;
				case TERRITORY:
					c.upperLimit = - 300;
					c.lowerLimit = 0;
					c.value = - 1;
					break;
				case TRADING_WITH_US:
					c.upperLimit = 300;
					c.lowerLimit = 0;
					c.value = 1;
					break;
				case TRADING_WITH_ENEMY:
					c.upperLimit = 0;
					c.lowerLimit = - 200;
					c.value = - 1;
					break;
				default:
					break;

			}
			put(t, c);
		}

		for(DiplStatusType t : DiplStatusType.values()) {
			DiplomacyConfigElement c = new DiplomacyConfigElement();
			c.statusType = t;
			switch(t) {
				case ALLIANCE:
					c.value = 1000;
					break;
				case ALLIANCE_WITH_ENEMY:
					c.value = - 500;
					break;
				case CLOSE_TERRITORY:
					c.value = - 100;
					break;
				case IN_WAR:
					c.value = - 10000;
					break;
				case IN_WAR_WITH_ENEMY:
					c.value = 500;
					break;
				case PIRATE:
					//dont use this by default
					continue;
				case POWER:
					c.value = 1;
					break;
				case NON_AGGRESSION:
					c.value = 1;
					break;
				case ALLIANCE_WITH_FRIENDS:
					c.value = 10;
					break;
				case IN_WAR_WITH_FRIENDS:
					c.value = - 10;
					break;
				case FACTION_MEMBER_AT_WAR_WITH_US:
					c.value = - 500;
					break;
				case FACTION_MEMBER_WE_DONT_LIKE:
					c.value = - 50;
					break;
				default:
					c.value = - 123;
					break;

			}
			put(t, c);
		}
	}

	public void appendXML(Document config, Element pp) {
		Comment comment = config.createComment(
				"Diplomacy configuration. There are actions and states. "
						+ "States will add a constant effect on points as long as its active. "
						+ "Actions add/remove over time as long as the action is active. "
						+ "Actions get reset if repeated. Reactions can exist both for actions "
						+ "(checked and executed when that action happens) or general, "
						+ "which are executed on action/status turn.");
		pp.appendChild(comment);
		pp.appendChild(config.createComment("Status Types: " + DiplStatusType.list()));
		pp.appendChild(config.createComment("Action Types: " + DiplActionType.list()));
		pp.appendChild(config.createComment("Reaction Types: " + ConditionReaction.list()));
		pp.appendChild(config.createComment("Condition Types: " + ConditionType.list()));


		Element ver = config.createElement("Version");
		ver.setTextContent(String.valueOf(VERSION));
		Comment vc = config.createComment(
				"To ensure compatibility on updates, the Diplomacy config will reset to a new default, "
						+ "should the version differ with what the game considers to be the "
						+ "latest diplomacy config format version");
		ver.appendChild(vc);
		pp.appendChild(ver);

		Element actTO = config.createElement("ActionTimeouts");
		actTO.appendChild(config.createComment("How long actions are valid in milliseconds."));
		for(DiplActionType b : DiplActionType.values()) {
			Element cc = config.createElement(b.name().substring(0, 1) + b.name().substring(1).toLowerCase(Locale.ENGLISH));
			cc.setTextContent(String.valueOf(actionTimeoutMap.get(b)));
			actTO.appendChild(cc);
		}
		pp.appendChild(actTO);

		Element dpl = config.createElement("DiplomacyElement");

		boolean has = false;
		for(DiplActionType b : DiplActionType.values()) {
			DiplomacyConfigElement elem = get(b);
			if(elem != null) {
				elem.appendXML(config, dpl, b);
				has = true;
			}
		}
		for(DiplStatusType b : DiplStatusType.values()) {
			DiplomacyConfigElement elem = get(b);
			if(elem != null) {
				elem.appendXML(config, dpl, b);
				has = true;
			}
		}

		if(has) {
			pp.appendChild(dpl);
		}
		if(reactions.size() == 0) {
			addDefaultActions();
		}
		if(reactions.size() > 0) {
			Element re = config.createElement("Reactions");
			for(FactionDiplomacyReaction r : reactions) r.appendXML(config, re);
			pp.appendChild(re);
		}
	}

	public void parse(Node configElementNode) throws ConfigParserException {
		if(! FactionConfig.recreate) reactions.clear();
		NodeList childs = configElementNode.getChildNodes();

		for(int i = 0; i < childs.getLength(); i++) {
			Node node = childs.item(i);

			if(node.getNodeType() == Node.ELEMENT_NODE &&
					node.getNodeName().toLowerCase(Locale.ENGLISH).equals("version")) {
				this.version = Byte.parseByte(node.getTextContent());
			}
			if(node.getNodeType() == Node.ELEMENT_NODE &&
					node.getNodeName().toLowerCase(Locale.ENGLISH).equals("actiontimeouts")) {
				NodeList diplChilds = node.getChildNodes();


				for(int c = 0; c < diplChilds.getLength(); c++) {
					Node item = diplChilds.item(c);
					if(item.getNodeType() == Node.ELEMENT_NODE) {
						try {
							DiplActionType d = DiplActionType.valueOf(item.getNodeName().toUpperCase(Locale.ENGLISH));
							long ms = Long.parseLong(item.getTextContent());
							actionTimeoutMap.put(d, ms);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			if(node.getNodeType() == Node.ELEMENT_NODE &&
					node.getNodeName().toLowerCase(Locale.ENGLISH).equals("diplomacyelement")) {
				NodeList diplChilds = node.getChildNodes();


				for(int c = 0; c < diplChilds.getLength(); c++) {
					Node item = diplChilds.item(c);
					if(item.getNodeType() == Node.ELEMENT_NODE) {
						if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("diplaction") ||
								node.getNodeName().toLowerCase(Locale.ENGLISH).equals("diplstatus")) {
							DiplomacyConfigElement e = new DiplomacyConfigElement();
							e.parse(item);
						}

					}
				}
			}
			if(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().toLowerCase(Locale.ENGLISH).equals("reactions")) {
				parseReactions(node);
			}
		}
	}

	private void parseReactions(Node rt) {

		NodeList childs = rt.getChildNodes();
		int dIndex = 0;
		for(int i = 0; i < childs.getLength(); i++) {
			Node item = childs.item(i);
			if(item.getNodeType() == Node.ELEMENT_NODE) {
				try {

					if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("reaction")) {
						reactions.add(FactionDiplomacyReaction.parse(item, dIndex++));
					}
				} catch(ConfigParserException e) {
					e.printStackTrace();
					System.err.println("NOT USING REACTION BECAUSE OF EXCEPTION");
				}
			}
		}
	}

	public void addDefaultActions() {
		int i = 0;
		{ //Declare war
			FactionDiplomacyReaction diplomacyReaction = new FactionDiplomacyReaction(i ++);
			diplomacyReaction.reaction =  FactionDiplomacyReaction.ConditionReaction.DECLARE_WAR;
			diplomacyReaction.name = "Declare War";
			diplomacyReaction.condition = new FactionDiplomacyConditionGroup();
			diplomacyReaction.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;

			{ //Has a valid war goal
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
				condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.HAS_WAR_GOAL;
				diplomacyReaction.condition.conditions.add(condition);
			}

			{ //Not allied
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
				condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
				condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.ALLIANCE;
				diplomacyReaction.condition.conditions.add(condition);
			}

			{ //No Non-aggression pact
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
				condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
				condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.NON_AGGRESSION_PACT;
				diplomacyReaction.condition.conditions.add(condition);
			}

			{ //Not in federation
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
				condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
				condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.IN_FEDERATION;
				diplomacyReaction.condition.conditions.add(condition);
			}

			{ //Not protecting
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
				condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
				condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.PROTECTING;
				diplomacyReaction.condition.conditions.add(condition);
			}

			reactions.add(diplomacyReaction);
		}

		{ //Offer peace deal
			FactionDiplomacyReaction diplomacyReaction = new FactionDiplomacyReaction(i++);
			diplomacyReaction.reaction = FactionDiplomacyReaction.ConditionReaction.OFFER_PEACE_DEAL;
			diplomacyReaction.name = "Offer Peace Deal";
			diplomacyReaction.condition = new FactionDiplomacyConditionGroup();
			diplomacyReaction.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;

			{ //At war
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
				condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.IN_WAR;
				diplomacyReaction.condition.conditions.add(condition);
			}

			reactions.add(diplomacyReaction);
		}

		{ //Improve relations
			
		}

		{ //Offer non-aggression
			FactionDiplomacyReaction diplomacyReaction = new FactionDiplomacyReaction(i++);
			diplomacyReaction.reaction = FactionDiplomacyReaction.ConditionReaction.OFFER_NON_AGGRESSION_PACT;
			diplomacyReaction.name = "Offer Non-Aggression Pact";
			diplomacyReaction.condition = new FactionDiplomacyConditionGroup();
			diplomacyReaction.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;

			{ //Not at war
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
				condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
				condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.IN_WAR;
				diplomacyReaction.condition.conditions.add(condition);
			}

			{ //Not allied
				FactionDiplomacyConditionGroup conditionGroup = new FactionDiplomacyConditionGroup();
				conditionGroup.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;

				{ //Relation must be above -30
					FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
					condition.type = FactionDiplomacyCondition.ConditionType.TOTAL_POINTS;
					condition.argumentValue = -30;
				}

				{ //Not allied
					FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
					condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
					condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
					condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.ALLIANCE;
					conditionGroup.conditions.add(condition);
				}

				{ //Not in federation
					FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
					condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
					condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
					condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.IN_FEDERATION;
					conditionGroup.conditions.add(condition);
				}
			}
		}

		{ //Offer alliance
			FactionDiplomacyReaction diplomacyReaction = new FactionDiplomacyReaction(i++);
			diplomacyReaction.reaction = FactionDiplomacyReaction.ConditionReaction.OFFER_ALLIANCE;
			diplomacyReaction.name = "Offer Alliance";
			diplomacyReaction.condition = new FactionDiplomacyConditionGroup();
			diplomacyReaction.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;

			{ //Not at war
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
				condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
				condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.IN_WAR;
				diplomacyReaction.condition.conditions.add(condition);
			}

			{ //Relation must be above 30
				FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
				condition.type = FactionDiplomacyCondition.ConditionType.TOTAL_POINTS;
				condition.argumentValue = 30;
			}

			{
				FactionDiplomacyConditionGroup conditionGroup = new FactionDiplomacyConditionGroup();
				conditionGroup.mod = FactionDiplomacyConditionGroup.ConditionMod.OR;

				{ //Not allied
					FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
					condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
					condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
					condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.ALLIANCE;
					conditionGroup.conditions.add(condition);
				}

				{ //Not in federation
					FactionDiplomacyCondition condition = new FactionDiplomacyCondition();
					condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
					condition.type = FactionDiplomacyCondition.ConditionType.EXISTS_STATUS;
					condition.argumentStatus = FactionDiplomacyEntity.DiploStatusType.IN_FEDERATION;
					conditionGroup.conditions.add(condition);
				}

				diplomacyReaction.condition.conditions.add(conditionGroup);
			}
		}
		/*
		{
			FactionDiplomacyReaction r = new FactionDiplomacyReaction(i++);
			r.name = "Offer Alliance";
			r.reaction = FactionDiplomacyReaction.ConditionReaction.OFFER_ALLIANCE;
			r.condition = new FactionDiplomacyConditionGroup();
			r.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;
			{
				FactionDiplomacyCondition d = new FactionDiplomacyCondition();
				d.type = FactionDiplomacyCondition.ConditionType.TOTAL_POINTS;
				d.argumentValue = 1000;
				r.condition.conditions.add(d);
			}
			{
				FactionDiplomacyCondition d = new FactionDiplomacyCondition();
				d.type = FactionDiplomacyCondition.ConditionType.STATUS_PERSISTED;
				d.argumentStatus = FactionDiplomacyEntity.DiploStatusType.NON_AGGRESSION;
				d.argumentValue = 120 * 60 * 1000;
				r.condition.conditions.add(d);
			}
			reactions.add(r);
		}

		{
			FactionDiplomacyReaction r = new FactionDiplomacyReaction(i++);
			r.reaction = FactionDiplomacyReaction.ConditionReaction.REMOVE_ALLIANCE_OFFER;
			r.name = "Remove Alliance Offer";
			r.condition = new FactionDiplomacyConditionGroup();
			r.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;

			{
				FactionDiplomacyCondition d = new FactionDiplomacyCondition();
				d.type = FactionDiplomacyCondition.ConditionType.ACTION_COUNTER;
				d.argumentAction = DiplomacyAction.DiplActionType.ATTACK;
				d.argumentValue = 1;
				r.condition.conditions.add(d);
			}
			reactions.add(r);
		}
		{
			FactionDiplomacyReaction r = new FactionDiplomacyReaction(i++);
			r.name = "Remove Peace Deal Offer";
			r.reaction = FactionDiplomacyReaction.ConditionReaction.REMOVE_PEACE_DEAL_OFFER;
			r.condition = new FactionDiplomacyConditionGroup();
			r.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;

			{
				FactionDiplomacyCondition d = new FactionDiplomacyCondition();
				d.type = FactionDiplomacyCondition.ConditionType.ACTION_COUNTER;
				d.argumentAction = DiplActionType.ATTACK;
				d.argumentValue = 1;
				r.condition.conditions.add(d);
			}
			reactions.add(r);
		}
		{
			FactionDiplomacyReaction r = new FactionDiplomacyReaction(i++);
			r.reaction = FactionDiplomacyReaction.ConditionReaction.REJECT_ALLIANCE_OFFER;
			r.name = "Reject Alliance Offer";
			r.condition = new FactionDiplomacyConditionGroup();
			r.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
			{
				FactionDiplomacyCondition d = new FactionDiplomacyCondition();
				d.type = FactionDiplomacyCondition.ConditionType.TOTAL_POINTS;
				d.argumentValue = 990;
				r.condition.conditions.add(d);
			}
			reactions.add(r);
		}
		{
			FactionDiplomacyReaction r = new FactionDiplomacyReaction(i++);
			r.reaction = FactionDiplomacyReaction.ConditionReaction.REJECT_PEACE_OFFER;
			r.name = "Reject Peace Deal Offer";
			r.condition = new FactionDiplomacyConditionGroup();
			r.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.NOT;
			{
				FactionDiplomacyCondition d = new FactionDiplomacyCondition();
				d.type = FactionDiplomacyCondition.ConditionType.TOTAL_POINTS;
				d.argumentValue = - 500;
				r.condition.conditions.add(d);
			}
			reactions.add(r);
		}
		{
			FactionDiplomacyReaction r = new FactionDiplomacyReaction(i++);
			r.reaction = FactionDiplomacyReaction.ConditionReaction.SEND_POPUP_MESSAGE;
			r.name = "Send message on one Attack";
			r.condition = new FactionDiplomacyConditionGroup();
			r.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;
			r.message = "Attacks will not be tolerated!";
			{
				FactionDiplomacyCondition d = new FactionDiplomacyCondition();
				d.type =FactionDiplomacyCondition.ConditionType.ACTION_COUNTER;
				d.argumentAction = DiplActionType.ATTACK;
				d.argumentValue = 1;
				r.condition.conditions.add(d);
			}
			reactions.add(r);
		}
		{
			FactionDiplomacyReaction r = new FactionDiplomacyReaction(i++);
			r.reaction = FactionDiplomacyReaction.ConditionReaction.DECLARE_WAR;
			r.name = "Declare War on hostile actions";
			r.condition = new FactionDiplomacyConditionGroup();
			r.condition.mod = FactionDiplomacyConditionGroup.ConditionMod.AND;
			{
				FactionDiplomacyCondition d = new FactionDiplomacyCondition();
				d.type = FactionDiplomacyCondition.ConditionType.ACTION_COUNTER;
				d.argumentAction = DiplActionType.ATTACK;
				d.argumentValue = 3;
				r.condition.conditions.add(d);
			}
			reactions.add(r);
		}
		 */
	}

	public int getIndex(DiplActionType action) {
		return (1000 * (action.ordinal() + 1));
	}

	public int getIndex(DiplStatusType status) {
		return (1000000 * (status.ordinal() + 1));
	}

	public DiplomacyConfigElement get(DiplActionType action) {
		return map.get(getIndex(action));
	}

	public DiplomacyConfigElement get(DiplStatusType status) {
		return map.get(getIndex(status));
	}

	public DiplomacyConfigElement put(DiplActionType action, DiplomacyConfigElement p) {
		return map.put(getIndex(action), p);
	}

	public DiplomacyConfigElement put(DiplStatusType status, DiplomacyConfigElement p) {
		return map.put(getIndex(status), p);
	}

	public class DiplomacyConfigElement {
		public DiplStatusType statusType;
		public DiplActionType actionType;
		public int upperLimit = 30;
		public int lowerLimit = 0;
		public int value = 0;
		public int existingModifier = 1;
		public int nonExistingModifier = 1;
		public float turnsActionDuration = 1;
		public float staticTimeoutTurns = 1;
		public DiplomacyReaction reaction;

		public void parse(Node node) throws ConfigParserException {
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				if(node.getNodeName().toLowerCase(Locale.ENGLISH).equals("diplaction")) {
					NodeList childNodes = node.getChildNodes();
					Integer value = null;
					Float upperLim = null;
					Float lowerLim = null;
					Float nonExt = null;
					Float ext = null;
					Float timeout = null;
					String name = null;

					for(int i = 0; i < childNodes.getLength(); i++) {
						Node item = childNodes.item(i);
						if(item.getNodeType() == Node.ELEMENT_NODE) {
							if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("name")) {
								name = item.getTextContent();
							}
							if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("upperlimit")) {
								try {
									upperLim = Float.parseFloat(item.getTextContent());
								} catch(NumberFormatException e) {
									e.printStackTrace();
									throw new ConfigParserException(
											"Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
													+ item.getParentNode().getNodeName() + "; "
													+ item.getParentNode().getParentNode().getNodeName());
								}
							}
							try {
								if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("reaction")) {
									reaction = DiplomacyReaction.parse(item, - 1);
								}
							} catch(ConfigParserException e) {
								e.printStackTrace();
								System.err.println("NOT USING REACTION BECAUSE OF EXCEPTION");
							}
							if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("lowerlimit")) {
								try {
									lowerLim = Float.parseFloat(item.getTextContent());
								} catch(NumberFormatException e) {
									e.printStackTrace();
									throw new ConfigParserException(
											"Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
													+ item.getParentNode().getNodeName() + "; "
													+ item.getParentNode().getParentNode().getNodeName());
								}
							}
							if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("nonexistingmodifier")) {
								try {
									nonExt = Float.parseFloat(item.getTextContent());
								} catch(NumberFormatException e) {
									e.printStackTrace();
									throw new ConfigParserException(
											"Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
													+ item.getParentNode().getNodeName() + "; "
													+ item.getParentNode().getParentNode().getNodeName());
								}
							}
							if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("existingmodifier")) {
								try {
									ext = Float.parseFloat(item.getTextContent());
								} catch(NumberFormatException e) {
									e.printStackTrace();
									throw new ConfigParserException(
											"Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
													+ item.getParentNode().getNodeName() + "; "
													+ item.getParentNode().getParentNode().getNodeName());
								}
							}
							if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("actiontimeoutduration")) {
								try {
									timeout = Float.parseFloat(item.getTextContent());
								} catch(NumberFormatException e) {
									e.printStackTrace();
									throw new ConfigParserException(
											"Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
													+ item.getParentNode().getNodeName() + "; "
													+ item.getParentNode().getParentNode().getNodeName());
								}
							}
							if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("value")) {
								try {
									value = (int) Float.parseFloat(item.getTextContent());
								} catch(NumberFormatException e) {
									e.printStackTrace();
									throw new ConfigParserException(
											"Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
													+ item.getParentNode().getNodeName() + "; "
													+ item.getParentNode().getParentNode().getNodeName());
								}
							}
						}

					}
					if(upperLim == null) {
						throw new ConfigParserException(
								"'UpperLimit' Tag needed. " + node.getNodeName() + "; "
										+ node.getParentNode().getNodeName());
					}
					if(lowerLim == null) {
						throw new ConfigParserException(
								"'LowerLimit' Tag needed. " + node.getNodeName() + "; "
										+ node.getParentNode().getNodeName());
					}
					if(nonExt == null) {
						throw new ConfigParserException(
								"'NonExistingModifier' Tag needed. " + node.getNodeName() + "; "
										+ node.getParentNode().getNodeName());
					}
					if(ext == null) {
						throw new ConfigParserException(
								"'ExistingModifier' Tag needed. " + node.getNodeName() + "; "
										+ node.getParentNode().getNodeName());
					}
					if(name == null) {
						throw new ConfigParserException(
								"'Name' Tag needed. " + node.getNodeName() + "; "
										+ node.getParentNode().getNodeName());
					}
					if(timeout == null) {
						throw new ConfigParserException(
								"'ActionTimeoutDuration' Tag needed. " + node.getNodeName() + "; "
										+ node.getParentNode().getNodeName());
					}
					if(value == null) {
						throw new ConfigParserException(
								"'Value' Tag needed. " + node.getNodeName() + "; "
										+ node.getParentNode().getNodeName());
					}

					DiplActionType t = DiplActionType.valueOf(name.toUpperCase(Locale.ENGLISH));
					if(t == null) {
						throw new ConfigParserException(
								"Unknown DiplActionType: " + name + "; " + node.getNodeName() + "; "
										+ node.getParentNode().getNodeName());
					}
					this.actionType = t;
					this.upperLimit = upperLim.intValue();
					this.lowerLimit = lowerLim.intValue();
					this.nonExistingModifier = nonExt.intValue();
					this.existingModifier = ext.intValue();
					this.turnsActionDuration = timeout;
					this.value = value;

					put(actionType, this);
				} else
					if(node.getNodeName().toLowerCase(Locale.ENGLISH).equals("diplstatus")) {
						NodeList childNodes = node.getChildNodes();

						Integer value = null;
						Float timeout = null;
						String name = null;
						for(int i = 0; i < childNodes.getLength(); i++) {
							Node item = childNodes.item(i);
							if(item.getNodeType() == Node.ELEMENT_NODE) {
								if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("name")) {
									name = item.getTextContent();
								}
								if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("timeout")) {
									try {
										timeout = Float.parseFloat(item.getTextContent());
									} catch(NumberFormatException e) {
										e.printStackTrace();
										throw new ConfigParserException(
												"Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
														+ item.getParentNode().getNodeName() + "; "
														+ item.getParentNode().getParentNode().getNodeName());
									}
								}
								if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("value")) {
									try {
										value = (int) Float.parseFloat(item.getTextContent());
									} catch(NumberFormatException e) {
										e.printStackTrace();
										throw new ConfigParserException(
												"Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
														+ item.getParentNode().getNodeName() + "; "
														+ item.getParentNode().getParentNode().getNodeName());
									}
								}
							}

						}
						if(name == null) {
							throw new ConfigParserException(
									"'Name' Tag needed. " + node.getNodeName() + "; "
											+ node.getParentNode().getNodeName());
						}
						if(timeout == null) {
							throw new ConfigParserException(
									"'Timeout' Tag needed. " + node.getNodeName() + "; "
											+ node.getParentNode().getNodeName());
						}
						if(value == null) {
							throw new ConfigParserException(
									"'Value' Tag needed. " + node.getNodeName() + "; "
											+ node.getParentNode().getNodeName());
						}

						DiplStatusType t = DiplStatusType.valueOf(name.toUpperCase(Locale.ENGLISH));
						if(t == null) {
							throw new ConfigParserException(
									"Unknown DiplStatusType: " + name + "; " + node.getNodeName() + "; "
											+ node.getParentNode().getNodeName());
						}
						this.statusType = t;
						this.value = value;
						this.staticTimeoutTurns = timeout;
						put(statusType, this);

					} else {
						throw new ConfigParserException("Diplomacy Config Error " + node.getNodeName() + "; " + node.getParentNode().getNodeName());
					}
			}
		}

		public void appendXML(Document config, Element dpl, DiplStatusType b) {
			Element s = config.createElement("DiplStatus");
			Element name = config.createElement("Name");
			Element to = config.createElement("Timeout");
			Element val = config.createElement("Value");


			name.setTextContent(b.name());
			to.setTextContent(String.valueOf(staticTimeoutTurns));
			to.appendChild(config.createComment("In Faction Turns"));
			val.setTextContent(String.valueOf(value));
			val.appendChild(config.createComment("In diplomacy points (no float)"));

			s.appendChild(name);
			s.appendChild(to);
			s.appendChild(val);

			dpl.appendChild(s);
		}

		public void appendXML(Document config, Element dpl, DiplActionType b) {
			Element s = config.createElement("DiplAction");
			Element name = config.createElement("Name");
			Element to = config.createElement("UpperLimit");
			Element tol = config.createElement("LowerLimit");
			Element nonExt = config.createElement("NonExistingModifier");
			Element ext = config.createElement("ExistingModifier");
			Element timeout = config.createElement("ActionTimeoutDuration");
			Element val = config.createElement("Value");


			name.setTextContent(b.name());
			to.setTextContent(String.valueOf(upperLimit));
			to.appendChild(config.createComment("If lower than lower limit, it's a minus effect on diplomacy"));

			tol.setTextContent(String.valueOf(lowerLimit));
			ext.setTextContent(String.valueOf(existingModifier));
			ext.appendChild(config.createComment("How much the modifier changes towards upper limit each diplomacy round"));
			nonExt.setTextContent(String.valueOf(nonExistingModifier));
			nonExt.appendChild(config.createComment("How much the modifier changes towards lower limit each diplomacy round"));
			val.setTextContent(String.valueOf(value));
			val.appendChild(config.createComment("In diplomacy points (no float)"));
			timeout.setTextContent(String.valueOf(turnsActionDuration));
			timeout.appendChild(config.createComment("Time for Action to timeout (in faction turns)"));

			s.appendChild(name);
			s.appendChild(to);
			s.appendChild(tol);
			s.appendChild(ext);
			s.appendChild(nonExt);
			s.appendChild(timeout);
			s.appendChild(val);

			if(reaction != null) {
				reaction.appendXML(config, s);
			}

			dpl.appendChild(s);
		}


	}
}
