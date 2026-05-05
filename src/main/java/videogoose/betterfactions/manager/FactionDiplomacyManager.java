package videogoose.betterfactions.manager;

import api.common.GameCommon;
import api.common.GameServer;
import api.utils.StarRunnable;
import org.schema.schine.resource.tag.Tag;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacy;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacyConfig;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacyEntity;
import videogoose.betterfactions.data.diplomacy.FactionDiplomacyReaction;
import videogoose.betterfactions.data.diplomacy.action.FactionDiplomacyAction;
import videogoose.betterfactions.utils.DataUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages faction diplomacy data, persistence, configuration, and reactions.
 */
public class FactionDiplomacyManager {

	public static final ConcurrentLinkedQueue<FactionDiplomacy> diplomacyChanged = new ConcurrentLinkedQueue<>();
	private static final ConcurrentHashMap<Integer, FactionDiplomacy> diplomacyCache = new ConcurrentHashMap<>();
	private static volatile boolean initialized;
	private static FactionDiplomacyConfig diplomacyConfig;

	public static void initialize() {
		if (initialized) return;

		// Initialize the diplomacy config with default reactions
		diplomacyConfig = new FactionDiplomacyConfig();
		if (diplomacyConfig.reactions.isEmpty()) {
			diplomacyConfig.addDefaultActions();
		}

		File dir = new File(DataUtils.getWorldDataPath() + "/diplomacy");
		if (!dir.exists()) dir.mkdirs();

		new StarRunnable() {
			@Override
			public void run() {
				try {
					File[] files = dir.listFiles();
					if (files == null) return;
					for (File f : files) {
						if (!f.getName().endsWith(".smdat")) continue;
						int factionId = Integer.parseInt(f.getName().split("\\.")[0]);
						FactionDiplomacy diplomacy = new FactionDiplomacy(
							GameCommon.getGameState().getFactionManager().getFaction(factionId)
						);
						try (InputStream in = Files.newInputStream(f.toPath())) {
							diplomacy.fromTag(Tag.readFrom(in, true, false));
						}
						diplomacyCache.put(factionId, diplomacy);
						diplomacy.update(GameServer.getServerState().getController().getTimer().currentTime);
					}
				} catch (IOException e) {
					BetterFactions.getInstance().logException("Failed to load diplomacy files!", e);
				}
			}
		}.runTimer(BetterFactions.getInstance(), 300);

		new StarRunnable() {
			@Override
			public void run() {
				FactionDiplomacy diplomacy;
				while ((diplomacy = diplomacyChanged.poll()) != null) {
					File output = new File(DataUtils.getWorldDataPath() + "/diplomacy/" + diplomacy.faction.getIdFaction() + ".smdat");
					try {
						if (output.exists()) output.delete();
						output.createNewFile();
						try (OutputStream out = Files.newOutputStream(output.toPath())) {
							diplomacy.toTag().writeTo(out, true);
						}
					} catch (IOException e) {
						BetterFactions.getInstance().logException("Failed to save diplomacy data for faction " + diplomacy.faction.getIdFaction(), e);
					}
				}
			}
		}.runTimer(BetterFactions.getInstance(), 300);

		initialized = true;
	}

	private static FactionDiplomacy loadFromDisk(int factionId, File file) {
		FactionDiplomacy diplomacy = new FactionDiplomacy(
			Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFaction(factionId)
		);
		if (!file.exists()) {
			try (OutputStream out = Files.newOutputStream(file.toPath())) {
				diplomacy.toTag().writeTo(out, true);
			} catch (IOException e) {
				BetterFactions.getInstance().logException("Failed to initialize diplomacy data for faction " + factionId, e);
			}
		} else {
			try (InputStream in = Files.newInputStream(file.toPath())) {
				diplomacy.fromTag(Tag.readFrom(in, true, false));
			} catch (IOException e) {
				BetterFactions.getInstance().logException("Failed to load diplomacy data for faction " + factionId, e);
			}
		}
		return diplomacy;
	}

	public static FactionDiplomacy getDiplomacy(int factionId) {
		if (!initialized) initialize();
		return diplomacyCache.computeIfAbsent(factionId, id -> {
			File file = new File(DataUtils.getWorldDataPath() + "/diplomacy/" + id + ".smdat");
			return loadFromDisk(id, file);
		});
	}

	public static void invalidateCache(int factionId) {
		diplomacyCache.remove(factionId);
	}

	public static FactionDiplomacyConfig getConfig() {
		if (diplomacyConfig == null) {
			diplomacyConfig = new FactionDiplomacyConfig();
			if (diplomacyConfig.reactions.isEmpty()) {
				diplomacyConfig.addDefaultActions();
			}
		}
		return diplomacyConfig;
	}

	/**
	 * Returns all diplomacy reactions (global, not per-faction).
	 * These are checked periodically by each FactionDiplomacyEntity.
	 */
	public static List<FactionDiplomacyReaction> getReactions(int factionId) {
		if (!initialized) initialize();
		return getConfig().reactions;
	}

	/**
	 * Returns the per-action reaction from the diplomacy config, if one exists.
	 * Per-action reactions are defined in the XML config on individual DiplomacyConfigElements.
	 */
	public static FactionDiplomacyReaction getReaction(FactionDiplomacyAction action) {
		FactionDiplomacyConfig.DiplomacyConfigElement element = getConfig().get(action.type);
		if (element != null && element.reaction != null) {
			// Config element has a game-level DiplomacyReaction, but we need FactionDiplomacyReaction.
			// The default config doesn't define per-action reactions, so this path is for custom XML configs.
			// For now, search the global reactions list for one matching this action type.
			for (FactionDiplomacyReaction r : getConfig().reactions) {
				if (r.isSatisfied(null)) continue; // Skip reactions with no entity context
				return r; // Return first applicable reaction
			}
		}
		return null;
	}

