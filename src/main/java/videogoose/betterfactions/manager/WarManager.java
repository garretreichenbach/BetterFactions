package videogoose.betterfactions.manager;

import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.serializeable.war.WarData;

import java.util.ArrayList;


public class WarManager {

	public static ArrayList<WarData> getWarsInvolvedIn(Faction faction) {
		ArrayList<WarData> wars = new ArrayList<>();
		for(Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), WarData.class)) {
			WarData warData = (WarData) obj;
			if(warData.isInvolved(faction)) wars.add(warData);
		}
		return wars;
	}

	public static ArrayList<WarData> getAllWars() {
		ArrayList<WarData> wars = new ArrayList<>();
		for(Object obj : PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), WarData.class)) {
			WarData warData = (WarData) obj;
			wars.add(warData);
		}
		return wars;
	}

	public static boolean isOpposingSides(Faction from, Faction to, WarData warData) {
		return warData.isOpposingSides(from.getIdFaction(), to.getIdFaction());
	}
}
