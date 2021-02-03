package thederpgamer.betterfactions.data.faction;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.FederationUtils;
import java.io.Serializable;

/**
 * FactionData.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class FactionData implements FactionScore, Serializable {

    //Info
    private int factionId;
    private int federationId;
    private String factionName;
    private String factionInfo;
    private String factionDescription;

    public FactionData(Faction faction) {
        factionId = faction.getIdFaction();
        federationId = -1;
        factionName = faction.getName();
        factionInfo = getInfo(faction);
        factionDescription = faction.getDescription();
    }

    public int getFactionId() {
        return factionId;
    }

    public String getFactionName() {
        return factionName;
    }

    public String getFactionDescription() {
        return factionDescription;
    }

    public String getFactionInfo() {
        return factionInfo;
    }

    public int getFederationId() {
        return federationId;
    }

    public void setFederationId(int federationId) {
        this.federationId = federationId;
    }

    private String getInfo(Faction faction) {
        StringBuilder builder = new StringBuilder();
        String federation = "Non-Aligned";
        FactionData factionData = FactionUtils.getFactionData(faction);
        if(FederationUtils.getFederation(factionData) != null) federation = FederationUtils.getFederation(factionData).getName();
        builder.append(federation);
        builder.append("\nSize: ").append(faction.getMembersUID().keySet().size());
        builder.append("\nPower: ").append(faction.factionPoints);
        builder.append("\nRelation: ").append(getRelationString(faction));
        builder.append("\nOpinion: ").append(getOpinionString(faction));
        return builder.toString();
    }

    private String getRelationString(Faction faction) {
        int playerFactionId = GameClient.getClientPlayerState().getFactionId();
        if(playerFactionId != 0) {
            if(playerFactionId == factionId) {
                return "Own Faction";
            } else {
                Faction playerFaction = GameCommon.getGameState().getFactionManager().getFaction(playerFactionId);
                FactionData playerFactionData = FactionUtils.getFactionData(playerFaction);
                if(playerFactionData.getFederationId() != -1) {
                    if(federationId != -1) {
                        if(playerFactionData.getFederationId() == federationId) {
                            return "In Federation";
                        } else {
                            return "Neutral";
                        }
                    } else {
                        return getRTypeString(factionId, playerFactionId);
                    }
                } else {
                    return getRTypeString(factionId, playerFactionId);
                }
            }
        } else {
            if(faction.getPersonalEnemies().contains(GameClient.getClientPlayerState().getName())) {
                return "Personal Enemy";
            } else {
                return getRTypeString(factionId, playerFactionId);
            }
        }
    }

    private String getRTypeString(int from, int to) {
        FactionRelation.RType relation = GameCommon.getGameState().getFactionManager().getRelation(from, to);
        if(relation.equals(FactionRelation.RType.ENEMY)) {
            return "At War";
        } else if(relation.equals(FactionRelation.RType.FRIEND)) {
            return "Allied";
        } else {
            return "Neutral";
        }
    }

    private String getOpinionString(Faction faction) { //Todo
        return "Todo";
    }

    public String getInfoString() {
        return factionInfo + "\n\n" + factionDescription;
    }

    public String[] getDataArray() {
        String[] dataArray = new String[3];
        dataArray[0] = "NAME: " + factionName;
        dataArray[1] = "ID: " + factionId;
        dataArray[2] = "FED: " + federationId;
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
