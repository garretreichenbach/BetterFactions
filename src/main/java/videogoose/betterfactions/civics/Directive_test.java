package videogoose.betterfactions.civics;

import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import org.w3c.dom.Element;
import videogoose.betterfactions.diplomacy.DiplomacyConditionGroup;
import videogoose.betterfactions.diplomacy.TreatyTypeRegistry;
import videogoose.betterfactions.template.Templates;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Pins directive templates: the shipped file reads and covers every Council role, a template's attributes
 * parse, weights multiply by side, and agendas keep their words through a save.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class Directive_test {

    private Directive_test() {
    }

    private static Element xml(String s) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
    }

    public static void main(String[] args) throws Exception {
        //the shipped file: reads whole, and every role has something to ask
        Element shipped = Templates.root(Directive.FILE);
        assert shipped != null : "directives.xml ships in the jar";
        List<Directive> all = Directive.load(shipped);
        assert all.size() == Templates.children(shipped, "Directive").size() : "every shipped directive parses";
        for (int r = 0; r < Council.ROLES; r++) {
            int role = r;
            assert all.stream().anyMatch(d -> d.roles.contains(role)) : "role " + Council.ROLE_IDS[r] + " asks nothing";
        }
        assert all.stream().anyMatch(d -> d.objective == Council.Objective.KILL_PLAYERS);
        assert all.stream().anyMatch(d -> d.pick == Directive.Pick.STRENGTH);

        //attributes
        Directive d = Directive.parse(xml("""
                <Directive id="pact" roles="peace, Diplomacy" objective="sign_treaty" pick="random" treaty="starmade:defensive_pact,x:y"
                           count="2-4" days="5" reward="100" rewardEach="10" penalty="40">
                  <Text>Sign a {treaty} with {target}.</Text>
                  <Quote>Any style.</Quote>
                  <Quote style="board">Board.</Quote>
                  <When><Condition><Type>AT_WAR_COUNT</Type><Value>1</Value></Condition></When>
                  <When side="us"><Condition><Type>AT_WAR_COUNT</Type><Value>2</Value></Condition></When>
                </Directive>"""));
        assert d.id.equals("pact") && d.roles.equals(List.of(Council.PEACE, Council.DIPLOMACY)) : d.roles;
        assert d.objective == Council.Objective.SIGN_TREATY && d.pick == Directive.Pick.RANDOM;
        assert d.treaties.length == 2 && d.treaties[1].equals("x:y");
        assert d.countMin == 2 && d.countMax == 4 && d.daysMin == 5 && d.daysMax == 5;
        assert d.reward(3) == 130 && d.penalty(130) == 40;
        assert d.whenThem != null && d.whenUs != null : "a targeted directive's <When> reads them unless it says us";
        assert d.quote(CouncilLore.Flavour.BOARD).equals("Board.") && d.quote(CouncilLore.Flavour.SHADOW).equals("Any style.");
        Directive timed = Directive.parse(xml("<Directive id=\"t\" roles=\"calm\" objective=\"KEEP_PEACE\" reward=\"200\"/>"));
        assert timed.countMin == 3 && !timed.targeted() && timed.penalty(200) == 120 : "timed: three days, three fifths";
        for (String bad : new String[]{
                "<Directive roles=\"war\" objective=\"DECLARE_WAR\"/>",
                "<Directive id=\"x\" roles=\"warlords\" objective=\"DECLARE_WAR\"/>",
                "<Directive id=\"x\" roles=\"war\" objective=\"CONQUER\"/>",
                "<Directive id=\"x\" roles=\"calm\" objective=\"KEEP_PEACE\"><When side=\"them\"><Condition><Type>AT_WAR_COUNT</Type><Value>1</Value></Condition></When></Directive>"}) {
            boolean threw = false;
            try {
                Directive.parse(xml(bad));
            } catch (Exception e) {
                threw = true;
            }
            assert threw : "should not parse: " + bad;
        }
        assert Directive.load(xml("<Directives><Directive id=\"ok\" roles=\"war\" objective=\"DECLARE_WAR\"/><Directive roles=\"war\"/></Directives>")).size() == 1
                : "one bad entry is skipped, not the file";

        //weights: a base times what holds, on its side
        DiplomacyConditionGroup a = new DiplomacyConditionGroup(), b = new DiplomacyConditionGroup();
        Templates.Weight w = new Templates.Weight(0)
                .add(Templates.Side.US, 3, a)
                .add(Templates.Side.THEM, 2, b)
                .add(Templates.Side.EITHER, 5, b);
        assert w.of(c -> false, c -> false) == 0 : "base 0: nothing holds, never";
        assert w.of(c -> c == a, null) == 3 : "base 0 becomes 1 before the first factor";
        assert w.of(c -> c == a, c -> c == b) == 30;
        assert w.of(c -> c == b, null) == 5 : "either reads us too; them without a target never";
        assert new Templates.Weight(2).add(Templates.Side.US, 0.5, a).of(c -> true, null) == 1;
        assert java.util.Arrays.equals(Templates.range("2-4", 0), new int[]{2, 4}) && java.util.Arrays.equals(Templates.range(" 5 ", 0), new int[]{5, 5})
                && java.util.Arrays.equals(Templates.range("", 7), new int[]{7, 7}) && java.util.Arrays.equals(Templates.range("4-2", 0), new int[]{2, 4});

        //targets: a directive's treaty list replaces the defaults, first not held
        java.util.function.Predicate<String> none = t -> false;
        String[] pact = {TreatyTypeRegistry.DEFENSIVE_PACT, TreatyTypeRegistry.ALLIANCE};
        assert TreatyTypeRegistry.DEFENSIVE_PACT.equals(Council.targetType(Council.Objective.SIGN_TREATY, 100, false, none, true, pact));
        assert TreatyTypeRegistry.ALLIANCE.equals(Council.targetType(Council.Objective.SIGN_TREATY, 100, false, TreatyTypeRegistry.DEFENSIVE_PACT::equals, true, pact));
        assert Council.targetType(Council.Objective.SIGN_TREATY, -100, false, none, true, pact) == null : "relations still gate it";
        assert Council.targetType(Council.Objective.KILL_PLAYERS, 0, true, none, false) != null && Council.targetType(Council.Objective.KILL_PLAYERS, 0, false, none, true) == null
                : "pilots only of those we are at war with";
        assert Council.pickWeight(Directive.Pick.STRENGTH, Council.Objective.DESTROY_SHIPS, 0, 100, 900) > Council.pickWeight(Directive.Pick.STRENGTH, Council.Objective.DESTROY_SHIPS, 0, 100, 50)
                : "the stronger, the likelier";
        assert Council.pickWeight(Directive.Pick.RANDOM, Council.Objective.DECLARE_WAR, -2000, 1, 1) == 1;

        //words: filled, saved, and an old save reads with the built-in ones
        assert Directive.fill("{count} of {target}'s {treaty} in {days}", "Dovan", 3, "pact", 4).equals("3 of Dovan's pact in 4");
        Council.Agenda ag = new Council.Agenda();
        ag.objective = Council.Objective.KILL_PLAYERS;
        ag.text = "Hunt {count} of {target}.";
        ag.quote = "not saved";
        ag.count = 2;
        Council.Agenda back = Council.Agenda.fromTag(ag.toTag());
        assert back.objective == Council.Objective.KILL_PLAYERS && back.text.equals(ag.text) && back.quote.isEmpty();
        assert Council.describe(back, id -> "Dovan").equals("Hunt 2 of Dovan.");
        Tag[] full = ag.toTag().getStruct();
        Tag[] old = java.util.Arrays.copyOf(full, 11);
        old[10] = FinishTag.INST;
        assert Council.Agenda.fromTag(new Tag(Tag.Type.STRUCT, null, old)).text.isEmpty() : "an agenda saved before directives";
        System.out.println("Directive_test: OK");
    }
}
