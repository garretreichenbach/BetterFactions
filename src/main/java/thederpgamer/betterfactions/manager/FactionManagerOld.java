package thederpgamer.betterfactions.manager;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionMember;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class FactionManagerOld {

	private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

	public static void initializeFactions() {
		for(Faction faction : Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFactionCollection()) {
			if(!org.schema.game.common.data.player.faction.FactionManager.isNPCFactionOrPirateOrTrader(faction.getIdFaction())) getFactionData(faction);
		}
		HashMap<Integer, FactionDataOld> factionDataMap = getFactionDataMap();
		if(!factionDataMap.containsKey(org.schema.game.common.data.player.faction.FactionManager.PIRATES_ID)) createFactionData(org.schema.game.common.data.player.faction.FactionManager.PIRATES_ID);
		if(!factionDataMap.containsKey(org.schema.game.common.data.player.faction.FactionManager.TRAIDING_GUILD_ID)) createFactionData(org.schema.game.common.data.player.faction.FactionManager.TRAIDING_GUILD_ID);
	}

	public static void removeFactionData(FactionDataOld factionData) {
		PersistentObjectUtil.removeObject(instance, factionData);
	}

	public static FactionDataOld getFactionData(int factionId) {
		return getFactionData(Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(factionId));
	}

	public static FactionDataOld getFactionData(Faction faction) {
		if(faction != null) {
			try {
				FactionDataOld factionData = getFactionDataMap().get(faction.getIdFaction());
				if(factionData == null) factionData = createFactionData(faction.getIdFaction());
				return factionData;
			} catch(Exception ignored) {
				return createFactionData(faction.getIdFaction());
			}
		} else return null;
	}

	public static boolean inFaction(PlayerState playerState) {
		return playerState.getFactionId() > 0;
	}

	public static Faction getFaction(PlayerState playerState) {
		if(playerState.getFactionId() <= 0) return null;
		else return Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(playerState.getFactionId());
	}

	public static HashMap<Integer, FactionDataOld> getFactionDataMap() {
		HashMap<Integer, FactionDataOld> factionDataMap = new HashMap<>();
		if(NetworkSyncManager.onServer()) {
			for(Object factionDataObject : PersistentObjectUtil.getObjects(instance, FactionDataOld.class)) {
				FactionDataOld factionData = (FactionDataOld) factionDataObject;
				factionDataMap.put(factionData.getFactionId(), factionData);
			}
		} else factionDataMap = NetworkSyncManager.getFactionDataCache();
		return factionDataMap;
	}

	public static FactionDataOld getPlayerFactionData(String playerName) {
		if(GameCommon.getPlayerFromName(playerName) != null) return getFactionData(Objects.requireNonNull(getFaction(GameCommon.getPlayerFromName(playerName))));
		else return null;
	}

	public static FactionMember getPlayerFactionMember(String playerName) {
		try {
			FactionMember member = getFactionData(Objects.requireNonNull(getFaction(GameCommon.getPlayerFromName(playerName)))).getMember(playerName);
			if(member.getFactionData().getMembers().size() == 1 && member.getFactionData().getMembers().get(0).equals(member)) member.getRank().addPermission("*");
			return member;
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		return null;
	}

	private static FactionDataOld createFactionData(int factionId) {
		if(NetworkSyncManager.onServer()) {
			ArrayList<FactionDataOld> toRemove = new ArrayList<>();
			for(Object obj : PersistentObjectUtil.getObjects(instance, FactionDataOld.class)) {
				if(((FactionDataOld) obj).getFactionId() == factionId) toRemove.add((FactionDataOld) obj);
			}
			for(FactionDataOld oldData : toRemove) PersistentObjectUtil.removeObject(instance, oldData);

			Faction faction = Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(factionId);
			FactionDataOld fData = new FactionDataOld(faction);

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
