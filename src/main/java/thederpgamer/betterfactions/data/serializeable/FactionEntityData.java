package thederpgamer.betterfactions.data.serializeable;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import thederpgamer.betterfactions.utils.EntityUtils;

import java.io.IOException;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/14/2021]
 */
public class FactionEntityData implements SerializeableData {

    public enum EntityType {
        ALL, SHIP, STATION; //Todo: Entity Sub Types

        public static EntityType getType(SegmentController segmentController) {
            switch(segmentController.getType()) {
                case SHIP: return EntityType.SHIP;
                case SPACE_STATION: return EntityType.STATION;
                default: return ALL;
            }
        }
    }

    public String name;
    public EntityType entityType;
    public int factionId;
    private Vector3i sector;
    private float status;

    public FactionEntityData(SegmentController segmentController) {
        name = segmentController.getRealName();
        entityType = EntityType.getType(segmentController);
        factionId = segmentController.getFactionId();
        sector = segmentController.getSector(new Vector3i());
        status = (float) segmentController.getHpController().getHpPercent();
    }

    public FactionEntityData(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        name = readBuffer.readString();
        entityType = EntityType.valueOf(readBuffer.readString());
        factionId = readBuffer.readInt();
        sector = readBuffer.readVector();
        status = readBuffer.readFloat();
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        update();
        writeBuffer.writeString(name);
        writeBuffer.writeString(entityType.name());
        writeBuffer.writeInt(factionId);
        writeBuffer.writeVector(sector);
        writeBuffer.writeFloat(status);
    }

    public SegmentController getEntity() {
        return EntityUtils.getEntity(name);
    }

    public Vector3i getSector() {
        update();
        return sector;
    }

    public float getStatus() {
        update();
        return status;
    }

    private void update() {
        SegmentController entity = getEntity();
        if(entity != null) {
            name = entity.getRealName();
            factionId = entity.getFactionId();
            sector = entity.getSector(new Vector3i());
            status = (float) entity.getHpController().getHpPercent();
        } else {
            //Todo: Delete this data
            /*
            if(GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer()) {
                ClientCacheManager.factionAssets.remove(name);
            }
             */
        }
    }
}
