package videogoose.betterfactions.manager;

import api.utils.simpleconfig.SimpleConfigBool;
import api.utils.simpleconfig.SimpleConfigContainer;
import api.utils.simpleconfig.SimpleConfigInt;
import videogoose.betterfactions.BetterFactions;

/**
 * Manages mod configuration using SimpleConfig for automatic network syncing.
 */
public class ConfigManager {

	// Main config
	private static SimpleConfigContainer mainConfig;
	public static SimpleConfigBool debugMode;
	public static SimpleConfigInt maxNewsBackup;

	// Diplomacy config
	private static SimpleConfigContainer diplomacyConfig;

	// Core diplomacy settings
	public static SimpleConfigInt diplomacyStartPoints;
	public static SimpleConfigInt diplomacyMinPoints;
	public static SimpleConfigInt diplomacyMaxPoints;
	public static SimpleConfigInt diplomacyApplyDelay;
	public static SimpleConfigInt diplomacyStatusCalcDelay;
	public static SimpleConfigInt diplomacyChangeCheckDelay;
	public static SimpleConfigInt diplomacyStaticTimeout;
	public static SimpleConfigInt diplomacyTurnTimeout;
	public static SimpleConfigInt diplomacyActionTimeout;
	public static SimpleConfigInt diplomacyDynamicUpper;
	public static SimpleConfigInt diplomacyDynamicLower;
	public static SimpleConfigInt diplomacyExistingActionModifier;
	public static SimpleConfigInt diplomacyNonExistingActionModifier;

	// Diplomacy status values
	public static SimpleConfigInt valuesWar;
	public static SimpleConfigInt valuesWarWithEnemy;
	public static SimpleConfigInt valuesCloseTerritory;
	public static SimpleConfigInt valuesPower;
	public static SimpleConfigInt valuesAlliance;
	public static SimpleConfigInt valuesAllianceWithEnemy;
	public static SimpleConfigInt valuesAllianceWithFriends;
	public static SimpleConfigInt valuesNonAggression;
	public static SimpleConfigInt valuesFactionMemberAtWarWithUs;
	public static SimpleConfigInt valuesFactionMemberWeDontLike;
	public static SimpleConfigInt valuesInFederation;
	public static SimpleConfigInt valuesFederationAlly;
	public static SimpleConfigInt valuesFederationEnemy;
	public static SimpleConfigInt valuesHasWarGoal;

	// Diplomacy status point ranges (min/max)
	public static SimpleConfigInt allianceMaxPoints;
	public static SimpleConfigInt allianceMinPoints;
	public static SimpleConfigInt allianceWithEnemyMaxPoints;
	public static SimpleConfigInt allianceWithEnemyMinPoints;
	public static SimpleConfigInt closeTerritoryMaxPoints;
	public static SimpleConfigInt closeTerritoryMinPoints;
	public static SimpleConfigInt inWarMaxPoints;
	public static SimpleConfigInt inWarMinPoints;
	public static SimpleConfigInt inWarWithEnemyMaxPoints;
	public static SimpleConfigInt inWarWithEnemyMinPoints;
	public static SimpleConfigInt inWarWithFriendsMaxPoints;
	public static SimpleConfigInt inWarWithFriendsMinPoints;
	public static SimpleConfigInt nonAggressionMaxPoints;
	public static SimpleConfigInt nonAggressionMinPoints;
	public static SimpleConfigInt powerMaxPoints;
	public static SimpleConfigInt powerMinPoints;
	public static SimpleConfigInt protectingMaxPoints;
	public static SimpleConfigInt protectingMinPoints;
	public static SimpleConfigInt beingProtectedMaxPoints;
	public static SimpleConfigInt beingProtectedMinPoints;
	public static SimpleConfigInt hasWarGoalMaxPoints;
	public static SimpleConfigInt hasWarGoalMinPoints;
	public static SimpleConfigInt targetOfWarGoalMaxPoints;
	public static SimpleConfigInt targetOfWarGoalMinPoints;
	public static SimpleConfigInt truceMaxPoints;
	public static SimpleConfigInt truceMinPoints;
	public static SimpleConfigInt nonAggressionPactMaxPoints;
	public static SimpleConfigInt nonAggressionPactMinPoints;
	public static SimpleConfigInt inFederationMaxPoints;
	public static SimpleConfigInt inFederationMinPoints;
	public static SimpleConfigInt federationAllyMaxPoints;
	public static SimpleConfigInt federationAllyMinPoints;
	public static SimpleConfigInt federationEnemyMaxPoints;
	public static SimpleConfigInt federationEnemyMinPoints;
	public static SimpleConfigInt allianceWithFriendsMaxPoints;
	public static SimpleConfigInt allianceWithFriendsMinPoints;

