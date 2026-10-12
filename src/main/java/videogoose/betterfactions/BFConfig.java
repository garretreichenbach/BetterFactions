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
	private static SimpleConfigInt maxSoftBattles;
	private static SimpleConfigInt softAiTickDivisor;
	private static SimpleConfigInt softEntityTickDivisor;
	private static SimpleConfigInt autoResolveSeconds;
	private static SimpleConfigInt battleStartBelowPercent;
	private static SimpleConfigInt battleHoldAbovePercent;
	private static SimpleConfigInt encounterTimeoutMinutes;

	private BFConfig() {
	}

	static void initialize(BetterFactions mod) {
		SimpleConfigContainer config = new SimpleConfigContainer(mod, "config", false);
		customEmblems = new SimpleConfigBool(config, "custom_emblems_enabled", false,
				"If true, factions may upload their own emblem image. Off by default: turn it on only if you are willing to moderate what players upload (admins can remove one with /faction_emblem_clear).");
		tradeDiscount = new SimpleConfigInt(config, "trade_agreement_shop_discount_percent", 10,
				"Percent off buying from, and extra when selling to, a shop whose faction has a trade agreement with yours. 0 turns it off.");
		maxSoftBattles = new SimpleConfigInt(config, "max_soft_loaded_battles", 2,
				"How many battle sectors with no player nearby the server loads at once so unloaded fleets can fight. 0 turns unloaded fleet combat off; more queue.");
		softAiTickDivisor = new SimpleConfigInt(config, "soft_loaded_ai_tick_divisor", 2,
				"In a battle sector with no player in it, ship AI runs one tick in this many. 1 runs it every tick.");
		softEntityTickDivisor = new SimpleConfigInt(config, "soft_loaded_entity_tick_divisor", 2,
				"In a battle sector with no player in it, each ship and station is updated one tick in this many, with that tick's elapsed time multiplied to match. 1 updates them every tick. Ship AI is divided further by soft_loaded_ai_tick_divisor.");
		autoResolveSeconds = new SimpleConfigInt(config, "battle_auto_resolve_after_seconds", 300,
				"A queued unloaded battle that waits this long for a soft-load slot is settled on paper instead: strongest side by stored combat rating (with a die roll) holds, the losing fleets withdraw, nothing is destroyed. 0 never settles; queued battles then wait for a slot.");
		battleStartBelowPercent = new SimpleConfigInt(config, "battle_start_below_tick_percent", 60,
				"Queued unloaded battles only start while the rest of the server (not counting battles) uses less than this percent of its 37 ms tick budget. 0 starts them whatever the load.");
		battleHoldAbovePercent = new SimpleConfigInt(config, "battle_hold_above_tick_percent", 90,
				"Battles with no player near get only what the rest of the server leaves under this percent of the tick budget, and skip ticks to stay under it. 0 never holds them back.");
		encounterTimeoutMinutes = new SimpleConfigInt(config, "battle_encounter_timeout_minutes", 20,
				"Fleets bound elsewhere that meet and fight break off after this many minutes of combat (longer while hits are still landing, up to double) and go back to their orders; the battle is reported. Fleets on sentry, defending, or sent to attack the sector fight on. 0 never breaks off.");
		config.readWriteFields();
	}

	public static int maxSoftBattles() {
		Integer v = maxSoftBattles != null ? maxSoftBattles.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int autoResolveSeconds() {
		Integer v = autoResolveSeconds != null ? autoResolveSeconds.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int battleStartBelowPercent() {
		Integer v = battleStartBelowPercent != null ? battleStartBelowPercent.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int battleHoldAbovePercent() {
		Integer v = battleHoldAbovePercent != null ? battleHoldAbovePercent.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int encounterTimeoutMinutes() {
		Integer v = encounterTimeoutMinutes != null ? encounterTimeoutMinutes.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int softAiTickDivisor() {
		Integer v = softAiTickDivisor != null ? softAiTickDivisor.getValue() : null;
		return v == null ? 1 : Math.max(1, v);
	}

	public static int softEntityTickDivisor() {
		Integer v = softEntityTickDivisor != null ? softEntityTickDivisor.getValue() : null;
		return v == null ? 1 : Math.max(1, v);
	}

	public static boolean customEmblemsEnabled() {
		return customEmblems != null && customEmblems.getValue() != null && customEmblems.getValue();
	}

	public static int tradeDiscountPercent() {
		Integer v = tradeDiscount != null ? tradeDiscount.getValue() : null;
		return v == null ? 0 : Math.max(0, Math.min(100, v));
	}
}
