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
	private static SimpleConfigInt tributePercent;
	private static SimpleConfigInt maxSoftBattles;
	private static SimpleConfigInt softAiTickDivisor;
	private static SimpleConfigInt softEntityTickDivisor;
	private static SimpleConfigInt autoResolveSeconds;
	private static SimpleConfigInt battleStartBelowPercent;
	private static SimpleConfigInt battleHoldAbovePercent;
	private static SimpleConfigInt encounterTimeoutMinutes;
	private static SimpleConfigInt civicSwapCost;
	private static SimpleConfigInt civicSwapCooldownDays;
	private static SimpleConfigInt civicRushCost;
	private static SimpleConfigInt proposeTreatyCost;
	private static SimpleConfigInt breakTreatyCost;
	private static SimpleConfigInt declareWarCost;
	private static SimpleConfigInt unjustWarCost;
	private static SimpleConfigInt giftCost;
	private static SimpleConfigInt giftOpinion;
	private static SimpleConfigInt fabricateClaimCost;
	private static SimpleConfigInt fabricateClaimHours;
	private static SimpleConfigInt claimLastsHours;
	private static SimpleConfigInt reputationHalfLifeDays;
	private static SimpleConfigInt spiceDealerPercent;
	private static SimpleConfigInt wreckLifetimeHours;
	private static SimpleConfigInt fleetUpkeepMassPerPoint;
	private static SimpleConfigInt institutionUpkeep;
	private static SimpleConfigInt senateVoteHours;
	private static SimpleConfigInt senateMaxVoteHours;

	private BFConfig() {
	}

	static void initialize(BetterFactions mod) {
		SimpleConfigContainer config = new SimpleConfigContainer(mod, "config", false);
		customEmblems = new SimpleConfigBool(config, "custom_emblems_enabled", false,
				"If true, factions may upload their own emblem image. Off by default: turn it on only if you are willing to moderate what players upload (admins can remove one with /faction_emblem_clear).");
		tradeDiscount = new SimpleConfigInt(config, "trade_agreement_shop_discount_percent", 10,
				"Percent off buying from, and extra when selling to, a shop whose faction has a trade agreement with yours. 0 turns it off.");
		tributePercent = new SimpleConfigInt(config, "tributary_income_percent", 25,
				"Percent of a tributary's faction point income paid to its overlord each income period. 0 turns tribute off.");
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
		civicSwapCost = new SimpleConfigInt(config, "civic_swap_cost", 1000,
				"Faction points it costs to replace a civic a faction holds. Filling an empty slot is free. 0 makes swaps free.");
		civicSwapCooldownDays = new SimpleConfigInt(config, "civic_swap_cooldown_days", 7,
				"Days after swapping a civic before the faction may swap one again. 0 means no wait.");
		civicRushCost = new SimpleConfigInt(config, "civic_rush_cost", 3000,
				"Extra faction points, on top of civic_swap_cost, to swap a civic before the cooldown is over. 0 means the cooldown cannot be skipped.");
		proposeTreatyCost = new SimpleConfigInt(config, "propose_treaty_cost", 100,
				"Faction points a player faction pays to propose a treaty, whether or not it is accepted. Peace offers are always free. 0 makes proposals free.");
		breakTreatyCost = new SimpleConfigInt(config, "break_treaty_cost", 1000,
				"Faction points a player faction pays to break a treaty in force on its own. Withdrawing a pending offer is free. 0 makes breaking free.");
		declareWarCost = new SimpleConfigInt(config, "declare_war_cost", 2000,
				"Faction points a player faction pays to declare war. Answering a call to arms is free. 0 makes declaring free.");
		unjustWarCost = new SimpleConfigInt(config, "unjust_war_cost", 3000,
				"Extra faction points, on top of declare_war_cost, to declare war with goals no casus belli justifies. 0 makes unjust wars cost only their aggression.");
		giftCost = new SimpleConfigInt(config, "gift_cost", 500,
				"Faction points a gift to another faction costs. The points are spent, not handed over. 0 turns gifts off.");
		giftOpinion = new SimpleConfigInt(config, "gift_opinion", 200,
				"Opinion a gift earns with the faction it is sent to. Each further gift sent within an hour of the last is worth less: half, then a third, and so on.");
		fabricateClaimCost = new SimpleConfigInt(config, "fabricate_claim_cost", 1500,
				"Faction points it costs to fabricate a claim on another faction: a casus belli for any war goal against it. 0 turns claims off.");
		fabricateClaimHours = new SimpleConfigInt(config, "fabricate_claim_hours", 12,
				"Hours of server uptime a fabricated claim takes before it can be used.");
		claimLastsHours = new SimpleConfigInt(config, "claim_lasts_hours", 72,
				"Hours of server uptime a fabricated claim can be used for once it is ready.");
		reputationHalfLifeDays = new SimpleConfigInt(config, "reputation_half_life_days", 14,
				"Days for half of a faction's galaxy-wide reputation for broken treaties and started fights to fade. 0 never fades.");
		spiceDealerPercent = new SimpleConfigInt(config, "spice_dealer_upkeep_percent", 25,
				"Percent more faction point upkeep a system costs for each Neighbourhood Spice Dealer faction owning a system that shares a face with it. 0 turns the effect off.");
		wreckLifetimeHours = new SimpleConfigInt(config, "wreck_lifetime_hours", 48,
				"A ship whose overheat timer runs out stays as an ownerless wreck for this many hours (counted while its sector is loaded) instead of vanishing, docked turrets and all; anyone may board and reboot it. 0 turns wrecks off: ships vanish as in vanilla.");
		fleetUpkeepMassPerPoint = new SimpleConfigInt(config, "fleet_upkeep_mass_per_point", 10000,
				"Player factions pay one faction point of upkeep each income period for every this much mass in their members' fleets (docked ships count with what they are docked to). With the vanilla config a 100k mass fleet costs about what one claimed system does. 0 turns fleet upkeep off.");
		institutionUpkeep = new SimpleConfigInt(config, "institution_upkeep_points", 10,
				"Faction points of upkeep each income period for a faction's first institution; the second adds twice this, the third three times, and so on. 0 makes institutions free.");
		senateVoteHours = new SimpleConfigInt(config, "senate_vote_hours", 24,
				"Hours a Senate vote stays open. It carries early once more than half the faction's active members are for it; at the close, more for than against carries.");
		senateMaxVoteHours = new SimpleConfigInt(config, "senate_vote_max_hours", 72,
				"Longest a faction may set its own Senate votes to stay open, in hours. senate_vote_hours is used until a faction chooses.");
		config.readWriteFields();
	}

	public static int senateMaxVoteHours() {
		Integer v = senateMaxVoteHours != null ? senateMaxVoteHours.getValue() : null;
		return v == null ? 72 : Math.max(1, v);
	}

	public static int reputationHalfLifeDays() {
		Integer v = reputationHalfLifeDays != null ? reputationHalfLifeDays.getValue() : null;
		return v == null ? 14 : Math.max(0, v);
	}

	public static int institutionUpkeep() {
		Integer v = institutionUpkeep != null ? institutionUpkeep.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int senateVoteHours() {
		Integer v = senateVoteHours != null ? senateVoteHours.getValue() : null;
		return v == null ? 24 : Math.max(1, v);
	}

	public static int fleetUpkeepMassPerPoint() {
		Integer v = fleetUpkeepMassPerPoint != null ? fleetUpkeepMassPerPoint.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int wreckLifetimeHours() {
		Integer v = wreckLifetimeHours != null ? wreckLifetimeHours.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int spiceDealerPercent() {
		Integer v = spiceDealerPercent != null ? spiceDealerPercent.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
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

	public static int civicSwapCost() {
		Integer v = civicSwapCost != null ? civicSwapCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int civicSwapCooldownDays() {
		Integer v = civicSwapCooldownDays != null ? civicSwapCooldownDays.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int civicRushCost() {
		Integer v = civicRushCost != null ? civicRushCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int proposeTreatyCost() {
		Integer v = proposeTreatyCost != null ? proposeTreatyCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int breakTreatyCost() {
		Integer v = breakTreatyCost != null ? breakTreatyCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int declareWarCost() {
		Integer v = declareWarCost != null ? declareWarCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int unjustWarCost() {
		Integer v = unjustWarCost != null ? unjustWarCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int giftCost() {
		Integer v = giftCost != null ? giftCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int giftOpinion() {
		Integer v = giftOpinion != null ? giftOpinion.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int fabricateClaimCost() {
		Integer v = fabricateClaimCost != null ? fabricateClaimCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static long fabricateClaimMs() {
		Integer v = fabricateClaimHours != null ? fabricateClaimHours.getValue() : null;
		return (v == null ? 12 : Math.max(0, v)) * 60L * 60L * 1000L;
	}

	public static long claimLastsMs() {
		Integer v = claimLastsHours != null ? claimLastsHours.getValue() : null;
		return (v == null ? 72 : Math.max(1, v)) * 60L * 60L * 1000L;
	}

	public static boolean customEmblemsEnabled() {
		return customEmblems != null && customEmblems.getValue() != null && customEmblems.getValue();
	}

	public static int tradeDiscountPercent() {
		Integer v = tradeDiscount != null ? tradeDiscount.getValue() : null;
		return v == null ? 0 : Math.max(0, Math.min(100, v));
	}

	public static int tributePercent() {
		Integer v = tributePercent != null ? tributePercent.getValue() : null;
		return v == null ? 0 : Math.max(0, Math.min(100, v));
	}
}
