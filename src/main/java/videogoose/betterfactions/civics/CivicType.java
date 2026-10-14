package videogoose.betterfactions.civics;

import org.schema.schine.common.language.Lng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A trait a faction picks for itself: Holy Leader, Raiders, Oathkeepers.
 *
 * <p>Same shape as {@code TreatyType}: a registry object with a namespaced id, so mods can add their
 * own and a save names civics by id. The handler (what a civic pays out for) and the government rules
 * come in later phases; see {@code docs/design/CIVICS_PLAN.md}.</p>
 */
public final class CivicType {

    private final String id;
    private final String registeredBy;
    private final String displayName;
    private final String description;
    private final Set<String> excludes;
    private final String impliesGovernment;
    private final boolean choosesResource;
    private final Set<String> requires;
    private final Kind kind;
    private final List<Unlock> unlocks;
    /** Replaced whole by {@code civics.xml}, never edited in place, so a reader holds a consistent list. */
    private volatile List<CivicPayouts.Payout> payouts;
    private final List<CivicPayouts.Payout> builtPayouts;
    private final CivicPayouts.CivicHandler handler;

    private CivicType(String id, String registeredBy, String displayName, String description, Set<String> excludes, String impliesGovernment, boolean choosesResource, Set<String> requires, Kind kind, List<Unlock> unlocks,
                      List<CivicPayouts.Payout> payouts, CivicPayouts.CivicHandler handler) {
        this.id = id;
        this.registeredBy = registeredBy;
        this.displayName = displayName;
        this.description = description;
        this.excludes = Collections.unmodifiableSet(excludes);
        this.impliesGovernment = impliesGovernment;
        this.choosesResource = choosesResource;
        this.requires = Collections.unmodifiableSet(requires);
        this.kind = kind;
        this.unlocks = List.copyOf(unlocks);
        this.builtPayouts = List.copyOf(payouts);
        this.payouts = builtPayouts;
        this.handler = handler;
    }

    /** What it pays out for: as registered, unless {@code civics.xml} replaced them. */
    public List<CivicPayouts.Payout> getPayouts() {
        return payouts;
    }

    void overridePayouts(List<CivicPayouts.Payout> p) {
        payouts = p == null ? builtPayouts : List.copyOf(p);
    }

    /** The code side, for what its payouts cannot express; null for none. */
    public CivicPayouts.CivicHandler getHandler() {
        return handler;
    }

    /**
     * A rule a faction must meet to pick this civic, beside {@link #getRequires} (the HOLDS_CIVIC rule).
     * All of a civic's rules must hold. Checked only when picking: a faction that later falls below one
     * keeps the civic.
     */
    public record Unlock(Rule rule, int value) {
        public enum Rule {
            STRENGTH_AT_LEAST,
            HELD_TREATIES_AT_LEAST,
            WARS_WON_AT_LEAST,
            CLAIMED_SYSTEMS_AT_LEAST,
            /** Given to a faction by an admin with {@code /faction_civic_grant}; value unused. */
            ADMIN_GRANTED
        }

        /** Whether a faction standing as {@code s} meets it. */
        public boolean met(String civic, Standing s) {
            return switch (rule) {
                case STRENGTH_AT_LEAST -> s.strength() >= value;
                case HELD_TREATIES_AT_LEAST -> s.treaties() >= value;
                case WARS_WON_AT_LEAST -> s.warsWon() >= value;
                case CLAIMED_SYSTEMS_AT_LEAST -> s.systems() >= value;
                case ADMIN_GRANTED -> s.granted().contains(civic);
            };
        }

        /** What it asks for, as the chooser shows it. */
        public String describe() {
            return switch (rule) {
                case STRENGTH_AT_LEAST -> Lng.str("Strength %s or more.", value);
                case HELD_TREATIES_AT_LEAST -> Lng.str("%s treaties held at once.", value);
                case WARS_WON_AT_LEAST -> Lng.str("%s wars won.", value);
                case CLAIMED_SYSTEMS_AT_LEAST -> Lng.str("%s claimed systems.", value);
                case ADMIN_GRANTED -> Lng.str("Granted by an administrator.");
            };
        }
    }

    /** What the unlock rules measure about one faction, taken when it picks. */
    public record Standing(double strength, int treaties, int warsWon, int systems, Set<String> granted) {
        public static final Standing NONE = new Standing(0, 0, 0, 0, Set.of());
    }

    public List<Unlock> getUnlocks() {
        return unlocks;
    }

    /** The first unlock rule {@code s} does not meet, described; null when every one is met. */
    public String locked(Standing s) {
        for (Unlock u : unlocks) {
            if (!u.met(id, s)) {
                return u.describe();
            }
        }
        return null;
    }

