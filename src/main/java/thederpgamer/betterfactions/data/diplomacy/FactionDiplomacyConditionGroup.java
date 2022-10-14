package thederpgamer.betterfactions.data.diplomacy;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.config.ConfigParserException;
import org.schema.common.util.LogInterface.LogLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Locale;


public class FactionDiplomacyConditionGroup {
	public final List<FactionDiplomacyConditionGroup> conditions = new ObjectArrayList<>();
	public enum ConditionMod{
		AND,
		OR,
		NOT
	}

	public ConditionMod mod = ConditionMod.AND;
	public boolean satisfied(FactionDiplomacyEntity ent){
		if(conditions.isEmpty()){
			ent.log("No conditions in this group -> evaluated true", LogLevel.DEBUG);
			return true;
		}
		switch(mod){
			case AND:
				for(FactionDiplomacyConditionGroup g : conditions){
					if(!g.satisfied(ent)){
						return false;
					}
				}
				return true;
			case NOT:
				if(conditions.size() != 1){
					throw new IllegalArgumentException("NOT confition invalid. must be exactly one member but are: "+conditions.size());
				}
				return !conditions.get(0).satisfied(ent);
			case OR:
				for(FactionDiplomacyConditionGroup g : conditions){
					if(g.satisfied(ent)){
						return true;
					}
				}
				return false;
		}

		throw new IllegalArgumentException("UNKNOWN CONDITION "+mod);
	}
	private static void parse(Node node, FactionDiplomacyConditionGroup r) throws ConfigParserException {
		NodeList childNodes = node.getChildNodes();
		for(int i = 0; i < childNodes.getLength(); i++){
			Node item = childNodes.item(i);
			if (item.getNodeType() == Node.ELEMENT_NODE) {
				if(item.getNodeName().toLowerCase(Locale.ENGLISH).equals("condition")){
					r.conditions.add(FactionDiplomacyCondition.parseCondition(item));
				}
				for(ConditionMod m : ConditionMod.values()){
					if(item.getNodeType() == Node.ELEMENT_NODE && item.getNodeName().toLowerCase(Locale.ENGLISH).equals(m.name().toLowerCase(Locale.ENGLISH))){
						FactionDiplomacyConditionGroup g = new FactionDiplomacyConditionGroup();
						g.mod = m;
						parse(item, g);
						r.conditions.add(g);
					}
				}
			}
		}
	}
	public static boolean canParse(Node node) throws ConfigParserException {
		for(ConditionMod m : ConditionMod.values()){
			if(node.getNodeType() == Node.ELEMENT_NODE &&
					node.getNodeName().toLowerCase(Locale.ENGLISH).equals(m.name().toLowerCase(Locale.ENGLISH))){

				return true;
			}
		}
		return false;
	}
	public static FactionDiplomacyConditionGroup parse(Node node) throws ConfigParserException {
		for(ConditionMod m : ConditionMod.values()){
			if(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().toLowerCase(Locale.ENGLISH).equals(m.name().toLowerCase(Locale.ENGLISH))){
				FactionDiplomacyConditionGroup g = new FactionDiplomacyConditionGroup();
				g.mod = m;
				parse(node, g);
				return g;
			}
		}
		throw new ConfigParserException("INVALID OPERATOR "+node.getNodeName());
	}

	public void check(){
		for(FactionDiplomacyConditionGroup c : conditions){
			if(c == this) throw new IllegalArgumentException("CONDITION SELF REFERENCING;");
			c.check();
		}
	}

	public void appendXML(Document config, Element dpl) {
		Element nCond = config.createElement(mod.toString());
		for(FactionDiplomacyConditionGroup c : conditions){
			if(c == this) throw new IllegalArgumentException("CONDITION SELF REFERENCING: "+dpl.getNodeName()+"; ");
			c.appendXML(config, nCond);
		}
		dpl.appendChild(nCond);
	}
}