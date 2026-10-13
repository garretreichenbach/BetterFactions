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

    private CivicType(String id, String registeredBy, String displayName, String description, Set<String> excludes, String impliesGovernment) {
        this.id = id;
        this.registeredBy = registeredBy;
        this.displayName = displayName;
        this.description = description;
        this.excludes = Collections.unmodifiableSet(excludes);
        this.impliesGovernment = impliesGovernment;
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

        CivicType build(String namespace) {
            Set<String> ex = new LinkedHashSet<>();
            for (String e : excludes) {
                ex.add(e.indexOf(':') >= 0 ? e : namespace + ":" + e);
            }
            return new CivicType(namespace + ":" + name, namespace, displayName, description, ex, impliesGovernment);
        }

        String getName() {
            return name;
        }
    }
}
