package thederpgamer.betterfactions.data.old.faction;

import api.common.GameClient;
import api.common.GameCommon;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.game.common.data.player.faction.FactionRoles;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.betterfactions.data.old.federation.FactionMessage;
import thederpgamer.betterfactions.data.faction.FactionScore;
import thederpgamer.betterfactions.data.federation.Federation;
import thederpgamer.betterfactions.manager.FactionManagerOld;
import thederpgamer.betterfactions.manager.data.FederationManager;
import thederpgamer.betterfactions.manager.ResourceManager;

import java.util.ArrayList;

/**
 * Stores persistent data for factions.
 *
 * @version 2.0 - [09/15/2021]
 * @author TheDerpGamer
 */
public class FactionDataOld implements FactionScore {

    private int factionId;
    private int federationId;
    private String factionName;
    private String factionDescription;
    private String factionLogo;
    private final ArrayList<FactionMember> members = new ArrayList<>();
    private final ArrayList<FactionRank> ranks = new ArrayList<>();
    private final ArrayList<FactionMessage> inbox = new ArrayList<>();

    public FactionDataOld(Faction faction) {
        factionId = faction.getIdFaction();
        federationId = -1;
        factionName = faction.getName();
        factionDescription = faction.getDescription();
    }

    /*
    public FactionData(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }


    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        factionId = readBuffer.readInt();
        federationId = readBuffer.readInt();
        factionName = readBuffer.readString();
        factionDescription = readBuffer.readString();
        factionLogo = readBuffer.readString();
        int size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) members.add(readBuffer.readObject(FactionMember.class)); //Todo: Convert to serialization interface
        size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) ranks.add(readBuffer.readObject(FactionRank.class)); //Todo: Convert to serialization interface
        size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) inbox.add(readBuffer.readObject(FactionMessage.class)); //Todo: Convert to serialization interface
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(factionId);
        writeBuffer.writeInt(federationId);
        writeBuffer.writeString(factionName);
        writeBuffer.writeString(factionDescription);
        writeBuffer.writeString(factionLogo);
        writeBuffer.writeInt(members.size());
        for(FactionMember member : members) writeBuffer.writeObject(member); //Todo: Convert to serialization interface
        writeBuffer.writeInt(ranks.size());
        for(FactionRank rank : ranks) writeBuffer.writeObject(rank); //Todo: Convert to serialization interface
        writeBuffer.writeInt(inbox.size());
        for(FactionMessage message : inbox) writeBuffer.writeObject(message); //Todo: Convert to serialization interface
    }

     */

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
        if(federationId != -1) return FederationManager.getFederation(this);
        else return null;
    }

    public int getFederationId() {
        return federationId;
    }

    public void setFederationId(int federationId) {
        this.federationId = federationId;
    }

    public Sprite getFactionLogo() {
        return ResourceManager.getSprite(factionLogo);
    }

    public void setFactionLogo(Sprite factionLogo) {
        this.factionLogo = factionLogo.getName();
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
                FactionDataOld playerFactionData = FactionManagerOld.getFactionData(playerFaction);
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

    public void removeMessage(FactionMessage message) {
        boolean remove = false;
        int i;
        for(i = 0; i < inbox.size(); i ++) if(inbox.get(i).date == message.date) {
            remove = true;
            break;
        }
        if(remove) inbox.remove(i);
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
        if(members.size() > 1) {
            getFaction().addOrModifyMember(playerName, playerName, FactionRoles.INDEX_DEFAULT_ROLE, System.currentTimeMillis(), GameCommon.getGameState(), true);
            members.add(new FactionMember(playerName, this, getLowestRank()));
        } else {
            getFaction().addOrModifyMember(playerName, playerName, FactionRoles.INDEX_ADMIN_ROLE, System.currentTimeMillis(), GameCommon.getGameState(), true);
            FactionRank founder = new FactionRank("Founder", FactionRoles.INDEX_ADMIN_ROLE, "*");
            addRank(founder);
            members.add(new FactionMember(playerName, this, founder));
        }
    }

    public void removeMember(String playerName) {
        ArrayList<FactionMember> membersTemp = new ArrayList<>(getMembers());
        for(FactionMember member : membersTemp) {
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
            for(FactionMember member : members) if(member.getRank().equals(rank)) member.setRank(getLowestRank());
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
