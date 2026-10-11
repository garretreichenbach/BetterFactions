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
        }

        static Asset deserialize(DataInput in) throws IOException {
            Asset a = new Asset();
            a.uid = in.readUTF();
            a.name = in.readUTF();
            a.type = in.readByte();
            a.sector = Vector3i.deserializeStatic(in);
            return a;
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
    }

    @Override
    public String toString() {
        return "FactionAssets [faction=" + factionId + ", assets=" + assets.size()
                + (truncated ? ", truncated" : "") + "]";
    }
}
