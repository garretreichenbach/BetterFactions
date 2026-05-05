package videogoose.betterfactions.data.serializeable.war;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import videogoose.betterfactions.data.serializeable.SerializeableData;

import java.io.IOException;

/**
 * Represents a single faction's participation in a war,
 * including their war goal, score, and exhaustion.
 */
public class WarParticipantData implements SerializeableData {

    public int factionId;
    public WarGoalData warGoal;
    public float score;
    public float exhaustion;

    public WarParticipantData() {}

    public WarParticipantData(int factionId, WarGoalData warGoal) {
        this.factionId = factionId;
        this.warGoal = warGoal;
        this.score = 0;
        this.exhaustion = 0;
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        factionId = readBuffer.readInt();
        warGoal = new WarGoalData();
        warGoal.deserialize(readBuffer);
        score = readBuffer.readFloat();
        exhaustion = readBuffer.readFloat();
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(factionId);
        warGoal.serialize(writeBuffer);
        writeBuffer.writeFloat(score);
        writeBuffer.writeFloat(exhaustion);
    }
}
