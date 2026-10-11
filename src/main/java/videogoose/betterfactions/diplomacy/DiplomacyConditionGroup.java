package videogoose.betterfactions.diplomacy;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;
import java.util.Locale;

import org.schema.common.config.ConfigParserException;
import org.schema.common.util.LogInterface.LogLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class DiplomacyConditionGroup {
    public final List<DiplomacyConditionGroup> conditions = new ObjectArrayList<DiplomacyConditionGroup>();
    public ConditionMod mod = ConditionMod.AND;

    private static void parse(Node node, DiplomacyConditionGroup r) throws ConfigParserException {
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                if (item.getNodeName().toLowerCase(Locale.ENGLISH).equals("condition")) {
                    r.conditions.add(DiplomacyCondition.parseCondition(item));
                }
                for (ConditionMod m : ConditionMod.values()) {
                    if (item.getNodeType() == Node.ELEMENT_NODE &&
                            item.getNodeName().toLowerCase(Locale.ENGLISH).equals(m.name().toLowerCase(Locale.ENGLISH))) {
                        DiplomacyConditionGroup g = new DiplomacyConditionGroup();
                        g.mod = m;
                        parse(item, g);
                        r.conditions.add(g);
                    }
                }
            }
        }
    }

    public static boolean canParse(Node node) throws ConfigParserException {
        for (ConditionMod m : ConditionMod.values()) {
            if (node.getNodeType() == Node.ELEMENT_NODE &&
                    node.getNodeName().toLowerCase(Locale.ENGLISH).equals(m.name().toLowerCase(Locale.ENGLISH))) {

                return true;
            }
        }
        return false;
    }

    public static DiplomacyConditionGroup parse(Node node) throws ConfigParserException {
        for (ConditionMod m : ConditionMod.values()) {
            if (node.getNodeType() == Node.ELEMENT_NODE &&
                    node.getNodeName().toLowerCase(Locale.ENGLISH).equals(m.name().toLowerCase(Locale.ENGLISH))) {
                DiplomacyConditionGroup g = new DiplomacyConditionGroup();
                g.mod = m;
                parse(node, g);
                return g;
            }
        }
        throw new ConfigParserException("INVALID OPERATOR " + node.getNodeName());
    }

    public boolean satisfied(DiplomacyEntity ent) {
        return satisfied(ent, null);
    }

    /**
     * @param unmet if non-null, collects a line per leaf condition that failed, so a caller can say
     *              why an offer was turned down rather than only that it was. Nothing on the
     *              evaluation path allocates when this is null.
     */
    public boolean satisfied(DiplomacyEntity ent, List<String> unmet) {
        if (conditions.isEmpty()) {
            ent.log("No conditions in this group -> evaluated true", LogLevel.DEBUG);
            return true;
        }
        switch (mod) {
            case AND:
                boolean all = true;
                for (DiplomacyConditionGroup g : conditions) {
                    if (!g.satisfied(ent, unmet)) {
                        if (unmet == null) {
                            return false;
                        }
                        //keep going: the caller wants every reason, not just the first
                        all = false;
                    }
                }
                return all;
            case NOT:
                if (conditions.size() != 1) {
                    throw new IllegalArgumentException("NOT confition invalid. must be exactly one member but are: " + conditions.size());
                }
                //an inverted group's failures are the reasons it passed, so they are never collected
                return !conditions.get(0).satisfied(ent, null);
            case OR:
                for (DiplomacyConditionGroup g : conditions) {
                    if (g.satisfied(ent, null)) {
                        return true;
                    }
                }
                if (unmet != null) {
                    for (DiplomacyConditionGroup g : conditions) {
                        g.satisfied(ent, unmet);
                    }
                }
                return false;
        }

        throw new IllegalArgumentException("UNKNOWN CONDITION " + mod);
    }

    public void check() {
        for (DiplomacyConditionGroup c : conditions) {
            if (c == this) {
                throw new IllegalArgumentException("CONDITION SELF REFERENCING;");
            }
            c.check();
        }
    }

    public void appendXML(Document config, Element dpl) {
        Element nCond = config.createElement(mod.toString());
        for (DiplomacyConditionGroup c : conditions) {
            if (c == this) {
                throw new IllegalArgumentException("CONDITION SELF REFERENCING: " + dpl.getNodeName() + "; ");
            }
            c.appendXML(config, nCond);
        }
        dpl.appendChild(nCond);
    }

    public enum ConditionMod {
        AND,
        OR,
        NOT
    }
}
