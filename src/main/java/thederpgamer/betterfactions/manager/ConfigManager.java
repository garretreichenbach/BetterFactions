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
			"diplomacy-non-existing-action-modifier: 1"
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