	// Action values
	public static SimpleConfigInt actionAttack;
	public static SimpleConfigInt actionAttackEnemy;
	public static SimpleConfigInt actionAttackFriend;
	public static SimpleConfigInt actionMining;
	public static SimpleConfigInt actionTerritory;
	public static SimpleConfigInt actionPeaceOffer;
	public static SimpleConfigInt actionPeaceOfferAccepted;
	public static SimpleConfigInt actionPeaceOfferRejected;
	public static SimpleConfigInt actionDeclarationOfWar;
	public static SimpleConfigInt actionAllianceRequest;
	public static SimpleConfigInt actionAllianceRequestAccepted;
	public static SimpleConfigInt actionAllianceRequestRejected;
	public static SimpleConfigInt actionAllianceCancel;
	public static SimpleConfigInt actionAllianceWithEnemy;
	public static SimpleConfigInt actionAllianceWithFriend;
	public static SimpleConfigInt actionTradingWithUs;
	public static SimpleConfigInt actionTradingWithEnemy;
	public static SimpleConfigInt actionFederationOfferAccepted;
	public static SimpleConfigInt actionFederationOfferRejected;
	public static SimpleConfigInt actionFederationLeave;
	public static SimpleConfigInt actionThreatening;
	public static SimpleConfigInt actionSendDemand;
	public static SimpleConfigInt actionAcceptDemand;
	public static SimpleConfigInt actionRejectDemand;

	public static void initialize(BetterFactions instance) {
		initMainConfig(instance);
		initDiplomacyConfig(instance);
	}

	private static void initMainConfig(BetterFactions instance) {
		mainConfig = new SimpleConfigContainer(instance, "config", false);
		debugMode = new SimpleConfigBool(mainConfig, "debug-mode", false);
		maxNewsBackup = new SimpleConfigInt(mainConfig, "max-news-backup", 30);
		mainConfig.readWriteFields();
	}

