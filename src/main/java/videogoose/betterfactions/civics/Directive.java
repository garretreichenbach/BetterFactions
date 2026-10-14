package videogoose.betterfactions.civics;

import org.schema.common.config.ConfigParserException;
import org.w3c.dom.Element;
import videogoose.betterfactions.diplomacy.DiplomacyConditionGroup;
import videogoose.betterfactions.template.Templates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A directive template: what a Council seat may ask for, read from {@code directives.xml}. Which seats ask
 * it, its objective, how its target is chosen, its size, deadline and stakes, its weight and its
 * {@code <When>}. {@link Council} makes the agendas; this only says what they may be.
 *
 * <pre>{@code
 * <Directive id="destroy_strongest" roles="glory" objective="DESTROY_SHIPS" pick="STRENGTH" count="2-4" days="3-5" rewardEach="120">
 *   <Text>Bring down {count} ships of {target}, the strongest foe we have.</Text>
 *   <Quote style="JUNTA">{target} thinks itself mighty. Prove it wrong.</Quote>
 *   <Weight base="1"><Modifier side="us" factor="2"><Condition>...</Condition></Modifier></Weight>
 *   <When side="them"><Condition>...</Condition></When>
 * </Directive>
 * }</pre>
 *
 * <p>Text and quotes take {@code {target}}, {@code {count}}, {@code {treaty}} and {@code {days}}. Without a
 * text the agenda reads as its objective's built-in sentence, which is translated; without a quote the
 * councilor speaks in the Council style's built-in voice.</p>
 */
public final class Directive {

    public static final String FILE = "directives.xml";

    /** How a target is chosen among the factions the objective and {@code <When>} allow. */
    public enum Pick {
        /** By relations: the further past the objective's line, the likelier (the default). */
        OPINION,
        /** The stronger they are against us, the likelier. */
        STRENGTH,
        /** Evenly. */
        RANDOM
    }

    public String id = "";
    /** Role indexes ({@link Council#ROLE_IDS}) whose seats may ask it. */
    public final List<Integer> roles = new ArrayList<>();
    public Council.Objective objective = Council.Objective.KEEP_PEACE;
    public Pick pick = Pick.OPINION;
    /** Treaty types it may name, in order of preference; empty for the objective's own list. */
    public String[] treaties = new String[0];
    /** For a timed objective, the days it must hold. */
    public int countMin = 1, countMax = 1;
    public int daysMin = 3, daysMax = 5;
    public int reward, rewardEach;
    /** Negative for three fifths of the reward. */
    public int penalty = -1;
    public Templates.Weight weight = new Templates.Weight(1);
    /** Conditions read about us, and about the target; null for none. */
    public DiplomacyConditionGroup whenUs, whenThem;
    /** Raw text with tokens; blank for the objective's built-in sentence. */
    public String text = "";
    /** Councilor's words by Council style name, "" for any style. */
    public final Map<String, String> quotes = new HashMap<>();

    public boolean targeted() {
        return !objective.timed() && objective != Council.Objective.CLAIM_SYSTEMS;
    }

    /** The quote for a Council style, or blank. */
    public String quote(CouncilLore.Flavour f) {
        return quotes.getOrDefault(f.name(), quotes.getOrDefault("", ""));
    }

    public int reward(int count) {
        return reward + rewardEach * count;
    }

    public int penalty(int reward) {
        return penalty >= 0 ? penalty : reward * 3 / 5;
    }

