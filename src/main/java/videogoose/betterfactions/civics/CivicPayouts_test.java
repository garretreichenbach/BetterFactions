package videogoose.betterfactions.civics;

import java.util.List;
import java.util.Set;

/**
 * Pins what needs no running server: daily caps, the week's income, and how {@code civics.xml} retunes
 * a built-in civic, adds new ones, skips broken ones and is undone on the next apply.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class CivicPayouts_test {

    private static final long DAY = CivicPayouts.DAY_MS;

    private CivicPayouts_test() {
    }

    public static void main(String[] args) {
        capsResetDaily();
        theWeekRollsOn();
        survivesATag();
        civicsXml();
        System.out.println("CivicPayouts_test: OK");
    }

    private static void capsResetDaily() {
        FactionCivics c = new FactionCivics();
        long now = 100 * DAY + 5;
        assert c.credit("a", 100, 250, now) == 100;
        assert c.credit("a", 100, 250, now) == 100;
        assert c.credit("a", 100, 250, now) == 50 : "only what is left under the cap";
        assert c.credit("a", 100, 250, now) == 0;
        assert c.credit("b", 100, 250, now) == 100 : "each payout has its own cap";
        assert c.credit("a", -400, 250, now) == -400 : "costs are never capped";
        assert c.credit("a", 100, 0, now) == 100 : "0 is no cap";
        assert c.credit("a", 100, 250, now + DAY) == 100 : "a new day, a new cap";
    }

    private static void theWeekRollsOn() {
        FactionCivics c = new FactionCivics();
        long start = 200 * DAY;
        for (int d = 0; d < 10; d++) {
            c.credit("x", 10, 0, start + d * DAY);
        }
        long last = start + 9 * DAY;
        assert c.weekIncome(last) == 70 : c.weekIncome(last);
        assert c.weekIncome(last + 3 * DAY) == 40 : "three days later, three days have dropped out";
        assert c.weekIncome(last + 30 * DAY) == 0;
        c.credit("x", 5, 0, last + 30 * DAY);
        assert c.weekIncome(last + 30 * DAY) == 5 : "a long gap clears the old week";
    }

    private static void survivesATag() {
        FactionCivics c = new FactionCivics();
        c.credit("a", 70, 100, 300 * DAY);
        c.dailyDone = 300;
        FactionCivics back = FactionCivics.fromTag(c.toTag());
        assert back.day == 300 && back.dailyDone == 300 && back.paidToday.equals(c.paidToday) : back.paidToday;
        assert back.weekIncome(300 * DAY) == 70;
        assert back.credit("a", 70, 100, 300 * DAY) == 30 : "the cap survives a restart";
    }

    private static void civicsXml() {
        try {
            int built = CivicRegistry.get(CivicRegistry.WARRIOR_CODE).getPayouts().size();
            List<String> skipped = CivicPayouts.apply("""
                    <Civics>
                      <Civic id="betterfactions:warrior_code">
                        <Payout event="SHIP_DESTROYED" where="VICTIM_AT_WAR" fp="25" dailyCap="500"/>
                      </Civic>
                      <Civic id="salvagers" name="Salvagers">
                        <Description>Wrecks are opportunities.</Description>
                        <Requires><Condition><Type>CLAIMED_SYSTEMS_AT_LEAST</Type><Value>2</Value></Condition>
                          <Condition><Type>HOLDS_CIVIC</Type><Argument>betterfactions:raiders</Argument></Condition></Requires>
                        <Excludes>betterfactions:isolationists</Excludes>
                        <Payout event="ship_destroyed" where="IN_OUR_TERRITORY, VICTIM_AT_WAR" fp="5" dailyCap="150"/>
                      </Civic>
                      <Civic id="broken"><Payout event="NOT_AN_EVENT" fp="1"/></Civic>
                    </Civics>""");
            assert skipped.size() == 1 && skipped.get(0).startsWith("broken") : skipped;

            List<CivicPayouts.Payout> wc = CivicRegistry.get(CivicRegistry.WARRIOR_CODE).getPayouts();
            assert wc.size() == 1 && wc.get(0).fp() == 25 && wc.get(0).dailyCap() == 500 : "retuned: " + wc;

            CivicType s = CivicRegistry.get("custom:salvagers");
            assert s != null && s.getDisplayName().equals("Salvagers") && s.getDescription().equals("Wrecks are opportunities.");
            assert s.getRequires().equals(Set.of(CivicRegistry.RAIDERS));
            assert s.getUnlocks().equals(List.of(new CivicType.Unlock(CivicType.Unlock.Rule.CLAIMED_SYSTEMS_AT_LEAST, 2)));
            assert s.conflictsWith(CivicRegistry.get(CivicRegistry.ISOLATIONISTS));
            assert s.getPayouts().equals(List.of(new CivicPayouts.Payout(CivicPayouts.Event.SHIP_DESTROYED,
                    Set.of(CivicPayouts.Filter.IN_OUR_TERRITORY, CivicPayouts.Filter.VICTIM_AT_WAR), 5, 150))) : s.getPayouts();

            assert CivicPayouts.apply("").isEmpty();
            assert CivicRegistry.get("custom:salvagers") == null : "a civic from the last file is gone";
            assert CivicRegistry.get(CivicRegistry.WARRIOR_CODE).getPayouts().size() == built : "retuned payouts are restored";

            assert CivicPayouts.apply("<Civics><oops").size() == 1 : "a file that does not parse is reported, not thrown";
        } finally {
            CivicRegistry.unregisterAll();
        }
    }
}
