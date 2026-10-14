package videogoose.betterfactions.diplomacy;

import org.schema.game.common.data.player.faction.FactionRelation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every {@link TreatyType} the game knows, vanilla and modded.
 *
 * <p>Shaped like {@code PlanetTypeConfig}: a registration-ordered map keyed by namespaced id, with
 * vanilla registered at class init and mods pushing their own. Registration order is preserved so
 * the diplomacy UI can list types in a deliberate order rather than a hash order.</p>
 *
 * <p>{@link #unregisterAll()} exists because mod teardown has to be able to drop everything a mod
 * added; without it, disabling and re-enabling a mod stacks duplicates. Vanilla types are
 * re-seeded, so the registry is never left empty.</p>
 */
public final class TreatyTypeRegistry {

    /**
     * Namespace vanilla types register under.
     */
    public static final String VANILLA = "starmade";

    public static final String WAR = VANILLA + ":war";
    public static final String NON_AGGRESSION = VANILLA + ":non_aggression";
    public static final String DEFENSIVE_PACT = VANILLA + ":defensive_pact";
    public static final String ALLIANCE = VANILLA + ":alliance";
    public static final String TRADE_AGREEMENT = VANILLA + ":trade_agreement";
    public static final String INTEL_SHARING = VANILLA + ":intel_sharing";
    public static final String VASSALAGE = VANILLA + ":vassalage";
    public static final String TRIBUTARY = VANILLA + ":tributary";
    public static final String FEDERATION = VANILLA + ":federation";
    /** Closed trade: declared by one side, lifted only by it, and blocking trade between the two either way. */
    public static final String EMBARGO = VANILLA + ":embargo";
    /** Tariffs: the declarer's shops charge the other side's members more, and pay them less. */
    public static final String TARIFF = VANILLA + ":tariff";

    private static final Map<String, TreatyType> TYPES = new LinkedHashMap<>();

    static {
        registerVanilla();
    }

    private TreatyTypeRegistry() {
    }

    /**
     * The types the engine ships.
     *
     * <p>Opinion values line up with the status point tables the NPC configs already ship: being at
     * war is catastrophic, an alliance is strongly positive, a quiet pact is mild.</p>
     *
     * <p>The naming templates are read by {@link TreatyNames}. A war is remembered by who fought it
     * or by what it was fought over, a pact by where it was signed, and anything one-sided by the
     * side that gave something up. A template naming {@code {ceded}} is only a candidate when a
     * system actually changed hands, so a concession is named after the system that was conceded and
     * never after a capital - nobody hands over their home system, which is exactly why that is a
     * token of its own rather than a reuse of {@code {toHome}}.</p>
     *
     * <p>{@code federation} is never signed directly - it is synthesised from membership so that
     * {@code getRelation} has one code path rather than a membership special case bolted beside the
     * treaty one - so it carries no templates and falls back to its display name.</p>
     */
    private static void registerVanilla() {
        register(VANILLA, new TreatyType.Builder("war")
                .displayName("War").asymmetric().implies(FactionRelation.RType.ENEMY).unilateral().unbreakable()
                .names("The {from}-{to} War", "The {toHome} Campaign",
                        "The War for {ceded}", "The {ceded} War"));
        register(VANILLA, new TreatyType.Builder("non_aggression")
                .displayName("Non-Aggression Pact").implies(FactionRelation.RType.NEUTRAL).importance(20)
                .names("The {fromHome} Accord", "The Truce of {toHome}", "The {fromHome} Non-Aggression Pact",
                        "The {ceded} Concession", "The Concession of {ceded}"));
        register(VANILLA, new TreatyType.Builder("defensive_pact")
                .displayName("Defensive Pact").implies(FactionRelation.RType.FRIEND).importance(30)
                .names("The {fromHome} Pact", "The Shield of {toHome}", "The {from}-{to} Defensive Pact"));
        register(VANILLA, new TreatyType.Builder("alliance")
                .displayName("Alliance").implies(FactionRelation.RType.FRIEND).importance(50)
                .names("The {from}-{to} Alliance", "The Grand Alliance of {fromHome}", "The {fromHome} Concord"));
        register(VANILLA, new TreatyType.Builder("trade_agreement")
                .displayName("Trade Agreement").importance(10)
                .names("The {fromHome} Trade Compact", "The {from}-{to} Trade Agreement", "The {toHome} Charter"));
        register(VANILLA, new TreatyType.Builder("intel_sharing")
                .displayName("Intel Sharing").importance(10)
                .names("The {fromHome} Understanding", "The {from}-{to} Intelligence Accord"));
        register(VANILLA, new TreatyType.Builder("vassalage")
                .displayName("Vassalage").asymmetric().implies(FactionRelation.RType.FRIEND).importance(60)
                .names("The {to} Submission", "The Vassalage of {to}", "The {ceded} Settlement"));
        register(VANILLA, new TreatyType.Builder("tributary")
                .displayName("Tributary").asymmetric().implies(FactionRelation.RType.NEUTRAL).importance(40)
                .names("The {to} Tribute", "The {to} Indemnity", "The Tribute of {toHome}",
                        "The {ceded} Concession"));
        register(VANILLA, new TreatyType.Builder("embargo")
                .displayName("Trade Embargo").asymmetric().unilateral()
                .names("The {from} Embargo", "The Closing of {toHome}", "The {fromHome} Blockade"));
        register(VANILLA, new TreatyType.Builder("tariff")
                .displayName("Tariffs").asymmetric().unilateral()
                .names("The {from} Tariffs", "The {fromHome} Duties", "The {toHome} Levy"));
        register(VANILLA, new TreatyType.Builder("federation")
                .displayName("Federation").implies(FactionRelation.RType.FRIEND).unilateral().unbreakable());
    }

    /**
     * @param namespace {@link #VANILLA} for engine types, else the registering mod's id
     * @return the registered type
     * @throws IllegalArgumentException if the namespace or name is unusable, or the id is taken
     */
    public static synchronized TreatyType register(String namespace, TreatyType.Builder builder) {
        if (namespace == null || namespace.isEmpty() || namespace.indexOf(':') >= 0) {
            throw new IllegalArgumentException("treaty namespace must be non-empty and contain no ':': " + namespace);
        }
        String name = builder.getName();
        if (name == null || name.isEmpty() || name.indexOf(':') >= 0) {
            throw new IllegalArgumentException("treaty name must be non-empty and contain no ':': " + name);
        }
        TreatyType type = builder.build(namespace);
        TreatyType existing = TYPES.get(type.getId());
        if (existing != null) {
            throw new IllegalArgumentException("treaty type already registered: " + type.getId()
                    + " (by " + existing.getRegisteredBy() + ")");
        }
        TYPES.put(type.getId(), type);
        return type;
    }

    /**
     * @return null for an id nothing has registered - a modded type whose mod is gone, say.
     */
    public static synchronized TreatyType get(String id) {
        return TYPES.get(id);
    }

    /**
     * Every registered id, for the comment block a generated config file carries.
     */
    public static synchronized String list() {
        StringBuilder b = new StringBuilder();
        for (TreatyType t : all()) {
            if (!b.isEmpty()) {
                b.append(", ");
            }
            b.append(t.getId());
        }
        return b.toString();
    }

    public static synchronized boolean exists(String id) {
        return TYPES.containsKey(id);
    }

    /**
     * In registration order, vanilla first.
     */
    public static synchronized Collection<TreatyType> all() {
        return Collections.unmodifiableCollection(new LinkedHashMap<>(TYPES).values());
    }

    public static synchronized boolean unregister(String id) {
        return TYPES.remove(id) != null;
    }

    /**
     * Drops every mod-registered type and re-seeds vanilla. Called from mod teardown.
     */
    public static synchronized void unregisterAll() {
        TYPES.clear();
        registerVanilla();
    }
}
