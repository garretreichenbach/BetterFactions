package videogoose.betterfactions.civics;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static videogoose.betterfactions.civics.CivicPayouts.Event.*;
import static videogoose.betterfactions.civics.CivicPayouts.Filter.*;

/**
 * Every {@link CivicType}, ours and modded, in registration order (the order the chooser lists them).
 * Shaped like {@code TreatyTypeRegistry}, including {@link #unregisterAll} for mod teardown.
 */
public final class CivicRegistry {

    public static final String NAMESPACE = "betterfactions";

    public static final String HOLY_LEADER = NAMESPACE + ":holy_leader";
    public static final String CORPORATE_CHAINS = NAMESPACE + ":corporate_chains";
    public static final String WARRIOR_CODE = NAMESPACE + ":warrior_code";
    public static final String OATHKEEPERS = NAMESPACE + ":oathkeepers";
    public static final String EXPANSIONISTS = NAMESPACE + ":expansionists";
    public static final String ISOLATIONISTS = NAMESPACE + ":isolationists";
    public static final String DIPLOMATIC_CORPS = NAMESPACE + ":diplomatic_corps";
    public static final String RAIDERS = NAMESPACE + ":raiders";
    public static final String SPICE_DEALER = NAMESPACE + ":spice_dealer";
    public static final String RESOURCE_SPECIALIST = NAMESPACE + ":resource_specialist";
    public static final String SCAVENGERS = NAMESPACE + ":scavengers";
    public static final String MINER_GUILDS = NAMESPACE + ":miner_guilds";
    public static final String PACIFISTS = NAMESPACE + ":pacifists";
    public static final String ELITE_CREWS = NAMESPACE + ":elite_crews";
    public static final String HORDE_TACTICS = NAMESPACE + ":horde_tactics";
    public static final String FAMOUS_ADMIRALS = NAMESPACE + ":famous_admirals";
    public static final String SENATE = NAMESPACE + ":senate";
    public static final String COUNCIL = NAMESPACE + ":council";
    public static final String INDUSTRIALIST = NAMESPACE + ":industrialist";
    public static final String RELIGIOUS = NAMESPACE + ":religious";

    private static final Map<String, CivicType> TYPES = new LinkedHashMap<>();
    /** Ids {@code civics.xml} registered or retuned, undone before it is applied again. */
    private static final java.util.Set<String> FROM_DATA = new java.util.HashSet<>();
    private static final java.util.Set<String> RETUNED = new java.util.HashSet<>();

    static {
        registerOwn();
    }

    private CivicRegistry() {
    }

