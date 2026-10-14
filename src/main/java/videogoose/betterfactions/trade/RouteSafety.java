package videogoose.betterfactions.trade;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.trade.TradeActive;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.diplomacy.Reputation;
import videogoose.betterfactions.net.BFPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * How safe each system is to trade through, and so each route. A system is safer the more deliveries pass
 * through it, and the nearer it lies to the territory of a reputable faction that is not given to aggression;
 * it is less safe the more it has been raided. Traffic and raids fade by the day. A route is as safe as the
 * least safe system on the straight line a delivery flies.
 *
 * <p>Server: counts traffic and raids, and rolls a pirate ambush whenever a delivery enters a system, likelier
 * the less safe it is. Public: the ingredients go to every client, which works out any route it asks about.</p>
 */
public final class RouteSafety {

    private static final RouteSafety SERVER = new RouteSafety();
    private static final RouteSafety CLIENT = new RouteSafety();

    public static RouteSafety server() {
        return SERVER;
    }

    public static RouteSafety client() {
        return CLIENT;
    }

    static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long SAMPLE_MS = 30_000L;
    private static final long PROTECTION_MS = 5 * 60_000L;
    private static final long SYNC_MS = 2 * 60_000L;
    /** Traffic, in delivery-samples (one per delivery per 30 s in the system), at which traffic gives half its share. */
    static final double TRAFFIC_HALF = 200;
    /** Raided ships at which raids take half of what they can. */
    static final double RAID_HALF = 3;
    static final double KEEP_PER_DAY = 0.9;
    /** Systems away from reputable territory at which its protection runs out. */
    static final int PROTECT_REACH = 4;
    /** Infamy and aggression a faction may carry and still protect the space around it. */
    static final double REPUTABLE_BELOW = 3;

    /** Per system: {traffic, raids}. */
    private final Map<Vector3i, double[]> systems = new HashMap<>();
    /** Systems owned by a reputable, peaceable faction. */
    private Set<Vector3i> protectedSystems = new HashSet<>();
    private long lastSample;
    private long lastProtection;
    private long lastSync;
    private int version;
    /** Server: the system each delivery's convoy was last seen in, so entering a new one is noticed. */
    private final Map<Long, Vector3i> convoyAt = new HashMap<>();

    // ---- rules, kept pure for RouteSafety_test

    /**
     * A system's safety, 0 to 1: a floor, plus up to 0.45 for traffic and 0.4 for nearby reputable territory
     * ({@code protectedAt} systems away, or -1 for none in reach); raids then take up to 80% of that away.
     */
    static double safety(double traffic, double raids, int protectedAt) {
        double t = traffic / (traffic + TRAFFIC_HALF);
        double p = protectedAt < 0 ? 0 : Math.max(0, 1 - (double) protectedAt / PROTECT_REACH);
        double r = raids / (raids + RAID_HALF);
        return Math.min(1, 0.15 + 0.45 * t + 0.4 * p) * (1 - 0.8 * r);
    }

