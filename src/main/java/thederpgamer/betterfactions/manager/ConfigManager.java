package thederpgamer.betterfactions.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.betterfactions.BetterFactions;

public class ConfigManager {

	private static FileConfiguration mainConfig;
	private static FileConfiguration diplomacyConfig;
	private static final String[] defaultMainConfig = {
			"debug-mode: false",
			"max-world-logs: 5"
	};

	private static final String[] defaultDiplomacyConfig = {
			"diplomacy-start-points: 0",
			"diplomacy-min-points: -300",
			"diplomacy-max-points: 300",
			"diplomacy-apply-delay: 5000",
			"diplomacy-status-calc-delay: 5000",
			"diplomacy-change-check-delay: 5000",
			"diplomacy-static-timeout: 5000000",
			"diplomacy-turn-timeout: 500000",
			"diplomacy-action-timeout: 500000",
			"diplomacy-dynamic-upper: 30",
			"diplomacy-dynamic-lower: 0",
			"diplomacy-existing-action-modifier: 2",
			"diplomacy-non-existing-action-modifier: 1",

			"diplomacy-values-war: -70",
			"diplomacy-values-war-with-enemy: 50",
			"diplomacy-values-close-territory: 10",
			"diplomacy-values-power: 15",
			"diplomacy-values-alliance: 100",
			"diplomacy-values-alliance-with-enemy: -40",
			"diplomacy-values-alliance-with-friends: 50",
			"diplomacy-values-non-aggression: 50",
			"diplomacy-values-faction-member-at-war-with-us: -50",
			"diplomacy-values-faction-member-we-dont-like: -20",
			"diplomacy-values-in-federation: 80",
			"diplomacy-values-federation-ally: 60",
			"diplomacy-values-federation-enemy: -40",
			"diplomacy-values-has-war-goal: -70",

			"diplomacy-alliance-max-points: 100",
			"diplomacy-alliance-min-points: 50",
			"diplomacy-alliance-with-enemy-max-points: -50",
			"diplomacy-alliance-with-enemy-min-points: -100",
			"diplomacy-close-territory-max-points: -10",
			"diplomacy-close-territory-min-points: -20",
			"diplomacy-in-war-max-points: -100",
			"diplomacy-in-war-min-points: -200",
			"diplomacy-in-war-with-enemy-max-points: 50",
			"diplomacy-in-war-with-enemy-min-points: 10",
			"diplomacy-in-war-with-friends-max-points: -50",
			"diplomacy-in-war-with-friends-min-points: -80",
			"diplomacy-non-aggression-max-points: 30",
			"diplomacy-non-aggression-min-points: 10",
			"diplomacy-power-max-points: -10",
			"diplomacy-power-min-points: -20",
			"diplomacy-protecting-max-points: 50",
			"diplomacy-protecting-min-points: 30",
			"diplomacy-being-protected-max-points: 50",
			"diplomacy-being-protected-min-points: 30",
			"diplomacy-has-war-goal-max-points: -30",
			"diplomacy-has-war-goal-min-points: -50",
			"diplomacy-target-of-war-goal-max-points: -30",
			"diplomacy-target-of-war-goal-min-points: -50",
			"diplomacy-truce-max-points: 15",
			"diplomacy-truce-min-points: 5",
			"diplomacy-non-aggression-pact-max-points: 30",
			"diplomacy-non-aggression-pact-min-points: 15",
			"diplomacy-in-federation-max-points: 70",
			"diplomacy-in-federation-min-points: 30",
			"diplomacy-federation-ally-max-points: 60",
			"diplomacy-federation-ally-min-points: 30",
			"diplomacy-federation-enemy-max-points: -30",
			"diplomacy-federation-enemy-min-points: -60",

			"action-values-attack: -80",
			"action-values-attack-enemy: 30",
			"action-values-attack-friend: -50",
			"action-values-mining: -15",
			"action-values-territory: -20",
			"action-values-peace-offer: 15",
			"action-values-peace-offer-accepted: 20",
			"action-values-peace-offer-rejected: -30",
			"action-values-declaration-of-war: -100",
			"action-values-alliance-request: 30",
			"action-values-alliance-request-accepted: 50",
			"action-values-alliance-request-rejected: -30",
			"action-values-alliance-cancel: -50",
			"action-values-alliance-with-enemy: -30",
			"action-values-alliance-with-friend: 20",
			"action-values-trading-with-us: 15",
			"action-values-trading-with-enemy: -20",
			"action-values-federation-offer-accepted: 40",
			"action-values-federation-offer-rejected: -40",
			"action-values-federation-leave: -50",
			"action-values-threatening: -20"
	};


	public static void initialize(BetterFactions instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);

		diplomacyConfig = instance.getConfig("diplomacy");
		diplomacyConfig.saveDefault(defaultDiplomacyConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}

	public static FileConfiguration getDiplomacyConfig() {
		return diplomacyConfig;
	}
}