    /** What a faction holds this as: a civic in a slot, its one fleet doctrine, or one of its institutions. */
    public enum Kind {
        CIVIC,
        /** Held in the faction's one doctrine slot ({@link FactionCivics#DOCTRINE}), never a civic slot. */
        DOCTRINE,
        /** Adopted or dropped freely, any number at once, each adding upkeep ({@link FactionCivics#institutions}). */
        INSTITUTION
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isDoctrine() {
        return kind == Kind.DOCTRINE;
    }

    /**
     * Ids that must be held in another slot before this civic can be picked: the HOLDS_CIVIC unlock rule.
     * Swapping one of them out drops this civic too ({@link FactionCivics#set}).
     */
    public Set<String> getRequires() {
        return requires;
    }

    /** Whether picking this civic also picks a resource, kept in {@link FactionCivics#resource}. */
    public boolean choosesResource() {
        return choosesResource;
    }

    /** Namespaced, stable, and what gets written to disk and the wire. */
    public String getId() {
        return id;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /** Ids this civic cannot be held alongside. Either side naming the other is enough. */
    public Set<String> getExcludes() {
        return excludes;
    }

    /**
     * The government this civic brings with it, or null. ponytail: a display name until government types
     * exist (#9), then a {@code GovernmentType} id.
     */
    public String getImpliesGovernment() {
        return impliesGovernment;
    }

    public boolean conflictsWith(CivicType other) {
        return excludes.contains(other.id) || other.excludes.contains(id);
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CivicType && id.equals(((CivicType) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static final class Builder {
        private final String name;
        private String displayName;
        private String description = "";
        private final Set<String> excludes = new LinkedHashSet<>();
        private String impliesGovernment;
        private boolean choosesResource;
        private final Set<String> requires = new LinkedHashSet<>();
        private Kind kind = Kind.CIVIC;
        private final List<Unlock> unlocks = new ArrayList<>();
        private final List<CivicPayouts.Payout> payouts = new ArrayList<>();
        private CivicPayouts.CivicHandler handler;

        public Builder(String name) {
            this.name = name;
            displayName = name;
        }

        public Builder displayName(String v) {
            displayName = v;
            return this;
        }

        public Builder description(String v) {
            description = v;
            return this;
        }

        /** @param ids namespaced ids, or bare names meaning the registering namespace */
        public Builder excludes(String... ids) {
            Collections.addAll(excludes, ids);
            return this;
        }

        public Builder impliesGovernment(String v) {
            impliesGovernment = v;
            return this;
        }

        /** @param ids namespaced ids, or bare names meaning the registering namespace */
        public Builder requires(String... ids) {
            Collections.addAll(requires, ids);
            return this;
        }

        /** Adds an unlock rule; every one added must hold. */
        public Builder unlock(Unlock.Rule rule, int value) {
            unlocks.add(new Unlock(rule, value));
            return this;
        }

        /**
         * On {@code event}, if every filter in {@code where} holds, pays {@code fp} faction points (negative
         * to charge), at most {@code dailyCap} a day; 0 for no cap. Servers can retune it in {@code civics.xml}.
         */
        public Builder payout(CivicPayouts.Event event, int fp, int dailyCap, CivicPayouts.Filter... where) {
            Set<CivicPayouts.Filter> w = java.util.EnumSet.noneOf(CivicPayouts.Filter.class);
            Collections.addAll(w, where);
            payouts.add(new CivicPayouts.Payout(event, Collections.unmodifiableSet(w), fp, dailyCap));
            return this;
        }

        public Builder handler(CivicPayouts.CivicHandler h) {
            handler = h;
            return this;
        }

        /** Only factions an admin has granted it to may pick it. */
        public Builder adminGranted() {
            return unlock(Unlock.Rule.ADMIN_GRANTED, 0);
        }

        public Builder doctrine() {
            kind = Kind.DOCTRINE;
            return this;
        }

        public Builder institution() {
            kind = Kind.INSTITUTION;
            return this;
        }

        public Builder choosesResource() {
            choosesResource = true;
            return this;
        }

        CivicType build(String namespace) {
            return new CivicType(namespace + ":" + name, namespace, displayName, description, qualify(excludes, namespace),
                    impliesGovernment, choosesResource, qualify(requires, namespace), kind, unlocks, payouts, handler);
        }

        private static Set<String> qualify(Set<String> ids, String namespace) {
            Set<String> out = new LinkedHashSet<>();
            for (String e : ids) {
                out.add(e.indexOf(':') >= 0 ? e : namespace + ":" + e);
            }
            return out;
        }

        String getName() {
            return name;
        }
    }
}
