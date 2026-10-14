package videogoose.betterfactions;

import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetManager;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Faction point upkeep on fleets, by mass: {@code fleet_upkeep_mass_per_point} mass costs one point a
 * turn. Charged on the engine's income turn beside system upkeep.
 *
 * <p>Neither the engine nor its database keeps a fleet ship's mass while the ship is unloaded, so the
 * last mass seen while it was loaded is kept here and saved. A ship never loaded since this arrived
 * costs nothing until it is.</p>
 */
public final class FleetUpkeep {

    private static FleetUpkeep instance = new FleetUpkeep();

    /** Entity UID to its last known mass, docks excluded (they ride on the root). ponytail: never pruned. */
    private final Map<String, Double> masses = new HashMap<>();

    public static FleetUpkeep get() {
        return instance;
    }

    public static void reset() {
        instance = new FleetUpkeep();
    }

    public synchronized void sawMass(String uid, double mass) {
        if (mass > 0) {
            masses.put(uid, mass);
        }
    }

    /** Last mass seen for this entity, 0 if never seen. */
    public synchronized double massOf(String uid) {
        return masses.getOrDefault(uid, 0.0);
    }

    /** Points owed for these ships; one whose mass was never seen counts as 0. */
    public synchronized float upkeep(List<String> uids, int massPerPoint) {
        if (massPerPoint <= 0) {
            return 0;
        }
        double mass = 0;
        for (String uid : uids) {
            mass += masses.getOrDefault(uid, 0.0);
        }
        return (float) (mass / massPerPoint);
    }

    /**
     * Points a station earns a turn: {@code base} at {@code minMass}, another {@code base} for each tenfold
     * mass past it. Below {@code minMass}, or never seen loaded, it earns nothing, so a field of tiny
     * stations is not a farm.
     */
    public static float stationIncome(double mass, int base, int minMass) {
        if (base <= 0 || mass <= 0 || mass < minMass) {
            return 0;
        }
        return (float) (base * (1 + Math.log10(mass / Math.max(1, minMass))));
    }

    /**
     * Server: UIDs of the fleet ships of every member of a player faction, cached fleets preferred as
     * fresher. Docked members are left out: their mass rides on their root.
     */
    public static List<String> shipsOf(Faction f) {
        List<String> out = new ArrayList<>();
        GameServerState state = GameServerState.instance;
        FleetManager fm = state.getFleetManager();
        for (String name : f.getMembersUID().keySet()) {
            for (Fleet stored : state.getDatabaseIndex().getTableManager().getFleetTable().loadFleetByOwner(state, name)) {
                if (stored == null) {
                    continue;
                }
                Fleet cached = fm.fleetCache.get(stored.dbid);
                for (FleetMember m : (cached != null ? cached : stored).getMembers()) {
                    if (m.getDockedToRootDbId() <= 0) {
                        out.add(m.UID);
                    }
                }
            }
        }
        return out;
    }

    public synchronized Tag toTag() {
        Tag[] t = new Tag[masses.size() + 1];
        int i = 0;
        for (Map.Entry<String, Double> e : masses.entrySet()) {
            t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.STRING, null, e.getKey()), new Tag(Tag.Type.DOUBLE, null, e.getValue()), FinishTag.INST});
        }
        t[i] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    public synchronized void fromTag(Tag tag) {
        for (Tag e : tag.getStruct()) {
            if (e.getType() == Tag.Type.STRUCT) {
                Tag[] pair = e.getStruct();
                masses.put(pair[0].getString(), pair[1].getDouble());
            }
        }
    }
}
