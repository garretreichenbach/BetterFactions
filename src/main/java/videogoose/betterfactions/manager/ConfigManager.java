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
	public static SimpleConfigInt actionUnjustifiedWar;
	public static SimpleConfigInt actionDeclareRivalry;
	public static SimpleConfigInt actionGuaranteeIndependence;
	public static SimpleConfigInt actionImproveRelations;
	public static SimpleConfigInt actionDecreaseRelations;
	public static SimpleConfigInt actionInsult;
	public static SimpleConfigInt actionSendGift;
	public static SimpleConfigInt actionEmbargo;
	public static SimpleConfigInt actionBreakGuarantee;
	public static SimpleConfigInt actionBanDiplomats;
	public static SimpleConfigInt banDiplomatsDurationMs;

	// Casus belli settings
	public static SimpleConfigInt containmentOpinionThreshold;

	// War exhaustion
	public static SimpleConfigBool warExhaustionEnabled;
	public static SimpleConfigInt warExhaustionPerDay;
	public static SimpleConfigInt warExhaustionMaxForStatusQuo;

	// Status values for new types
	public static SimpleConfigInt valuesRival;
	public static SimpleConfigInt valuesGuaranteedBy;
	public static SimpleConfigInt valuesContestedClaims;

	public static void initialize(BetterFactions instance) {
		initMainConfig(instance);
		initDiplomacyConfig(instance);
	}

	private static void initMainConfig(BetterFactions instance) {
		mainConfig = new SimpleConfigContainer(instance, "config", false);
		debugMode = new SimpleConfigBool(mainConfig, "debug_mode", false);
		maxNewsBackup = new SimpleConfigInt(mainConfig, "max_news_backup", 30);
		mainConfig.readWriteFields();
	}

	private static void initDiplomacyConfig(BetterFactions instance) {
		diplomacyConfig = new SimpleConfigContainer(instance, "diplomacy", false);

		// Core settings
		diplomacyStartPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_start_points", 0);
		diplomacyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_min_points", -300);
		diplomacyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_max_points", 300);
		diplomacyApplyDelay = new SimpleConfigInt(diplomacyConfig, "diplomacy_apply_delay", 5000);
		diplomacyStatusCalcDelay = new SimpleConfigInt(diplomacyConfig, "diplomacy_status_calc_delay", 5000);
		diplomacyChangeCheckDelay = new SimpleConfigInt(diplomacyConfig, "diplomacy_change_check_delay", 5000);
		diplomacyStaticTimeout = new SimpleConfigInt(diplomacyConfig, "diplomacy_static_timeout", 5000000);
		diplomacyTurnTimeout = new SimpleConfigInt(diplomacyConfig, "diplomacy_turn_timeout", 500000);
		diplomacyActionTimeout = new SimpleConfigInt(diplomacyConfig, "diplomacy_action_timeout", 500000);
		diplomacyDynamicUpper = new SimpleConfigInt(diplomacyConfig, "diplomacy_dynamic_upper", 30);
		diplomacyDynamicLower = new SimpleConfigInt(diplomacyConfig, "diplomacy_dynamic_lower", 0);
		diplomacyExistingActionModifier = new SimpleConfigInt(diplomacyConfig, "diplomacy_existing_action_modifier", 2);
		diplomacyNonExistingActionModifier = new SimpleConfigInt(diplomacyConfig, "diplomacy_non_existing_action_modifier", 1);

		// Status values
		valuesWar = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_war", -70);
		valuesWarWithEnemy = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_war_with_enemy", 50);
		valuesCloseTerritory = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_close_territory", 10);
		valuesPower = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_power", 15);
		valuesAlliance = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_alliance", 100);
		valuesAllianceWithEnemy = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_alliance_with_enemy", -40);
		valuesAllianceWithFriends = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_alliance_with_friends", 50);
		valuesNonAggression = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_non_aggression", 50);
		valuesFactionMemberAtWarWithUs = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_faction_member_at_war_with_us", -50);
		valuesFactionMemberWeDontLike = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_faction_member_we_dont_like", -20);
		valuesInFederation = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_in_federation", 80);
		valuesFederationAlly = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_federation_ally", 60);
		valuesFederationEnemy = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_federation_enemy", -40);
		valuesHasWarGoal = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_has_war_goal", -70);

		// Status point ranges
		allianceMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_alliance_max_points", 100);
		allianceMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_alliance_min_points", 50);
		allianceWithEnemyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_alliance_with_enemy_max_points", -50);
		allianceWithEnemyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_alliance_with_enemy_min_points", -100);
		allianceWithFriendsMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_alliance_with_friends_max_points", 50);
		allianceWithFriendsMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_alliance_with_friends_min_points", 10);
		closeTerritoryMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_close_territory_max_points", -10);
		closeTerritoryMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_close_territory_min_points", -20);
		inWarMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_in_war_max_points", -100);
		inWarMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_in_war_min_points", -200);
		inWarWithEnemyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_in_war_with_enemy_max_points", 50);
		inWarWithEnemyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_in_war_with_enemy_min_points", 10);
		inWarWithFriendsMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_in_war_with_friends_max_points", -50);
		inWarWithFriendsMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_in_war_with_friends_min_points", -80);
		nonAggressionMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_non_aggression_max_points", 30);
		nonAggressionMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_non_aggression_min_points", 10);
		powerMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_power_max_points", -10);
		powerMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_power_min_points", -20);
		protectingMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_protecting_max_points", 50);
		protectingMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_protecting_min_points", 30);
		beingProtectedMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_being_protected_max_points", 50);
		beingProtectedMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_being_protected_min_points", 30);
		hasWarGoalMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_has_war_goal_max_points", -30);
		hasWarGoalMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_has_war_goal_min_points", -50);
		targetOfWarGoalMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_target_of_war_goal_max_points", -30);
		targetOfWarGoalMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_target_of_war_goal_min_points", -50);
		truceMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_truce_max_points", 15);
		truceMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_truce_min_points", 5);
		nonAggressionPactMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_non_aggression_pact_max_points", 30);
		nonAggressionPactMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_non_aggression_pact_min_points", 15);
		inFederationMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_in_federation_max_points", 70);
		inFederationMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_in_federation_min_points", 30);
		federationAllyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_federation_ally_max_points", 60);
		federationAllyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_federation_ally_min_points", 30);
		federationEnemyMaxPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_federation_enemy_max_points", -30);
		federationEnemyMinPoints = new SimpleConfigInt(diplomacyConfig, "diplomacy_federation_enemy_min_points", -60);

		// Action values
		actionAttack = new SimpleConfigInt(diplomacyConfig, "action_values_attack", -80);
		actionAttackEnemy = new SimpleConfigInt(diplomacyConfig, "action_values_attack_enemy", 30);
		actionAttackFriend = new SimpleConfigInt(diplomacyConfig, "action_values_attack_friend", -50);
		actionMining = new SimpleConfigInt(diplomacyConfig, "action_values_mining", -15);
		actionTerritory = new SimpleConfigInt(diplomacyConfig, "action_values_territory", -20);
		actionPeaceOffer = new SimpleConfigInt(diplomacyConfig, "action_values_peace_offer", 15);
		actionPeaceOfferAccepted = new SimpleConfigInt(diplomacyConfig, "action_values_peace_offer_accepted", 20);
		actionPeaceOfferRejected = new SimpleConfigInt(diplomacyConfig, "action_values_peace_offer_rejected", -30);
		actionDeclarationOfWar = new SimpleConfigInt(diplomacyConfig, "action_values_declaration_of_war", -100);
		actionAllianceRequest = new SimpleConfigInt(diplomacyConfig, "action_values_alliance_request", 30);
		actionAllianceRequestAccepted = new SimpleConfigInt(diplomacyConfig, "action_values_alliance_request_accepted", 50);
		actionAllianceRequestRejected = new SimpleConfigInt(diplomacyConfig, "action_values_alliance_request_rejected", -30);
		actionAllianceCancel = new SimpleConfigInt(diplomacyConfig, "action_values_alliance_cancel", -50);
		actionAllianceWithEnemy = new SimpleConfigInt(diplomacyConfig, "action_values_alliance_with_enemy", -30);
		actionAllianceWithFriend = new SimpleConfigInt(diplomacyConfig, "action_values_alliance_with_friend", 20);
		actionTradingWithUs = new SimpleConfigInt(diplomacyConfig, "action_values_trading_with_us", 15);
		actionTradingWithEnemy = new SimpleConfigInt(diplomacyConfig, "action_values_trading_with_enemy", -20);
		actionFederationOfferAccepted = new SimpleConfigInt(diplomacyConfig, "action_values_federation_offer_accepted", 40);
		actionFederationOfferRejected = new SimpleConfigInt(diplomacyConfig, "action_values_federation_offer_rejected", -40);
		actionFederationLeave = new SimpleConfigInt(diplomacyConfig, "action_values_federation_leave", -50);
		actionThreatening = new SimpleConfigInt(diplomacyConfig, "action_values_threatening", -20);
		actionSendDemand = new SimpleConfigInt(diplomacyConfig, "action_values_send_demand", -15);
		actionAcceptDemand = new SimpleConfigInt(diplomacyConfig, "action_values_accept_demand", 10);
		actionRejectDemand = new SimpleConfigInt(diplomacyConfig, "action_values_reject_demand", -25);
		actionUnjustifiedWar = new SimpleConfigInt(diplomacyConfig, "action_values_unjustified_war", -100);
		actionDeclareRivalry = new SimpleConfigInt(diplomacyConfig, "action_values_declare_rivalry", -30);
		actionGuaranteeIndependence = new SimpleConfigInt(diplomacyConfig, "action_values_guarantee_independence", 40);
		actionImproveRelations = new SimpleConfigInt(diplomacyConfig, "action_values_improve_relations", 2);
		actionDecreaseRelations = new SimpleConfigInt(diplomacyConfig, "action_values_decrease_relations", -2);
		actionInsult = new SimpleConfigInt(diplomacyConfig, "action_values_insult", -40);
		actionSendGift = new SimpleConfigInt(diplomacyConfig, "action_values_send_gift", 20);
		actionEmbargo = new SimpleConfigInt(diplomacyConfig, "action_values_embargo", -30);
		actionBreakGuarantee = new SimpleConfigInt(diplomacyConfig, "action_values_break_guarantee", -50);
		actionBanDiplomats = new SimpleConfigInt(diplomacyConfig, "action_values_ban_diplomats", -20);
		banDiplomatsDurationMs = new SimpleConfigInt(diplomacyConfig, "ban_diplomats_duration_ms", 600000);

		// Casus belli
		containmentOpinionThreshold = new SimpleConfigInt(diplomacyConfig, "containment_opinion_threshold", -50);

		// War exhaustion
		warExhaustionEnabled = new SimpleConfigBool(diplomacyConfig, "war_exhaustion_enabled", false);
		warExhaustionPerDay = new SimpleConfigInt(diplomacyConfig, "war_exhaustion_per_day", 2);
		warExhaustionMaxForStatusQuo = new SimpleConfigInt(diplomacyConfig, "war_exhaustion_forced_peace", 100);

		// New status values
		valuesRival = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_rival", -50);
		valuesGuaranteedBy = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_guaranteed_by", 30);
		valuesContestedClaims = new SimpleConfigInt(diplomacyConfig, "diplomacy_values_contested_claims", -25);

		diplomacyConfig.readWriteFields();
	}
}
