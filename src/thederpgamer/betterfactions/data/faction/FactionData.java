package thederpgamer.betterfactions.data.faction;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.player.faction.FactionRoles;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.betterfactions.data.federation.FactionMessage;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.manager.SpriteManager;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.FederationUtils;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @since 01/30/2021
 */
public class FactionData implements FactionScore, Serializable {

    //Info
    public int factionId;
    public int federationId;
    public String factionName;
    public String factionDescription;
    public String factionLogo;
    public ArrayList<FactionMember> members = new ArrayList<>();
    public ArrayList<FactionRank> ranks = new ArrayList<>();
    public ArrayList<FactionMessage> inbox = new ArrayList<>();

    public FactionData(Faction faction) {
        factionId = faction.getIdFaction();
        federationId = -1;
        factionName = faction.getName();
        factionDescription = faction.getDescription();
        factionLogo = SpriteManager.getFactionLogo(this).getName();
    }

    public Faction getFaction() {
        return GameCommon.getGameState().getFactionManager().getFaction(factionId);
    }

    public int getFactionId() {
        return factionId;
    }

    public String getFactionName() {
        return factionName;
    }

    public void setFactionName(String factionName) {
        this.factionName = factionName;
    }

    public String getFactionDescription() {
        return factionDescription;
    }

    public void setFactionDescription(String factionDescription) {
        this.factionDescription = factionDescription;
    }

    public Federation getFederation() {
        if(federationId != -1) return FederationUtils.getFederation(this);
        else return null;
    }

    public int getFederationId() {
        return federationId;
    }

    public void setFederationId(int federationId) {
        this.federationId = federationId;
    }

    public Sprite getFactionLogo() {
        return SpriteManager.getSprite(factionLogo);
    }

    public void setFactionLogo(Sprite factionLogo) {
        this.factionLogo = factionLogo.getName();
    }

    public String getInfo() {
        StringBuilder builder = new StringBuilder();
        String federation = "Non-Aligned";
        if(FederationUtils.getFederation(this) != null) federation = FederationUtils.getFederation(this).getName();
        builder.append(federation);
        builder.append("\n");
        builder.append(getRelationString());
        //builder.append("\nSize: ").append(getFaction().getMembersUID().keySet().size());
        //builder.append(" | Power: ").append(getFaction().factionPoints);
        //builder.append("\nRelation: ").append(getRelationString());
        //builder.append(" | Opinion: ").append(getOpinionString());
        return builder.toString();
    }

    public String getRelationString() {
        int playerFactionId = GameClient.getClientPlayerState().getFactionId();
        if(playerFactionId != 0) {
            if(playerFactionId == factionId) return "Own Faction";
            else {
                Faction playerFaction = GameCommon.getGameState().getFactionManager().getFaction(playerFactionId);
                FactionData playerFactionData = FactionUtils.getFactionData(playerFaction);
                if(playerFactionData.getFederationId() != -1) {
                    if(federationId != -1) {
                        if(playerFactionData.getFederationId() == federationId) return Lng.str("In Federation");
                        else return Lng.str("Neutral");
                    } else return getRTypeString(factionId, playerFactionId);
                } else return getRTypeString(factionId, playerFactionId);
            }
        } else {
            if(getFaction().getPersonalEnemies().contains(GameClient.getClientPlayerState().getName())) {
                return Lng.str("Personal Enemy");
            } else return getRTypeString(factionId, playerFactionId);
        }
    }

    private String getRTypeString(int from, int to) {
        FactionRelation.RType relation = GameCommon.getGameState().getFactionManager().getRelation(from, to);
        if(relation.equals(FactionRelation.RType.ENEMY)) return Lng.str("At War");
        else if(relation.equals(FactionRelation.RType.FRIEND)) return Lng.str("Allied");
        else return Lng.str("Neutral");
    }

    private String getOpinionString() { //Todo
        return "Todo";
    }

    public String getInfoString() {
        return getInfo();
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

    public void addMessage(FactionMessage message) {
        inbox.add(message);
    }

    public ArrayList<FactionMessage> getInbox() {
        return inbox;
    }

    public ArrayList<FactionMember> getMembers() {
        return members;
    }

    public FactionMember getMember(String playerName) {
        for(FactionMember member : members) {
            if(member.getName().equalsIgnoreCase(playerName)) return member;
        }
        return null;
    }

    public boolean hasMember(String playerName) {
        return getMember(playerName) != null;
    }

    public void addMember(String playerName) {
        getFaction().addOrModifyMember(playerName, playerName, FactionRoles.INDEX_DEFAULT_ROLE, System.currentTimeMillis(), GameCommon.getGameState(), true);
        members.add(new FactionMember(playerName, this, getLowestRank()));
    }

    public void removeMember(String playerName) {
        for(FactionMember member : getMembers()) {
            if(member.getName().equalsIgnoreCase(playerName)) {
                getFaction().removeMember(playerName, GameCommon.getGameState());
                members.remove(member);
            }
        }
    }

    public Vector3i getHomebaseSector() {
        return getFaction().getHomeSector();
    }

    public ArrayList<FactionRank> getRanks() {
        getLowestRank();
        return ranks;
    }

    public boolean addRank(FactionRank rank) {
        for(FactionRank factionRank : ranks) if(factionRank.getRankName().equalsIgnoreCase(rank.getRankName())) return false;
        ranks.add(rank);
        return true;
    }

    public boolean removeRank(FactionRank rank) {
        if(ranks.size() > 0 || getLowestRank().equals(rank)) return false;
        else {
            for(FactionMember member : members) {
                if(member.getRank().equals(rank)) member.setRank(getLowestRank());
            }
            ranks.remove(rank);
            return true;
        }
    }

    public FactionRank getLowestRank() {
        if(ranks.size() > 0) {
            FactionRank lowest = ranks.get(0);
            for(FactionRank rank : ranks) if(rank.getRankLevel() < lowest.getRankLevel()) lowest = rank;
            return lowest;
        } else {
            ranks.add(FactionRank.getDefaultRank());
            return ranks.get(0);
        }
    }
}
