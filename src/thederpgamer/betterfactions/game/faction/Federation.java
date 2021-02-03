package thederpgamer.betterfactions.game.faction;

import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionScore;
import thederpgamer.betterfactions.utils.FederationUtils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Federation.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class Federation implements Serializable, FactionScore {

    private int id;
    private String name;
    private ArrayList<FactionData> members;

    public Federation(String name, FactionData founder) {
        this.name = name;
        this.members = new ArrayList<>();
        this.members.add(founder);
        this.id = FederationUtils.getNewId(this);
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
