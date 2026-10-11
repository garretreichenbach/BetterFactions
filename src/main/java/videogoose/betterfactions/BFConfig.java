package videogoose.betterfactions;

import api.config.simpleconfig.SimpleConfigBool;
import api.config.simpleconfig.SimpleConfigContainer;
import api.config.simpleconfig.SimpleConfigInt;

/**
 * Server settings, synced to clients so the GUI can hide what the server would refuse anyway.
 */
public final class BFConfig {

	private static SimpleConfigBool customEmblems;
	private static SimpleConfigInt tradeDiscount;

	private BFConfig() {
	}

	static void initialize(BetterFactions mod) {
		SimpleConfigContainer config = new SimpleConfigContainer(mod, "config", false);
		customEmblems = new SimpleConfigBool(config, "custom_emblems_enabled", false,
				"If true, factions may upload their own emblem image. Off by default: turn it on only if you are willing to moderate what players upload (admins can remove one with /faction_emblem_clear).");
		tradeDiscount = new SimpleConfigInt(config, "trade_agreement_shop_discount_percent", 10,
				"Percent off buying from, and extra when selling to, a shop whose faction has a trade agreement with yours. 0 turns it off.");
		config.readWriteFields();
	}

	public static boolean customEmblemsEnabled() {
		return customEmblems != null && customEmblems.getValue() != null && customEmblems.getValue();
	}

	public static int tradeDiscountPercent() {
		Integer v = tradeDiscount != null ? tradeDiscount.getValue() : null;
		return v == null ? 0 : Math.max(0, Math.min(100, v));
	}
}
