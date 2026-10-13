package videogoose.betterfactions.assets;

import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.database.DatabaseEntry;
import videogoose.betterfactions.assets.FactionAssets;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.GameServerState;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The server's answer to "what does this faction hold", cached per faction.
 *
 * <p>The entity table is the only place that knows, and it is not indexed by anything a faction screen
 * asks about, so the query is a real one rather than a lookup. Cached because every member of a faction
 * asking gets the same answer, and because opening a tab should not be able to put a query on the tick for
 * each person who does it.</p>
 *
 * <p>Staleness is bounded by {@link #TTL_MS} rather than by invalidation hooks. A holding appearing or
 * changing hands is not something anybody watches happen on this screen, the answer carries the clock it
 * was taken at, and the screen re-asks on its own while it is open.</p>
 *
 * <p>Todo: the query runs on the caller's thread, which is the server tick. It is one indexed seek
 * behind a cache, and {@code FactionManager} and the region-rename path already read the database there.
 * If it ever shows up in a tick profile, {@link GameMapProvider} is the shape to move it to - a worker
 * thread with a request queue and an answer queue drained on the tick.</p>
 */
public class FactionAssetIndex {

    /**
     * How long an answer is served without going back to the database.
     */
    public static final long TTL_MS = 30000;
    /**
     * Rows past this are dropped and the answer is flagged {@link FactionAssets#truncated}. A faction
     * with more stations than this has a problem no list can solve, and the packet has a size limit.
     */
    public static final int MAX_ASSETS = 2000;
    /**
     * Stations and shops. Ships are the fleet menu's subject - see {@link FactionAssets}.
     */
    private static final int[] HOLDING_TYPES = {
            SimpleTransformableSendableObject.EntityType.SPACE_STATION.dbTypeId,
            SimpleTransformableSendableObject.EntityType.SHOP.dbTypeId};

    private final GameServerState state;
    /**
     * Concurrent because the two things that touch it do not share a thread: a request arrives on the
     * tick, and {@code SegmentController#setFactionId} can invalidate from wherever a station changed
     * hands. Same reasoning as the per-owner caches in {@code ClientChannel}.
     */
    private final ConcurrentHashMap<Integer, FactionAssets> cache = new ConcurrentHashMap<Integer, FactionAssets>();

    public FactionAssetIndex(GameServerState state) {
        this.state = state;
    }

    /**
     * @return what the faction holds, possibly up to {@link #TTL_MS} old, or null if the query failed
     */
    public FactionAssets get(int factionId) {
        if (factionId == 0) {
            return null;
        }
        FactionAssets cached = cache.get(factionId);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.queriedAt < TTL_MS) {
            return cached;
        }
        FactionAssets fresh = query(factionId, now);
        if (fresh != null) {
            cache.put(factionId, fresh);
        }
        return fresh != null ? fresh : cached;
    }

    /**
     * Forces the next {@link #get} for this faction back to the database. For the places that know a
     * holding just changed hands and would rather not make the screen wait out the TTL.
     */
    public void invalidate(int factionId) {
        cache.remove(factionId);
    }

    /**
     * Whether this kind of entity is one the index would list, so a caller can skip the invalidation
     * rather than drop a cache over a ship changing hands.
     */
    public static boolean tracks(SimpleTransformableSendableObject.EntityType type) {
        if (type == null) {
            return false;
        }
        for (int t : HOLDING_TYPES) {
            if (t == type.dbTypeId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mass of the entity if it is loaded, else -1. The entity table has no mass column, and reading an
     * unloaded station's file to find out is far more than a list row is worth.
     */
    private double loadedMass(DatabaseEntry e) {
        SegmentController sc = state.getSegmentControllersByName().get(e.uid);
        if (sc == null) {
            try {
                sc = state.getSegmentControllersByName().get(e.getFullUid());
            } catch (DatabaseEntry.EntityTypeNotFoundException ignored) {
            }
        }
        return sc == null ? -1 : sc.getMassWithoutDockIncludingStation();
    }

    private FactionAssets query(int factionId, long now) {
        try {
            List<DatabaseEntry> rows = state.getDatabaseIndex().getTableManager().getEntityTable()
                    .getByFaction(factionId, HOLDING_TYPES, MAX_ASSETS + 1);
            FactionAssets a = new FactionAssets(factionId, now);
            for (DatabaseEntry e : rows) {
                if (e.dockedTo > 0) {
                    continue; //docked to another entity: part of it, not a holding of its own
                }
                if (a.assets.size() >= MAX_ASSETS) {
                    a.truncated = true;
                    break;
                }
                FactionAssets.Asset asset = new FactionAssets.Asset(e.uid, e.realName, (byte) e.type, e.sectorPos);
                asset.mass = loadedMass(e);
                a.assets.add(asset);
            }
            return a;
        } catch (SQLException e) {
            System.err.println("[SERVER][FACTION] asset query failed for faction " + factionId);
            e.printStackTrace();
            return null;
        }
    }
}