    /** The systems on the straight line between two, both ends included, one step at a time along the longest axis. */
    static List<Vector3i> systemsAlong(Vector3i from, Vector3i to) {
        int dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        int steps = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        List<Vector3i> out = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double f = steps == 0 ? 0 : (double) i / steps;
            Vector3i v = new Vector3i((int) Math.round(from.x + dx * f), (int) Math.round(from.y + dy * f), (int) Math.round(from.z + dz * f));
            if (out.isEmpty() || !out.get(out.size() - 1).equals(v)) {
                out.add(v);
            }
        }
        return out;
    }

    /** The chance an ambush waits as a delivery enters a system this safe: none when safe, the most when not at all. */
    static double ambushChance(double safety, int maxPercent) {
        double danger = 1 - Math.max(0, Math.min(1, safety));
        return maxPercent / 100.0 * danger * danger;
    }

    // ---- shared

    public synchronized int version() {
        return version;
    }

    /** How safe a system is, 0 to 1. */
    public synchronized double systemSafety(Vector3i system) {
        double[] v = systems.get(system);
        return safety(v == null ? 0 : v[0], v == null ? 0 : v[1], protectedDistance(system));
    }

    /** How safe a delivery between two sectors is: the least safe system on its way. */
    public synchronized double routeSafety(Vector3i fromSector, Vector3i toSector) {
        double least = 1;
        for (Vector3i s : systemsAlong(VoidSystem.getContainingSystem(fromSector, new Vector3i()), VoidSystem.getContainingSystem(toSector, new Vector3i()))) {
            least = Math.min(least, systemSafety(s));
        }
        return least;
    }

    /** Systems from {@code system} to the nearest protected one, the furthest way, or -1 beyond reach. */
    private int protectedDistance(Vector3i system) {
        if (protectedSystems.isEmpty()) {
            return -1;
        }
        Vector3i v = new Vector3i();
        for (int d = 0; d <= PROTECT_REACH; d++) {
            for (int x = -d; x <= d; x++) {
                for (int y = -d; y <= d; y++) {
                    for (int z = -d; z <= d; z++) {
                        if (Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) != d) {
                            continue;
                        }
                        v.set(system.x + x, system.y + y, system.z + z);
                        if (protectedSystems.contains(v)) {
                            return d;
                        }
                    }
                }
            }
        }
        return -1;
    }

    // ---- server

    /** Server: a raid cost a delivery a ship in this sector. */
    public synchronized void onRaid(Vector3i sector) {
        systems.computeIfAbsent(VoidSystem.getContainingSystem(sector, new Vector3i()), k -> new double[2])[1] += 1;
        version++;
        lastSync = 0;
    }

    /** Server: count traffic, fade the old, find who protects what, roll ambushes, and tell everyone. Call every tick. */
    public void update(GameServerState state, long now) {
        if (now - lastSample < SAMPLE_MS) {
            return;
        }
        long elapsed = lastSample == 0 ? 0 : now - lastSample;
        lastSample = now;
        if (now - lastProtection >= PROTECTION_MS) {
            lastProtection = now;
            refreshProtection(state);
        }
        List<Vector3i[]> ambushes = new ArrayList<>();
        synchronized (this) {
            //fading, by the share of a day since the last sample
            double keep = Math.pow(KEEP_PER_DAY, (double) elapsed / DAY_MS);
            systems.values().removeIf(v -> {
                v[0] *= keep;
                v[1] *= keep;
                return v[0] < 0.5 && v[1] < 0.05;
            });
            Set<Long> seen = new HashSet<>();
            for (TradeActive t : state.getGameState().getTradeManager().getTradeActiveMap().getTradeList()) {
                Vector3i sector = sectorOf(state, t);
                if (sector == null) {
                    continue;
                }
                Vector3i system = VoidSystem.getContainingSystem(sector, new Vector3i());
                systems.computeIfAbsent(system, k -> new double[2])[0] += 1;
                long key = t.getFleetId() >= 0 ? t.getFleetId() : -1 - t.getFromId();
                seen.add(key);
                Vector3i was = convoyAt.put(key, system);
                //a delivery entering a system meets whatever waits there
                if (was != null && !was.equals(system) && t.getFleetId() >= 0
                        && ThreadLocalRandom.current().nextDouble() < ambushChance(systemSafety(system), BFConfig.ambushChancePercent())) {
                    ambushes.add(new Vector3i[]{new Vector3i(sector), new Vector3i(system)});
                }
            }
            convoyAt.keySet().retainAll(seen);
            version++;
        }
        for (Vector3i[] a : ambushes) {
            ambush(state, a[0], systemSafety(a[1]));
        }
        if (now - lastSync >= SYNC_MS) {
            lastSync = now;
            broadcast(state);
        }
    }

    /** Where a delivery is: its convoy's flagship, or the engine's own reckoning for one carried on paper. */
    private static Vector3i sectorOf(GameServerState state, TradeActive t) {
        Fleet f = t.getFleetId() >= 0 ? state.getFleetManager().getByFleetDbId(t.getFleetId()) : null;
        FleetMember flag = f == null ? null : f.getFlagShip();
        return flag != null ? flag.getSector() : t.getCurrentSector();
    }

    /** Pirates come for the convoy from a sector nearby, more of them the less safe the space. */
    private static void ambush(GameServerState state, Vector3i convoy, double safety) {
        int count = 1 + (int) Math.round((1 - safety) * 3);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vector3i from = new Vector3i(convoy.x + (r.nextBoolean() ? 3 : -3), convoy.y + r.nextInt(-1, 2), convoy.z + (r.nextBoolean() ? 3 : -3));
        state.getSimulationManager().createRandomPiratePatrolGroup(from, new Vector3i(convoy), count);
    }

    /** The systems of factions reputable and peaceable enough to make the space around them safer. */
    private void refreshProtection(GameServerState state) {
        Reputation rep = DiplomacyManager.server().getReputation();
        Set<Vector3i> found = new HashSet<>();
        for (Faction f : state.getFactionManager().getFactionCollection()) {
            int id = f.getIdFaction();
            if (id == 0 || id == FactionManager.PIRATES_ID || rep.infamyOf(id) >= REPUTABLE_BELOW || rep.aggressionOf(id) >= REPUTABLE_BELOW) {
                continue;
            }
            //the sectors of its claimed systems, kept fresh each income turn
            for (Vector3i s : new ArrayList<>(f.lastSystemSectors)) {
                found.add(VoidSystem.getContainingSystem(s, new Vector3i()));
            }
        }
        synchronized (this) {
            protectedSystems = found;
            version++;
        }
    }

    // ---- sync

    private void broadcast(GameServerState state) {
        Tag tag = toTag();
        for (PlayerState p : state.getPlayerStatesByName().values()) {
            if (p.getClientChannel() != null) {
                BFPacket.toPlayer(p, BFPacket.Kind.ROUTE_SAFETY, tag);
            }
        }
    }

    public void sendTo(PlayerState p) {
        BFPacket.toPlayer(p, BFPacket.Kind.ROUTE_SAFETY, toTag());
    }

    /** Also what is saved, but for protection, which is worked out again within minutes. */
    public synchronized Tag toTag() {
        Tag[] s = new Tag[systems.size() + 1];
        int i = 0;
        for (Map.Entry<Vector3i, double[]> e : systems.entrySet()) {
            s[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.VECTOR3i, null, new Vector3i(e.getKey())),
                    new Tag(Tag.Type.DOUBLE, null, e.getValue()[0]), new Tag(Tag.Type.DOUBLE, null, e.getValue()[1]), FinishTag.INST});
        }
        s[i] = FinishTag.INST;
        Tag[] p = new Tag[protectedSystems.size() + 1];
        int j = 0;
        for (Vector3i v : protectedSystems) {
            p[j++] = new Tag(Tag.Type.VECTOR3i, null, new Vector3i(v));
        }
        p[j] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.STRUCT, null, s), new Tag(Tag.Type.STRUCT, null, p), FinishTag.INST});
    }

    public synchronized void fromTag(Tag tag) {
        systems.clear();
        Tag[] t = tag.getStruct();
        for (Tag e : t[0].getStruct()) {
            if (e.getType() == Tag.Type.STRUCT) {
                Tag[] v = e.getStruct();
                systems.put(v[0].getVector3i(), new double[]{v[1].getDouble(), v[2].getDouble()});
            }
        }
        Set<Vector3i> p = new HashSet<>();
        for (Tag e : t[1].getStruct()) {
            if (e.getType() == Tag.Type.VECTOR3i) {
                p.add(e.getVector3i());
            }
        }
        protectedSystems = p;
        version++;
    }

    public synchronized void clear() {
        systems.clear();
        protectedSystems = new HashSet<>();
        convoyAt.clear();
        lastSample = 0;
        lastProtection = 0;
        lastSync = 0;
        version++;
    }

    /**
     * Client: a trade network row's system column, with how safe the route there is from the shop being used (or
     * the player). The column prints the system, then the sector, so the station's sector is its last three numbers.
     */
    public static Object tradeListText(Object system, org.schema.game.common.controller.ShopInterface shop) {
        return new Object() {
            @Override
            public String toString() {
                String s = String.valueOf(system);
                List<Vector3i> found = videogoose.betterfactions.fleet.TradeFleetOrders.parseSectors(s);
                if (found.size() < 2) {
                    return s;
                }
                Vector3i to = found.get(found.size() - 1);
                org.schema.game.client.data.GameClientState gs = org.schema.game.client.data.GameClientState.instance;
                Vector3i from = shop != null && shop.getSegmentController() != null ? shop.getSegmentController().getSector(new Vector3i())
                        : gs != null && gs.getPlayer() != null ? gs.getPlayer().getCurrentSector() : null;
                return from == null ? s : s + "  -  " + percent(CLIENT.routeSafety(from, to)) + " safe";
            }
        };
    }

    /** A safety as players read it. */
    public static String percent(double safety) {
        return Math.round(safety * 100) + "%";
    }
}
