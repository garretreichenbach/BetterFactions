package thederpgamer.betterfactions.data.faction;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import thederpgamer.betterfactions.data.SerializationInterface;
import thederpgamer.betterfactions.manager.data.FactionRankManager;
import thederpgamer.betterfactions.utils.PermissionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * FactionRank
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/15/2021
 */
public class FactionRank implements SerializationInterface {

    private int id;
    private String rankName;
    private int rankLevel;
    private final ArrayList<String> permissions = new ArrayList<>();

    public FactionRank(String rankName, int rankLevel, String... permissions) {
        this.id = FactionRankManager.instance.generateNewId();
        this.rankName = rankName;
        this.rankLevel = rankLevel;
        if(permissions != null) addPermission(permissions);
    }

    public FactionRank(PacketReadBuffer readBuffer) throws IOException {
        deserialize(readBuffer);
    }

    public String getRankName() {
        return rankName;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName;
    }

    public int getRankLevel() {
        return rankLevel;
    }

    public void setRankLevel(int rankLevel) {
        this.rankLevel = rankLevel;
    }

    public ArrayList<String> getPermissions() {
        return permissions;
    }

    public void addPermission(String... permissions) {
        if(permissions != null) {
            for(String permission : permissions) if(!this.permissions.contains(permission)) this.permissions.add(permission);
            //this.permissions.addAll(Arrays.asList(permissions));
        }
        //this.permissions = mergePermissions(this.permissions);
    }

    public void removePermission(String... permissions) {
        //this.permissions = mergePermissions(this.permissions);
        if(permissions != null) this.permissions.removeAll(Arrays.asList(permissions));
    }

    private ArrayList<String> mergePermissions(ArrayList<String> permissions) {
        ArrayList<String> mergedList = new ArrayList<>();
        for(String permission : permissions) {
            String[] split = permission.split("\\.");
            StringBuilder builder = new StringBuilder();
            for(String s : split) {
                builder.append(s).append(".");
                if(s.equalsIgnoreCase("*")) {
                    mergedList.addAll(mergePermissions(PermissionUtils.getSubPermissions(builder.toString())));
                }
            }
        }
        return mergedList;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getName() {
        return rankName;
    }

    @Override
    public boolean equals(SerializationInterface data) {
        return data instanceof FactionRank && data.getId() == id;
    }

    @Override
    public void deserialize(PacketReadBuffer readBuffer) throws IOException {
        id = readBuffer.readInt();
        rankName = readBuffer.readString();
        rankLevel = readBuffer.readInt();
        int size = readBuffer.readInt();
        for(int i = 0; i < size; i ++) permissions.add(readBuffer.readString());
    }

    @Override
    public void serialize(PacketWriteBuffer writeBuffer) throws IOException {
        writeBuffer.writeInt(id);
        writeBuffer.writeString(rankName);
        writeBuffer.writeInt(rankLevel);
        writeBuffer.writeInt(permissions.size());
        for(String permission : permissions) writeBuffer.writeString(permission);
    }

    public static FactionRank getDefaultRank() {
        return new FactionRank("Member", 1, "chat.channel.faction", "chat.channel.federation",
                "entity.station.%HOMEBASE%.use.storage.*", "entity.station.%HOMEBASE%.use.factory.*", "entity.station.%HOMEBASE%.use.undeathenator",
                "entity.ship.%TYPE_MINER%, %TYPE_SALVAGER%, %TYPE_CARGO%, %TYPE_SUPPORT%.pilot",
                "entity.station.%HOMEBASE%.dock.*", "entity.station.%HOMEBASE%.undock.%TYPE_MINER%, %TYPE_SALVAGER%, %TYPE_CARGO%, %TYPE_SUPPORT%");
    }

    public static FactionRank getFounderRank() {
        return new FactionRank("Founder", 4, "*");
    }
}
