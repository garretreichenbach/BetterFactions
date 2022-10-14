package thederpgamer.betterfactions.data.diplomacy;

import org.schema.common.config.ConfigParserException;
import org.schema.common.util.StringTools;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyAction;
import org.schema.game.server.data.simulation.npc.diplomacy.DiplomacyAction.DiplActionType;
import org.w3c.dom.*;

import java.util.Locale;

public class FactionDiplomacyCondition extends FactionDiplomacyConditionGroup {

	public enum ConditionType{
		EXISTS_ACTION("does the action currently exist", true, false, false),
		EXISTS_STATUS("does the status currently exist", false, true, false),
		STATUS_POINTS("Value the status is affecting its entity with is equal or bigger than X", false, true, true),
		STATUS_PERSISTED("Min time the status has been applied for equal or bigger than X", false, true, true),
		ACTION_PERSISTED("Min time the action has been applied for equal or bigger than X. the action will persist to exist as long as it is repeated it timeouts", true, false, true),
		ACTION_COUNTER("Amount of actions triggered (within timeout) equal or bigger than", true, false, true),
		TOTAL_POINTS("Total diplomacy points equal or bigger than X", false, false, true),
		RAW_POINTS("Raw diplomacy points (without sttaus effects) equal or bigger than X", false, false, true),
		;
		public static String list() {
			return StringTools.listEnum(ConditionType.values());
		}
		public final String desc;
		public final boolean needsAction;
		public final boolean needsStatus;
		public final boolean needsValue;

		private ConditionType(String desc, boolean needsAction, boolean needsStatus, boolean needsValue){
			this.desc = desc;
			this.needsAction = needsAction;
			this.needsStatus = needsStatus;
			this.needsValue = needsValue;
		}
	}

	public ConditionType type;
	public DiplomacyAction.DiplActionType argumentAction;
	public FactionDiplomacyEntity.DiploStatusType argumentStatus;
	public double argumentValue;

	@Override
	public boolean satisfied(FactionDiplomacyEntity ent){
		switch (type) {
			case ACTION_COUNTER:
				return ent.getActionCount(argumentAction) >= argumentValue;
			case ACTION_PERSISTED:
				return ent.persistedActionModifier(argumentAction) >= argumentValue;
			case EXISTS_ACTION:
				return ent.getActionCount(argumentAction) > 0;
			case EXISTS_STATUS:
				return ent.existsStatusModifier(argumentStatus);
			case STATUS_PERSISTED:
				return ent.persistedStatusModifier(argumentStatus) >= argumentValue;
			case STATUS_POINTS:
				return ent.calculateStatus(argumentStatus) >= argumentValue;
			case RAW_POINTS:
				return ent.getRawPoints() >= argumentValue;
			case TOTAL_POINTS:
				return ent.getPoints() >= argumentValue;
		}


		throw new IllegalArgumentException("Unknown condition type: "+type);
	}

	public static FactionDiplomacyCondition parseCondition(Node node) throws ConfigParserException {
		FactionDiplomacyCondition r = new FactionDiplomacyCondition();
		NodeList childNodes = node.getChildNodes();

		DiplomacyAction.DiplActionType act = null;
		FactionDiplomacyEntity.DiploStatusType stat = null;
		Double argVal = null;

		for(int i = 0; i < childNodes.getLength(); i++){
			Node item = childNodes.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("type")){
					try{
						r.type = ConditionType.valueOf(item.getTextContent().toUpperCase(Locale.ENGLISH));
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("action")){
					try{
						act = DiplActionType.valueOf(item.getTextContent().toUpperCase(Locale.ENGLISH));
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("status")){
					try{
						stat = FactionDiplomacyEntity.DiploStatusType.valueOf(item.getTextContent().toUpperCase(Locale.ENGLISH));
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("value")){
					try{
						argVal = Double.parseDouble(item.getTextContent());
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
		}
		if(r.type == null){
			throw new ConfigParserException("Unknown condition type for "+node.getParentNode().getNodeName()+" -> "+node.getNodeName());
		}
		if(r.type.needsAction && act == null){
			throw new ConfigParserException("Need action for condition "+r.type.name()+" in "+node.getParentNode().getNodeName()+" -> "+node.getNodeName());
		}
		if(r.type.needsStatus && stat == null){
			throw new ConfigParserException("Need status for condition "+r.type.name()+" in "+node.getParentNode().getNodeName()+" -> "+node.getNodeName());
		}
		if(r.type.needsValue && argVal == null){
			throw new ConfigParserException("Need value for condition "+r.type.name()+" in "+node.getParentNode().getNodeName()+" -> "+node.getNodeName());
		}

		r.argumentAction = act;
		r.argumentStatus = stat;
		r.argumentValue = argVal != null ? argVal.doubleValue() : 0d;
		return r;
	}

	@Override
	public void appendXML(Document config, Element dpl) {
		Element nCond = config.createElement("Condition");
		Comment comment = config.createComment(type.desc);
		Element nType = config.createElement("Type");
		nType.setTextContent(type.name());
		nCond.appendChild(nType);
		nCond.appendChild(comment);

		if(type.needsAction){
			Element n = config.createElement("Action");
			n.setTextContent(argumentAction.name());
			nCond.appendChild(n);
		}
		if(type.needsStatus){
			Element n = config.createElement("Status");
			n.setTextContent(argumentStatus.name());
			nCond.appendChild(n);
		}
		if(type.needsValue){
			Element n = config.createElement("Value");
			n.setTextContent(String.valueOf(argumentValue));
			nCond.appendChild(n);
		}
		dpl.appendChild(nCond);
	}
}