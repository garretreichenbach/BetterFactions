package videogoose.betterfactions.diplomacy;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

/**
 * Names an agreement, when nobody named it themselves.
 *
 * <p>A treaty's identity is its type and its signatories, which makes every non-aggression pact in
 * the galaxy read as "Non-Aggression Pact" - true, and useless the moment a faction holds more than
 * one of anything. Real agreements are named after something: who conceded, where it was signed,
 * what changed hands. So each {@link TreatyType} carries a handful of templates and the engine fills
 * them in from what the signing actually involved.</p>
 *
 * <h2>Tokens</h2>
 * <table>
 * <tr><td>{@code {from}}</td><td>the proposing faction, or the senior side of an asymmetric treaty
 * (the overlord)</td></tr>
 * <tr><td>{@code {to}}</td><td>the other signatory, or the junior side (the vassal, the tributary,
 * the side making a concession)</td></tr>
 * <tr><td>{@code {fromHome}} / {@code {toHome}}</td><td>the star system each side's home base sits
 * in - the closest thing the game has to "where it was signed"</td></tr>
 * <tr><td>{@code {ceded}}</td><td>the system that actually changed hands, or that the war was
 * fought over. <b>Never a home system</b>, which is the whole reason it is a token of its own</td></tr>
 * <tr><td>{@code {type}}</td><td>the type's own display name</td></tr>
 * </table>
 *
 * <h2>A template only runs if its tokens mean something</h2>
 * A faction does not concede its capital, so "The {@code {toHome}} Concession" would name an
 * agreement after a system that by definition did not change hands. Templates therefore declare what
 * they need by using it: a template naming {@code {ceded}} is only a candidate when a system really
 * was ceded, and is skipped otherwise. That is what lets a type carry both a plain name and a name
 * for the version of itself that came out of a peace settlement, without the second one lying.
 *
 * <p>{@code {fromHome}} and {@code {toHome}} are softer - a faction with no home base still signed
 * the thing somewhere, so those fall back to the faction's own name rather than disqualifying the
 * template.</p>
 *
 * <p>Deliberately pure: the caller resolves the tokens and this decides nothing else, which is what
 * lets a self-check cover the substitution, the filtering and the fallbacks without a galaxy.</p>
 */
public final class TreatyNames {

    /**
     * Long enough for "The Kepler Reaches Non-Aggression Accord", short enough to render in a list
     * row without eating the columns beside it.
     */
    public static final int MAX_LENGTH = 48;

    /**
     * Tokens that disqualify a template when nothing fills them, rather than falling back.
     */
    private static final String[] REQUIRED = {"{ceded}"};

    private TreatyNames() {
    }

    /**
     * The name a treaty gets when its proposer did not write one.
     *
     * @param seed decides which of the candidate templates is used; pass something stable for the
     *             agreement (the signing time works) so the name does not change under the players
     * @return a non-empty name, always
     */
    public static String defaultName(TreatyType type, long seed, Tokens tokens) {
        return type == null ? "Agreement" : defaultName(type, type.getNameTemplates(), seed, tokens);
    }

    /**
     * A war a bound faction declares on the one holding it. {@code {from}} is the rebel.
     */
    public static final String[] INDEPENDENCE_WAR = {"The {from} War of Independence",
            "The {fromHome} Rebellion", "The {from} Uprising"};
    /**
     * A war declared to make the target a vassal. {@code {to}} is the one to be subjugated.
     */
    public static final String[] SUBJUGATION_WAR = {"The Subjugation of {to}", "The Conquest of {toHome}",
            "The {to} Subjugation"};

    /**
     * As {@link #defaultName(TreatyType, long, Tokens)}, but from templates the caller picked because
     * of who the signatories are to each other, rather than the type's own.
     */
    public static String defaultName(TreatyType type, String[] templates, long seed, Tokens tokens) {
        Tokens t = tokens == null ? new Tokens() : tokens;
        List<String> usable = candidates(templates, t);
        if (usable.isEmpty()) {
            return type.getDisplayName();
        }
        String from = orElse(t.from, "Someone");
        String to = orElse(t.to, "Someone Else");
        String out = usable.get(Math.floorMod(seed, usable.size()))
                .replace("{fromHome}", orElse(t.fromHome, from))
                .replace("{toHome}", orElse(t.toHome, to))
                .replace("{ceded}", orElse(t.ceded, ""))
                .replace("{from}", from)
                .replace("{to}", to)
                .replace("{type}", type.getDisplayName());
        String sanitized = sanitize(out);
        return sanitized != null ? sanitized : type.getDisplayName();
    }

    /**
     * Normalizes a name that came from a client into either a usable one or null, which means "use
     * the generated name". Same trust boundary as a renamed star system: this string renders in
     * other players' lists, so it may not carry line breaks or run unbounded.
     */
    public static String sanitize(String name) {
        if (name == null) {
            return null;
        }
        StringBuilder b = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            b.append(c < ' ' ? ' ' : c);
        }
        String s = b.toString().trim();
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH).trim();
        }
        return s.isEmpty() ? null : s;
    }

    /**
     * The templates whose required tokens this signing can actually fill.
     */
    private static List<String> candidates(String[] templates, Tokens t) {
        List<String> out = new ObjectArrayList<String>(templates.length);
        for (String template : templates) {
            if (usable(template, t)) {
                out.add(template);
            }
        }
        return out;
    }

    private static boolean usable(String template, Tokens t) {
        for (String token : REQUIRED) {
            if (template.contains(token) && orElse(valueOf(token, t), "").isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String valueOf(String token, Tokens t) {
        return "{ceded}".equals(token) ? t.ceded : null;
    }

    private static String orElse(String value, String fallback) {
        String s = value == null ? null : value.trim();
        return s == null || s.isEmpty() ? fallback : s;
    }

    /**
     * What a particular signing has to name itself after.
     *
     * <p>Plain fields rather than a constructor: the list grows as the settlement terms in
     * {@code Phase 5} give the engine more to name an agreement from, and a caller that knows only
     * some of them should not have to pass nulls positionally for the rest.</p>
     */
    public static final class Tokens {

        /**
         * The proposing faction, or the senior side of an asymmetric treaty.
         */
        public String from;
        /**
         * The other signatory, or the junior side.
         */
        public String to;
        /**
         * The system holding {@link #from}'s home base.
         */
        public String fromHome;
        /**
         * The system holding {@link #to}'s home base.
         */
        public String toHome;
        /**
         * The system that changed hands, or that the war names as its goal. Never a home system - a
         * faction does not concede its capital - so leaving this null is what tells a concession
         * template it has nothing to name and should stand aside.
         */
        public String ceded;

        public Tokens from(String v) {
            from = v;
            return this;
        }

        public Tokens to(String v) {
            to = v;
            return this;
        }

        public Tokens homes(String fromHome, String toHome) {
            this.fromHome = fromHome;
            this.toHome = toHome;
            return this;
        }

        public Tokens ceded(String v) {
            ceded = v;
            return this;
        }
    }
}
