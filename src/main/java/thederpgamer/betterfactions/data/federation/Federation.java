package thederpgamer.betterfactions.data.federation;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.manager.data.FactionDataManager;
import thederpgamer.betterfactions.manager.LogManager;
import thederpgamer.betterfactions.manager.data.FederationManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    private String description;
    private String logo;
    //Todo: Policy fields?

    protected final ArrayList<Integer> members = new ArrayList<>();

    public Federation(int id, String name, FactionData founder1, FactionData founder2) {
        this.id = id;
        this.name = name;
        this.description = "A federation between " + founder1.getName() + " and " + founder2.getName() + ".";
        //this.logo = DEFAULT_LOGO;
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
        description = readBuffer.readString();
        logo = readBuffer.readString();
        int size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) members.add(readBuffer.readInt());
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(id);
        writeBuffer.writeString(name);
        writeBuffer.writeString(description);
        writeBuffer.writeString(logo);
        writeBuffer.writeInt(members.size());
        for(int id : members) writeBuffer.writeInt(id);
    }

	public void addMember(FactionData factionData) {
        members.add(factionData.getId());
        FederationManager.instance.saveData();
	}


    /*
    public ArrayList<FactionDataOld> getMembers() {
        return members;
    }

    public void addMember(FactionDataOld factionData) {
        members.add(factionData);
        factionData.setFederationId(id);
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationJoinNews(this, factionData));
        GUIManager.updateTabs();
    }

    public void removeMember(FactionDataOld factionData) {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationLeaveNews(this, factionData));
        members.remove(factionData);
        factionData.setFederationId(-1);
        if(members.isEmpty()) disband();
        GUIManager.updateTabs();
    }

    public void disband() {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationDisbandNews(this));
        for(FactionDataOld factionData : members) factionData.setFederationId(-1);
        FederationManager.removeFederation(this);
    }

    public String[] getDataArray() {
        String[] dataArray = new String[3];
        dataArray[0] = "NAME: " + name;
        dataArray[1] = "ID: " + id;
        StringBuilder membersBuilder = new StringBuilder();
        membersBuilder.append(" {");
        for(int i = 0; i < members.size(); i ++) {
            membersBuilder.append(members.get(i).getFactionName());
            if(i < members.size() - 1) membersBuilder.append(", ");
        }
        membersBuilder.append("}");
        dataArray[2] = "MEMBERS: " + membersBuilder.toString();
        return dataArray;
    }

    public String[] getScoreArray() {
        String[] scoreArray = new String[6];
        scoreArray[0] = "FP: " + factionPoints;
        scoreArray[1] = "INFL: " + influenceScore;
        scoreArray[2] = "TER: " + territoryScore;
        scoreArray[3] = "ECON: " + economicScore;
        scoreArray[4] = "MIL: " + militaryScore;
        scoreArray[5] = "AGR: " + aggressionScore;
        return scoreArray;
    }

    public String[] getInfoArray() {
        String[] infoArray = new String[9];
        String[] dataArray = getDataArray();
        String[] scoreArray = getScoreArray();
        System.arraycopy(dataArray, 0, infoArray, 0, dataArray.length);
        System.arraycopy(scoreArray, 0, infoArray, dataArray.length, scoreArray.length);
        return infoArray;
    }
     */
}
