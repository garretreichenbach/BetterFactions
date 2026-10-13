package videogoose.betterfactions.assets;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.util.linAlg.Vector3i;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

/**
 * What a faction holds, as the server last read it out of the entity table.
 *
 * <p>Only the server can answer this: a station the faction owns three galaxies away exists nowhere on a
 * client until somebody flies to it, and the entity table is the only place that knows about all of them
 * at once. So this is a snapshot with an age rather than live state, and the screen showing it says so.</p>
 *
 * <p>Answered for the sender's own faction and no other. There is deliberately no faction id on the
 * request: an asset list is an order of battle, and a query that cannot name a target cannot be pointed at
 * one. {@link #factionId} on the answer is there so a client that changed faction between asking and being
 * answered can drop a reply meant for the faction it just left.</p>
 *
 * <p>Ships are not in here. A faction's ships are the fleet menu's subject, and listing every faction-tagged
 * hull anyone ever built would bury the handful of things this screen is about.</p>
 */
public class FactionAssets {

    /**
     * One holding. The system it sits in and whether it is the homebase are both derived on the client -
     * the first is a pure function of {@link #sector}, the second a comparison against the faction's own
     * homebase UID, which is already synced. Neither is worth a field on every row.
     */
    public static final class Asset {
        public String uid = "";
        public String name = "";
        /**
         * {@code EntityType.dbTypeId}, which is the id the entity table itself stores - a stable key
         * rather than an enum ordinal, so the enum stays free to be reordered.
         */
        public byte type;
        public Vector3i sector = new Vector3i();
        /**
         * Mass with docked entities, or -1 when the server did not have it loaded.
         */
        public double mass = -1;

        public Asset() {
        }

        public Asset(String uid, String name, byte type, Vector3i sector) {
            this.uid = uid;
            this.name = name;
            this.type = type;
            this.sector = sector;
        }

        void serialize(DataOutput out) throws IOException {
            out.writeUTF(uid != null ? uid : "");
            out.writeUTF(name != null ? name : "");
            out.writeByte(type);
            sector.serialize(out);
            out.writeDouble(mass);
        }

        static Asset deserialize(DataInput in) throws IOException {
            Asset a = new Asset();
            a.uid = in.readUTF();
            a.name = in.readUTF();
            a.type = in.readByte();
            a.sector = Vector3i.deserializeStatic(in);
            a.mass = in.readDouble();
            return a;
        }
    }

    /**
     * A fleet running a Resources Reorganized route - see {@link FleetRoutes}. Keyed on the fleet's dbid,
     * which the client already holds for the fleets it may see, so the row is the fleet's own row.
     */
    public static final class Route {
        public long fleetDbId;
        /**
         * The logistics order this run is carrying ("40 Iron: A -> B"), or empty for a standing route.
         */
        public String order = "";
        public String status = "";
        /**
         * 1-based, 0 while the fleet undocks from a schedule that was replaced under it.
         */
        public int stop;
        public int stops;
        public String station = "";
        public boolean paused;
        /**
         * Rough time left to the current stop as of the answer, 0 when unknown or not travelling.
         */
        public long etaMs;
        /**
         * Local clock when this arrived, so {@link #etaLeft} counts down between answers without trusting
         * the two machines' clocks to agree.
         */
        private final long receivedAt = System.currentTimeMillis();

        public Route() {
        }

        public Route(long fleetDbId, String order, String status, int stop, int stops, String station, boolean paused,
                     long etaMs) {
            this.fleetDbId = fleetDbId;
            this.order = order;
            this.status = status;
            this.stop = stop;
            this.stops = stops;
            this.station = station;
            this.paused = paused;
            this.etaMs = etaMs;
        }

        /**
         * @return ms still to go on the estimate, 0 when there is none or it has run out
         */
        public long etaLeft() {
            return etaMs <= 0 ? 0 : Math.max(0, etaMs - (System.currentTimeMillis() - receivedAt));
        }

        public boolean isDelivery() {
            return !order.isEmpty();
        }

        void serialize(DataOutput out) throws IOException {
            out.writeLong(fleetDbId);
            out.writeUTF(order);
            out.writeUTF(status);
            out.writeInt(stop);
            out.writeInt(stops);
            out.writeUTF(station);
            out.writeBoolean(paused);
            out.writeLong(etaMs);
        }

        static Route deserialize(DataInput in) throws IOException {
            return new Route(in.readLong(), in.readUTF(), in.readUTF(), in.readInt(), in.readInt(), in.readUTF(),
                    in.readBoolean(), in.readLong());
        }
    }

    public int factionId;
    /**
     * Server clock at the moment the table was read, so the screen can say how stale it is.
     */
    public long queriedAt;
    /**
     * The faction holds more than {@code MAX_ASSETS}; what is here is the first page of them.
     */
    public boolean truncated;
    public final List<Asset> assets = new ObjectArrayList<Asset>();
    /**
     * Filled fresh on every answer rather than cached with the holdings: a route moves on in seconds.
     */
    public final List<Route> routes = new ObjectArrayList<Route>();

    public FactionAssets() {
    }

    public FactionAssets(int factionId, long queriedAt) {
        this.factionId = factionId;
        this.queriedAt = queriedAt;
    }

    public void serialize(DataOutput out) throws IOException {
        out.writeInt(factionId);
        out.writeLong(queriedAt);
        out.writeBoolean(truncated);
        out.writeInt(assets.size());
        for (Asset a : assets) {
            a.serialize(out);
        }
        out.writeInt(routes.size());
        for (Route r : routes) {
            r.serialize(out);
        }
    }

    public void deserialize(DataInput in) throws IOException {
        factionId = in.readInt();
        queriedAt = in.readLong();
        truncated = in.readBoolean();
        assets.clear();
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            assets.add(Asset.deserialize(in));
        }
        routes.clear();
        n = in.readInt();
        for (int i = 0; i < n; i++) {
            routes.add(Route.deserialize(in));
        }
    }

    /**
     * The holding with this UID, or null.
     */
    public Asset find(String uid) {
        for (Asset a : assets) {
            if (a.uid.equals(uid)) {
                return a;
            }
        }
        return null;
    }

    /**
     * The route this fleet is running, or null.
     */
    public Route route(long fleetDbId) {
        for (Route r : routes) {
            if (r.fleetDbId == fleetDbId) {
                return r;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "FactionAssets [faction=" + factionId + ", assets=" + assets.size()
                + (truncated ? ", truncated" : "") + "]";
    }
}
