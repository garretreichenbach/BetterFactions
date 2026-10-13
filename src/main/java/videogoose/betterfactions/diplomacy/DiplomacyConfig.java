package videogoose.betterfactions.diplomacy;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.config.ConfigParserException;
import org.schema.schine.common.language.Lng;
import org.w3c.dom.*;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DiplomacyConfig {
    public static final byte VERSION = 4;
    /**
     * Key an acceptance rule with no treaty type is filed under - a plain peace offer.
     */
    private static final String PEACE = "";
    public final List<DiplomacyReaction> reactions = new ObjectArrayList<DiplomacyReaction>();
    /**
     * What this archetype is prepared to agree to, one rule per treaty type id. A type with no rule
     * here is refused, so an archetype opts in to each kind of agreement rather than out of it.
     */
    public final Object2ObjectOpenHashMap<String, DiplomacyConditionGroup> acceptance = new Object2ObjectOpenHashMap<String, DiplomacyConditionGroup>();
    /**
     * Keyed like {@link #acceptance}: what this faction says when it refuses that kind of offer.
     * Kept beside the rules rather than inside them because a rule is a tree and the message belongs
     * to the whole of it, not to any one branch.
     */
    public final Object2ObjectOpenHashMap<String, String> refusals = new Object2ObjectOpenHashMap<String, String>();
    public final Object2LongOpenHashMap<DiplomacyAction.DiplActionType> actionTimeoutMap = new Object2LongOpenHashMap<DiplomacyAction.DiplActionType>();
    private final Int2ObjectOpenHashMap<DiplomacyConfigElement> map = new Int2ObjectOpenHashMap<DiplomacyConfigElement>();
    public byte version;
    /**
     * How the archetype weighs what makes a faction formidable. Kept per archetype rather than
     * global because "strong" is a judgement: a trading power counts systems, a raider counts hulls.
     */
    public double powerSurplusWeight = 2;
    public double powerSystemWeight = 100;
    public double powerMemberWeight = 25;
    /** Per point of the square root of combat rating; see {@link FactionStrength#militaryTerm}. */
    public double powerMilitaryWeight = 1;
    /** What peace between equals is worth, in faction points; see {@link SettlementPricing}. */
    public double settlementPeaceValue = 500;
    /** What one treaty given or received in a settlement is worth, in faction points. */
    public double settlementTreatyValue = 1000;

    /**
     * Casus belli: what has to be true of a faction before war may be declared on it with a goal of
     * this kind. Read from the declarer's own ledger of the target, so the conditions speak of what
     * the target did to the declarer. A kind with no rule here cannot be claimed as a war goal; a war
     * with no goals needs none, and can only end in a peace the two sides negotiate.
     */
    public final java.util.EnumMap<SettlementTerm.Kind, DiplomacyConditionGroup> casusBelli = new java.util.EnumMap<>(SettlementTerm.Kind.class);
    /**
     * Keyed like {@link #casusBelli}: what the declarer is told when the claim does not hold.
     */
    public final java.util.EnumMap<SettlementTerm.Kind, String> casusBelliRefusals = new java.util.EnumMap<>(SettlementTerm.Kind.class);

    public DiplomacyConfig() {
        addDefaultAcceptance();
        addDefaultCasusBelli();


        for (DiplomacyAction.DiplActionType t : DiplomacyAction.DiplActionType.values()) {
            actionTimeoutMap.put(t, 60000 * 5);
        }
        //an NPC will not re-offer a treaty while the refusal is live, so this is how long "no" lasts
        actionTimeoutMap.put(DiplomacyAction.DiplActionType.TREATY_REFUSED, 60000 * 60);
        for (DiplomacyAction.DiplActionType t : DiplomacyAction.DiplActionType.values()) {
            DiplomacyConfigElement c = new DiplomacyConfigElement();
            c.actionType = t;
            switch (t) {
                case ALLIANCE_WITH_ENEMY:
                    c.upperLimit = -10;
                    c.lowerLimit = 0;
                    c.value = -10;
                    break;
                case ALLIANCE_CANCEL:
                    c.upperLimit = -10;
                    c.lowerLimit = 0;
                    c.value = -10;
                    break;
                case ALLIANCE_REQUEST:
                    //whether the offer is taken up is an acceptance rule now, not a reaction
                    c.upperLimit = 0;
                    c.lowerLimit = 0;
                    break;
                case ATTACK:
                    c.upperLimit = -500;
                    c.lowerLimit = 0;
                    c.value = -50;
                    break;
                case ATTACK_ENEMY:
                    c.upperLimit = 500;
                    c.lowerLimit = 0;
                    c.value = 50;
                    break;
                case DECLARATION_OF_WAR:
                    c.upperLimit = -1000;
                    c.lowerLimit = 0;
                    c.value = -1000;
                    break;
                case MINING:
                    c.upperLimit = -30;
                    c.lowerLimit = 0;
                    c.value = -30;
                    break;
                case PEACE_OFFER:
                    c.upperLimit = 0;
                    c.lowerLimit = 0;
                    break;
                case TERRITORY:
                    c.upperLimit = -300;
                    c.lowerLimit = 0;
                    c.value = -1;
                    break;
                case TRADING_WITH_US:
                    c.upperLimit = 300;
                    c.lowerLimit = 0;
                    c.value = 1;
                    break;
                case TRADING_WITH_ENEMY:
                    c.upperLimit = 0;
                    c.lowerLimit = -200;
                    c.value = -1;
                    break;
                case TREATY_PROPOSED:
                case TREATY_SIGNED:
                case TREATY_REFUSED:
                case TREATY_EXPIRED:
                    //recorded so acceptance rules can ask about them; the tuned point values for the
                    //three relations that had their own action type stay on those
                    c.upperLimit = 0;
                    c.lowerLimit = 0;
                    c.value = 0;
                    break;
                case TREATY_BROKEN:
                    //the exception: going back on an agreement is the one treaty event that should
                    //cost something no matter which treaty it was
                    c.upperLimit = -300;
                    c.lowerLimit = 0;
                    c.value = -300;
                    break;
                default:
                    break;

            }
            put(t, c);
        }

        for (DiplomacyEntity.DiplStatusType t : DiplomacyEntity.DiplStatusType.values()) {
            DiplomacyConfigElement c = new DiplomacyConfigElement();
            c.statusType = t;
            switch (t) {
                case ALLIANCE:
                    c.value = 1000;
                    break;
                case ALLIANCE_WITH_ENEMY:
                    c.value = -500;
                    break;

                case IN_WAR:
                    c.value = -10000;
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
                    c.value = -10;
                    break;
                case FACTION_MEMBER_AT_WAR_WITH_US:
                    c.value = -500;
                    break;
                case FACTION_MEMBER_WE_DONT_LIKE:
                    c.value = -50;
                    break;
                case TRUST:
                    c.value = 1;
                    break;
                case INFAMY:
                    //per broken treaty, fading; the wronged party's own TREATY_BROKEN hit comes on top
                    c.value = -150;
                    break;
                case AGGRESSION:
                    //per war declared, a quarter of it per unprovoked attack
                    c.value = -100;
                    break;
                default:
                    c.value = -123;
                    break;

            }
            put(t, c);
        }
    }

    private static DiplomacyConditionGroup and(DiplomacyConditionGroup... parts) {
        DiplomacyConditionGroup g = new DiplomacyConditionGroup();
        g.mod = DiplomacyConditionGroup.ConditionMod.AND;
        Collections.addAll(g.conditions, parts);
        return g;
    }

    private static DiplomacyCondition rawPoints(double min) {
        DiplomacyCondition c = new DiplomacyCondition();
        c.type = DiplomacyCondition.ConditionType.RAW_POINTS;
        c.argumentValue = min;
        return c;
    }

    /**
     * How long they have to have gone without attacking us.
     */
    private static DiplomacyCondition calm(double ms) {
        DiplomacyCondition c = new DiplomacyCondition();
        c.type = DiplomacyCondition.ConditionType.STATUS_PERSISTED;
        c.argumentStatus = DiplomacyEntity.DiplStatusType.NON_AGGRESSION;
        c.argumentValue = ms;
        return c;
    }

    private static DiplomacyCondition sharesEnemy(double count) {
        DiplomacyCondition c = new DiplomacyCondition();
        c.type = DiplomacyCondition.ConditionType.SHARES_ENEMY;
        c.argumentValue = count;
        return c;
    }

    private static DiplomacyAction.DiplActionType actionByName(String name) {
        try {
            return DiplomacyAction.DiplActionType.valueOf(name.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static DiplomacyEntity.DiplStatusType statusByName(String name) {
        try {
            return DiplomacyEntity.DiplStatusType.valueOf(name.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public DiplomacyConditionGroup getAcceptance(String treatyId) {
        return acceptance.get(treatyId == null ? PEACE : treatyId);
    }

    /**
     * What this faction says when it turns down an offer of this kind, or null to let the engine
     * assemble a refusal out of the conditions that were not met.
     *
     * <p>Worth having because a condition list explains the rule and an archetype's own line explains
     * the faction. "They do not think well enough of you yet" is accurate for all three shipped
     * presets and sounds like none of them.</p>
     */
    public String getRefusal(String treatyId) {
        return refusals.get(treatyId == null ? PEACE : treatyId);
    }

    public void putAcceptance(String treatyId, DiplomacyConditionGroup rule) {
        acceptance.put(treatyId == null ? PEACE : treatyId, rule);
    }

    public void putRefusal(String treatyId, String message) {
        if (message != null && !message.trim().isEmpty()) {
            refusals.put(treatyId == null ? PEACE : treatyId, message.trim());
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
        pp.appendChild(config.createComment("Status Types: " + DiplomacyEntity.DiplStatusType.list()));
        pp.appendChild(config.createComment("Action Types: " + DiplomacyAction.DiplActionType.list()));
        pp.appendChild(config.createComment("Reaction Types: " + DiplomacyReaction.ConditionReaction.list()));
        pp.appendChild(config.createComment("Condition Types: " + DiplomacyCondition.ConditionType.list()));


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
        for (DiplomacyAction.DiplActionType b : DiplomacyAction.DiplActionType.values()) {
            Element cc = config.createElement(b.name().charAt(0) + b.name().substring(1).toLowerCase(Locale.ENGLISH));
            cc.setTextContent(String.valueOf(actionTimeoutMap.get(b)));
            actTO.appendChild(cc);
        }
        pp.appendChild(actTO);

        Element dpl = config.createElement("DiplomacyElement");

        boolean has = false;
        for (DiplomacyAction.DiplActionType b : DiplomacyAction.DiplActionType.values()) {
            DiplomacyConfigElement elem = get(b);
            if (elem != null) {
                elem.appendXML(config, dpl, b);
                has = true;
            }
        }
        for (DiplomacyEntity.DiplStatusType b : DiplomacyEntity.DiplStatusType.values()) {
            DiplomacyConfigElement elem = get(b);
            if (elem != null) {
                elem.appendXML(config, dpl, b);
                has = true;
            }
        }

        if (has) {
            pp.appendChild(dpl);
        }
        Element power = config.createElement("PowerWeights");
        power.appendChild(config.createComment(
                "What this faction counts when sizing another up. Used by the POWER status and the "
                        + "RELATIVE_STRENGTH condition, which compare the two totals as a ratio - so only the "
                        + "weights relative to each other matter, and scaling all four changes nothing. "
                        + "PointSurplus is faction points spare per period, not the balance."));
        Element pw = config.createElement("PointSurplus");
        pw.setTextContent(String.valueOf(powerSurplusWeight));
        Element sw = config.createElement("ClaimedSystems");
        sw.setTextContent(String.valueOf(powerSystemWeight));
        Element mw = config.createElement("Members");
        mw.setTextContent(String.valueOf(powerMemberWeight));
        Element milw = config.createElement("Military");
        milw.setTextContent(String.valueOf(powerMilitaryWeight));
        power.appendChild(milw);
        power.appendChild(pw);
        power.appendChild(sw);
        power.appendChild(mw);
        pp.appendChild(power);

        if (reactions.size() == 0) {
            addDefaultActions();
        }
        if (reactions.size() > 0) {
            Element re = config.createElement("Reactions");
            for (DiplomacyReaction r : reactions) {
                r.appendXML(config, re);
            }
            pp.appendChild(re);
        }

        if (acceptance.isEmpty()) {
            addDefaultAcceptance();
        }
        Element acc = config.createElement("Acceptance");
        acc.appendChild(config.createComment(
                "Whether this faction agrees when somebody offers it a treaty, one rule per type. "
                        + "A type with no rule here is always refused. Treaty types: " + TreatyTypeRegistry.list()
                        + ". An empty Treaty means a plain peace offer. Refusal is what this faction "
                        + "says when it turns such an offer down; leave it empty and the engine explains "
                        + "which conditions were not met instead."));
        for (Map.Entry<String, DiplomacyConditionGroup> e : acceptance.object2ObjectEntrySet()) {
            Element rule = config.createElement("Accept");
            Element t = config.createElement("Treaty");
            t.setTextContent(e.getKey());
            rule.appendChild(t);
            Element refusal = config.createElement("Refusal");
            refusal.setTextContent(refusals.containsKey(e.getKey()) ? refusals.get(e.getKey()) : "");
            rule.appendChild(refusal);
            e.getValue().appendXML(config, rule);
            acc.appendChild(rule);
        }
        pp.appendChild(acc);
    }

    /**
     * @param keepExistingReactions true only when regenerating a default config file, where the
     *                              in-memory reaction list is the source rather than the document.
     */
    public void parse(Node configElementNode, boolean keepExistingReactions) throws ConfigParserException {
        if (!keepExistingReactions) {
            reactions.clear();
        }
        NodeList childs = configElementNode.getChildNodes();

        for (int i = 0; i < childs.getLength(); i++) {
            Node node = childs.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE &&
                    "version".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                version = Byte.parseByte(node.getTextContent().trim());
            }
            if (node.getNodeType() == Node.ELEMENT_NODE &&
                    "actiontimeouts".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                NodeList diplChilds = node.getChildNodes();


                for (int c = 0; c < diplChilds.getLength(); c++) {
                    Node item = diplChilds.item(c);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        try {
                            DiplomacyAction.DiplActionType d = DiplomacyAction.DiplActionType.valueOf(item.getNodeName().toUpperCase(Locale.ENGLISH));
                            long ms = Long.parseLong(item.getTextContent().trim());
                            actionTimeoutMap.put(d, ms);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (node.getNodeType() == Node.ELEMENT_NODE &&
                    "diplomacyelement".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                NodeList diplChilds = node.getChildNodes();


                for (int c = 0; c < diplChilds.getLength(); c++) {
                    Node item = diplChilds.item(c);
                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        //the second test used to read `node`, the enclosing DiplomacyElement, so it was
                        //never "diplstatus" and every DiplStatus entry in every config was silently
                        //skipped - status point values have never actually been configurable
                        String tag = item.getNodeName().toLowerCase(Locale.ENGLISH);
                        if ("diplaction".equals(tag) || "diplstatus".equals(tag)) {
                            DiplomacyConfigElement e = new DiplomacyConfigElement();
                            e.parse(item);
                        }

                    }
                }
            }
            if (node.getNodeType() == Node.ELEMENT_NODE && "reactions".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                parseReactions(node);
            }
            if (node.getNodeType() == Node.ELEMENT_NODE && "powerweights".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                parsePowerWeights(node);
            }
            if (node.getNodeType() == Node.ELEMENT_NODE && "acceptance".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                parseAcceptance(node);
            }
            if (node.getNodeType() == Node.ELEMENT_NODE && "casusbelli".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                parseCasusBelli(node);
            }
            if (node.getNodeType() == Node.ELEMENT_NODE && "settlement".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                parseSettlement(node);
            }
        }
    }

    /** {@code <Settlement><PeaceValue>500</PeaceValue><TreatyValue>1000</TreatyValue></Settlement>}. */
    private void parseSettlement(Node node) {
        NodeList childs = node.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node item = childs.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String n = item.getNodeName().toLowerCase(Locale.ENGLISH);
            try {
                double v = Double.parseDouble(item.getTextContent().trim());
                if ("peacevalue".equals(n)) {
                    settlementPeaceValue = v;
                } else if ("treatyvalue".equals(n)) {
                    settlementTreatyValue = v;
                }
            } catch (NumberFormatException e) {
                System.err.println("[DIPLOMACY][CONFIG] bad settlement value for " + n + ": " + item.getTextContent());
            }
        }
    }

    private void parsePowerWeights(Node node) {
        NodeList childs = node.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node item = childs.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String n = item.getNodeName().toLowerCase(Locale.ENGLISH);
            try {
                double v = Double.parseDouble(item.getTextContent().trim());
                if ("pointsurplus".equals(n)) {
                    powerSurplusWeight = v;
                } else if ("claimedsystems".equals(n)) {
                    powerSystemWeight = v;
                } else if ("members".equals(n)) {
                    powerMemberWeight = v;
                } else if ("military".equals(n)) {
                    powerMilitaryWeight = v;
                }
            } catch (NumberFormatException e) {
                System.err.println("[DIPLOMACY][CONFIG] bad power weight for " + n + ": " + item.getTextContent());
            }
        }
    }

    /**
     * {@code <CasusBelli><Goal><Kind>TRANSFER_FACTION_POINTS</Kind><Refusal>..</Refusal><Or>..</Or></Goal></CasusBelli>}.
     * A section that is present replaces the defaults entirely, so a server can also take a kind away.
     */
    public void parseCasusBelli(Node node) {
        casusBelli.clear();
        casusBelliRefusals.clear();
        NodeList childs = node.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node item = childs.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE || !"goal".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                continue;
            }
            SettlementTerm.Kind kind = null;
            String refusal = null;
            DiplomacyConditionGroup rule = null;
            NodeList parts = item.getChildNodes();
            for (int c = 0; c < parts.getLength(); c++) {
                Node part = parts.item(c);
                if (part.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                String n = part.getNodeName().toLowerCase(Locale.ENGLISH);
                try {
                    if ("kind".equals(n)) {
                        kind = DiplomacyTags.byName(SettlementTerm.Kind.class, part.getTextContent().trim().toUpperCase(Locale.ENGLISH));
                    } else if ("refusal".equals(n)) {
                        refusal = part.getTextContent().trim();
                    } else if (DiplomacyConditionGroup.canParse(part)) {
                        rule = DiplomacyConditionGroup.parse(part);
                    }
                } catch (ConfigParserException e) {
                    e.printStackTrace();
                }
            }
            if (kind == null || rule == null) {
                System.err.println("[DIPLOMACY][CONFIG] skipping a casus belli with no known kind or no rule");
                continue;
            }
            casusBelli.put(kind, rule);
            if (refusal != null && !refusal.isEmpty()) {
                casusBelliRefusals.put(kind, refusal);
            }
        }
    }

    private static DiplomacyConditionGroup or(DiplomacyConditionGroup... parts) {
        DiplomacyConditionGroup g = and(parts);
        g.mod = DiplomacyConditionGroup.ConditionMod.OR;
        return g;
    }

    private static DiplomacyConditionGroup not(DiplomacyConditionGroup part) {
        DiplomacyConditionGroup g = and(part);
        g.mod = DiplomacyConditionGroup.ConditionMod.NOT;
        return g;
    }

    private static DiplomacyCondition did(DiplomacyAction.DiplActionType action) {
        DiplomacyCondition c = new DiplomacyCondition();
        c.type = DiplomacyCondition.ConditionType.EXISTS_ACTION;
        c.argumentAction = action;
        return c;
    }

    /**
     * A grievance: they attacked us, broke a treaty with us, declared war on us, or we simply loathe
     * them. Reparations and imposed treaties need one; dismantling their alliance also accepts their
     * being allied with our enemies.
     */
    public void addDefaultCasusBelli() {
        casusBelli.clear();
        casusBelliRefusals.clear();
        casusBelli.put(SettlementTerm.Kind.TRANSFER_FACTION_POINTS, grievance());
        casusBelli.put(SettlementTerm.Kind.SIGN_TREATY, grievance());
        casusBelli.put(SettlementTerm.Kind.BREAK_TREATY, or(grievance(), did(DiplomacyAction.DiplActionType.ALLIANCE_WITH_ENEMY)));
        casusBelliRefusals.put(SettlementTerm.Kind.TRANSFER_FACTION_POINTS, Lng.str("They have done nothing to you that would justify reparations."));
        casusBelliRefusals.put(SettlementTerm.Kind.SIGN_TREATY, Lng.str("They have done nothing to you that would justify imposing a treaty."));
        casusBelliRefusals.put(SettlementTerm.Kind.BREAK_TREATY, Lng.str("Neither a grievance nor an alliance with your enemies justifies breaking up their treaties."));
    }

    private static DiplomacyConditionGroup grievance() {
        return or(
                did(DiplomacyAction.DiplActionType.ATTACK),
                did(DiplomacyAction.DiplActionType.TREATY_BROKEN),
                did(DiplomacyAction.DiplActionType.DECLARATION_OF_WAR),
                not(rawPoints(-500)));
    }

    private void parseAcceptance(Node node) {
        acceptance.clear();
        refusals.clear();
        NodeList childs = node.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node item = childs.item(i);
            if (item.getNodeType() != Node.ELEMENT_NODE || !"accept".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                continue;
            }
            String treaty = null;
            String refusal = null;
            DiplomacyConditionGroup rule = null;
            NodeList parts = item.getChildNodes();
            for (int c = 0; c < parts.getLength(); c++) {
                Node part = parts.item(c);
                if (part.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                try {
                    if ("treaty".equals(part.getNodeName().toLowerCase(Locale.ENGLISH))) {
                        treaty = part.getTextContent().trim();
                    } else if ("refusal".equals(part.getNodeName().toLowerCase(Locale.ENGLISH))) {
                        refusal = part.getTextContent().trim();
                    } else if (DiplomacyConditionGroup.canParse(part)) {
                        rule = DiplomacyConditionGroup.parse(part);
                    }
                } catch (ConfigParserException e) {
                    e.printStackTrace();
                    System.err.println("[DIPLOMACY][CONFIG] NOT USING ACCEPTANCE RULE BECAUSE OF EXCEPTION");
                }
            }
            if (rule == null) {
                continue;
            }
            if (treaty != null && !treaty.isEmpty() && !TreatyTypeRegistry.exists(treaty)) {
                //a modded treaty whose mod is gone, or a typo; either way nothing can offer it
                System.err.println("[DIPLOMACY][CONFIG] acceptance rule for unregistered treaty type: " + treaty);
                continue;
            }
            String key = treaty == null || treaty.isEmpty() ? null : treaty;
            putAcceptance(key, rule);
            putRefusal(key, refusal);
        }
    }

    private void parseReactions(Node rt) {

        NodeList childs = rt.getChildNodes();
        int dIndex = 0;
        for (int i = 0; i < childs.getLength(); i++) {
            Node item = childs.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                try {

                    if ("reaction".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                        reactions.add(DiplomacyReaction.parse(item, dIndex));
                        dIndex++;
                    }
                } catch (ConfigParserException e) {
                    e.printStackTrace();
                    System.err.println("NOT USING REACTION BECAUSE OF EXCEPTION");
                }
            }
        }
    }

    public void addDefaultActions() {
        reactions.clear();
        int i = 0;
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.reaction = DiplomacyReaction.ConditionReaction.PROPOSE_TREATY;
            r.treatyId = TreatyTypeRegistry.WAR;
            r.name = "Declare War on bad score";
            r.message = "Relations with your faction have broken down beyond repair.";
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.NOT;
            {
                DiplomacyCondition m = new DiplomacyCondition();
                m.type = DiplomacyCondition.ConditionType.TOTAL_POINTS;
                m.argumentValue = -1000;
                r.condition.conditions.add(m);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.reaction = DiplomacyReaction.ConditionReaction.PROPOSE_TREATY;
            r.name = "Offer Peace Deal";
            r.message = "This conflict is costing us both. We propose an end to it.";
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.TOTAL_POINTS;
                d.argumentValue = -500;
                r.condition.conditions.add(d);
            }
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.STATUS_PERSISTED;
                d.argumentStatus = DiplomacyEntity.DiplStatusType.NON_AGGRESSION;
                d.argumentValue = 15 * 60 * 1000;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.name = "Offer Peace Deal on non agression only";
            r.message = "There has been no trouble between us for some time. Let us make that official.";
            r.reaction = DiplomacyReaction.ConditionReaction.PROPOSE_TREATY;
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.STATUS_PERSISTED;
                d.argumentStatus = DiplomacyEntity.DiplStatusType.NON_AGGRESSION;
                d.argumentValue = 120 * 60 * 1000;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.name = "Offer Non-Aggression Pact";
            r.message = "We propose a non-aggression pact. Neither of us gains from the alternative.";
            r.reaction = DiplomacyReaction.ConditionReaction.PROPOSE_TREATY;
            r.treatyId = TreatyTypeRegistry.NON_AGGRESSION;
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.TOTAL_POINTS;
                d.argumentValue = 200;
                r.condition.conditions.add(d);
            }
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.STATUS_PERSISTED;
                d.argumentStatus = DiplomacyEntity.DiplStatusType.NON_AGGRESSION;
                d.argumentValue = 30 * 60 * 1000;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.name = "Offer Alliance";
            r.message = "You have dealt with us honestly. We propose a formal alliance.";
            r.reaction = DiplomacyReaction.ConditionReaction.PROPOSE_TREATY;
            r.treatyId = TreatyTypeRegistry.ALLIANCE;
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.TOTAL_POINTS;
                d.argumentValue = 1000;
                r.condition.conditions.add(d);
            }
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.STATUS_PERSISTED;
                d.argumentStatus = DiplomacyEntity.DiplStatusType.NON_AGGRESSION;
                d.argumentValue = 120 * 60 * 1000;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.reaction = DiplomacyReaction.ConditionReaction.WITHDRAW_PROPOSAL;
            r.treatyId = TreatyTypeRegistry.ALLIANCE;
            r.name = "Remove Alliance Offer";
            r.message = "Our offer of alliance is withdrawn.";
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.ACTION_COUNTER;
                d.argumentAction = DiplomacyAction.DiplActionType.ATTACK;
                d.argumentValue = 1;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.name = "Remove Peace Deal Offer";
            r.message = "Our offer of peace is withdrawn.";
            r.reaction = DiplomacyReaction.ConditionReaction.WITHDRAW_PROPOSAL;
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.ACTION_COUNTER;
                d.argumentAction = DiplomacyAction.DiplActionType.ATTACK;
                d.argumentValue = 1;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.name = "Break Alliance on repeated attacks";
            r.reaction = DiplomacyReaction.ConditionReaction.BREAK_TREATY;
            r.treatyId = TreatyTypeRegistry.ALLIANCE;
            r.message = "You attacked us. Consider our alliance over.";
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.ACTION_COUNTER;
                d.argumentAction = DiplomacyAction.DiplActionType.ATTACK;
                d.argumentValue = 2;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.reaction = DiplomacyReaction.ConditionReaction.SEND_POPUP_MESSAGE;
            r.name = "Send message on one Attack";
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            r.message = "Attacks will not be tolerated!";
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.ACTION_COUNTER;
                d.argumentAction = DiplomacyAction.DiplActionType.ATTACK;
                d.argumentValue = 1;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.reaction = DiplomacyReaction.ConditionReaction.PROPOSE_TREATY;
            r.treatyId = TreatyTypeRegistry.WAR;
            r.name = "Declare War on three hostile actions";
            r.message = "Three hostile acts is not a misunderstanding. We are at war.";
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.ACTION_COUNTER;
                d.argumentAction = DiplomacyAction.DiplActionType.ATTACK;
                d.argumentValue = 3;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        {
            //last, so adding it did not renumber the reactions saved worlds have marked as fired
            DiplomacyReaction r = new DiplomacyReaction(i);
            i++;
            r.reaction = DiplomacyReaction.ConditionReaction.PROPOSE_TREATY;
            r.name = "Sue for Peace when losing";
            r.message = "We have lost enough ships to this war. We propose an end to it.";
            r.condition = new DiplomacyConditionGroup();
            r.condition.mod = DiplomacyConditionGroup.ConditionMod.AND;
            {
                DiplomacyCondition d = new DiplomacyCondition();
                d.type = DiplomacyCondition.ConditionType.WEARINESS;
                d.argumentValue = 0.5;
                r.condition.conditions.add(d);
            }
            reactions.add(r);
        }
        addDefaultAcceptance();
    }

    /**
     * What a faction with no configured personality will agree to.
     *
     * <p>The first two carry over the thresholds the old hardcoded {@code ACCEPT_PEACE_OFFER} and
     * {@code ACCEPT_ALLIANCE_OFFER} reactions used, so an archetype that says nothing behaves as it
     * did. The rest are new, and set the shape of the thing: something cheap to give away is offered
     * on a low bar, something that commits you to a war needs a real relationship first. A treaty
     * type absent from this map is refused, which is why vassalage and tribute are not here - the
     * machinery that would honour them does not exist yet, and an NPC should not agree to something
     * it cannot then do.</p>
     */
    public void addDefaultAcceptance() {
        acceptance.clear();
        refusals.clear();
        //plain peace: they will stop shooting once they have been left alone for a while
        putAcceptance(null, and(
                rawPoints(-500),
                calm(30 * 60 * 1000)));
        putAcceptance(TreatyTypeRegistry.NON_AGGRESSION, and(
                rawPoints(-200),
                calm(30 * 60 * 1000)));
        putAcceptance(TreatyTypeRegistry.TRADE_AGREEMENT, and(
                rawPoints(0)));
        putAcceptance(TreatyTypeRegistry.INTEL_SHARING, and(
                rawPoints(500),
                calm(60 * 60 * 1000)));
        putAcceptance(TreatyTypeRegistry.ALLIANCE, and(
                rawPoints(1000),
                calm(30 * 60 * 1000)));
        //a defensive pact means fighting somebody else's war, so it wants a reason beyond goodwill:
        //an enemy in common
        putAcceptance(TreatyTypeRegistry.DEFENSIVE_PACT, and(
                rawPoints(1500),
                sharesEnemy(1)));

        //Generic on purpose: these are what a faction says before its archetype gives it a voice, so
        //they have to be true of a pirate and a merchant alike. A shipped preset overrides them in
        //its own npcConfig.xml, which is where personality belongs.
        putRefusal(null, Lng.str("They are not ready to stop shooting."));
        putRefusal(TreatyTypeRegistry.NON_AGGRESSION, Lng.str("They will not promise you peace yet."));
        putRefusal(TreatyTypeRegistry.TRADE_AGREEMENT, Lng.str("They see nothing in trading with you."));
        putRefusal(TreatyTypeRegistry.INTEL_SHARING, Lng.str("They do not trust you with what they know."));
        putRefusal(TreatyTypeRegistry.ALLIANCE, Lng.str("They will not stand beside you."));
        putRefusal(TreatyTypeRegistry.DEFENSIVE_PACT, Lng.str("They will not fight your wars for you."));
    }

    public int getIndex(DiplomacyAction.DiplActionType action) {
        return (1000 * (action.ordinal() + 1));
    }

    public int getIndex(DiplomacyEntity.DiplStatusType status) {
        return (1000000 * (status.ordinal() + 1));
    }

    public DiplomacyConfigElement get(DiplomacyAction.DiplActionType action) {
        return map.get(getIndex(action));
    }

    public DiplomacyConfigElement get(DiplomacyEntity.DiplStatusType status) {
        return map.get(getIndex(status));
    }

    public DiplomacyConfigElement put(DiplomacyAction.DiplActionType action, DiplomacyConfigElement p) {
        return map.put(getIndex(action), p);
    }

    public DiplomacyConfigElement put(DiplomacyEntity.DiplStatusType status, DiplomacyConfigElement p) {
        return map.put(getIndex(status), p);
    }

    public class DiplomacyConfigElement {
        public DiplomacyEntity.DiplStatusType statusType;
        public DiplomacyAction.DiplActionType actionType;
        public int upperLimit = 30;
        public int lowerLimit;
        public int value;
        public int existingModifier = 1;
        public int nonExistingModifier = 1;
        public float turnsActionDuration = 1;
        public float staticTimeoutTurns = 1;
        public DiplomacyReaction reaction;

        public void parse(Node node) throws ConfigParserException {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if ("diplaction".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                    NodeList childNodes = node.getChildNodes();
                    Integer value = null;
                    Float upperLim = null;
                    Float lowerLim = null;
                    Float nonExt = null;
                    Float ext = null;
                    Float timeout = null;
                    String name = null;

                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node item = childNodes.item(i);
                        if (item.getNodeType() == Node.ELEMENT_NODE) {
                            if ("name".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                name = item.getTextContent();
                            }
                            if ("upperlimit".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                try {
                                    upperLim = Float.parseFloat(item.getTextContent().trim());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new ConfigParserException(
                                            "Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
                                                    + item.getParentNode().getNodeName() + "; "
                                                    + item.getParentNode().getParentNode().getNodeName());
                                }
                            }
                            try {
                                if ("reaction".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                    reaction = DiplomacyReaction.parse(item, -1);
                                }
                            } catch (ConfigParserException e) {
                                e.printStackTrace();
                                System.err.println("NOT USING REACTION BECAUSE OF EXCEPTION");
                            }
                            if ("lowerlimit".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                try {
                                    lowerLim = Float.parseFloat(item.getTextContent().trim());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new ConfigParserException(
                                            "Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
                                                    + item.getParentNode().getNodeName() + "; "
                                                    + item.getParentNode().getParentNode().getNodeName());
                                }
                            }
                            if ("nonexistingmodifier".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                try {
                                    nonExt = Float.parseFloat(item.getTextContent().trim());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new ConfigParserException(
                                            "Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
                                                    + item.getParentNode().getNodeName() + "; "
                                                    + item.getParentNode().getParentNode().getNodeName());
                                }
                            }
                            if ("existingmodifier".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                try {
                                    ext = Float.parseFloat(item.getTextContent().trim());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new ConfigParserException(
                                            "Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
                                                    + item.getParentNode().getNodeName() + "; "
                                                    + item.getParentNode().getParentNode().getNodeName());
                                }
                            }
                            if ("actiontimeoutduration".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                try {
                                    timeout = Float.parseFloat(item.getTextContent().trim());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new ConfigParserException(
                                            "Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
                                                    + item.getParentNode().getNodeName() + "; "
                                                    + item.getParentNode().getParentNode().getNodeName());
                                }
                            }
                            if ("value".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                try {
                                    value = (int) Float.parseFloat(item.getTextContent().trim());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new ConfigParserException(
                                            "Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
                                                    + item.getParentNode().getNodeName() + "; "
                                                    + item.getParentNode().getParentNode().getNodeName());
                                }
                            }
                        }

                    }
                    if (upperLim == null) {
                        throw new ConfigParserException(
                                "'UpperLimit' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }
                    if (lowerLim == null) {
                        throw new ConfigParserException(
                                "'LowerLimit' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }
                    if (nonExt == null) {
                        throw new ConfigParserException(
                                "'NonExistingModifier' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }
                    if (ext == null) {
                        throw new ConfigParserException(
                                "'ExistingModifier' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }
                    if (name == null) {
                        throw new ConfigParserException(
                                "'Name' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }
                    if (timeout == null) {
                        throw new ConfigParserException(
                                "'ActionTimeoutDuration' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }
                    if (value == null) {
                        throw new ConfigParserException(
                                "'Value' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }

                    DiplomacyAction.DiplActionType t = actionByName(name);
                    if (t == null) {
                        //a name this build no longer has: skip the entry rather than refuse the file.
                        //Dropping a value from the vocabulary would otherwise break every config
                        //written while it existed, shipped presets and server-authored ones alike.
                        System.err.println("[DIPLOMACY][CONFIG] ignoring unknown action '" + name + "'");
                        return;
                    }
                    actionType = t;
                    upperLimit = upperLim.intValue();
                    lowerLimit = lowerLim.intValue();
                    nonExistingModifier = nonExt.intValue();
                    existingModifier = ext.intValue();
                    turnsActionDuration = timeout;
                    this.value = value;

                    put(actionType, this);
                } else if ("diplstatus".equals(node.getNodeName().toLowerCase(Locale.ENGLISH))) {
                    NodeList childNodes = node.getChildNodes();

                    Integer value = null;
                    Float timeout = null;
                    String name = null;
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node item = childNodes.item(i);
                        if (item.getNodeType() == Node.ELEMENT_NODE) {
                            if ("name".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                name = item.getTextContent();
                            }
                            if ("timeout".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                try {
                                    timeout = Float.parseFloat(item.getTextContent().trim());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new ConfigParserException(
                                            "Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
                                                    + item.getParentNode().getNodeName() + "; "
                                                    + item.getParentNode().getParentNode().getNodeName());
                                }
                            }
                            if ("value".equals(item.getNodeName().toLowerCase(Locale.ENGLISH))) {
                                try {
                                    value = (int) Float.parseFloat(item.getTextContent().trim());
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    throw new ConfigParserException(
                                            "Invalid Number Format: " + item.getTextContent() + "; " + item.getNodeName() + "; "
                                                    + item.getParentNode().getNodeName() + "; "
                                                    + item.getParentNode().getParentNode().getNodeName());
                                }
                            }
                        }

                    }
                    if (name == null) {
                        throw new ConfigParserException(
                                "'Name' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }
                    if (timeout == null) {
                        throw new ConfigParserException(
                                "'Timeout' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }
                    if (value == null) {
                        throw new ConfigParserException(
                                "'Value' Tag needed. " + node.getNodeName() + "; "
                                        + node.getParentNode().getNodeName());
                    }

                    DiplomacyEntity.DiplStatusType t = statusByName(name);
                    if (t == null) {
                        System.err.println("[DIPLOMACY][CONFIG] ignoring unknown status '" + name + "'");
                        return;
                    }
                    statusType = t;
                    this.value = value;
                    staticTimeoutTurns = timeout;
                    put(statusType, this);

                } else {
                    throw new ConfigParserException("Diplomacy Config Error " + node.getNodeName() + "; " + node.getParentNode().getNodeName());
                }
            }
        }

        public void appendXML(Document config, Element dpl, DiplomacyEntity.DiplStatusType b) {
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

        public void appendXML(Document config, Element dpl, DiplomacyAction.DiplActionType b) {
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

            if (reaction != null) {
                reaction.appendXML(config, s);
            }

            dpl.appendChild(s);
        }


    }
}


