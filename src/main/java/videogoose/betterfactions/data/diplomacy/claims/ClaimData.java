package videogoose.betterfactions.data.diplomacy.claims;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import videogoose.betterfactions.data.serializeable.SerializeableData;

import java.io.IOException;

/**
 * Represents a faction's territorial claim on a star system.
 * Claims provide war goals and generate border friction CBs when contested.
 */
public class ClaimData implements SerializeableData {

    public int factionId;
    public Vector3i systemCoords;
    public long claimTime;
    public ClaimOrigin origin;
    public float strength; // 0.0 to 1.0, decays if not occupied

    public enum ClaimOrigin {
        FABRICATED("Fabricated Claim", 0.5f),
        HISTORICAL("Historical Claim", 0.8f),
        CONQUEST("Conquered Territory", 1.0f),
        CORE("Core Territory", 1.0f);

        public final String displayName;
        public final float initialStrength;

        ClaimOrigin(String displayName, float initialStrength) {
            this.displayName = displayName;
            this.initialStrength = initialStrength;
        }
    }

    public ClaimData() {}

    public ClaimData(int factionId, Vector3i systemCoords, ClaimOrigin origin) {
        this.factionId = factionId;
        this.systemCoords = systemCoords;
        this.claimTime = System.currentTimeMillis();
        this.origin = origin;
        this.strength = origin.initialStrength;
    }

    /**
     * Check if this claim is contested by another faction's claim on the same system.
     */
    public boolean isContestedBy(ClaimData other) {
        return other.factionId != this.factionId
            && other.systemCoords.equals(this.systemCoords);
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        factionId = readBuffer.readInt();
        int x = readBuffer.readInt();
        int y = readBuffer.readInt();
        int z = readBuffer.readInt();
        systemCoords = new Vector3i(x, y, z);
        claimTime = readBuffer.readLong();
        origin = ClaimOrigin.values()[readBuffer.readInt()];
        strength = readBuffer.readFloat();
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(factionId);
        writeBuffer.writeInt(systemCoords.x);
        writeBuffer.writeInt(systemCoords.y);
        writeBuffer.writeInt(systemCoords.z);
        writeBuffer.writeLong(claimTime);
        writeBuffer.writeInt(origin.ordinal());
        writeBuffer.writeFloat(strength);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClaimData other
            && other.factionId == factionId
            && other.systemCoords.equals(systemCoords);
    }

    @Override
    public int hashCode() {
        return factionId * 31 + systemCoords.hashCode();
    }
}
