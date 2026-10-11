package videogoose.betterfactions;

import api.config.simpleconfig.SimpleConfigBool;
import api.config.simpleconfig.SimpleConfigContainer;

/**
 * Server settings, synced to clients so the GUI can hide what the server would refuse anyway.
 */
public final class BFConfig {

	private static SimpleConfigBool customEmblems;

	private BFConfig() {
	}

	static void initialize(BetterFactions mod) {
		SimpleConfigContainer config = new SimpleConfigContainer(mod, "config", false);
		customEmblems = new SimpleConfigBool(config, "custom_emblems_enabled", false,
				"If true, factions may upload their own emblem image. Off by default: turn it on only if you are willing to moderate what players upload (admins can remove one with /faction_emblem_clear).");
		config.readWriteFields();
	}

	public static boolean customEmblemsEnabled() {
		return customEmblems != null && customEmblems.getValue() != null && customEmblems.getValue();
	}
}
