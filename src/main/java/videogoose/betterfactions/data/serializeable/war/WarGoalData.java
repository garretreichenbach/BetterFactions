package videogoose.betterfactions.data.serializeable.war;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import videogoose.betterfactions.data.serializeable.SerializeableData;

import java.io.IOException;

/**
 * Represents a war goal — the objective a faction pursues in a war.
 * Each war goal type has different cost modifiers for peace deal demands.
 */
public class WarGoalData implements SerializeableData {

    public enum WarGoalType {
        DEFEND_SELF("Defender", 0.35f, true, true, false),
        DEFEND_ALLY("Defend Ally", 0.15f, false, false, true), //Support ally in defensive war
        SUPPORT_ALLY("Support Ally", 0.12f, false, false, true), //Support ally in offensive war
        TERRITORY("Take Territory", 0.23f, true, true, true),
        ECONOMIC("Extract Economic Concessions", 0.25f, true, true, true),
        FORCE_DIPLO("Force Diplomatic Change", 0.3f, true, true, true),
        SHOW_SUPERIORITY("Show Superiority", 0.15f, true, false, true),
        CONTAINMENT("Contain Aggression", 0.5f, false, true, true),
        HUMILIATE_RIVAL("Humiliate Rival", 0.4f, true, true, true);

        public final String displayName;
        public final float maxCostModifier;
        public final boolean warLeader;
        public final boolean canCallAllies;
        public final boolean selectable;

        WarGoalType(String displayName, float maxCostModifier, boolean warLeader, boolean canCallAllies, boolean selectable) {
            this.displayName = displayName;
            this.maxCostModifier = maxCostModifier;
            this.warLeader = warLeader;
            this.canCallAllies = canCallAllies;
            this.selectable = selectable;
        }
    }

    public WarGoalType warGoalType;
    public int fromFactionId;
    public int toFactionId;
    public float score;
    public boolean demanded;

    public WarGoalData() {}

    public WarGoalData(WarGoalType warGoalType, int fromFactionId, int toFactionId) {
        this.warGoalType = warGoalType;
        this.fromFactionId = fromFactionId;
        this.toFactionId = toFactionId;
        this.score = 0;
        this.demanded = false;
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        warGoalType = WarGoalType.values()[readBuffer.readInt()];
        fromFactionId = readBuffer.readInt();
        toFactionId = readBuffer.readInt();
        score = readBuffer.readFloat();
        demanded = readBuffer.readBoolean();
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(warGoalType.ordinal());
        writeBuffer.writeInt(fromFactionId);
        writeBuffer.writeInt(toFactionId);
        writeBuffer.writeFloat(score);
        writeBuffer.writeBoolean(demanded);
    }
}
