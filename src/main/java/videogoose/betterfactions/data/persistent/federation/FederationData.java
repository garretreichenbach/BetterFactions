package videogoose.betterfactions.data.persistent.federation;

import api.common.GameCommon;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.data.persistent.PersistentData;
import videogoose.betterfactions.manager.FederationManager;
import videogoose.betterfactions.manager.FactionManager;
import videogoose.betterfactions.manager.GUIManager;
import videogoose.betterfactions.manager.NetworkSyncManager;
import videogoose.betterfactions.mixin.BetterFactionAccessor;
import videogoose.betterfactions.utils.FactionNewsUtils;

import java.util.ArrayList;

/**
 * Federation.java
 * <Description>
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class FederationData implements PersistentData {

    private final int id;
    private String name;
    private final ArrayList<Integer> memberIds;
    private transient boolean needsUpdate = true;

    public FederationData(String name, Faction fromFaction, Faction toFaction) {
        this.name = name;
        this.memberIds = new ArrayList<>();
        this.memberIds.add(fromFaction.getIdFaction());
        this.memberIds.add(toFaction.getIdFaction());
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

    public ArrayList<Integer> getMemberIds() {
        return memberIds;
    }

    /**
     * Get the name of a member faction by index.
     * Returns "Unknown" if the faction cannot be resolved.
     */
    public String getMemberName(int index) {
        if (index < 0 || index >= memberIds.size()) return "Unknown";
        if (GameCommon.getGameState() == null) return "Unknown";
        Faction faction = GameCommon.getGameState().getFactionManager().getFaction(memberIds.get(index));
        return faction != null ? faction.getName() : "Unknown";
    }

    public void addMember(Faction faction) {
        memberIds.add(faction.getIdFaction());
        ((BetterFactionAccessor) faction).setFederationId(id);
        FactionManager.saveStore(faction.getIdFaction());
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationJoinNews(this, faction));
        GUIManager.updateTabs();
        queueUpdate(true);
    }

    public void removeMember(Faction faction) {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationLeaveNews(this, faction));
        memberIds.remove(Integer.valueOf(faction.getIdFaction()));
        ((BetterFactionAccessor) faction).setFederationId(-1);
        FactionManager.saveStore(faction.getIdFaction());
        if(memberIds.isEmpty()) disband();
        GUIManager.updateTabs();
        queueUpdate(true);
    }

    public void disband() {
        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFederationDisbandNews(this));
        for(int factionId : memberIds) {
            if (GameCommon.getGameState() == null) continue;
            Faction faction = GameCommon.getGameState().getFactionManager().getFaction(factionId);
            if (faction != null) {
                ((BetterFactionAccessor) faction).setFederationId(-1);
                FactionManager.saveStore(factionId);
            }
        }
        FederationManager.removeFederation(this);
        queueUpdate(true);
    }

    public String[] getDataArray() {
        String[] dataArray = new String[3];
        dataArray[0] = "NAME: " + name;
        dataArray[1] = "ID: " + id;
        StringBuilder membersBuilder = new StringBuilder();
        membersBuilder.append(" {");
        for(int i = 0; i < memberIds.size(); i ++) {
            membersBuilder.append(getMemberName(i));
            if(i < memberIds.size() - 1) membersBuilder.append(", ");
        }
        membersBuilder.append("}");
        dataArray[2] = "MEMBERS: " + membersBuilder.toString();
        return dataArray;
    }

    public String[] getScoreArray() {
        //TODO: Implement real scoring
        return new String[]{"FP: 0", "INFL: 0", "TER: 0", "ECON: 0", "MIL: 0", "AGR: 0"};
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
