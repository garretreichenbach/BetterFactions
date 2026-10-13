package videogoose.betterfactions.civics;

import java.util.Collections;
import java.util.LinkedHashSet;
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

    private CivicType(String id, String registeredBy, String displayName, String description, Set<String> excludes, String impliesGovernment, boolean choosesResource, Set<String> requires, Kind kind) {
        this.id = id;
        this.registeredBy = registeredBy;
        this.displayName = displayName;
        this.description = description;
        this.excludes = Collections.unmodifiableSet(excludes);
        this.impliesGovernment = impliesGovernment;
        this.choosesResource = choosesResource;
        this.requires = Collections.unmodifiableSet(requires);
        this.kind = kind;
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
     * Ids that must be held in another slot before this civic can be picked. ponytail: the HOLDS_CIVIC
     * case of #19's unlock rules; folds into {@code requires} conditions when those land.
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
                    impliesGovernment, choosesResource, qualify(requires, namespace), kind);
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
