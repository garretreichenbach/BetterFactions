package thederpgamer.betterfactions.utils;

import api.common.GameClient;
import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.schine.graphicsengine.forms.Sprite;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionMember;
import thederpgamer.betterfactions.manager.ResourceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * FactionUtils.java
 * <Description>
 *
 * @since 01/30/2021
 * @author TheDerpGamer
 */
public class FactionUtils {

    private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

    private static final String defaultDescription = "A Faction";
    private static final String piratesDescription = "Small clans of ravaging space pirates that attack and loot everything in sight. Despite their savagery, they are currently quite weak due to their lack of an organized leadership.";
    private static final String tradingGuildDescription = "A friendly organization made up of wealthy trading guilds spread across the galaxy. They boast a large navy made up of the combined forces of their many guild members and may sometimes defend weaker factions from larger aggressors.";

    public static boolean inFaction(PlayerState playerState) {
        return playerState.getFactionId() != 0;
    }

    public static Faction getFaction(PlayerState playerState) {
        if(playerState.getFactionId() == 0) return null;
        else return GameCommon.getGameState().getFactionManager().getFaction(playerState.getFactionId());
    }

    public static HashMap<Integer, FactionData> getFactionDataMap() {
        HashMap<Integer, FactionData> factionDataMap = new HashMap<>();
        for(Object factionDataObject : PersistentObjectUtil.getObjects(instance, FactionData.class)) {
            FactionData factionData = (FactionData) factionDataObject;
            factionDataMap.put(factionData.factionId, factionData);
        }
        return factionDataMap;
    }

    public static FactionData getPlayerFactionData() {
        if(GameClient.getClientPlayerState().getFactionId() != 0) {
            return getFactionData(Objects.requireNonNull(getFaction(GameClient.getClientPlayerState())));
        } else return null;
    }

    public static FactionMember getPlayerFactionMember() {
        if(GameClient.getClientPlayerState().getFactionId() != 0) {
            return getFactionData(Objects.requireNonNull(getFaction(GameClient.getClientPlayerState()))).getMember(GameClient.getClientPlayerState().getName());
        } else return null;
    }

    public static FactionData getFactionData(int factionId) {
        return getFactionData(GameCommon.getGameState().getFactionManager().getFaction(factionId));
    }

    public static FactionData getFactionData(Faction faction) {
        return getFactionDataMap().get(faction.getIdFaction());
    }

    public static void initializeFactions() {
        for(Faction faction : GameCommon.getGameState().getFactionManager().getFactionCollection()) {
            if(!FactionManager.isNPCFactionOrPirateOrTrader(faction.getIdFaction())) getFactionData(faction);
        }
        HashMap<Integer, FactionData> factionDataMap = getFactionDataMap();
        if(!factionDataMap.containsKey(FactionManager.PIRATES_ID)) PersistentObjectUtil.addObject(instance, initializeNPCFaction(FactionManager.PIRATES_ID));
        if(!factionDataMap.containsKey(FactionManager.TRAIDING_GUILD_ID)) PersistentObjectUtil.addObject(instance, initializeNPCFaction(FactionManager.TRAIDING_GUILD_ID));
        saveData();
    }

    public static void saveData() {
        ArrayList<FactionData> fDataToDelete = new ArrayList<>();
        for(FactionData factionData :  getFactionDataMap().values()) {
            if(!GameCommon.getGameState().getFactionManager().getFactionMap().containsKey(factionData.getFactionId())) {
                fDataToDelete.add(factionData);
            }
        }
        for(FactionData fData : fDataToDelete) {
            for(FactionMember factionMember : fData.getMembers()) factionMember.setFactionId(0);
            PersistentObjectUtil.removeObject(instance, fData);
        }
        PersistentObjectUtil.save(instance);
    }

    private static FactionData initializeNPCFaction(int factionId) {
        Faction faction = GameCommon.getGameState().getFactionManager().getFaction(factionId);
        FactionData fData = new FactionData(faction);

        if(factionId == FactionManager.PIRATES_ID) {
            fData.setFactionLogo(ResourceManager.getSprite("pirates-logo"));
            fData.setFactionDescription(piratesDescription);
        } else if(factionId == FactionManager.TRAIDING_GUILD_ID) {
            fData.setFactionLogo(ResourceManager.getSprite("traders-logo"));
            fData.setFactionDescription(tradingGuildDescription);
        } else {
            fData.setFactionLogo(ResourceManager.getSprite("default-logo"));
            fData.setFactionDescription(defaultDescription);
        }
        return fData;
    }

    public static Sprite getFactionLogo(FactionData factionData) {
        String spriteName = factionData.factionName.replace(" ", "-") + "-logo";
        if(ResourceManager.getSprite(spriteName) != null) return ResourceManager.getSprite(spriteName);
        else return ResourceManager.getSprite("default-logo");
    }
}
