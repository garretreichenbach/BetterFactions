package thederpgamer.betterfactions.data.persistent.federation;

import thederpgamer.betterfactions.data.persistent.PersistentData;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.persistent.faction.FactionScore;
import thederpgamer.betterfactions.manager.FederationManager;
import thederpgamer.betterfactions.manager.GUIManager;
import thederpgamer.betterfactions.manager.NetworkSyncManager;
import thederpgamer.betterfactions.utils.FactionNewsUtils;

import java.util.ArrayList;

/**
 * Federation.java
 * <Description>
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class FederationData implements PersistentData, FactionScore {

    private final int id;
    private String name;
    private final ArrayList<FactionData> members;
    private transient boolean needsUpdate = true;

    public FederationData(String name, FactionData fromFaction, FactionData toFaction) {
        this.name = name;
        this.members = new ArrayList<>();
        this.members.add(fromFaction);
        this.members.add(toFaction);
        this.id = FederationManager.getNewId();
        queueUpdate(true);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        queueUpdate(true);
    }

    public ArrayList<FactionData> getMembers() {
        return members;
    }

    public void addMember(FactionData factionData) {
        members.add(factionData);
        factionData.setFederationId(id);
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationJoinNews(this, factionData));
        GUIManager.updateTabs();
        queueUpdate(true);
    }

    public void removeMember(FactionData factionData) {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationLeaveNews(this, factionData));
        members.remove(factionData);
        factionData.setFederationId(-1);
        if(members.isEmpty()) disband();
        GUIManager.updateTabs();
        queueUpdate(true);
    }

    public void disband() {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationDisbandNews(this));
        for(FactionData factionData : members) factionData.setFederationId(-1);
        FederationManager.removeFederation(this);
        queueUpdate(true);
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

    @Override
    public int getDataType() {
        return NetworkSyncManager.FEDERATION_DATA;
    }

    @Override
    public int getDataId() {
        return getId();
    }

    @Override
    public boolean needsUpdate() {
        return needsUpdate;
    }

    @Override
    public void queueUpdate(boolean update) {
        needsUpdate = update;
    }
}
