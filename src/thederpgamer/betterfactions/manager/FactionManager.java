package thederpgamer.betterfactions.manager;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.persistent.faction.FactionData;
import thederpgamer.betterfactions.data.persistent.faction.FactionMember;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * <Description>
 *
 * @version 1.0 - [01/30/2021]
 * @author TheDerpGamer
 */
public class FactionManager {

    private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

    private static final String defaultDescription = "A Faction";
    private static final String piratesDescription = "Small clans of ravaging space pirates that attack and loot everything in sight. Despite their savagery, they are currently quite weak due to their lack of an organized leadership.";
    private static final String tradingGuildDescription = "A friendly organization made up of wealthy trading guilds spread across the galaxy. They boast a large navy made up of the combined forces of their many guild members and may sometimes defend weaker factions from larger aggressors.";

    public static boolean inFaction(PlayerState playerState) {
        return playerState.getFactionId() > 0;
    }

    public static Faction getFaction(PlayerState playerState) {
        if(playerState.getFactionId() <= 0) return null;
        else return Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(playerState.getFactionId());
    }

    public static HashMap<Integer, FactionData> getFactionDataMap() {
        HashMap<Integer, FactionData> factionDataMap = new HashMap<>();
        if(NetworkSyncManager.onServer()) {
            for(Object factionDataObject : PersistentObjectUtil.getObjects(instance, FactionData.class)) {
                FactionData factionData = (FactionData) factionDataObject;
                factionDataMap.put(factionData.getFactionId(), factionData);
            }
        } else factionDataMap = NetworkSyncManager.getFactionDataCache();
        return factionDataMap;
    }

    public static FactionData getPlayerFactionData(String playerName) {
        if(GameCommon.getPlayerFromName(playerName) != null) return getFactionData(Objects.requireNonNull(getFaction(GameCommon.getPlayerFromName(playerName))));
        else return null;
    }

    public static FactionMember getPlayerFactionMember(String playerName) {
        try {
            return getFactionData(Objects.requireNonNull(getFaction(GameCommon.getPlayerFromName(playerName)))).getMember(playerName);
        } catch(Exception exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public static FactionData getFactionData(int factionId) {
        return getFactionData(Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(factionId));
    }

    public static FactionData getFactionData(Faction faction) {
        if(faction != null) {
            try {
                FactionData factionData = getFactionDataMap().get(faction.getIdFaction());
                if(factionData == null) factionData = createFactionData(faction.getIdFaction());
                return factionData;
            } catch(Exception ignored) {
                return createFactionData(faction.getIdFaction());
            }
        } else return null;
    }

    public static void initializeFactions() {
        for(Faction faction : Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFactionCollection()) {
            if(!org.schema.game.common.data.player.faction.FactionManager.isNPCFactionOrPirateOrTrader(faction.getIdFaction())) getFactionData(faction);
        }
        HashMap<Integer, FactionData> factionDataMap = getFactionDataMap();
        if(!factionDataMap.containsKey(org.schema.game.common.data.player.faction.FactionManager.PIRATES_ID)) createFactionData(org.schema.game.common.data.player.faction.FactionManager.PIRATES_ID);
        if(!factionDataMap.containsKey(org.schema.game.common.data.player.faction.FactionManager.TRAIDING_GUILD_ID)) createFactionData(org.schema.game.common.data.player.faction.FactionManager.TRAIDING_GUILD_ID);
    }

    private static FactionData createFactionData(int factionId) {
        if(NetworkSyncManager.onServer()) {
            ArrayList<FactionData> toRemove = new ArrayList<>();
            for(Object obj : PersistentObjectUtil.getObjects(instance, FactionData.class)) {
                if(((FactionData) obj).getFactionId() == factionId) toRemove.add((FactionData) obj);
            }
            for(FactionData oldData : toRemove) PersistentObjectUtil.removeObject(instance, oldData);

            Faction faction = Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(factionId);
            FactionData fData = new FactionData(faction);

            if(factionId == org.schema.game.common.data.player.faction.FactionManager.PIRATES_ID) {
                fData.setFactionLogo(ResourceManager.getSprite("pirates-logo"));
                fData.setFactionDescription(piratesDescription);
            } else if(factionId == org.schema.game.common.data.player.faction.FactionManager.TRAIDING_GUILD_ID) {
                fData.setFactionLogo(ResourceManager.getSprite("traders-logo"));
                fData.setFactionDescription(tradingGuildDescription);
            } else {
                fData.setFactionLogo(ResourceManager.getSprite("default-logo"));
                fData.setFactionDescription(defaultDescription);
                for(FactionPermission fp : faction.getMembersUID().values()) {
                    if(!fData.hasMember(fp.playerUID)) fData.addMember(fp.playerUID);
                }
            }
            PersistentObjectUtil.addObject(instance, fData);
            BetterFactions.getInstance().updateClientData();
            return fData;
        } else return null;
    }
}