    private static void registerOwn() {
        //payout values are defaults; civics.xml retunes them per server
        register(NAMESPACE, new CivicType.Builder("holy_leader").displayName("Holy Leader").impliesGovernment("Holy Empire").requires("religious")
                .payout(PLAYER_KILLED, 100, 500, BY_LEADER, VICTIM_AT_WAR, FAIR_KILL)
                .payout(PLAYER_LOST, -200, 0, BY_LEADER)
                .handler(CivicPayouts::holyLeader)
                .description("Ruled by a self-declared god. The leader's kills of enemy players earn faction points; the leader's death costs them."));
        register(NAMESPACE, new CivicType.Builder("corporate_chains").displayName("Corporate Chains")
                .description("Trade through our shops and our trade agreements earn faction points."));
        register(NAMESPACE, new CivicType.Builder("warrior_code").displayName("Warrior Code")
                .payout(SHIP_DESTROYED, 10, 300, VICTIM_AT_WAR)
                .payout(SHIP_DESTROYED, 10, 300, VICTIM_AT_WAR, WITH_CASUS_BELLI)
                .description("Enemy ships destroyed in a declared war earn faction points, more when the war had a just cause."));
        register(NAMESPACE, new CivicType.Builder("oathkeepers").displayName("Oathkeepers")
                .payout(TREATY_KEPT, 150, 450)
                .payout(CALL_TO_ARMS_ANSWERED, 100, 300)
                .payout(CALL_TO_ARMS_REFUSED, -200, 0)
                .description("Treaties held to term and calls to arms answered earn faction points. Refusing a call costs double."));
        register(NAMESPACE, new CivicType.Builder("expansionists").displayName("Expansionists")
                .payout(SYSTEM_CLAIMED, 100, 500)
                .payout(SYSTEM_LOST, -100, 0)
                .description("Every newly claimed system earns faction points; losing one costs them."));
        register(NAMESPACE, new CivicType.Builder("isolationists").displayName("Isolationists")
                .payout(DAILY, 50, 0, ONLY_NON_AGGRESSION)
                .description("Days spent with no treaty beyond non-aggression earn faction points.")
                .excludes("diplomatic_corps", "expansionists"));
        register(NAMESPACE, new CivicType.Builder("diplomatic_corps").displayName("Diplomatic Corps")
                .payout(TREATY_SIGNED, 50, 200)
                .description("Signing treaties earns faction points, and other factions' opinion of us fades more slowly."));
        register(NAMESPACE, new CivicType.Builder("raiders").displayName("Raiders")
                .payout(SHIP_DESTROYED, 15, 300, VICTIM_NOT_AT_WAR)
                .handler(CivicPayouts::raiders)
                .description("Destroying ships of factions we are not at war with earns faction points. Every victim gains a just cause against us."));
        register(NAMESPACE, new CivicType.Builder("spice_dealer").displayName("Neighbourhood Spice Dealer")
                .description("Other factions pay more faction point upkeep on their systems that border ours."));
        register(NAMESPACE, new CivicType.Builder("resource_specialist").displayName("Resource Specialist").choosesResource()
                .description("Pick one resource when choosing this civic. Systems rich in it are a war goal; mining it yields more and other resources less."));
        register(NAMESPACE, new CivicType.Builder("industrialist").displayName("Industrialist")
                .description("Ship mass built at our shipyards earns faction points. Religious factions think less of us."));
        register(NAMESPACE, new CivicType.Builder("religious").displayName("Religious")
                .description("Other Religious factions think better of us, and Industrialists worse. Industrialists are a just cause for war.")
                .excludes("industrialist"));
        register(NAMESPACE, new CivicType.Builder("scavengers").displayName("Scavengers")
                .description("Salvage beams on wrecks and overheating ships yield double resources."));
        register(NAMESPACE, new CivicType.Builder("miner_guilds").displayName("Miner Guilds")
                .description("The guilds set mining goals. Sacrifice the resources they ask for to earn faction points."));
        register(NAMESPACE, new CivicType.Builder("pacifists").displayName("Pacifists")
                .description("Planets and stations count double toward our strength, and fleets count half. Fleets cost double faction point upkeep.")
                .excludes("raiders", "warrior_code"));

        //fleet doctrines: one per faction, in its own slot
        register(NAMESPACE, new CivicType.Builder("elite_crews").displayName("Elite Crews").doctrine()
                .description("Fewer, better-kept ships. Shipyard repairs are cheaper and faster; fleet upkeep per mass is higher."));
        register(NAMESPACE, new CivicType.Builder("horde_tactics").displayName("Horde Tactics").doctrine()
                .description("Many cheap ships. Fleet upkeep per mass is lower and shipyard builds cost less; repairs cost more."));
        register(NAMESPACE, new CivicType.Builder("famous_admirals").displayName("Famous Admirals").doctrine()
                .description("Captains over fleets. A ship a player flies pays no fleet upkeep; AI-run fleet ships cost more."));

        //institutions: any number, each adding upkeep
        register(NAMESPACE, new CivicType.Builder("senate").displayName("Senate").institution()
                .description("Our members sit as senators, and any of them may propose. Proposing, accepting or countering a treaty, declaring war and breaking a treaty all go to a Senate vote."));
        register(NAMESPACE, new CivicType.Builder("council").displayName("Council").institution()
                .description("A ring of councilors, each with an agenda of their own: goals against real factions, paid in faction points, fail or refuse one and we pay. Our civics decide who they are. With a Senate, members can be voted onto it."));
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
        FROM_DATA.clear();
        RETUNED.clear();
        registerOwn();
    }

    /** Undoes what the last {@code civics.xml} did: its civics go, retuned payouts return to as built. */
    static synchronized void resetData() {
        FROM_DATA.forEach(TYPES::remove);
        FROM_DATA.clear();
        for (String id : RETUNED) {
            CivicType t = TYPES.get(id);
            if (t != null) {
                t.overridePayouts(null);
            }
        }
        RETUNED.clear();
    }

    static synchronized void registerData(String namespace, CivicType.Builder b) {
        FROM_DATA.add(register(namespace, b).getId());
    }

    static synchronized void overridePayouts(String id, java.util.List<CivicPayouts.Payout> payouts) {
        TYPES.get(id).overridePayouts(payouts);
        RETUNED.add(id);
    }
}
