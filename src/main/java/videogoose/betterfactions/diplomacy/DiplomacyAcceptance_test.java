package videogoose.betterfactions.diplomacy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Pins the archetype config: that it survives a write/read round trip, and that the shipped NPC
 * presets still load and still describe three different personalities.
 *
 * <p>The presets are the feature. Acceptance rules only mean anything if a server owner can edit
 * them, which means the file has to keep parsing after the vocabulary changes - and the vocabulary
 * is expected to keep changing. A dropped enum value used to make the parser throw
 * {@code IllegalArgumentException} out of {@code valueOf}, which took the whole preset with it; this
 * checks the tolerant path as well as the happy one.</p>
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class DiplomacyAcceptance_test {

    private static final String PRESETS = "src/main/resources/data/npc/";

    private DiplomacyAcceptance_test() {
    }

    public static void main(String[] args) throws Exception {
        defaultsCoverTheTreatiesTheEngineCanHonour();
        roundTripsThroughXml();
        unknownNamesAreSkippedNotFatal();
        shippedPresetsLoad();
        shippedPresetsDiffer();
        everyShippedRuleSaysNoInItsOwnWords();
        refusalsSurviveXml();
        whatIsDemandedRatherThanOffered();
        casusBelliDefaultsAndOverrides();
        System.out.println("DiplomacyAcceptance_test: OK");
    }

    /**
     * Every treaty type with a default rule must be one the engine can actually act on. Agreeing to
     * vassalage before anything implements it would leave a signed treaty nothing honours.
     */
    private static void defaultsCoverTheTreatiesTheEngineCanHonour() {
        DiplomacyConfig c = new DiplomacyConfig();
        assert c.getAcceptance(null) != null : "a faction that will never make peace is a dead end";
        assert c.getAcceptance(TreatyTypeRegistry.ALLIANCE) != null;
        assert c.getAcceptance(TreatyTypeRegistry.TRADE_AGREEMENT) != null;
        //not yet: nothing transfers tribute or constrains a vassal, so nothing should sign for one
        assert c.getAcceptance(TreatyTypeRegistry.VASSALAGE) == null;
        assert c.getAcceptance(TreatyTypeRegistry.TRIBUTARY) == null;
        //an unregistered id is simply not agreed to, rather than throwing
        assert c.getAcceptance("gonemod:pact") == null;
    }

    /**
     * What {@code appendXML} writes, {@code parse} has to be able to read back.
     */
    private static void roundTripsThroughXml() throws Exception {
        DiplomacyConfig written = new DiplomacyConfig();
        written.addDefaultActions();
        written.powerSystemWeight = 37;
        DiplomacyConditionGroup custom = new DiplomacyConditionGroup();
        custom.mod = DiplomacyConditionGroup.ConditionMod.AND;
        DiplomacyCondition hasPact = new DiplomacyCondition();
        hasPact.type = DiplomacyCondition.ConditionType.HAS_TREATY;
        hasPact.argumentText = TreatyTypeRegistry.NON_AGGRESSION;
        custom.conditions.add(hasPact);
        written.putAcceptance(TreatyTypeRegistry.INTEL_SHARING, custom);

        Document doc = newDocument();
        Element root = doc.createElement("Diplomacy");
        doc.appendChild(root);
        written.appendXML(doc, root);

        DiplomacyConfig read = new DiplomacyConfig();
        read.parse(root, false);

        assert read.version == DiplomacyConfig.VERSION : read.version;
        assert read.powerSystemWeight == 37 : read.powerSystemWeight;
        assert read.reactions.size() == written.reactions.size()
                : read.reactions.size() + " vs " + written.reactions.size();
        //the treaty a reaction acts on has to survive, or every PROPOSE_TREATY becomes a peace offer
        boolean sawTreatyReaction = false;
        for (DiplomacyReaction r : read.reactions) {
            if (r.reaction == DiplomacyReaction.ConditionReaction.PROPOSE_TREATY && TreatyTypeRegistry.WAR.equals(r.treatyId)) {
                sawTreatyReaction = true;
                break;
            }
        }
        assert sawTreatyReaction : "reaction treaty ids did not survive the round trip";

        DiplomacyConditionGroup back = read.getAcceptance(TreatyTypeRegistry.INTEL_SHARING);
        assert back != null : "acceptance rule lost";
        DiplomacyCondition leaf = (DiplomacyCondition) back.conditions.get(0);
        assert leaf.type == DiplomacyCondition.ConditionType.HAS_TREATY : leaf.type;
        //the free-text argument is the only way a condition can name a treaty
        assert TreatyTypeRegistry.NON_AGGRESSION.equals(leaf.argumentText) : leaf.argumentText;
    }

    /**
     * A config naming a status or action this build no longer has must lose that one entry, not the
     * whole file - otherwise retiring any vocabulary breaks every config written while it existed.
     */
    private static void unknownNamesAreSkippedNotFatal() throws Exception {
        Document doc = newDocument();
        Element root = doc.createElement("Diplomacy");
        doc.appendChild(root);
        Element ver = doc.createElement("Version");
        ver.setTextContent(String.valueOf(DiplomacyConfig.VERSION));
        root.appendChild(ver);

        Element elems = doc.createElement("DiplomacyElement");
        elems.appendChild(status(doc, "CLOSE_TERRITORY", -100));
        elems.appendChild(status(doc, "IN_WAR", -9999));
        root.appendChild(elems);

        DiplomacyConfig c = new DiplomacyConfig();
        c.parse(root, false);
        //the live one still landed
        assert c.get(DiplomacyEntity.DiplStatusType.IN_WAR).value == -9999
                : c.get(DiplomacyEntity.DiplStatusType.IN_WAR).value;
    }

    private static void shippedPresetsLoad() throws Exception {
        for (String name : presetFiles()) {
            DiplomacyConfig c = loadPreset(name);
            assert c.version == DiplomacyConfig.VERSION
                    : name + " is diplomacy config version " + c.version + ", expected " + DiplomacyConfig.VERSION;
            assert !c.reactions.isEmpty() : name + " has no reactions";
            assert !c.acceptance.isEmpty() : name + " agrees to nothing";
            assert c.getAcceptance(null) != null : name + " can never be made peace with";
            for (String treatyId : c.acceptance.keySet()) {
                assert treatyId.isEmpty() || TreatyTypeRegistry.exists(treatyId)
                        : name + " accepts unregistered treaty type " + treatyId;
            }
            for (DiplomacyReaction r : c.reactions) {
                assert r.reaction != null : name + " has a reaction with no action";
                assert r.treatyId == null || TreatyTypeRegistry.exists(r.treatyId)
                        : name + " reaction '" + r.name + "' names unregistered treaty " + r.treatyId;
                //the message is what the player actually reads - a proposal, a warning, a faction
                //mail. Left empty, the engine substitutes one generic line for every archetype.
                assert r.message != null && !r.message.trim().isEmpty()
                        : name + " reaction '" + r.name + "' has no message";
                //a NOT group with anything but one member throws when it is evaluated, which would
                //happen on a live server rather than here
                r.check();
                assertSaneNots(name, r.condition);
                //a grievance at war is just the war: a mail or a declaration on every shot is noise
                boolean speaks = r.reaction == DiplomacyReaction.ConditionReaction.SEND_FACTION_MAIL
                        || r.reaction == DiplomacyReaction.ConditionReaction.SEND_POPUP_MESSAGE
                        || r.reaction == DiplomacyReaction.ConditionReaction.PROPOSE_TREATY && TreatyTypeRegistry.WAR.equals(r.treatyId);
                assert !speaks || !countsAttacks(r.condition) || mentionsWarStatus(r.condition)
                        : name + " reaction '" + r.name + "' fires on attacks without checking we are already at war";
            }
        }
    }

    /**
     * The point of per-archetype config: three presets that behave the same are three of one.
     */
    private static void shippedPresetsDiffer() throws Exception {
        DiplomacyConfig outcasts = loadPreset("outcasts.xml");
        DiplomacyConfig scavengers = loadPreset("scavengers.xml");
        DiplomacyConfig guild = loadPreset("trading guild.xml");

        //the raider will not sign a defensive pact at any price; the merchant will, for a reason
        assert outcasts.getAcceptance(TreatyTypeRegistry.DEFENSIVE_PACT) == null;
        assert guild.getAcceptance(TreatyTypeRegistry.DEFENSIVE_PACT) != null;
        //and trade is the merchant's business, not the raider's
        assert outcasts.getAcceptance(TreatyTypeRegistry.TRADE_AGREEMENT) == null;
        assert guild.getAcceptance(TreatyTypeRegistry.TRADE_AGREEMENT) != null;
        assert scavengers.getAcceptance(TreatyTypeRegistry.TRADE_AGREEMENT) != null;

        //they do not even agree on what makes a faction formidable
        assert guild.powerSystemWeight > outcasts.powerSystemWeight
                : "the merchant should count territory for more than the raider does";
        assert outcasts.powerMemberWeight > guild.powerMemberWeight
                : "the raider should count crew for more than the merchant does";
        //every weight is checked, not just two: a tag the parser does not recognise is skipped
        //silently, so a renamed one would leave every preset quietly sharing the engine default
        assert guild.powerSurplusWeight > outcasts.powerSurplusWeight
                : "the merchant should care more about whether a faction can pay its bills";
        assert new DiplomacyConfig().powerSurplusWeight != guild.powerSurplusWeight
                : "PowerWeights fell back to the default - is the XML tag name still right?";
    }

    /**
     * A refusal is one of the few places the diplomacy model speaks to a player directly. A rule with
     * nothing to say falls back to a list of unmet conditions, which is accurate and sounds like a
     * config file - so a shipped preset that forgets one is a bug worth failing the build over.
     */
    private static void everyShippedRuleSaysNoInItsOwnWords() throws Exception {
        for (String name : presetFiles()) {
            DiplomacyConfig c = loadPreset(name);
            for (String treatyId : c.acceptance.keySet()) {
                String refusal = c.refusals.get(treatyId);
                assert refusal != null && !refusal.trim().isEmpty()
                        : name + " has no refusal message for '" + treatyId + "'";
                //an id in the text means somebody pasted the rule key in rather than writing a line
                assert !refusal.contains("starmade:")
                        : name + " refusal for '" + treatyId + "' leaks a type id: " + refusal;
            }
        }
    }

    private static void refusalsSurviveXml() throws Exception {
        DiplomacyConfig written = new DiplomacyConfig();
        written.addDefaultActions();
        written.putAcceptance(TreatyTypeRegistry.ALLIANCE, new DiplomacyConditionGroup());
        written.putRefusal(TreatyTypeRegistry.ALLIANCE, "We bow to no one.");
        //a blank message means "let the engine explain", and must not be stored as an empty line.
        //Vassalage because it is the one type the defaults deliberately leave unanswered.
        written.putAcceptance(TreatyTypeRegistry.VASSALAGE, new DiplomacyConditionGroup());
        written.putRefusal(TreatyTypeRegistry.VASSALAGE, "   ");
        assert written.getRefusal(TreatyTypeRegistry.VASSALAGE) == null;

        Document doc = newDocument();
        Element root = doc.createElement("Diplomacy");
        doc.appendChild(root);
        written.appendXML(doc, root);

        DiplomacyConfig read = new DiplomacyConfig();
        read.parse(root, false);
        assert "We bow to no one.".equals(read.getRefusal(TreatyTypeRegistry.ALLIANCE))
                : read.getRefusal(TreatyTypeRegistry.ALLIANCE);
        assert read.getRefusal(TreatyTypeRegistry.VASSALAGE) == null
                : "an empty Refusal came back as a message";
    }

    /**
     * NPC factions refuse every demand outright, so which types count as demands decides what that
     * rule covers. The classification is derived rather than declared - agreed, but asymmetric - so a
     * type that quietly changed one of those flags would move in or out of the rule with nothing else
     * in the build noticing.
     */
    private static void whatIsDemandedRatherThanOffered() {
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.VASSALAGE).getKind() == TreatyType.Kind.DEMAND;
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.TRIBUTARY).getKind() == TreatyType.Kind.DEMAND;
        //war is declared, not demanded: nobody is being asked, so an acceptance rule never sees it
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.WAR).getKind() == TreatyType.Kind.DECLARATION;
        //and the agreements between equals must stay offerable, or the refusal swallows the game
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.ALLIANCE).getKind() == TreatyType.Kind.PACT;
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.NON_AGGRESSION).getKind() == TreatyType.Kind.PACT;
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.TRADE_AGREEMENT).getKind() == TreatyType.Kind.PACT;
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.DEFENSIVE_PACT).getKind() == TreatyType.Kind.PACT;
        assert TreatyTypeRegistry.get(TreatyTypeRegistry.INTEL_SHARING).getKind() == TreatyType.Kind.PACT;

        //no shipped preset may agree to a demand, because the engine refuses them before the rule is
        //ever consulted - a rule for one would be config that silently does nothing
        for (String name : presetFiles()) {
            try {
                DiplomacyConfig c = loadPreset(name);
                for (String treatyId : c.acceptance.keySet()) {
                    TreatyType t = treatyId.isEmpty() ? null : TreatyTypeRegistry.get(treatyId);
                    assert t == null || t.getKind() != TreatyType.Kind.DEMAND
                            : name + " has an acceptance rule for '" + treatyId + "', which an NPC always refuses";
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static boolean countsAttacks(DiplomacyConditionGroup g) {
        if (g instanceof DiplomacyCondition c) {
            return c.type == DiplomacyCondition.ConditionType.ACTION_COUNTER && c.argumentAction == DiplomacyAction.DiplActionType.ATTACK;
        }
        return g != null && g.conditions.stream().anyMatch(DiplomacyAcceptance_test::countsAttacks);
    }

    private static boolean mentionsWarStatus(DiplomacyConditionGroup g) {
        if (g instanceof DiplomacyCondition c) {
            return c.type == DiplomacyCondition.ConditionType.EXISTS_STATUS && c.argumentStatus == DiplomacyEntity.DiplStatusType.IN_WAR;
        }
        return g != null && g.conditions.stream().anyMatch(DiplomacyAcceptance_test::mentionsWarStatus);
    }

    private static void assertSaneNots(String name, DiplomacyConditionGroup g) {
        if (g == null || g instanceof DiplomacyCondition) {
            return;
        }
        assert g.mod != DiplomacyConditionGroup.ConditionMod.NOT || g.conditions.size() == 1
                : name + ": NOT group with " + g.conditions.size() + " members";
        for (DiplomacyConditionGroup c : g.conditions) {
            assertSaneNots(name, c);
        }
    }

    private static List<String> presetFiles() {
        List<String> l = new ArrayList<String>();
        File[] files = new File(PRESETS).listFiles((d, n) -> n.endsWith(".xml"));
        assert files != null : "no NPC presets at " + new File(PRESETS).getAbsolutePath();
        for (File f : files) {
            //the engine lowercases preset names, and a jar lookup is case-sensitive: a capitalised
            //file is never found and its faction silently speaks with default.xml's lines
            assert f.getName().equals(f.getName().toLowerCase(java.util.Locale.ENGLISH))
                    : "preset file names must be lowercase to be found: " + f.getName();
            l.add(f.getName());
        }
        assert l.size() >= 4 : "expected the three shipped presets plus the default, got " + l;
        return l;
    }

    private static DiplomacyConfig loadPreset(String relative) throws Exception {
        File f = new File(PRESETS + relative);
        assert f.exists() : f.getAbsolutePath();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
        NodeList nodes = doc.getElementsByTagName("Diplomacy");
        assert nodes.getLength() == 1 : relative + " has " + nodes.getLength() + " Diplomacy blocks";
        DiplomacyConfig c = new DiplomacyConfig();
        c.parse(nodes.item(0), false);
        return c;
    }

    private static Element status(Document doc, String name, int value) {
        Element s = doc.createElement("DiplStatus");
        Element n = doc.createElement("Name");
        n.setTextContent(name);
        Element t = doc.createElement("Timeout");
        t.setTextContent("1.0");
        Element v = doc.createElement("Value");
        v.setTextContent(String.valueOf(value));
        s.appendChild(n);
        s.appendChild(t);
        s.appendChild(v);
        return s;
    }

    private static Document newDocument() throws Exception {
        DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return b.newDocument();
    }

    /**
     * Every war-goal kind has a default casus belli, so a fresh server can fight over anything it can
     * settle; and a server's own section replaces the defaults outright, so leaving a kind out is how a
     * server forbids claiming it. A kind this build does not know is skipped, not fatal.
     */
    private static void casusBelliDefaultsAndOverrides() throws Exception {
        DiplomacyConfig c = new DiplomacyConfig();
        for (SettlementTerm.Kind k : SettlementTerm.Kind.values()) {
            assert c.casusBelli.get(k) != null : "no default casus belli for " + k;
            assert c.casusBelliRefusals.get(k) != null : "no refusal for " + k;
        }

        String xml = "<Diplomacy><CasusBelli>"
                + "<Goal><Kind>transfer_faction_points</Kind><Refusal>Not over money.</Refusal>"
                + "<And><Condition><Type>TOTAL_POINTS</Type><Value>-100</Value></Condition></And></Goal>"
                + "<Goal><Kind>CEDE_THE_MOON</Kind><And/></Goal>"
                + "</CasusBelli></Diplomacy>";
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        c.parse(doc.getDocumentElement(), false);
        assert c.casusBelli.size() == 1 : c.casusBelli.keySet();
        assert c.casusBelli.get(SettlementTerm.Kind.TRANSFER_FACTION_POINTS) != null;
        assert "Not over money.".equals(c.casusBelliRefusals.get(SettlementTerm.Kind.TRANSFER_FACTION_POINTS));
        assert c.casusBelli.get(SettlementTerm.Kind.SIGN_TREATY) == null : "an omitted kind stayed claimable";
    }
}
