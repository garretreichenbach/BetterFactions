package thederpgamer.betterfactions.data.federation;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.manager.data.FactionDataManager;
import thederpgamer.betterfactions.manager.data.FederationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

/**
 * <Description>
 *
 * @version 2.0 [04/09/2022]
 * @author TheDerpGamer
 */
public class Federation implements SerializationInterface {

    private int id;
    private String name;
    private String namePlural;
    private String description;
    private String logo;
    //Todo: Policy fields?

    protected final ArrayList<Integer> members = new ArrayList<>();

    public Federation(int id, String name, FactionData founder1, FactionData founder2) {
        this.id = id;
        this.name = name;
        this.namePlural = name;
        this.description = "A federation between " + founder1.getName() + " and " + founder2.getName() + ".";
        this.logo = "default-logo";
    }

    public Federation(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamePlural() {
        return namePlural;
    }

    public void setNamePlural(String namePlural) {
        this.namePlural = namePlural;
    }

    public String getDescription() {
        return description;
    }

    public HashMap<Integer, FactionData> getMembers() {
        HashMap<Integer, FactionData> map = new HashMap<>();
        for(int id : members) {
            try {
                map.put(id, FactionDataManager.instance.getCache().get(id));
            } catch(ExecutionException exception) {
                LogManager.logFailure("Failed to execute fetch request for Federation data", false);
            }
        }
        return map;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    @Override
    public boolean equals(SerializationInterface data) {
        return false;
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        id = readBuffer.readInt();
        name = readBuffer.readString();
        namePlural = readBuffer.readString();
        description = readBuffer.readString();
        logo = readBuffer.readString();
        int size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) members.add(readBuffer.readInt());
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(id);
        writeBuffer.writeString(name);
        writeBuffer.writeString(namePlural);
        writeBuffer.writeString(description);
        writeBuffer.writeString(logo);
        writeBuffer.writeInt(members.size());
        for(int id : members) writeBuffer.writeInt(id);
    }

	public void addMember(FactionData factionData) {
        members.add(factionData.getId());
        FederationManager.instance.saveData(this);
	}
}
