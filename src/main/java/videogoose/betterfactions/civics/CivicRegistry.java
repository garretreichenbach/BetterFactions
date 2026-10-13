package videogoose.betterfactions.civics;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every {@link CivicType}, ours and modded, in registration order (the order the chooser lists them).
 * Shaped like {@code TreatyTypeRegistry}, including {@link #unregisterAll} for mod teardown.
 */
public final class CivicRegistry {

    public static final String NAMESPACE = "betterfactions";

    public static final String HOLY_LEADER = NAMESPACE + ":holy_leader";
    public static final String SHADOW_COUNCIL = NAMESPACE + ":shadow_council";
    public static final String CORPORATE_CHAINS = NAMESPACE + ":corporate_chains";
    public static final String WARRIOR_CODE = NAMESPACE + ":warrior_code";
    public static final String OATHKEEPERS = NAMESPACE + ":oathkeepers";
    public static final String EXPANSIONISTS = NAMESPACE + ":expansionists";
    public static final String ISOLATIONISTS = NAMESPACE + ":isolationists";
    public static final String DIPLOMATIC_CORPS = NAMESPACE + ":diplomatic_corps";
    public static final String RAIDERS = NAMESPACE + ":raiders";

    private static final Map<String, CivicType> TYPES = new LinkedHashMap<>();

    static {
        registerOwn();
    }

    private CivicRegistry() {
    }

    private static void registerOwn() {
        register(NAMESPACE, new CivicType.Builder("holy_leader").displayName("Holy Leader").impliesGovernment("Holy Empire")
                .description("Ruled by a self-declared god. The leader's kills of enemy players earn faction points; the leader's death costs them."));
        register(NAMESPACE, new CivicType.Builder("shadow_council").displayName("Shadow Council")
                .description("A council in the shadows issues directives. Fulfil them for faction points; fail or refuse them and pay."));
        register(NAMESPACE, new CivicType.Builder("corporate_chains").displayName("Corporate Chains")
                .description("Trade through our shops and our trade agreements earn faction points."));
        register(NAMESPACE, new CivicType.Builder("warrior_code").displayName("Warrior Code")
                .description("Enemy ships destroyed in a declared war earn faction points, more when the war had a just cause."));
        register(NAMESPACE, new CivicType.Builder("oathkeepers").displayName("Oathkeepers")
                .description("Treaties held to term and calls to arms answered earn faction points. Refusing a call costs double."));
        register(NAMESPACE, new CivicType.Builder("expansionists").displayName("Expansionists")
                .description("Every newly claimed system earns faction points; losing one costs them."));
        register(NAMESPACE, new CivicType.Builder("isolationists").displayName("Isolationists")
                .description("Days spent with no treaty beyond non-aggression earn faction points.")
                .excludes("diplomatic_corps"));
        register(NAMESPACE, new CivicType.Builder("diplomatic_corps").displayName("Diplomatic Corps")
                .description("Signing treaties earns faction points, and other factions' opinion of us fades more slowly."));
        register(NAMESPACE, new CivicType.Builder("raiders").displayName("Raiders")
                .description("Destroying ships of factions we are not at war with earns faction points. Every victim gains a just cause against us."));
    }

    /**
     * @param namespace {@link #NAMESPACE} for ours, else the registering mod's id
     * @throws IllegalArgumentException if the namespace or name is unusable, or the id is taken
     */
    public static synchronized CivicType register(String namespace, CivicType.Builder builder) {
        if (namespace == null || namespace.isEmpty() || namespace.indexOf(':') >= 0) {
            throw new IllegalArgumentException("civic namespace must be non-empty and contain no ':': " + namespace);
        }
        String name = builder.getName();
        if (name == null || name.isEmpty() || name.indexOf(':') >= 0) {
            throw new IllegalArgumentException("civic name must be non-empty and contain no ':': " + name);
        }
        CivicType type = builder.build(namespace);
        CivicType existing = TYPES.get(type.getId());
        if (existing != null) {
            throw new IllegalArgumentException("civic already registered: " + type.getId() + " (by " + existing.getRegisteredBy() + ")");
        }
        TYPES.put(type.getId(), type);
        return type;
    }

    /** @return null for an id nothing has registered, such as a modded civic whose mod is gone */
    public static synchronized CivicType get(String id) {
        return TYPES.get(id);
    }

    /** The display name, or the bare id for a civic whose mod is gone. */
    public static String nameOf(String id) {
        CivicType t = get(id);
        return t != null ? t.getDisplayName() : id;
    }

    public static synchronized Collection<CivicType> all() {
        return Collections.unmodifiableCollection(new LinkedHashMap<>(TYPES).values());
    }

    /** Drops every mod-registered civic and re-seeds ours. Called from mod teardown. */
    public static synchronized void unregisterAll() {
        TYPES.clear();
        registerOwn();
    }
}
