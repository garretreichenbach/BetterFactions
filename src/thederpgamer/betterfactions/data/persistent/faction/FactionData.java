package thederpgamer.betterfactions.data.persistent.faction;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.player.faction.FactionRoles;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.betterfactions.data.persistent.PersistentData;
import thederpgamer.betterfactions.data.persistent.federation.FactionMessage;
import thederpgamer.betterfactions.data.persistent.federation.FederationData;
import thederpgamer.betterfactions.manager.FactionManager;
import thederpgamer.betterfactions.manager.FederationManager;
import thederpgamer.betterfactions.manager.NetworkSyncManager;
import thederpgamer.betterfactions.manager.ResourceManager;

import java.util.ArrayList;

/**
 * Stores persistent data for factions.
 *
 * @version 2.0 - [09/15/2021]
 * @author TheDerpGamer
 */
public class FactionData implements PersistentData, FactionScore {

    private final int factionId;
    private int federationId;
    private String factionName;
    private String factionDescription;
    private String factionLogo;
    private final ArrayList<FactionMember> members = new ArrayList<>();
    private final ArrayList<FactionRank> ranks = new ArrayList<>();
    private final ArrayList<FactionMessage> inbox = new ArrayList<>();
    private transient boolean needsUpdate = true;

    public FactionData(Faction faction) {
        factionId = faction.getIdFaction();
        federationId = -1;
        factionName = faction.getName();
        factionDescription = faction.getDescription();
        queueUpdate(true);
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
        queueUpdate(true);
    }

    public String getFactionDescription() {
        return factionDescription;
    }

    public void setFactionDescription(String factionDescription) {
        this.factionDescription = factionDescription;
        queueUpdate(true);
    }

    public FederationData getFederation() {
        if(federationId != -1) return FederationManager.getFederation(this);
        else return null;
    }

    public int getFederationId() {
        return federationId;
    }

    public void setFederationId(int federationId) {
        this.federationId = federationId;
        queueUpdate(true);
    }

    public Sprite getFactionLogo() {
        return ResourceManager.getSprite(factionLogo);
    }

    public void setFactionLogo(Sprite factionLogo) {
        this.factionLogo = factionLogo.getName();
        queueUpdate(true);
    }

    public String getInfo() {
        StringBuilder builder = new StringBuilder();
        String federation = "Non-Aligned";
        if(FederationManager.getFederation(this) != null) federation = FederationManager.getFederation(this).getName();
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
                FactionData playerFactionData = FactionManager.getFactionData(playerFaction);
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
        queueUpdate(true);
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
        queueUpdate(true);
    }

    public void removeMember(String playerName) {
        ArrayList<FactionMember> membersTemp = new ArrayList<>(getMembers());
        for(FactionMember member : membersTemp) {
            if(member.getName().equalsIgnoreCase(playerName)) {
                getFaction().removeMember(playerName, GameCommon.getGameState());
                members.remove(member);
            }
        }
        queueUpdate(true);
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
        queueUpdate(true);
        return true;
    }

    public boolean removeRank(FactionRank rank) {
        if(ranks.size() > 0 || getLowestRank().equals(rank)) return false;
        else {
            for(FactionMember member : members) if(member.getRank().equals(rank)) member.setRank(getLowestRank());
            ranks.remove(rank);
            queueUpdate(true);
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

    @Override
    public int getDataType() {
        return NetworkSyncManager.FACTION_DATA;
    }

    @Override
    public int getDataId() {
        return getFactionId();
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
