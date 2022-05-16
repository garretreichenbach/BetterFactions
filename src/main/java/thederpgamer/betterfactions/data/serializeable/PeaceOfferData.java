package thederpgamer.betterfactions.data.serializeable;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;

import java.io.IOException;
import java.util.ArrayList;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/15/2021]
 */
public class PeaceOfferData implements SerializeableData {

    public FactionData from;
    public FactionData to;
    public ArrayList<DiplomaticData> dataList = new ArrayList<>();

    public PeaceOfferData(FactionData from, FactionData to, ArrayList<DiplomaticData> dataList) {
        this.from = from;
        this.to = to;
        this.dataList = dataList;
        //Todo: Multi-faction wars
    }

    public PeaceOfferData(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        from = new FactionData(readBuffer);
        to = new FactionData(readBuffer);
        int size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) dataList.add(new DiplomaticData(readBuffer));
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        from.serialize(writeBuffer);
        to.serialize(writeBuffer);
        writeBuffer.writeInt(dataList.size());
        for(DiplomaticData data : dataList) data.serialize(writeBuffer);
    }
}
