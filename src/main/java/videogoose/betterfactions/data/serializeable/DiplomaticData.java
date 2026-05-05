package videogoose.betterfactions.data.serializeable;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;

import java.io.IOException;

/**
 * Represents a single demand or offer in a peace deal negotiation.
 * Each instance has a type, a war score cost, and type-specific field values.
 */
public class DiplomaticData implements SerializeableData {

    public enum DiplomaticDataType {
        // Offers (defender can select these to give up things for peace)
        WHITE_PEACE("White Peace", "End the war with no demands from either side.",
            true, true, 0.0f, 0.0f, 0.1f),
        OFFER_TERRITORY("Offer Territory", "Transfer a system to the other side.",
            true, true, 0.15f, -0.1f, 0.2f, Vector3i.class),
        OFFER_CREDITS("Offer Credits", "Pay credits to the other side.",
            true, true, 0.10f, -0.07f, 0.12f, Integer.class),
        OFFER_RESOURCES("Offer Resources", "Pay resources to the other side.",
            true, true, 0.12f, -0.1f, 0.15f, Short.class, Integer.class),
        OFFER_DIPLO("Offer Diplomatic Change", "Change diplomatic relations (break alliance, leave federation, etc.).",
            true, true, 0.10f, -0.15f, 0.23f, DiplomaticChangeData.class),

        // Demands (attacker can select these to take things)
        DEMAND_TERRITORY("Demand Territory", "Demand a system from the losing side.",
            true, true, 0.20f, 0.15f, 0.05f, Vector3i.class),
        DEMAND_CREDITS("Demand Credits", "Demand a credit payment from the losing side.",
            true, true, 0.12f, 0.10f, 0.08f, Integer.class),
        DEMAND_RESOURCES("Demand Resources", "Demand resources from the losing side.",
            true, true, 0.15f, 0.12f, 0.10f, Short.class, Integer.class),
        DEMAND_DIPLO("Demand Diplomatic Change", "Force the losing side to change their diplomacy.",
            true, true, 0.18f, 0.20f, 0.15f, DiplomaticChangeData.class),

        // Special
        HUMILIATE("Humiliate", "Humiliate the losing faction, reducing their prestige.",
            false, true, 0.10f, 0.25f, 0.05f),
        STATUS_QUO("Status Quo", "End the war with current territorial holdings.",
            true, true, 0.05f, 0.0f, 0.15f);

        public final String display;
        public final String description;
        public final boolean selectableByDefender;
        public final boolean selectableByAttacker;
        public final float warScoreCost;
        public final float aggressionModifier;
        public final float truceModifier;
        public final Class<?>[] fields;

        DiplomaticDataType(String display, String description, boolean selectableByDefender, boolean selectableByAttacker,
                           float warScoreCost, float aggressionModifier, float truceModifier, Class<?>... fields) {
            this.display = display;
            this.description = description;
            this.selectableByDefender = selectableByDefender;
            this.selectableByAttacker = selectableByAttacker;
            this.warScoreCost = warScoreCost;
            this.aggressionModifier = aggressionModifier;
            this.truceModifier = truceModifier;
            this.fields = fields;
        }

        public boolean isDemand() {
            return this == DEMAND_TERRITORY || this == DEMAND_CREDITS || this == DEMAND_RESOURCES
                || this == DEMAND_DIPLO || this == HUMILIATE;
        }

        public boolean isOffer() {
            return this == OFFER_TERRITORY || this == OFFER_CREDITS || this == OFFER_RESOURCES || this == OFFER_DIPLO;
        }
    }

    public DiplomaticDataType type;
    public String display;

    // Field values — used based on type
    public Vector3i systemCoords;   // For OFFER/DEMAND_TERRITORY
    public int creditAmount;        // For OFFER/DEMAND_CREDITS
    public short resourceType;      // For OFFER/DEMAND_RESOURCES
    public int resourceAmount;      // For OFFER/DEMAND_RESOURCES
    public DiplomaticChangeData diplomaticChange; // For OFFER/DEMAND_DIPLO

    public DiplomaticData() {}

    public DiplomaticData(DiplomaticDataType type) {
        this.type = type;
        this.display = type.display;
    }

    public DiplomaticData(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    public float getWarScoreCost() {
        return type != null ? type.warScoreCost : 0;
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        type = DiplomaticDataType.values()[readBuffer.readInt()];
        display = readBuffer.readString();
        switch (type) {
            case OFFER_TERRITORY, DEMAND_TERRITORY -> {
                int x = readBuffer.readInt();
                int y = readBuffer.readInt();
                int z = readBuffer.readInt();
                systemCoords = new Vector3i(x, y, z);
            }
            case OFFER_CREDITS, DEMAND_CREDITS -> creditAmount = readBuffer.readInt();
            case OFFER_RESOURCES, DEMAND_RESOURCES -> {
                resourceType = readBuffer.readShort();
                resourceAmount = readBuffer.readInt();
            }
            case OFFER_DIPLO, DEMAND_DIPLO -> {
                diplomaticChange = new DiplomaticChangeData();
                diplomaticChange.deserialize(readBuffer);
            }
            default -> {} // WHITE_PEACE, HUMILIATE, STATUS_QUO have no extra fields
        }
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(type.ordinal());
        writeBuffer.writeString(display);
        switch (type) {
            case OFFER_TERRITORY, DEMAND_TERRITORY -> {
                writeBuffer.writeInt(systemCoords != null ? systemCoords.x : 0);
                writeBuffer.writeInt(systemCoords != null ? systemCoords.y : 0);
                writeBuffer.writeInt(systemCoords != null ? systemCoords.z : 0);
            }
            case OFFER_CREDITS, DEMAND_CREDITS -> writeBuffer.writeInt(creditAmount);
            case OFFER_RESOURCES, DEMAND_RESOURCES -> {
                writeBuffer.writeShort(resourceType);
                writeBuffer.writeInt(resourceAmount);
            }
            case OFFER_DIPLO, DEMAND_DIPLO -> {
                if (diplomaticChange == null) diplomaticChange = new DiplomaticChangeData();
                diplomaticChange.serialize(writeBuffer);
            }
            default -> {}
        }
    }

    @Override
    public String toString() {
        return switch (type) {
            case OFFER_TERRITORY, DEMAND_TERRITORY -> display + " [" + systemCoords + "]";
            case OFFER_CREDITS, DEMAND_CREDITS -> display + " [" + creditAmount + " credits]";
            case OFFER_RESOURCES, DEMAND_RESOURCES -> display + " [" + resourceAmount + "x type " + resourceType + "]";
            default -> display;
        };
    }
}
