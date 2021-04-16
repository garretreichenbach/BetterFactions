package thederpgamer.betterfactions.data.faction;

import api.common.GameCommon;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.utils.FactionUtils;
import java.io.Serializable;

/**
 * FactionMember
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/15/2021
 */
public class FactionMember implements Serializable {

    public String name;
    public int factionId;
    public FactionRank rank;

    public FactionMember(String name, FactionData factionData, FactionRank rank) {
        this.name = name;
        this.factionId = factionData.getFactionId();
        this.rank = rank;
    }

    public FactionMember(String name, FactionData factionData) {
        this(name, factionData, FactionRank.getDefaultRank());
    }

    public FactionMember(PlayerState playerState, FactionData factionData) {
        this(playerState.getName(), factionData);
    }

    public String getName() {
        return name;
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
                if(permission.contains("[ANY]")) permission = permission.replace("[ANY]", "");
                if(rank.getPermissions().contains(permission)) return true;
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
        return FactionUtils.getFactionData(factionId);
    }
}