	private static void initDiplomacyConfig(BetterFactions instance) {
		diplomacyConfig = new SimpleConfigContainer(instance, "diplomacy", false);

		// Core settings
		diplomacyStartPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-start-points", 0);
		diplomacyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-min-points", -300);
		diplomacyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-max-points", 300);
		diplomacyApplyDelay = new SimpleConfigInt(diplomacyConfig, "diplomacy-apply-delay", 5000);
		diplomacyStatusCalcDelay = new SimpleConfigInt(diplomacyConfig, "diplomacy-status-calc-delay", 5000);
		diplomacyChangeCheckDelay = new SimpleConfigInt(diplomacyConfig, "diplomacy-change-check-delay", 5000);
		diplomacyStaticTimeout = new SimpleConfigInt(diplomacyConfig, "diplomacy-static-timeout", 5000000);
		diplomacyTurnTimeout = new SimpleConfigInt(diplomacyConfig, "diplomacy-turn-timeout", 500000);
		diplomacyActionTimeout = new SimpleConfigInt(diplomacyConfig, "diplomacy-action-timeout", 500000);
		diplomacyDynamicUpper = new SimpleConfigInt(diplomacyConfig, "diplomacy-dynamic-upper", 30);
		diplomacyDynamicLower = new SimpleConfigInt(diplomacyConfig, "diplomacy-dynamic-lower", 0);
		diplomacyExistingActionModifier = new SimpleConfigInt(diplomacyConfig, "diplomacy-existing-action-modifier", 2);
		diplomacyNonExistingActionModifier = new SimpleConfigInt(diplomacyConfig, "diplomacy-non-existing-action-modifier", 1);

		// Status values
		valuesWar = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-war", -70);
		valuesWarWithEnemy = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-war-with-enemy", 50);
		valuesCloseTerritory = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-close-territory", 10);
		valuesPower = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-power", 15);
		valuesAlliance = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-alliance", 100);
		valuesAllianceWithEnemy = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-alliance-with-enemy", -40);
		valuesAllianceWithFriends = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-alliance-with-friends", 50);
		valuesNonAggression = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-non-aggression", 50);
		valuesFactionMemberAtWarWithUs = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-faction-member-at-war-with-us", -50);
		valuesFactionMemberWeDontLike = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-faction-member-we-dont-like", -20);
		valuesInFederation = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-in-federation", 80);
		valuesFederationAlly = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-federation-ally", 60);
		valuesFederationEnemy = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-federation-enemy", -40);
		valuesHasWarGoal = new SimpleConfigInt(diplomacyConfig, "diplomacy-values-has-war-goal", -70);

		// Status point ranges
		allianceMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-alliance-max-points", 100);
		allianceMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-alliance-min-points", 50);
		allianceWithEnemyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-alliance-with-enemy-max-points", -50);
		allianceWithEnemyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-alliance-with-enemy-min-points", -100);
		allianceWithFriendsMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-alliance-with-friends-max-points", 50);
		allianceWithFriendsMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-alliance-with-friends-min-points", 10);
		closeTerritoryMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-close-territory-max-points", -10);
		closeTerritoryMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-close-territory-min-points", -20);
		inWarMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-in-war-max-points", -100);
		inWarMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-in-war-min-points", -200);
		inWarWithEnemyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-in-war-with-enemy-max-points", 50);
		inWarWithEnemyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-in-war-with-enemy-min-points", 10);
		inWarWithFriendsMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-in-war-with-friends-max-points", -50);
		inWarWithFriendsMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-in-war-with-friends-min-points", -80);
		nonAggressionMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-non-aggression-max-points", 30);
		nonAggressionMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-non-aggression-min-points", 10);
		powerMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-power-max-points", -10);
		powerMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-power-min-points", -20);
		protectingMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-protecting-max-points", 50);
		protectingMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-protecting-min-points", 30);
		beingProtectedMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-being-protected-max-points", 50);
		beingProtectedMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-being-protected-min-points", 30);
		hasWarGoalMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-has-war-goal-max-points", -30);
		hasWarGoalMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-has-war-goal-min-points", -50);
		targetOfWarGoalMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-target-of-war-goal-max-points", -30);
		targetOfWarGoalMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-target-of-war-goal-min-points", -50);
		truceMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-truce-max-points", 15);
		truceMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-truce-min-points", 5);
		nonAggressionPactMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-non-aggression-pact-max-points", 30);
		nonAggressionPactMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-non-aggression-pact-min-points", 15);
		inFederationMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-in-federation-max-points", 70);
		inFederationMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-in-federation-min-points", 30);
		federationAllyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-federation-ally-max-points", 60);
		federationAllyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-federation-ally-min-points", 30);
		federationEnemyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-federation-enemy-max-points", -30);
		federationEnemyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy-federation-enemy-min-points", -60);

		// Action values
		actionAttack = new SimpleConfigInt(diplomacyConfig, "action-values-attack", -80);
		actionAttackEnemy = new SimpleConfigInt(diplomacyConfig, "action-values-attack-enemy", 30);
		actionAttackFriend = new SimpleConfigInt(diplomacyConfig, "action-values-attack-friend", -50);
		actionMining = new SimpleConfigInt(diplomacyConfig, "action-values-mining", -15);
		actionTerritory = new SimpleConfigInt(diplomacyConfig, "action-values-territory", -20);
		actionPeaceOffer = new SimpleConfigInt(diplomacyConfig, "action-values-peace-offer", 15);
		actionPeaceOfferAccepted = new SimpleConfigInt(diplomacyConfig, "action-values-peace-offer-accepted", 20);
		actionPeaceOfferRejected = new SimpleConfigInt(diplomacyConfig, "action-values-peace-offer-rejected", -30);
		actionDeclarationOfWar = new SimpleConfigInt(diplomacyConfig, "action-values-declaration-of-war", -100);
		actionAllianceRequest = new SimpleConfigInt(diplomacyConfig, "action-values-alliance-request", 30);
		actionAllianceRequestAccepted = new SimpleConfigInt(diplomacyConfig, "action-values-alliance-request-accepted", 50);
		actionAllianceRequestRejected = new SimpleConfigInt(diplomacyConfig, "action-values-alliance-request-rejected", -30);
		actionAllianceCancel = new SimpleConfigInt(diplomacyConfig, "action-values-alliance-cancel", -50);
		actionAllianceWithEnemy = new SimpleConfigInt(diplomacyConfig, "action-values-alliance-with-enemy", -30);
		actionAllianceWithFriend = new SimpleConfigInt(diplomacyConfig, "action-values-alliance-with-friend", 20);
		actionTradingWithUs = new SimpleConfigInt(diplomacyConfig, "action-values-trading-with-us", 15);
		actionTradingWithEnemy = new SimpleConfigInt(diplomacyConfig, "action-values-trading-with-enemy", -20);
		actionFederationOfferAccepted = new SimpleConfigInt(diplomacyConfig, "action-values-federation-offer-accepted", 40);
		actionFederationOfferRejected = new SimpleConfigInt(diplomacyConfig, "action-values-federation-offer-rejected", -40);
		actionFederationLeave = new SimpleConfigInt(diplomacyConfig, "action-values-federation-leave", -50);
		actionThreatening = new SimpleConfigInt(diplomacyConfig, "action-values-threatening", -20);
		actionSendDemand = new SimpleConfigInt(diplomacyConfig, "action-values-send-demand", -15);
		actionAcceptDemand = new SimpleConfigInt(diplomacyConfig, "action-values-accept-demand", 10);
		actionRejectDemand = new SimpleConfigInt(diplomacyConfig, "action-values-reject-demand", -25);

		diplomacyConfig.readWriteFields();
	}
}
