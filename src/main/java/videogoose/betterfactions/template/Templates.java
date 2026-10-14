package videogoose.betterfactions.template;

import org.schema.common.config.ConfigParserException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.diplomacy.DiplomacyConditionGroup;
import videogoose.betterfactions.diplomacy.DiplomacyEntity;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The template core directives (Council agendas) and incidents share: where a template file is read from,
 * how its weight is scored, and which side a condition reads.
 *
 * <p>A file ships at {@code /data/<name>} in the jar and a server overrides it whole with
 * {@code moddata/BetterFactions/<name>}, as the NPC presets do.</p>
 */
public final class Templates {

    private Templates() {
    }

    /** The template file's root element: the server's override if there is one, else the shipped default; null if neither reads. */
    public static Element root(String name) {
        File override = new File("moddata/BetterFactions/" + name);
        for (boolean useOverride : new boolean[]{true, false}) {
            if (useOverride && !override.exists()) {
                continue;
            }
            try (InputStream in = useOverride ? new FileInputStream(override) : Templates.class.getResourceAsStream("/data/" + name)) {
                if (in != null) {
                    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in).getDocumentElement();
                }
            } catch (Exception e) {
                System.err.println("[BetterFactions] could not read " + (useOverride ? override.getPath() : name) + ": " + e);
            }
        }
        return null;
    }

    /** Direct child elements named {@code tag}, any case. */
    public static List<Element> children(Node parent, String tag) {
        List<Element> out = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element e && e.getNodeName().equalsIgnoreCase(tag)) {
                out.add(e);
            }
        }
        return out;
    }

    public static Element child(Node parent, String tag) {
        List<Element> c = children(parent, tag);
        return c.isEmpty() ? null : c.get(0);
    }

    /** The AND of a {@code <When>}'s conditions, or null for none. */
    public static DiplomacyConditionGroup conditions(Element holder) throws ConfigParserException {
        if (holder == null) {
            return null;
        }
        DiplomacyConditionGroup g = new DiplomacyConditionGroup();
        NodeList nodes = holder.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (n.getNodeName().equalsIgnoreCase("condition")) {
                g.conditions.add(videogoose.betterfactions.diplomacy.DiplomacyCondition.parseCondition(n));
            } else if (DiplomacyConditionGroup.canParse(n)) {
                g.conditions.add(DiplomacyConditionGroup.parse(n));
            }
        }
        return g.conditions.isEmpty() ? null : g;
    }

    /** {@code "3"} or {@code "2-4"}, as {lo, hi}; {@code def} when blank. */
    public static int[] range(String s, int def) throws ConfigParserException {
        if (s == null || s.isBlank()) {
            return new int[]{def, def};
        }
        try {
            int dash = s.indexOf('-', 1);
            int lo = Integer.parseInt(s.substring(0, dash < 0 ? s.length() : dash).trim());
            int hi = dash < 0 ? lo : Integer.parseInt(s.substring(dash + 1).trim());
            return new int[]{Math.min(lo, hi), Math.max(lo, hi)};
        } catch (NumberFormatException e) {
            throw new ConfigParserException("not a number or range: " + s);
        }
    }

    public static int intAttr(Element e, String name, int def) throws ConfigParserException {
        return range(e.getAttribute(name), def)[0];
    }

    /** Whose standing a condition reads: ours, theirs, or either one's. */
    public enum Side {
        US, THEM, EITHER;

        static Side of(String s) {
            return s == null || s.isBlank() ? US : valueOf(s.trim().toUpperCase(Locale.ENGLISH));
        }
    }

    /**
     * The ledger entry conditions are read through, so that they read {@code about}: the condition grammar
     * always asks about the entry's other faction (it was written for an NPC sizing up a proposer). Kept as
     * the {@code from} faction's view of {@code about}; one that does not exist yet is made up, not stored.
     * {@code about == from} works for conditions on a faction itself, such as {@code HOLDS_CIVIC}.
     */
    public static DiplomacyEntity entity(DiplomacyManager dm, int from, int about) {
        org.schema.game.common.data.player.faction.Faction f = ((org.schema.game.server.data.FactionState) dm.getState()).getFactionManager().getFaction(from);
        DiplomacyEntity e = f != null ? dm.diplomacyOf(f).entities.get((long) about) : null;
        return e != null ? e : new DiplomacyEntity(dm, from, about);
    }

    /**
     * A template's weight: a base times each modifier whose condition holds on its side. A multiplied 0
     * stays 0, so a modifier on a base-0 weight first makes it 1: base 0 means "only when a modifier says so".
     *
     * <p>ponytail: simple product; swap for additive scoring if packs find it hard to tune.</p>
     */
    public static final class Weight {
        public final double base;
        final List<Side> sides = new ArrayList<>();
        final List<Double> factors = new ArrayList<>();
        final List<DiplomacyConditionGroup> when = new ArrayList<>();

        public Weight(double base) {
            this.base = base;
        }

        public static Weight parse(Element e) throws ConfigParserException {
            if (e == null) {
                return new Weight(1);
            }
            Weight w = new Weight(e.hasAttribute("base") ? Double.parseDouble(e.getAttribute("base")) : 1);
            for (Element m : children(e, "Modifier")) {
                DiplomacyConditionGroup c = conditions(m);
                if (c == null) {
                    throw new ConfigParserException("a weight modifier needs a condition");
                }
                w.add(Side.of(m.getAttribute("side")), m.hasAttribute("factor") ? Double.parseDouble(m.getAttribute("factor")) : 1, c);
            }
            return w;
        }

        public Weight add(Side side, double factor, DiplomacyConditionGroup c) {
            sides.add(side);
            factors.add(factor);
            when.add(c);
            return this;
        }

        /**
         * The weight between us and them.
         *
         * @param us   whether a condition holds on our side
         * @param them the same on theirs; null when there is no other side, and their modifiers never apply
         */
        public double of(java.util.function.Predicate<DiplomacyConditionGroup> us, java.util.function.Predicate<DiplomacyConditionGroup> them) {
            double w = base;
            for (int i = 0; i < when.size(); i++) {
                DiplomacyConditionGroup c = when.get(i);
                boolean holds = switch (sides.get(i)) {
                    case US -> us.test(c);
                    case THEM -> them != null && them.test(c);
                    case EITHER -> us.test(c) || them != null && them.test(c);
                };
                if (holds) {
                    w = (w == 0 ? 1 : w) * factors.get(i);
                }
            }
            return w;
        }
    }

    /** The index {@code roll} (0 to the sum) lands on, walking the weights. */
    public static int pick(double[] weights, double roll) {
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return i;
            }
        }
        return weights.length - 1;
    }
}
