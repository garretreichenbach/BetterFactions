package thederpgamer.betterfactions.data.serializeable;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/15/2021]
 */
public class DiplomaticData implements SerializeableData {

    public enum DiplomaticDataType {
        WHITE_PEACE("White Peace", "White peace - nobody will gain or lose anything.", true, true, 0.05f, 0.0f, 0.1f),
        OFFER_TERRITORY("Offer Territory", "Offer to transfer a system your faction owns to the attacker in exchange for peace.", true, false, 0.1f, -0.1f, 0.2f, Vector3i.class),
        OFFER_CREDITS("Offer Credits", "Offer to pay the attacker credits in exchange for peace.", true, false, 0.12f, -0.07f, 0.12f, Integer.class),
        OFFER_RESOURCES("Offer Resources", "Offer to pay the attacker resources in exchange for peace.", true, false, 0.15f, -0.1f, 0.15f, Short.class, Integer.class),
        OFFER_DILPO("Offer Diplomatic Change", "Offer to realign your faction's relations (like breaking an alliance or leaving a federation) in exchange for peace.", true, false, 0.1f, -0.15f, 0.23f, DiplomaticChangeData.class),
        ;
//        DEMAND_TERRITORY("Demand Territory", "Demand a system currently owned by the defender to be transfered to your faction in exchange for peace.", false, true, ),
//        DEMAND_CREDITS(),
//        DEMAND_RESOURCES(),
//        DEMAND_DIPLO();

        public final String display;
        public final String description;
        public final boolean selectableByDefender;
        public final boolean selectableByAttacker;
        public final float costModifier;
        public final float aggressionModifier;
        public final float truceModifier;
        public final Class<?>[] fields;

        DiplomaticDataType(String display, String description, boolean selectableByDefender, boolean selectableByAttacker, float costModifier, float aggressionModifier, float truceModifier, Class<?>... fields) {
            this.display = display;
            this.description = description;
            this.selectableByDefender = selectableByDefender;
            this.selectableByAttacker = selectableByAttacker;
            this.costModifier = costModifier;
            this.aggressionModifier = aggressionModifier;
            this.truceModifier = truceModifier;
            this.fields = fields;
        }
    }

    public DiplomaticData() {

    }

    public DiplomaticData(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {

    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {

    }

    @Override
    public String toString() {
        return "DiplomaticData{}"; //Todo
    }
}
