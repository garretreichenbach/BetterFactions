package thederpgamer.betterfactions.data.old.faction;

import thederpgamer.betterfactions.utils.PermissionUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * FactionRank
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/15/2021
 */
public class FactionRank implements Serializable {

    public String rankName;
    public int rankLevel;
    public ArrayList<String> permissions;

    public FactionRank(String rankName, int rankLevel, String... permissions) {
        this.rankName = rankName;
        this.rankLevel = rankLevel;
        this.permissions = new ArrayList<>();
        if(permissions != null) addPermission(permissions);
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

    public static FactionRank getDefaultRank() {
        return new FactionRank("Member", 1, "chat.channel.faction", "chat.channel.federation",
                "entity.station.%HOMEBASE%.use.storage.*", "entity.station.%HOMEBASE%.use.factory.*", "entity.station.%HOMEBASE%.use.undeathenator",
                "entity.ship.%TYPE_MINER%, %TYPE_SALVAGER%, %TYPE_CARGO%, %TYPE_SUPPORT%.pilot",
                "entity.station.%HOMEBASE%.dock.*", "entity.station.%HOMEBASE%.undock.%TYPE_MINER%, %TYPE_SALVAGER%, %TYPE_CARGO%, %TYPE_SUPPORT%");
    }
}
