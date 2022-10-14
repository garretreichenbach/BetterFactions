package thederpgamer.betterfactions.data.faction;

import api.common.GameCommon;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.manager.data.FactionDataManager;
import thederpgamer.betterfactions.manager.data.FactionMemberManager;

import java.io.IOException;

/**
 * FactionMember
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/15/2021
 */
public class FactionMember implements SerializationInterface {

    private int id;
    private String name;
    private int factionId;
    private FactionRank rank;

    public FactionMember(String name, FactionData factionData, FactionRank rank) {
        this.id = FactionMemberManager.instance.generateNewId();
        this.name = name;
        this.factionId = factionData.getId();
        this.rank = rank;
    }

    public FactionMember(String name, FactionData factionData) {
        this(name, factionData, FactionRank.getDefaultRank());
    }

    public FactionMember(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    @Override
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(SerializationInterface data) {
        return data instanceof FactionMember && data.getId() == id;
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        id = readBuffer.readInt();
        name = readBuffer.readString();
        factionId = readBuffer.readInt();
        rank = new FactionRank(readBuffer);
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {

    }

    public int getFactionId() {
        return factionId;
    }

    public void setFactionId(int factionId) {
        this.factionId = factionId;
    }

    public FactionRank getRank() {
        return rank;
    }

    public void setRank(FactionRank rank) {
        this.rank = rank;
    }

    public boolean hasPermission(String... permissions) {
        if(permissions != null) {
            for(String permission : permissions) {
                if(rank.getPermissions().contains("*")) return true;
                if(permission.contains("[ANY]")) permission = permission.replace("[ANY]", "");
                if(rank.getPermissions().contains(permission) || rank.getPermissions().contains("*")) return true;
            }
        }
        return false;
    }

    public boolean isOnline() {
        return GameCommon.getPlayerFromName(name) != null;
    }

    public Vector3i getLocation() {
        if(GameCommon.getPlayerFromName(name) != null) return GameCommon.getPlayerFromName(name).getCurrentSector();
        else return null;
    }

    public FactionData getFactionData() {
        return FactionDataManager.instance.getFactionData(factionId);
    }
}
