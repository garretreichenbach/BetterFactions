package thederpgamer.betterfactions.data.serializeable.war;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.serializeable.SerializeableData;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/18/2021]
 */
public class WarGoalData implements SerializeableData {

    public enum WarGoalType {
        DEFEND_SELF("Defender", 0.35f, true, true, false),
        DEFEND_ALLY("Defend Ally", 0.15f, false, false, true),
        SUPPORT_ALLY("Support Ally", 0.12f, false, false, true),
        TERRITORY("Take Territory", 0.23f, true, true, true),
        ECONOMIC("Extract Economic Concessions", 0.25f, true, true, true),
        FORCE_DIPLO("Force Diplomatic Change", 0.3f, true, true, true),
        SHOW_SUPERIORITY("Show Superiority", 0.15f, true, false, true),
        CONTAINMENT("Contain Aggression", 0.5f, false, true, true);

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

    public WarGoalData(WarGoalType warGoalType) {
        this.warGoalType = warGoalType;
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {

    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {

    }
}
