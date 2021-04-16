package thederpgamer.betterfactions.data.federation;

import api.common.GameClient;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionScore;
import thederpgamer.betterfactions.utils.FactionNewsUtils;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.FederationUtils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Federation.java
 * <Description>
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class Federation implements Serializable, FactionScore {

    public int id;
    public String name;
    public ArrayList<FactionData> members;

    public Federation(String name, FactionData fromFaction, FactionData toFaction) {
        this.name = name;
        this.members = new ArrayList<>();
        this.members.add(fromFaction);
        this.members.add(toFaction);
        this.id = FederationUtils.getNewId();
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

    public ArrayList<FactionData> getMembers() {
        return members;
    }

    public void addMember(FactionData factionData) {
        members.add(factionData);
        factionData.setFederationId(id);
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationJoinNews(this, factionData));
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(FactionUtils.inFaction(GameClient.getClientPlayerState()) && FactionUtils.getPlayerFactionData().getFederationId() == id) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
    }

    public void removeMember(FactionData factionData) {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationLeaveNews(this, factionData));
        boolean updateGUI = FactionUtils.inFaction(GameClient.getClientPlayerState()) && FactionUtils.getPlayerFactionData().getFederationId() == id;
        members.remove(factionData);
        factionData.setFederationId(-1);
        if(members.isEmpty()) disband();
        BetterFactions.getInstance().newFactionPanel.factionDiplomacyTab.updateTab();
        if(updateGUI) {
            BetterFactions.getInstance().newFactionPanel.factionManagementTab.updateTab();
            BetterFactions.getInstance().newFactionPanel.federationManagementTab.updateTab();
        }
    }

    public void disband() {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationDisbandNews(this));
        for(FactionData factionData : members) factionData.setFederationId(-1);
        FederationUtils.removeFederation(this);
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