    static Directive parse(Element e) throws ConfigParserException {
        Directive d = new Directive();
        d.id = e.getAttribute("id").trim();
        if (d.id.isEmpty()) {
            throw new ConfigParserException("a directive needs an id");
        }
        try {
            d.objective = Council.Objective.valueOf(e.getAttribute("objective").trim().toUpperCase(Locale.ENGLISH));
            if (e.hasAttribute("pick")) {
                d.pick = Pick.valueOf(e.getAttribute("pick").trim().toUpperCase(Locale.ENGLISH));
            }
        } catch (IllegalArgumentException ex) {
            throw new ConfigParserException(d.id + ": unknown objective or pick");
        }
        for (String r : e.getAttribute("roles").split(",")) {
            int i = List.of(Council.ROLE_IDS).indexOf(r.trim().toLowerCase(Locale.ENGLISH));
            if (i < 0) {
                throw new ConfigParserException(d.id + ": unknown role '" + r.trim() + "', expected one of " + String.join(", ", Council.ROLE_IDS));
            }
            d.roles.add(i);
        }
        if (!e.getAttribute("treaty").isBlank()) {
            d.treaties = e.getAttribute("treaty").replace(" ", "").split(",");
        }
        int[] count = Templates.range(e.getAttribute("count"), d.objective.timed() ? 3 : 1);
        d.countMin = Math.max(1, count[0]);
        d.countMax = Math.max(d.countMin, count[1]);
        int[] days = Templates.range(e.getAttribute("days"), 3);
        d.daysMin = Math.max(1, days[0]);
        d.daysMax = Math.max(d.daysMin, days[1]);
        d.reward = Templates.intAttr(e, "reward", 0);
        d.rewardEach = Templates.intAttr(e, "rewardEach", 0);
        d.penalty = Templates.intAttr(e, "penalty", -1);
        d.weight = Templates.Weight.parse(Templates.child(e, "Weight"));
        for (Element w : Templates.children(e, "When")) {
            String side = w.getAttribute("side").trim().toLowerCase(Locale.ENGLISH);
            boolean us = side.equals("us") || side.isEmpty() && !d.targeted();
            DiplomacyConditionGroup c = Templates.conditions(w);
            if (c != null) {
                if (us) {
                    d.whenUs = and(d.whenUs, c);
                } else {
                    if (!d.targeted()) {
                        throw new ConfigParserException(d.id + ": " + d.objective + " has no target for a <When side=\"them\">");
                    }
                    d.whenThem = and(d.whenThem, c);
                }
            }
        }
        Element text = Templates.child(e, "Text");
        d.text = text != null ? text.getTextContent().trim() : "";
        for (Element q : Templates.children(e, "Quote")) {
            d.quotes.put(q.getAttribute("style").trim().toUpperCase(Locale.ENGLISH), q.getTextContent().trim());
        }
        return d;
    }

    private static DiplomacyConditionGroup and(DiplomacyConditionGroup a, DiplomacyConditionGroup b) {
        if (a == null) {
            return b;
        }
        a.conditions.add(b);
        return a;
    }

    /** Fills a template's tokens. */
    public static String fill(String raw, String target, int count, String treaty, int days) {
        return raw.replace("{target}", target).replace("{count}", String.valueOf(count))
                .replace("{treaty}", treaty).replace("{days}", String.valueOf(days));
    }

    // ---- the loaded set

    private static volatile List<Directive> all;

    /** Every directive, read on first use. A file that does not read leaves the shipped one; one bad entry is skipped. */
    public static List<Directive> all() {
        List<Directive> a = all;
        if (a == null) {
            a = load(Templates.root(FILE));
            all = a;
        }
        return a;
    }

    /** Server: read the file again, e.g. after an admin edited the override. */
    public static int reload() {
        all = null;
        return all().size();
    }

    static List<Directive> load(Element root) {
        List<Directive> out = new ArrayList<>();
        if (root == null) {
            return out;
        }
        for (Element e : Templates.children(root, "Directive")) {
            try {
                out.add(parse(e));
            } catch (Exception ex) {
                System.err.println("[BetterFactions] skipping directive '" + e.getAttribute("id") + "': " + ex.getMessage());
            }
        }
        return out;
    }

    /** The directives a seat of {@code role} may ask. */
    public static List<Directive> forRole(int role) {
        List<Directive> out = new ArrayList<>();
        for (Directive d : all()) {
            if (d.roles.contains(role)) {
                out.add(d);
            }
        }
        return out;
    }
}
