package videogoose.betterfactions.diplomacy;

import org.schema.common.config.ConfigParserException;
import org.schema.common.util.StringTools;
import org.schema.schine.common.language.Lng;
import org.w3c.dom.*;

import java.util.List;
import java.util.Locale;

public class DiplomacyCondition extends DiplomacyConditionGroup {


    public ConditionType type;
    public DiplomacyAction.DiplActionType argumentAction;
    public DiplomacyEntity.DiplStatusType argumentStatus;
    public double argumentValue;
    /**
     * A treaty type id, or a faction kind - whatever {@link ConditionType#needsText} asks for.
     */
    public String argumentText;

    public static DiplomacyCondition parseCondition(Node node) throws ConfigParserException {
        DiplomacyCondition r = new DiplomacyCondition();
        NodeList childNodes = node.getChildNodes();

        DiplomacyAction.DiplActionType act = null;
        DiplomacyEntity.DiplStatusType stat = null;
        Double argVal = null;
        String argText = null;

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node item = childNodes.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                if ("type".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                    try {
                        r.type = ConditionType.valueOf(item.getTextContent().trim().toUpperCase(Locale.ENGLISH));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if ("action".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                    try {
                        act = DiplomacyAction.DiplActionType.valueOf(item.getTextContent().trim().toUpperCase(Locale.ENGLISH));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if ("status".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                    try {
                        stat = DiplomacyEntity.DiplStatusType.valueOf(item.getTextContent().trim().toUpperCase(Locale.ENGLISH));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if ("value".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                    try {
                        argVal = Double.parseDouble(item.getTextContent().trim());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if ("argument".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                    argText = item.getTextContent().trim();
                    if (argText.isEmpty()) {
                        argText = null;
                    }
                }
            }
        }
        if (r.type == null) {
            throw new ConfigParserException("Unknown condition type for " + node.getParentNode().getNodeName() + " -> " + node.getNodeName());
        }
        if (r.type.needsAction && act == null) {
            throw new ConfigParserException("Need action for condition " + r.type.name() + " in " + node.getParentNode().getNodeName() + " -> " + node.getNodeName());
        }
        if (r.type.needsStatus && stat == null) {
            throw new ConfigParserException("Need status for condition " + r.type.name() + " in " + node.getParentNode().getNodeName() + " -> " + node.getNodeName());
        }
        if (r.type.needsValue && argVal == null) {
            throw new ConfigParserException("Need value for condition " + r.type.name() + " in " + node.getParentNode().getNodeName() + " -> " + node.getNodeName());
        }
        if (r.type.needsText && argText == null) {
            throw new ConfigParserException("Need argument for condition " + r.type.name() + " in " + node.getParentNode().getNodeName() + " -> " + node.getNodeName());
        }

        r.argumentAction = act;
        r.argumentStatus = stat;
        r.argumentText = argText;
        r.argumentValue = argVal != null ? argVal.doubleValue() : 0.0d;


        return r;
    }

    @Override
    public boolean satisfied(DiplomacyEntity ent, List<String> unmet) {
        boolean ok = evaluate(ent);
        if (!ok && unmet != null) {
            unmet.add(describe());
        }
        return ok;
    }

    /**
     * What this condition wanted, phrased for the player who was just turned down.
     *
     * <p>It used to print the enum name and the threshold - {@code RAW_POINTS >= 1000} - which tells a
     * server owner reading a log exactly what they need and tells a player nothing they can act on.
     * The refusal is one of the few places the diplomacy model speaks directly to somebody, so it says
     * what would have to change, in the terms the game shows them elsewhere.</p>
     */
    public String describe() {
        switch (type) {
            case RAW_POINTS:
            case TOTAL_POINTS:
                return Lng.str("they do not think well enough of you yet");
            case STATUS_POINTS:
                return argumentStatus != null
                        ? Lng.str("they weigh '%s' against you", argumentStatus.getDescription())
                        : Lng.str("they do not think well enough of you yet");
            case EXISTS_STATUS:
                return argumentStatus != null
                        ? Lng.str("'%s' does not hold between you", argumentStatus.getDescription())
                        : Lng.str("something they look for is missing");
            case STATUS_PERSISTED:
                return argumentStatus != null
                        ? Lng.str("'%s' has not held for long enough", argumentStatus.getDescription())
                        : Lng.str("not enough time has passed");
            case EXISTS_ACTION:
            case ACTION_COUNTER:
                return argumentAction != null
                        ? Lng.str("they are waiting on '%s'", argumentAction.getDescription())
                        : Lng.str("something they look for has not happened");
            case ACTION_PERSISTED:
                if (argumentAction == DiplomacyAction.DiplActionType.ATTACK) {
                    //by far the most common one to hit, and the only one with an obvious remedy
                    return Lng.str("the fighting is too recent - leave them alone for a while");
                }
                return argumentAction != null
                        ? Lng.str("'%s' has not been the case for long enough", argumentAction.getDescription())
                        : Lng.str("not enough time has passed");
            case HAS_TREATY:
                return Lng.str("they want %s in place first", treatyName());
            case TREATY_COUNT:
                return Lng.str("they already hold as many of these as they will");
            case RELATIVE_STRENGTH:
                return argumentValue >= 0
                        ? Lng.str("they do not deal with anyone this weak")
                        : Lng.str("they will not tie themselves to anyone this strong");
            case AT_WAR_COUNT:
                return Lng.str("they have too few wars of their own to want this");
            case SHARES_ENEMY:
                return Lng.str("you have no enemy in common");
            case SHARES_FRIEND:
                return Lng.str("you have no friend in common");
            case IS_FACTION_TYPE:
                return Lng.str("they do not deal with your kind");
            case WEARINESS:
                return Lng.str("they have not lost enough to you to want out");
            case REPUTATION:
                return Lng.str("word is you break your treaties");
            case AGGRESSION:
                return Lng.str("you have not shown enough teeth for them");
            case HOLDS_CIVIC:
                return Lng.str("they look for %s", videogoose.betterfactions.civics.CivicRegistry.nameOf(argumentText));
            default:
                return Lng.str("they are not satisfied");
        }
    }

    /**
     * The display name of the treaty type this condition names, falling back to the raw id for a
     * modded type whose mod is gone.
     */
    private String treatyName() {
        TreatyType t = argumentText != null ? TreatyTypeRegistry.get(argumentText) : null;
        return t != null ? t.getDisplayName() : String.valueOf(argumentText);
    }

    /**
     * The same condition as a server owner needs to read it, enum names and thresholds included.
     */
    public String describeForConfig() {
        StringBuilder b = new StringBuilder(type.name());
        if (type.needsAction && argumentAction != null) {
            b.append(' ').append(argumentAction.name());
        }
        if (type.needsStatus && argumentStatus != null) {
            b.append(' ').append(argumentStatus.name());
        }
        if (type.needsText && argumentText != null) {
            b.append(' ').append(argumentText);
        }
        if (type.needsValue) {
            b.append(" >= ").append(argumentValue);
        }
        return b.toString();
    }

    private boolean evaluate(DiplomacyEntity ent) {
        switch (type) {
            case HAS_TREATY:
                return ent.hasTreatyWithThem(argumentText);
            case TREATY_COUNT:
                return ent.countOurTreatiesOfType(argumentText) >= argumentValue;
            case RELATIVE_STRENGTH:
                return ent.getRelativeStrength() >= argumentValue;
            case AT_WAR_COUNT:
                return ent.getOurWarCount() >= argumentValue;
            case SHARES_ENEMY:
                return ent.calculateStatus(DiplomacyEntity.DiplStatusType.IN_WAR_WITH_ENEMY) >= argumentValue;
            case SHARES_FRIEND:
                return ent.calculateStatus(DiplomacyEntity.DiplStatusType.ALLIANCE_WITH_FRIENDS) >= argumentValue;
            case IS_FACTION_TYPE:
                return ent.isOfKind(argumentText);
            case WEARINESS:
                return ent.getWeariness() >= argumentValue;
            case REPUTATION:
                return -ent.getInfamy() >= argumentValue;
            case AGGRESSION:
                return ent.getAggression() >= argumentValue;
            case HOLDS_CIVIC:
                return ent.holdsCivic(argumentText);
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


        throw new IllegalArgumentException("Unknown condition type: " + type);
    }

    @Override
    public void appendXML(Document config, Element dpl) {
        Element nCond = config.createElement("Condition");

        Comment comment = config.createComment(type.desc);

        Element nType = config.createElement("Type");
        nType.setTextContent(type.name());
        nCond.appendChild(nType);
        nCond.appendChild(comment);


        if (type.needsAction) {
            Element n = config.createElement("Action");
            n.setTextContent(argumentAction.name());
            nCond.appendChild(n);
        }
        if (type.needsStatus) {
            Element n = config.createElement("Status");
            n.setTextContent(argumentStatus.name());
            nCond.appendChild(n);
        }
        if (type.needsValue) {
            Element n = config.createElement("Value");
            n.setTextContent(String.valueOf(argumentValue));
            nCond.appendChild(n);
        }
        if (type.needsText) {
            Element n = config.createElement("Argument");
            n.setTextContent(argumentText != null ? argumentText : "");
            nCond.appendChild(n);
        }


        dpl.appendChild(nCond);
    }

    public enum ConditionType {
        EXISTS_ACTION("does the action currently exist", true, false, false),
        EXISTS_STATUS("does the status currently exist", false, true, false),
        STATUS_POINTS("Value the status is affecting its entity with is equal or bigger than X", false, true, true),
        STATUS_PERSISTED("Min time the status has been applied for equal or bigger than X", false, true, true),
        ACTION_PERSISTED("Min time the action has been applied for equal or bigger than X. the action will persist to exist as long as it is repeated it timeouts", true, false, true),
        ACTION_COUNTER("Amount of actions triggered (within timeout) equal or bigger than", true, false, true),
        TOTAL_POINTS("Total diplomacy points equal or bigger than X", false, false, true),
        RAW_POINTS("Raw diplomacy points (without sttaus effects) equal or bigger than X", false, false, true),
        HAS_TREATY("They hold this treaty with us. Argument: a treaty type id, e.g. starmade:alliance", false, false, false, true),
        TREATY_COUNT("How many treaties of this type we already hold with anyone, equal or bigger than X. Argument: a treaty type id", false, false, true, true),
        RELATIVE_STRENGTH("How much stronger they are than us, counted in doublings: 0 is an equal, 1 is twice our strength, -1 is half. Equal or bigger than X", false, false, true),
        AT_WAR_COUNT("How many factions we are at war with, equal or bigger than X", false, false, true),
        SHARES_ENEMY("How many of our enemies they are also at war with, equal or bigger than X", false, false, true),
        SHARES_FRIEND("How many of our friends they are also allied with, equal or bigger than X", false, false, true),
        IS_FACTION_TYPE("They are of this kind. Argument: PLAYER, NPC or PIRATE", false, false, false, true),
        WEARINESS("How badly our recent battles against them have gone: 0 is no recent losses, 1 is the worst losing streak there is. Equal or bigger than X", false, false, true),
        REPUTATION("Their galaxy-wide record of keeping treaties, with anyone: 0 is clean, -1 is one recently broken treaty, fading over time. Equal or bigger than X", false, false, true),
        AGGRESSION("How many fights they have started lately, with anyone: 1 is one declared war or four unprovoked attacks, fading over time. Equal or bigger than X", false, false, true),
        HOLDS_CIVIC("They hold this civic, institution or fleet doctrine. Argument: its id, e.g. betterfactions:religious", false, false, false, true),
        ;

        public final String desc;
        public final boolean needsAction;
        public final boolean needsStatus;
        public final boolean needsValue;
        /**
         * Whether the condition takes a free-text &lt;Argument&gt;: a treaty id, a faction kind.
         */
        public final boolean needsText;
        ConditionType(String desc, boolean needsAction, boolean needsStatus, boolean needsValue) {
            this(desc, needsAction, needsStatus, needsValue, false);
        }

        ConditionType(String desc, boolean needsAction, boolean needsStatus, boolean needsValue, boolean needsText) {
            this.desc = desc;
            this.needsAction = needsAction;
            this.needsStatus = needsStatus;
            this.needsValue = needsValue;
            this.needsText = needsText;
        }

        public static String list() {
            return StringTools.listEnum(values());
        }
    }
}
