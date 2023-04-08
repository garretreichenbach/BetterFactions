package thederpgamer.betterfactions.manager;

import api.common.GameCommon;
import api.common.GameServer;
import api.utils.StarRunnable;
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
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class FactionDiplomacyManager {

	public static final Queue<FactionDiplomacy> diplomacyChanged = new ConcurrentLinkedQueue<>();
	private static boolean initialized;

	public static void initialize() {
		final File file = new File(DataUtils.getWorldDataPath() + "/diplomacy");
		if(!file.exists()) file.mkdirs();
		new StarRunnable() {
			@Override
			public void run() {
				try {
					for(File f : Objects.requireNonNull(file.listFiles())) {
						if(f.getName().endsWith(".smdat")) {
							FactionDiplomacy diplomacy = new FactionDiplomacy(GameCommon.getGameState().getFactionManager().getFaction(Integer.parseInt(f.getName().split("\\.")[0])));
							diplomacy.fromTag(Tag.readFrom(Files.newInputStream(f.toPath()), true, false));
							diplomacy.update(GameServer.getServerState().getController().getTimer().currentTime);
						}
					}
				} catch(IOException exception) {
					BetterFactions.log.log(Level.WARNING, "Failed to load diplomacy files!");
				}
			}
		}.runTimer(BetterFactions.getInstance(), 300);
		new StarRunnable() {
			@Override
			public void run() {
				FactionDiplomacy diplomacy = diplomacyChanged.poll();
				while(diplomacy != null) {
					File output = new File(DataUtils.getWorldDataPath() + "/diplomacy/" + diplomacy.faction.getIdFaction() + ".smdat");
					if(output.exists()) output.delete();
					try {
						output.createNewFile();
						diplomacy.toTag().writeTo(Files.newOutputStream(output.toPath()), true);
						diplomacy = diplomacyChanged.poll();
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}.runTimer(BetterFactions.getInstance(), 300);
		initialized = true;
	}

	private static void initDiplomacyData(int factionId, File file) {
		FactionDiplomacy diplomacy = new FactionDiplomacy(Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(factionId));
		try {
			diplomacy.toTag().writeTo(Files.newOutputStream(file.toPath()), true);
		} catch(IOException exception) {
			BetterFactions.log.log(Level.WARNING, "Failed to initialize diplomacy data for faction " + factionId + "!", exception);
		}
	}

	public static FactionDiplomacy getDiplomacy(int factionId) {
		if(!initialized) initialize();
		File file = new File(DataUtils.getWorldDataPath() + "/diplomacy/" + factionId + ".smdat");
		if(!file.exists()) initDiplomacyData(factionId, file);
		FactionDiplomacy diplomacy = new FactionDiplomacy(Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(factionId));
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
			case HAS_WAR_GOAL:
				return ConfigManager.getDiplomacyConfig().getInt("diplomacy-values-has-war-goal");
			default:
				return 0;
		}
	}

	public static int getActionValue(FactionDiplomacyAction.DiploActionType action) {
		switch(action) {
			case ATTACK:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-attack");
			case ATTACK_ENEMY:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-attack-enemy");
			case ATTACK_ALLY:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-attack-friend");
			case MINING:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-mining");
			case TERRITORY:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-territory");
			case PEACE_OFFER:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-peace-offer");
			case ACCEPT_PEACE_OFFER:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-peace-offer-accepted");
			case REJECT_PEACE_OFFER:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-peace-offer-rejected");
			case DECLARATION_OF_WAR:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-declaration-of-war");
			case ALLIANCE_REQUEST:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-alliance-request");
			case ACCEPT_ALLIANCE:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-alliance-request-accepted");
			case REJECT_ALLIANCE:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-alliance-request-rejected");
			case ALLIANCE_CANCEL:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-alliance-cancel");
			case ALLIANCE_WITH_ENEMY:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-alliance-with-enemy");
			case ALLIANCE_WITH_FRIEND:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-alliance-with-friend");
			case TRADING_WITH_US:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-trading-with-us");
			case TRADING_WITH_ENEMY:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-trading-with-enemy");
			case ACCEPT_FEDERATION_OFFER:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-federation-offer-accepted");
			case REJECT_FEDERATION_OFFER:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-federation-offer-rejected");
			case THREATENING:
				return ConfigManager.getDiplomacyConfig().getInt("action-values-threatening");
			default:
				return 0;
		}
	}

	public static FactionDiplomacyReaction getReaction(FactionDiplomacyAction action) {
		return null; //TODO: Implement
	}

	public static boolean existsAction(FactionDiplomacyTurnMod d) {
		return false; //TODO: Implement
	}

	public static void forceDiplomacyAction(int faction1, int faction2, FactionDiplomacyAction.DiploActionType diplomacyAction) {
		FactionDiplomacy diplomacy1 = getDiplomacy(faction1);
		FactionDiplomacy diplomacy2 = getDiplomacy(faction2);
		diplomacy1.diplomacyAction(diplomacyAction, diplomacy2.faction.getIdFaction());
	}
}
