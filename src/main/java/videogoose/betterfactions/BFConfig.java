package videogoose.betterfactions;

import api.config.simpleconfig.SimpleConfigBool;
import api.config.simpleconfig.SimpleConfigContainer;
import api.config.simpleconfig.SimpleConfigDouble;
import api.config.simpleconfig.SimpleConfigInt;
import api.config.simpleconfig.SimpleConfigString;

/**
 * Server settings, synced to clients so the GUI can hide what the server would refuse anyway.
 */
public final class BFConfig {

	private static SimpleConfigBool customEmblems;
	private static SimpleConfigInt tradeDiscount;
	private static SimpleConfigInt tariff;
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
	private static SimpleConfigString civicSlotMilestones;
	private static SimpleConfigString civicsDisabled;
	private static SimpleConfigInt proposeTreatyCost;
	private static SimpleConfigInt breakTreatyCost;
	private static SimpleConfigInt declareWarCost;
	private static SimpleConfigInt unjustWarCost;
	private static SimpleConfigInt giftCost;
	private static SimpleConfigInt giftOpinion;
	private static SimpleConfigInt fabricateClaimCost;
	private static SimpleConfigInt fabricateClaimHours;
	private static SimpleConfigInt claimLastsHours;
	private static SimpleConfigInt claimSystemCost;
	private static SimpleConfigInt miningBonusRampHours;
	private static SimpleConfigInt reputationHalfLifeDays;
	private static SimpleConfigInt spiceDealerPercent;
	private static SimpleConfigInt wreckLifetimeHours;
	private static SimpleConfigInt fleetUpkeepMassPerPoint;
	private static SimpleConfigDouble fleetSpeedReferenceTmr;
	private static SimpleConfigInt institutionUpkeep;
	private static SimpleConfigInt stationIncome;
	private static SimpleConfigInt stationIncomeMinMass;
	private static SimpleConfigInt senateVoteHours;
	private static SimpleConfigInt senateMaxVoteHours;
	private static SimpleConfigInt corporationCutPercent;
	private static SimpleConfigInt corporationCreditsPerPoint;
	private static SimpleConfigInt corporationDividendCap;
	private static SimpleConfigInt franchiseBaseFee;
	private static SimpleConfigInt contractFeePercent;
	private static SimpleConfigInt ambushChancePercent;
	private static SimpleConfigInt npcBountyKills;
	private static SimpleConfigInt npcBountyReward;
	private static SimpleConfigInt franchiseCutPercent;
	private static SimpleConfigInt franchiseRoyaltyPercent;
	private static SimpleConfigInt franchiseTrafficUnit;

	private BFConfig() {
	}

