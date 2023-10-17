package thederpgamer.betterfactions.data.diplomacy;

import org.schema.common.config.ConfigParserException;
import org.schema.common.util.LogInterface.LogLevel;
import org.schema.common.util.StringTools;
import org.schema.schine.resource.tag.Tag;
import org.w3c.dom.*;

import java.util.Locale;

public class FactionDiplomacyReaction {

	public void fromTag(Tag t) {
	}

	public enum ConditionReaction {
		DECLARE_WAR(""),
		OFFER_PEACE_DEAL(""),
		REMOVE_PEACE_DEAL_OFFER(""),
		OFFER_ALLIANCE(""),
		REMOVE_ALLIANCE_OFFER(""),
		ACCEPT_ALLIANCE_OFFER(""),
		REMOVE_ALLIANCE(""),
		ACCEPT_PEACE_OFFER(""),
		REJECT_ALLIANCE_OFFER(""),
		REJECT_PEACE_OFFER(""),
		SEND_POPUP_MESSAGE(""),
		OFFER_NON_AGGRESSION_PACT(""),
		REJECT_NON_AGGRESSION_PACT(""),
		ACCEPT_NON_AGGRESSION_PACT(""),
		REMOVE_NON_AGGRESSION_PACT(""),
		IMPROVE_RELATIONS(""),
		DECREASE_RELATIONS("");

		public final String desc;

		ConditionReaction(String desc){
			this.desc = desc;
		}
		public static String list() {
			return StringTools.listEnum(ConditionReaction.values());
		}
	}
	public final int index;
	public FactionDiplomacyReaction(int index) {
		this.index = index;
	}
	public FactionDiplomacyConditionGroup condition;
	public ConditionReaction reaction;
	public String name;
	public String message;
	public boolean isSatisfied(FactionDiplomacyEntity ent){
		if(condition == null) ent.log("DiplReaction: "+ this +": Condition true (no condition set)", LogLevel.DEBUG);
		return condition == null || condition.satisfied(ent);
	}

	@Override
	public String toString() {
		return name+" -> "+reaction.name();
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public boolean equals(Object obj) {
		FactionDiplomacyReaction other = (FactionDiplomacyReaction) obj;
		return index == other.index;
	}

	public static FactionDiplomacyReaction parse(Node node, int index) throws ConfigParserException {
		FactionDiplomacyReaction r = new FactionDiplomacyReaction(index);
		NodeList childNodes = node.getChildNodes();
		for(int i = 0; i < childNodes.getLength(); i++){
			Node item = childNodes.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("name")) r.name = item.getTextContent();
				if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("message")) r.message = item.getTextContent();
				if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("action")) {
					try {
						r.reaction = ConditionReaction.valueOf(item.getTextContent().toUpperCase(Locale.ENGLISH));
					} catch(Exception e) {
						e.printStackTrace();
						r.message = "UNKNOWN REACTION TYPE IN CONFIG: "+item.getTextContent().toUpperCase(Locale.ENGLISH);
						r.reaction = ConditionReaction.SEND_POPUP_MESSAGE;
					}
				}
				if(FactionDiplomacyConditionGroup.canParse(item)){
					r.condition = FactionDiplomacyConditionGroup.parse(item);
				}
			}
		}
		r.check();
		return r;
	}

	public void appendXML(Document config, Element pp) {
		Comment comment = config.createComment("Diplomacy Reaction");
		pp.appendChild(comment);

		Element dpl = config.createElement("Reaction");

		Element nElem = config.createElement("Name");
		nElem.setTextContent(name);

		Element nMessage = config.createElement("Message");
		nMessage.setTextContent(message != null ? message : "");

		Element nReact = config.createElement("Action");
		nReact.setTextContent(reaction.name());
		Comment cc = config.createComment(reaction.desc);
		nReact.appendChild(cc);

		dpl.appendChild(nElem);
		dpl.appendChild(nMessage);
		dpl.appendChild(nReact);
		condition.appendXML(config, dpl);
		pp.appendChild(dpl);
		check();
	}

	public void check(){
		condition.check();
	}
}