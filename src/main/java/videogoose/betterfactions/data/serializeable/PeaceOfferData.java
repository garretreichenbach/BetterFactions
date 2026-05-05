package videogoose.betterfactions.data.serializeable;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;

import java.io.IOException;
import java.util.ArrayList;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/15/2021]
 */
public class PeaceOfferData implements SerializeableData {

    public int fromFactionId;
    public int toFactionId;
    public ArrayList<DiplomaticData> dataList = new ArrayList<>();

    public PeaceOfferData(int fromFactionId, int toFactionId, ArrayList<DiplomaticData> dataList) {
        this.fromFactionId = fromFactionId;
        this.toFactionId = toFactionId;
        this.dataList = dataList;
        //Todo: Multi-faction wars
    }

    public PeaceOfferData(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        fromFactionId = readBuffer.readInt();
        toFactionId = readBuffer.readInt();
        int size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) dataList.add(new DiplomaticData(readBuffer));
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(fromFactionId);
        writeBuffer.writeInt(toFactionId);
        writeBuffer.writeInt(dataList.size());
        for(DiplomaticData data : dataList) data.serialize(writeBuffer);
    }
}