	public static int getDiplomacyValue(FactionDiplomacyEntity.DiploStatusType status) {
		return switch (status) {
			case IN_WAR -> ConfigManager.valuesWar.getValue();
			case IN_WAR_WITH_ENEMY -> ConfigManager.valuesWarWithEnemy.getValue();
			case CLOSE_TERRITORY -> ConfigManager.valuesCloseTerritory.getValue();
			case POWER -> ConfigManager.valuesPower.getValue();
			case ALLIANCE -> ConfigManager.valuesAlliance.getValue();
			case ALLIANCE_WITH_ENEMY -> ConfigManager.valuesAllianceWithEnemy.getValue();
			case ALLIANCE_WITH_FRIENDS -> ConfigManager.valuesAllianceWithFriends.getValue();
			case NON_AGGRESSION -> ConfigManager.valuesNonAggression.getValue();
			case FACTION_MEMBER_AT_WAR_WITH_US -> ConfigManager.valuesFactionMemberAtWarWithUs.getValue();
			case FACTION_MEMBER_WE_DONT_LIKE -> ConfigManager.valuesFactionMemberWeDontLike.getValue();
			case IN_FEDERATION -> ConfigManager.valuesInFederation.getValue();
			case FEDERATION_ALLY -> ConfigManager.valuesFederationAlly.getValue();
			case FEDERATION_ENEMY -> ConfigManager.valuesFederationEnemy.getValue();
			case HAS_WAR_GOAL -> ConfigManager.valuesHasWarGoal.getValue();
			case RIVAL -> ConfigManager.valuesRival.getValue();
			case GUARANTEED_BY -> ConfigManager.valuesGuaranteedBy.getValue();
			case CONTESTED_CLAIMS -> ConfigManager.valuesContestedClaims.getValue();
			default -> 0;
		};
	}

	public static int getActionValue(FactionDiplomacyAction.DiploActionType action) {
		return switch (action) {
			case ATTACK -> ConfigManager.actionAttack.getValue();
			case ATTACK_ENEMY -> ConfigManager.actionAttackEnemy.getValue();
			case ATTACK_ALLY -> ConfigManager.actionAttackFriend.getValue();
			case MINING -> ConfigManager.actionMining.getValue();
			case TERRITORY -> ConfigManager.actionTerritory.getValue();
			case PEACE_OFFER -> ConfigManager.actionPeaceOffer.getValue();
			case ACCEPT_PEACE_OFFER -> ConfigManager.actionPeaceOfferAccepted.getValue();
			case REJECT_PEACE_OFFER -> ConfigManager.actionPeaceOfferRejected.getValue();
			case DECLARATION_OF_WAR -> ConfigManager.actionDeclarationOfWar.getValue();
			case ALLIANCE_REQUEST -> ConfigManager.actionAllianceRequest.getValue();
			case ACCEPT_ALLIANCE -> ConfigManager.actionAllianceRequestAccepted.getValue();
			case REJECT_ALLIANCE -> ConfigManager.actionAllianceRequestRejected.getValue();
			case ALLIANCE_CANCEL -> ConfigManager.actionAllianceCancel.getValue();
			case ALLIANCE_WITH_ENEMY -> ConfigManager.actionAllianceWithEnemy.getValue();
			case ALLIANCE_WITH_FRIEND -> ConfigManager.actionAllianceWithFriend.getValue();
			case TRADING_WITH_US -> ConfigManager.actionTradingWithUs.getValue();
			case TRADING_WITH_ENEMY -> ConfigManager.actionTradingWithEnemy.getValue();
			case ACCEPT_FEDERATION_OFFER -> ConfigManager.actionFederationOfferAccepted.getValue();
			case REJECT_FEDERATION_OFFER -> ConfigManager.actionFederationOfferRejected.getValue();
			case THREATENING -> ConfigManager.actionThreatening.getValue();
			case SEND_DEMAND -> ConfigManager.actionSendDemand.getValue();
			case ACCEPT_DEMAND -> ConfigManager.actionAcceptDemand.getValue();
			case REJECT_DEMAND -> ConfigManager.actionRejectDemand.getValue();
			case UNJUSTIFIED_WAR -> ConfigManager.actionUnjustifiedWar.getValue();
			case DECLARE_RIVALRY -> ConfigManager.actionDeclareRivalry.getValue();
			case GUARANTEE_INDEPENDENCE -> ConfigManager.actionGuaranteeIndependence.getValue();
			case IMPROVE_RELATIONS -> ConfigManager.actionImproveRelations.getValue();
			case DECREASE_RELATIONS -> ConfigManager.actionDecreaseRelations.getValue();
			case INSULT -> ConfigManager.actionInsult.getValue();
			case SEND_GIFT -> ConfigManager.actionSendGift.getValue();
			case EMBARGO -> ConfigManager.actionEmbargo.getValue();
			case BREAK_GUARANTEE -> ConfigManager.actionBreakGuarantee.getValue();
			case BAN_DIPLOMATS -> ConfigManager.actionBanDiplomats.getValue();
			default -> 0;
		};
	}

	public static void forceDiplomacyAction(int faction1, int faction2, FactionDiplomacyAction.DiploActionType diplomacyAction) {
		FactionDiplomacy diplomacy1 = getDiplomacy(faction1);
		FactionDiplomacy diplomacy2 = getDiplomacy(faction2);
		diplomacy1.diplomacyAction(diplomacyAction, diplomacy2.faction.getIdFaction());
	}
}
