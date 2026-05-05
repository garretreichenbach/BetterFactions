package videogoose.betterfactions.data.serializeable.war;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.data.persistent.faction.FactionData;
import videogoose.betterfactions.data.serializeable.SerializeableData;
import videogoose.betterfactions.manager.UpdateManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active war between factions, including all participants,
 * their war goals, scores, and exhaustion levels.
 */
public class WarData implements SerializeableData {

    private long id;
    private String name;
    public final ConcurrentHashMap<Integer, WarParticipantData> attackers = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer, WarParticipantData> defenders = new ConcurrentHashMap<>();

    public WarData() {
        this.id = System.currentTimeMillis();
    }

    public WarData(String name) {
        this.id = System.currentTimeMillis();
        this.name = name;
    }

    public WarData(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addAttacker(int factionId, WarGoalData warGoal) {
        attackers.put(factionId, new WarParticipantData(factionId, warGoal));
        sendUpdate();
    }

    public void addDefender(int factionId, WarGoalData warGoal) {
        defenders.put(factionId, new WarParticipantData(factionId, warGoal));
        sendUpdate();
    }

    public void removeAttacker(int factionId) {
        attackers.remove(factionId);
        sendUpdate();
    }

    public void removeDefender(int factionId) {
        defenders.remove(factionId);
        sendUpdate();
    }

    public boolean isInvolved(int factionId) {
        return attackers.containsKey(factionId) || defenders.containsKey(factionId);
    }

    public boolean isInvolved(Faction faction) {
        return isInvolved(faction.getIdFaction());
    }

    public boolean isInvolved(FactionData factionData) {
        return isInvolved(factionData.getFactionId());
    }

    public boolean isOpposingSides(int factionA, int factionB) {
        return (attackers.containsKey(factionA) && defenders.containsKey(factionB))
            || (defenders.containsKey(factionA) && attackers.containsKey(factionB));
    }

    public List<WarGoalData> getGoals(int factionId) {
        List<WarGoalData> goals = new ArrayList<>();
        WarParticipantData participant = attackers.get(factionId);
        if (participant != null && participant.warGoal != null) goals.add(participant.warGoal);
        participant = defenders.get(factionId);
        if (participant != null && participant.warGoal != null) goals.add(participant.warGoal);
        return goals;
    }

    public List<WarGoalData> getGoals(Faction faction) {
        return getGoals(faction.getIdFaction());
    }

    public float getTotalProgress(int factionId) {
        WarParticipantData participant = attackers.get(factionId);
        if (participant == null) participant = defenders.get(factionId);
        return participant != null ? participant.score : 0;
    }

    public float getTotalProgress(Faction faction) {
        return getTotalProgress(faction.getIdFaction());
    }

    public float getTotalExhaustion(int factionId) {
        WarParticipantData participant = attackers.get(factionId);
        if (participant == null) participant = defenders.get(factionId);
        return participant != null ? participant.exhaustion : 0;
    }

    public float getTotalExhaustion(Faction faction) {
        return getTotalExhaustion(faction.getIdFaction());
    }

    public Faction getAttackerLeaderFaction() {
        for (WarParticipantData p : attackers.values()) {
            if (p.warGoal != null && p.warGoal.warGoalType.warLeader) {
                return GameCommon.getGameState().getFactionManager().getFaction(p.factionId);
            }
        }
        return null;
    }

    public Faction getDefenderLeaderFaction() {
        for (WarParticipantData p : defenders.values()) {
            if (p.warGoal != null && p.warGoal.warGoalType.warLeader) {
                return GameCommon.getGameState().getFactionManager().getFaction(p.factionId);
            }
        }
        return null;
    }

    public void sendUpdate() {
        UpdateManager.sendUpdate(UpdateManager.UpdateType.UPDATE_WAR_DATA, this);
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        id = readBuffer.readLong();
        name = readBuffer.readString();
        int attackerCount = readBuffer.readInt();
        attackers.clear();
        for (int i = 0; i < attackerCount; i++) {
            WarParticipantData p = new WarParticipantData();
            p.deserialize(readBuffer);
            attackers.put(p.factionId, p);
        }
        int defenderCount = readBuffer.readInt();
        defenders.clear();
        for (int i = 0; i < defenderCount; i++) {
            WarParticipantData p = new WarParticipantData();
            p.deserialize(readBuffer);
            defenders.put(p.factionId, p);
        }
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeLong(id);
        writeBuffer.writeString(name);
        writeBuffer.writeInt(attackers.size());
        for (WarParticipantData p : attackers.values()) p.serialize(writeBuffer);
        writeBuffer.writeInt(defenders.size());
        for (WarParticipantData p : defenders.values()) p.serialize(writeBuffer);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WarData other && other.id == id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
