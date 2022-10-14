package thederpgamer.betterfactions.manager;

import api.common.GameCommon;
import org.schema.schine.resource.tag.Tag;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacy;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacyEntity;
import thederpgamer.betterfactions.data.diplomacy.FactionDiplomacyReaction;
import thederpgamer.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import thederpgamer.betterfactions.data.diplomacy.modifier.FactionDiplomacyTurnMod;
import thederpgamer.betterfactions.utils.DataUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class FactionDiplomacyManager {

	public static final ArrayList<FactionDiplomacy> diplomacyChanged = new ArrayList<>(); //Todo: Update these
	private static boolean initialized = false;

	private static void initialize() {
		File file = new File(DataUtils.getWorldDataPath() + "diplomacy");
		if(!file.exists()) file.mkdirs();
	}

	private static void initDiplomacyData(int factionId, File file) {
		FactionDiplomacy diplomacy = new FactionDiplomacy(GameCommon.getGameState().getFactionManager().getFaction(factionId));
		try {
			diplomacy.toTag().writeTo(Files.newOutputStream(file.toPath()), true);
		} catch(IOException exception) {
			BetterFactions.log.log(Level.WARNING, "Failed to initialize diplomacy data for faction " + factionId + "!", exception);
		}
	}

	private static void initReactionData(int factionId, File file) {
	}


	public static FactionDiplomacy getDiplomacy(int factionId) {
		if(!initialized) initialize();
		File file = new File(DataUtils.getWorldDataPath() + "diplomacy/" + factionId + ".smdat");
		if(!file.exists()) initDiplomacyData(factionId, file);
		FactionDiplomacy diplomacy = new FactionDiplomacy(GameCommon.getGameState().getFactionManager().getFaction(factionId));
		try {
			diplomacy.fromTag(Tag.readFrom(Files.newInputStream(file.toPath()), true, false));
		} catch(IOException exception) {
			BetterFactions.log.log(Level.WARNING, "Failed to load diplomacy data for faction " + factionId + "!", exception);
		}
		return diplomacy;
	}

	public static List<FactionDiplomacyReaction> getReactions(int factionId) {
		if(!initialized) initialize();
		return new ArrayList<>(); //Todo: Implement
	}

	public static int getDiplomacyValue(FactionDiplomacyEntity.DiploStatusType status) {
		switch(status) {
			case IN_WAR:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-war");
			case IN_WAR_WITH_ENEMY:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-war-with-enemy");
			case CLOSE_TERRITORY:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-close-territory");
			case POWER:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-power");
			case ALLIANCE:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-alliance");
			case ALLIANCE_WITH_ENEMY:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-alliance-with-enemy");
			case ALLIANCE_WITH_FRIENDS:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-alliance-with-friends");
			case NON_AGGRESSION:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-non-aggression");
			case FACTION_MEMBER_AT_WAR_WITH_US:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-faction-member-at-war-with-us");
			case FACTION_MEMBER_WE_DONT_LIKE:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-faction-member-we-dont-like");
			case IN_FEDERATION:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-in-federation");
			case FEDERATION_ALLY:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-federation-ally");
			case FEDERATION_ENEMY:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-federation-enemy");
			default:
				return 0;
		}
	}

	public static int getActionValue(FactionDiplomacyAction.DiplActionType action) {
		return 0; //TODO: Implement
	}

	public static FactionDiplomacyReaction getReaction(FactionDiplomacyAction action) {
		return null; //TODO: Implement
	}

	public static boolean existsAction(FactionDiplomacyTurnMod d) {
		return false; //TODO: Implement
	}
}