	static void initialize(BetterFactions mod) {
		SimpleConfigContainer config = new SimpleConfigContainer(mod, "config", false);
		customEmblems = new SimpleConfigBool(config, "custom_emblems_enabled", false,
				"If true, factions may upload their own emblem image. Off by default: turn it on only if you are willing to moderate what players upload (admins can remove one with /faction_emblem_clear).");
		tradeDiscount = new SimpleConfigInt(config, "trade_agreement_shop_discount_percent", 10,
				"Percent off buying from, and extra when selling to, a shop whose faction has a trade agreement with yours. 0 turns it off.");
		tariff = new SimpleConfigInt(config, "tariff_percent", 20,
				"Percent extra when buying from, and less when selling to, a shop whose faction has put tariffs on yours. 0 turns tariffs' price effect off.");
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
		civicSlotMilestones = new SimpleConfigString(config, "civic_slot_milestones", "500,1200,2500",
				"Faction strengths at which a faction earns one more civic slot, comma separated. Every faction starts with 3. Earned slots are kept if strength falls. Empty means 3 slots for everyone.");
		civicsDisabled = new SimpleConfigString(config, "civics_disabled", "",
				"Civic ids no faction may pick, comma separated, e.g. betterfactions:raiders. A faction already holding one keeps it.");
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
		claimSystemCost = new SimpleConfigInt(config, "claim_system_cost", 500,
				"Faction points a player faction pays, once, to claim a system. A faction holding no systems claims its first for free. 0 makes claims free.");
		miningBonusRampHours = new SimpleConfigInt(config, "mining_bonus_ramp_hours", 48,
				"Hours after a system is claimed before its owner gets the full owner mining bonus; until then it climbs evenly from the unowned bonus. 0 gives it at once.");
		reputationHalfLifeDays = new SimpleConfigInt(config, "reputation_half_life_days", 14,
				"Days for half of a faction's galaxy-wide reputation for broken treaties and started fights to fade. 0 never fades.");
		spiceDealerPercent = new SimpleConfigInt(config, "spice_dealer_upkeep_percent", 25,
				"Percent more faction point upkeep a system costs for each Neighbourhood Spice Dealer faction owning a system that shares a face with it. 0 turns the effect off.");
		wreckLifetimeHours = new SimpleConfigInt(config, "wreck_lifetime_hours", 48,
				"A ship whose overheat timer runs out stays as an ownerless wreck for this many hours (counted while its sector is loaded) instead of vanishing, docked turrets and all; anyone may board and reboot it. 0 turns wrecks off: ships vanish as in vanilla.");
		fleetSpeedReferenceTmr = new SimpleConfigDouble(config, "fleet_speed_reference_thrust_mass_ratio", 1.0,
				"Unloaded fleets cross sectors at the server's FLEET_OUT_OF_SECTOR_MOVEMENT time divided by (slowest member's thrust to mass ratio / this), clamped to 0.25x to 4x speed. Members never seen loaded are left out. 0 turns it off (vanilla speed).");
		fleetUpkeepMassPerPoint = new SimpleConfigInt(config, "fleet_upkeep_mass_per_point", 10000,
				"Player factions pay one faction point of upkeep each income period for every this much mass in their members' fleets (docked ships count with what they are docked to). With the vanilla config a 100k mass fleet costs about what one claimed system does. 0 turns fleet upkeep off.");
		institutionUpkeep = new SimpleConfigInt(config, "institution_upkeep_points", 10,
				"Faction points of upkeep each income period for a faction's first institution; the second adds twice this, the third three times, and so on. 0 makes institutions free.");
		stationIncome = new SimpleConfigInt(config, "station_income_points", 2,
				"Faction points each station a player faction owns earns each income period, at station_income_min_mass. Each tenfold mass past that adds this much again (10x the mass earns twice, 100x three times). 0 turns station income off.");
		stationIncomeMinMass = new SimpleConfigInt(config, "station_income_min_mass", 10000,
				"Mass a station needs to earn station income at all. A station never seen loaded since this arrived earns nothing until it is.");
		senateVoteHours = new SimpleConfigInt(config, "senate_vote_hours", 24,
				"Hours a Senate vote stays open. It carries early once more than half the faction's active members are for it; at the close, more for than against carries.");
		senateMaxVoteHours = new SimpleConfigInt(config, "senate_vote_max_hours", 72,
				"Longest a faction may set its own Senate votes to stay open, in hours. senate_vote_hours is used until a faction chooses.");
		corporationCutPercent = new SimpleConfigInt(config, "corporation_shop_cut_percent", 20,
				"Percent of every sale at a station's shops that the corporation running the station takes from the shop into its treasury.");
		corporationCreditsPerPoint = new SimpleConfigInt(config, "corporation_credits_per_fp", 10000,
				"Credits of a corporation's dividend that make one faction point.");
		corporationDividendCap = new SimpleConfigInt(config, "corporation_dividend_cap_points", 100,
				"Most faction points one corporation pays its faction an income period. -1 for no cap.");
		franchiseBaseFee = new SimpleConfigInt(config, "franchise_base_fee", 100000000,
				"Credits a franchise on a quiet Trading Guild outpost costs. A Trade Station costs twice this, and a busy shop up to four times more.");
		franchiseCutPercent = new SimpleConfigInt(config, "franchise_cut_percent", 8,
				"Percent of every sale at a franchised Trading Guild shop that goes to the franchisee, before the Guild's royalty.");
		franchiseRoyaltyPercent = new SimpleConfigInt(config, "franchise_royalty_percent", 20,
				"Percent of a franchise's takings the Trading Guild keeps.");
		franchiseTrafficUnit = new SimpleConfigInt(config, "franchise_traffic_unit", 50000000,
				"Credits of recent sales at a Guild shop that add the base fee again to its franchise's price. Sales fade by a tenth a day.");
		contractFeePercent = new SimpleConfigInt(config, "contract_posting_fee_percent", 5,
				"Percent of a contract's reward its poster loses when it lapses unfinished. Withdrawing one nobody has taken costs nothing.");
		npcBountyKills = new SimpleConfigInt(config, "npc_bounty_kills", 3,
				"Ships of one NPC faction a player destroys within six hours before that faction puts a bounty on them.");
		npcBountyReward = new SimpleConfigInt(config, "npc_bounty_reward", 5000000,
				"Credits an NPC faction's first bounty on a player pays. Each further bounty it has standing on them adds this again.");
		ambushChancePercent = new SimpleConfigInt(config, "ambush_chance_percent", 15,
				"Percent chance pirates ambush a trade delivery as it enters a system that is not safe at all; safer systems less, down to none. 0 turns ambushes off.");
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

	public static int stationIncome() {
		Integer v = stationIncome != null ? stationIncome.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int stationIncomeMinMass() {
		Integer v = stationIncomeMinMass != null ? stationIncomeMinMass.getValue() : null;
		return v == null ? 10000 : Math.max(0, v);
	}

	public static int corporationCutPercent() {
		Integer v = corporationCutPercent != null ? corporationCutPercent.getValue() : null;
		return v == null ? 20 : Math.max(0, Math.min(100, v));
	}

	public static int corporationCreditsPerPoint() {
		Integer v = corporationCreditsPerPoint != null ? corporationCreditsPerPoint.getValue() : null;
		return v == null ? 10000 : Math.max(1, v);
	}

	public static int corporationDividendCap() {
		Integer v = corporationDividendCap != null ? corporationDividendCap.getValue() : null;
		return v == null ? 100 : Math.max(-1, v);
	}

	public static int franchiseBaseFee() {
		Integer v = franchiseBaseFee != null ? franchiseBaseFee.getValue() : null;
		return v == null ? 100000000 : Math.max(0, v);
	}

	public static int franchiseCutPercent() {
		Integer v = franchiseCutPercent != null ? franchiseCutPercent.getValue() : null;
		return v == null ? 8 : Math.max(0, Math.min(100, v));
	}

	public static int franchiseRoyaltyPercent() {
		Integer v = franchiseRoyaltyPercent != null ? franchiseRoyaltyPercent.getValue() : null;
		return v == null ? 20 : Math.max(0, Math.min(100, v));
	}

	public static int franchiseTrafficUnit() {
		Integer v = franchiseTrafficUnit != null ? franchiseTrafficUnit.getValue() : null;
		return v == null ? 50000000 : Math.max(1, v);
	}

	public static int contractFeePercent() {
		Integer v = contractFeePercent != null ? contractFeePercent.getValue() : null;
		return v == null ? 5 : Math.max(0, Math.min(100, v));
	}

	public static int npcBountyKills() {
		Integer v = npcBountyKills != null ? npcBountyKills.getValue() : null;
		return v == null ? 3 : Math.max(1, v);
	}

	public static long npcBountyReward() {
		Integer v = npcBountyReward != null ? npcBountyReward.getValue() : null;
		return v == null ? 5000000L : Math.max(0, v);
	}

	public static int ambushChancePercent() {
		Integer v = ambushChancePercent != null ? ambushChancePercent.getValue() : null;
		return v == null ? 15 : Math.max(0, Math.min(100, v));
	}

	public static int institutionUpkeep() {
		Integer v = institutionUpkeep != null ? institutionUpkeep.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static int senateVoteHours() {
		Integer v = senateVoteHours != null ? senateVoteHours.getValue() : null;
		return v == null ? 24 : Math.max(1, v);
	}

	public static float fleetSpeedReferenceTmr() {
		Double v = fleetSpeedReferenceTmr != null ? fleetSpeedReferenceTmr.getValue() : null;
		return v == null ? 1f : v.floatValue();
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

	/** The strengths that each earn a civic slot, lowest first; unparsable entries are skipped. */
	public static int[] civicSlotMilestones() {
		String v = civicSlotMilestones != null ? civicSlotMilestones.getValue() : null;
		return parseMilestones(v == null ? "500,1200,2500" : v);
	}

	static int[] parseMilestones(String v) {
		return java.util.Arrays.stream(v.split(",")).map(String::trim).filter(x -> x.matches("\\d+"))
				.mapToInt(Integer::parseInt).sorted().toArray();
	}

	public static boolean civicDisabled(String id) {
		String v = civicsDisabled != null ? civicsDisabled.getValue() : null;
		if (v == null || v.isBlank()) {
			return false;
		}
		for (String d : v.split(",")) {
			if (d.trim().equals(id)) {
				return true;
			}
		}
		return false;
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

	public static int claimSystemCost() {
		Integer v = claimSystemCost != null ? claimSystemCost.getValue() : null;
		return v == null ? 0 : Math.max(0, v);
	}

	public static long miningBonusRampMs() {
		Integer v = miningBonusRampHours != null ? miningBonusRampHours.getValue() : null;
		return (v == null ? 48 : Math.max(0, v)) * 60L * 60L * 1000L;
	}

	public static boolean customEmblemsEnabled() {
		return customEmblems != null && customEmblems.getValue() != null && customEmblems.getValue();
	}

	public static int tradeDiscountPercent() {
		Integer v = tradeDiscount != null ? tradeDiscount.getValue() : null;
		return v == null ? 0 : Math.max(0, Math.min(100, v));
	}

	public static int tariffPercent() {
		Integer v = tariff != null ? tariff.getValue() : null;
		return v == null ? 0 : Math.max(0, Math.min(100, v));
	}

	public static int tributePercent() {
		Integer v = tributePercent != null ? tributePercent.getValue() : null;
		return v == null ? 0 : Math.max(0, Math.min(100, v));
	}
}
