package thederpgamer.betterfactions.data.old.federation;

import thederpgamer.betterfactions.data.old.faction.FactionDataOld;
import thederpgamer.betterfactions.data.old.faction.FactionScore;
import thederpgamer.betterfactions.manager.FederationManager;
import thederpgamer.betterfactions.manager.GUIManager;
import thederpgamer.betterfactions.utils.FactionNewsUtils;

import java.util.ArrayList;

/**
 * Federation.java
 * <Description>
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class FederationData implements FactionScore {

    private final int id;
    private String name;
    private final ArrayList<FactionDataOld> members;
    private transient boolean needsUpdate = true;

    public FederationData(String name, FactionDataOld fromFaction, FactionDataOld toFaction) {
        this.name = name;
        this.members = new ArrayList<>();
        this.members.add(fromFaction);
        this.members.add(toFaction);
        this.id = FederationManager.getNewId();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
}
