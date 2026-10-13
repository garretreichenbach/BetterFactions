package videogoose.betterfactions.diplomacy;

import java.util.Locale;

import org.schema.common.config.ConfigParserException;
import org.schema.common.util.StringTools;
import org.schema.common.util.LogInterface.LogLevel;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DiplomacyReaction {

    public final int index;
    public DiplomacyConditionGroup condition;
    public ConditionReaction reaction;
    public String name;
    public String message;
    /**
     * Treaty type id this reaction acts on. Null means a plain relation change - white peace.
     */
    public String treatyId;
    public DiplomacyReaction(int index) {
        this.index = index;
    }

    public static DiplomacyReaction parse(Node node, int index) throws ConfigParserException {
        DiplomacyReaction r = new DiplomacyReaction(index);
        NodeList childNodes = node.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                if (item.getNodeName().trim().toLowerCase(Locale.ENGLISH).equals("name")) {
                    r.name = item.getTextContent();
                }
                if (item.getNodeName().trim().toLowerCase(Locale.ENGLISH).equals("message")) {
                    //a message wrapped across lines in the xml carries the line break and indentation with it
                    r.message = item.getTextContent().trim().replaceAll("\\s+", " ");
                }
                if (item.getNodeName().trim().toLowerCase(Locale.ENGLISH).equals("treaty")) {
                    String t = item.getTextContent().trim();
                    r.treatyId = t.isEmpty() ? null : t;
                }
                if (item.getNodeName().trim().toLowerCase(Locale.ENGLISH).equals("action")) {
                    String actionText = item.getTextContent().trim().toUpperCase(Locale.ENGLISH);
                    try {
                        r.reaction = ConditionReaction.valueOf(actionText);
                    } catch (IllegalArgumentException e) {
                        //a reaction this build no longer has. Every caller already skips a reaction
                        //it cannot parse, which is the right outcome: this used to turn it into a
                        //popup that told the player about the server's config file instead.
                        throw new ConfigParserException("Unknown reaction type in config: " + actionText);
                    }
                }
                if (DiplomacyConditionGroup.canParse(item)) {
                    r.condition = DiplomacyConditionGroup.parse(item);
                }
            }
        }
        r.check();
        return r;
    }

    public boolean isSatisfied(DiplomacyEntity ent) {
        if (condition == null) {
            ent.log("DiplReaction: " + this + ": Condition true (no condition set)", LogLevel.DEBUG);
        }
        return condition == null || condition.satisfied(ent);
    }

    @Override
    public String toString() {
        return name + " -> " + reaction.name();
    }

    @Override
    public int hashCode() {
        return index;
    }

    @Override
    public boolean equals(Object obj) {
        DiplomacyReaction other = (DiplomacyReaction) obj;
        return index == other.index;
    }

    public void appendXML(Document config, Element pp) {
        Comment comment = config.createComment(
                "Diplomacy Reaction");
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

        Element nTreaty = config.createElement("Treaty");
        nTreaty.setTextContent(treatyId != null ? treatyId : "");

        dpl.appendChild(nElem);
        dpl.appendChild(nMessage);
        dpl.appendChild(nReact);
        dpl.appendChild(nTreaty);
        condition.appendXML(config, dpl);

        pp.appendChild(dpl);


        check();
    }

    public void check() {
        condition.check();
    }

    /**
     * What an NPC faction can do about a condition being met.
     *
     * <p>These are the faction's own initiative, on its diplomacy turn. Answering an offer somebody
     * else made is not here: that is the {@code Acceptance} block in the config, evaluated the
     * moment a proposal arrives, which also gives the GUI something to preview. The six
     * type-specific entries this list used to carry - offer, remove and accept/reject for alliances
     * and for peace separately - collapsed into the three generic ones below, each naming a treaty
     * type in its &lt;Treaty&gt; element.</p>
     */
    public enum ConditionReaction {
        PROPOSE_TREATY("Offer them a treaty. Treaty: the type id to offer, or empty to offer plain peace"),
        WITHDRAW_PROPOSAL("Take back a treaty we offered that has not been answered yet. Treaty: the type id, or empty for a plain peace offer"),
        BREAK_TREATY("Walk away from a treaty already in force. Treaty: the type id"),
        SEND_POPUP_MESSAGE("Show the player a popup carrying Message"),
        SEND_FACTION_MAIL("Send their faction an in-game mail carrying Name as its subject and Message as its body");

        public final String desc;

        ConditionReaction(String desc) {
            this.desc = desc;
        }

        public static String list() {
            return StringTools.listEnum(values());
        }
    }

}
