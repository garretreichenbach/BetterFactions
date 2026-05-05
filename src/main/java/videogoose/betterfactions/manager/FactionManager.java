package videogoose.betterfactions.manager;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.persistent.faction.FactionData;
import videogoose.betterfactions.data.persistent.faction.FactionMember;
import videogoose.betterfactions.data.persistent.federation.FactionMessage;

import java.util.ArrayList;
import java.util.HashMap;

public class FactionManager {

    private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

    private static final String defaultDescription = "A Faction";
    private static final String piratesDescription = "Small clans of ravaging space pirates that attack and loot everything in sight. Despite their savagery, they are currently quite weak due to their lack of an organized leadership.";
    private static final String tradingGuildDescription = "A friendly organization made up of wealthy trading guilds spread across the galaxy. They boast a large navy made up of the combined forces of their many guild members and may sometimes defend weaker factions from larger aggressors.";

    public static boolean inFaction(PlayerState playerState) {
        return playerState != null && playerState.getFactionId() > 0;
    }

    public static Faction getFaction(PlayerState playerState) {
        if(playerState == null || playerState.getFactionId() <= 0) return null;
        if(GameCommon.getGameState() == null) return null;
        return GameCommon.getGameState().getFactionManager().getFaction(playerState.getFactionId());
    }

    public static HashMap<Integer, FactionData> getFactionDataMap() {
        HashMap<Integer, FactionData> factionDataMap = new HashMap<>();
        if(NetworkSyncManager.onServer()) {
            for(Object factionDataObject : PersistentObjectUtil.getObjects(instance, FactionData.class)) {
                if(factionDataObject instanceof FactionData factionData) {
                    factionDataMap.put(factionData.getFactionId(), factionData);
                }
            }
        } else factionDataMap = NetworkSyncManager.getFactionDataCache();
        return factionDataMap;
    }

    public static FactionData getPlayerFactionData(String playerName) {
        PlayerState player = GameCommon.getPlayerFromName(playerName);
        if(player == null) return null;
        Faction faction = getFaction(player);
        if(faction == null) return null;
        return getFactionData(faction);
    }

    public static FactionMember getPlayerFactionMember(String playerName) {
        try {
            PlayerState player = GameCommon.getPlayerFromName(playerName);
            if(player == null) return null;
            Faction faction = getFaction(player);
            if(faction == null) return null;
            FactionData factionData = getFactionData(faction);
            if(factionData == null) return null;
            FactionMember member = factionData.getMember(playerName);
            if(member == null) return null;
            if(member.getFactionData().getMembers().size() == 1 && member.getFactionData().getMembers().get(0).equals(member)) member.getRank().addPermission("*");
            return member;
        } catch(NullPointerException | IllegalStateException exception) {
            BetterFactions.getInstance().logException("Failed to get faction member for player '" + playerName + "'", exception);
        }
        return null;
    }

    public static void removeFactionData(FactionData factionData) {
        PersistentObjectUtil.removeObject(instance, factionData);
    }

    public static FactionData getFactionData(int factionId) {
        if(GameCommon.getGameState() == null) return null;
        Faction faction = GameCommon.getGameState().getFactionManager().getFaction(factionId);
        return getFactionData(faction);
    }

    public static FactionData getFactionData(Faction faction) {
        if(faction != null) {
            try {
                FactionData factionData = getFactionDataMap().get(faction.getIdFaction());
                if(factionData == null) factionData = createFactionData(faction.getIdFaction());
                return factionData;
            } catch(RuntimeException exception) {
                BetterFactions.getInstance().logException("Failed to retrieve faction data for faction " + faction.getIdFaction() + ", creating new data", exception);
                return createFactionData(faction.getIdFaction());
            }
        } else return null;
    }

    public static void updateData(Object data) {
        if(data instanceof FactionMessage message) {
            FactionData recipientData = getFactionData(message.toId);
            if(recipientData == null) {
                BetterFactions.getInstance().logWarning("Cannot update message data: no faction data found for faction " + message.toId);
                return;
            }
            FactionMessage toDelete = null;
            for(FactionMessage m : recipientData.getInbox()) {
                if(m.date == message.date) {
                    toDelete = m;
                    break;
                }
            }
            if(toDelete != null) recipientData.removeMessage(toDelete);
            recipientData.addMessage(message);
            updateData(recipientData);
        } else if(data instanceof FactionData factionData) {
            FactionData toDelete = null;
            for(FactionData fData : getFactionDataMap().values()) {
                if(fData.getFactionId() == factionData.getFactionId()) {
                    toDelete = fData;
                    break;
                }
            }
            if(toDelete != null) removeFactionData(toDelete);
            PersistentObjectUtil.addObject(instance, factionData);
        }
    }

    public static void initializeFactions() {
        if(GameCommon.getGameState() == null) {
            BetterFactions.getInstance().logWarning("Cannot initialize factions: game state is null");
            return;
        }
        for(Faction faction : GameCommon.getGameState().getFactionManager().getFactionCollection()) {
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
                if(obj instanceof FactionData existingData && existingData.getFactionId() == factionId) toRemove.add(existingData);
            }
            for(FactionData oldData : toRemove) PersistentObjectUtil.removeObject(instance, oldData);

            if(GameCommon.getGameState() == null) return null;
            Faction faction = GameCommon.getGameState().getFactionManager().getFaction(factionId);
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
