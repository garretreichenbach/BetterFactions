# BetterFactions — Developer Guide

How the mod works today. Start here; each system has its own page:

| Page | Covers |
|------|--------|
| [DIPLOMACY.md](DIPLOMACY.md) | Treaties, proposals, opinion, NPC presets, costs and claims, skirmishes, reputation, federations, emblems |
| [BATTLES.md](BATTLES.md) | Battle tracking, soft-loading unwatched fights, server load, auto-resolve, reports, admin tools, world tests |
| [CIVICS.md](CIVICS.md) | Civics, doctrines, payouts, Senate, Council, Corporations, contracts |
| [FLEETS.md](FLEETS.md) | Intercept, escort, raid and repair orders, route safety, upkeep and income, claims, wrecks |

For why things work the way they do, see [design/](design): the original audit and phase plans, written against
the engine branch this started on, so their file and line references are to that branch.

Package root: `src/main/java/videogoose/betterfactions/`.

---

## Layout

| Path | What |
|------|------|
| `BetterFactions.java` | Mod entry. Registers the packet, engine listeners, commands, guides and world tests |
| `DiplomacyManager.java` | Diplomacy state and server logic, packet dispatch, and the update that drives the civics classes. One instance per side: `server()` / `client()` |
| `BFStore.java` | Save/load of every store |
| `BFConfig.java` | Mod config (below) |
| `Perms.java` | The mod's rank rights, mapped onto spare engine bits |
| `FleetUpkeep`, `FactionPointMods`, `ClaimAges`, `Wrecks` | Upkeep and income, point lines, mining ramp, wrecks ([FLEETS.md](FLEETS.md)) |
| `InGameChecks.java` | Runs the `*_test` self-checks in game |
| `diplomacy/` | Treaties, proposals, opinion ledgers, NPC config, federations, strength, ratings, skirmishes, reputation |
| `battle/` | `BattleTracker`, reports, names, world tests |
| `civics/` | Civics, Senate, Council, Corporations, contracts, Spice Dealer |
| `fleet/` | Intercepts, trade escort and raid orders, mission labels, NPC fleet freeze |
| `trade/` | Route safety and its map overlay |
| `repair/` | Shipyard repairs, unloaded repairs, components, faction designs |
| `assets/` | Faction holdings query, Resources Reorganized route hook |
| `emblem/` | Emblem validation, client cache, GUI element |
| `template/` | Loads data files from moddata, falling back to the jar |
| `gui/` | Faction panel tabs and dialogs |
| `mixin/` | Engine patches (below) |
| `net/BFPacket.java` | The one packet: a kind byte plus a tag |
| `commands/` | Admin chat commands |
| `api/` | Events other mods can listen to |
| `src/main/resources/data/` | NPC presets (`npc/`), Council directives (`directives.xml`) |
| `src/main/resources/guides/` | In-game guide pages |
| `src/main/resources/tests/` | World tests |

## Faction panel

`FactionPanelTabs` builds the tabs through `FactionPanelCreateListener`:

| Tab | Contents |
|-----|----------|
| Overview | Vitals, power bar, FP detail, description and emblem, modifiers. Sub-tabs News (our log or the galaxy) and Battle Reports |
| Assets | Holdings, and Designs (the blueprint each ship repairs to). Own faction only |
| Governance | Institutions, then a sub-tab per institution held: Senate, Council, Corporations. Own faction only |
| Diplomacy | Faction list, treaties, opinion (with gift and fabricate claim) |
| Federation | Members, governance and votes; or, outside one, the list of blocs |
| Settings | Members (invite, ranks), and Faction: identity, ranks, civics, doctrine, standing orders, reset/leave |

The first time a session opens the panel, an editor whose faction has no civics gets the civic chooser. Ranks are
edited in `FactionRankDialog` (the `FactionRankMatrix` grid), not the engine's dialog.

## Engine hooks

Listeners registered in `BetterFactions.registerEvents`:

| Listener | Use |
|----------|-----|
| `FactionRelationQueryListener` | Returns the treaty- and skirmish-derived relation. This is what makes a treaty change combat |
| `FactionDeleteListener` | Drops the faction's treaties, federation seat, emblem, ledgers and offers |
| `FactionDiplomacyActionListener` | Feeds engine opinion events into the ledgers; an attack opens a skirmish |
| `NPCExecuteDiplomacyReactionListener` | Always cancels, so only the mod's ledger acts for NPCs |
| `ServerInitializeListener` | Reset and load every store, clear preset cache, build asset index |
| `WorldSaveListener` | Save every store |
| `ServerUpdateListener` | Server tick: diplomacy (and civics), battles, world tests, repairs, intercepts, trade fleets, route safety |
| `PlayerJoinWorldListener` | Queue the join snapshot |
| `PlayerJoinFactionListener` | Join times for the FAIR_KILL payout filter |
| `PlayerDeathListener` | Bounties, Council progress, PLAYER_KILLED / PLAYER_LOST payouts |
| `SegmentControllerOverheatListener` | Battle losses, raid seizures, SHIP_DESTROYED payouts, NPC bounty tracking |
| `SegmentControllerChangeFactionListener` | Invalidate the asset cache |
| `SegmentControllerUnloadListener` | Remember blocks for unloaded repairs |
| `ServerSendableRemoveListener` | On permanent delete, drop combat rating and repair entries |
| `SystemClaimListener` | Claim cost and rules, claim ages, Council progress, claim payouts |
| `ExtractorMiningBonusCalculateListener` | Mining bonus ramp for extractors |
| `ShopPriceListener` | Trade agreement discount, tariffs, embargo opinion pressure |
| `BuyTradeListener` / `SellTradeListener` | Embargoes cancel trade orders; corporation cut |
| `GameMapDrawListener` | Route safety map overlay |
| `FactionPanelCreateListener` | Build the faction panel tabs |
| `ClientDisconnectListener` | Reset the client mirrors |

Plus `StarMod.onRegisterGuides` (see [Guides](#guides)), `onRegisterTests`, and the `UNDEF5-7` permission bits.

### Mixins

Listed in `betterfactions.mixins.json`. Each is described on its system's page.

| Area | Mixins |
|------|--------|
| Battles ([BATTLES.md](BATTLES.md)) | `CheckAttackMixin`, `SoftRailCollisionMixin`, `SoftShipCollisionMixin`, `WatchCcdMixin`, `TimerAccessor`, `SoftAiTickMixin`, `BattleLoadMixin`, `BattleSectorLoadMixin`, `ProfilerBattlesTabMixin`, `SoftLootMixin`, `NPCEntityAccessor`, `SpectatorPingMixin`, `SpectatorCameraMixin`, `ServerTickMixin`, `TableAccessor`, `BlueprintRatingMixin`, `ReactorLostOverheatMixin` |
| Fleets ([FLEETS.md](FLEETS.md)) | `FleetRepairOrderMixin`, `FleetCommandReceiveMixin`, `FleetResendMixin`, `FleetJoinResendMixin`, `FleetMissionLabelMixin`, `FleetMassColumnMixin`, `FleetMemberMassColumnMixin`, `FleetMemberMassMixin`, `FleetUnloadedMoveMixin`, `FleetLoadedFreezeMixin`, `FleetMoveFreezeMixin`, `MapInterceptMixin`, `MapJammedFleetMixin`, `RaidHostilityMixin` |
| Repairs ([FLEETS.md](FLEETS.md#shipyard-repair)) | `ShipyardDockingMixin`, `ShipyardQueueButtonMixin`, `ShipyardInputsButtonMixin`, `ShipyardRepairComponentsMixin`, `ShipyardRepairRefundMixin`, `ShipyardRepairGoalMixin` |
| Trade | `ShopEmbargoMixin` ([DIPLOMACY.md](DIPLOMACY.md#treaties)), `ShopSaleMixin` ([CIVICS.md](CIVICS.md#corporations)), `TradeNodeSafetyMixin`, `MapSafetyToggleMixin` ([FLEETS.md](FLEETS.md#trade-route-safety)) |
| Economy | `FleetUpkeepMixin`, `TributeTurnMixin`, `FactionIncomeMixin`, `FactionPointStatsMixin`, `SpiceDealerUpkeepMixin`, `FactionAccessor`, `MiningBonusRampMixin` |
| Wrecks | `WreckOverheatMixin`, `TurretKnockedOffMixin` |
| GUI | `MemberRowButtonsMixin`, `NewsRowButtonsMixin`: expanded member and news rows get full-width button tables (`ExpandedRowButtons`) |

## Ranks and rights

The engine's founder rank already holds the `UNDEF5-7` bits; `Perms.define()` names them with `PermType.define`
on every enable, which is what makes them show up in the rank editor. The engine hides them again on mod reset.

| Right | Bit | Rank editor label | Gates |
|-------|-----|-------------------|-------|
| `Perms.TREATIES` | `UNDEF5` | Sign treaties | Every treaty but war and peace |
| `Perms.FEDERATION_VOTE` | `UNDEF6` | Speak for us in the federation | Every federation command |
| `Perms.ASSETS_VIEW` | `UNDEF7` | View holdings | The holdings query |

War and peace need the engine's relationship right. Civics, institutions, Senate rules, Council and Corporations
use the engine's `FACTION_EDIT_PERMISSION`.

The engine branch gives each faction 2 to 10 ranks (`FactionRoles.MIN_ROLES`/`MAX_ROLES`). Rank indices are a
fixed space with the founder always at 9 (`INDEX_ADMIN_ROLE`); a faction with *n* ranks uses slots `10 - n` to 9,
and ranks are only added or removed at the bottom. That keeps structure rank requirements (`player rank >=
required`, stored on possibly unloaded entities) and fleet access (counted down from the founder) meaning the same
through any change. Old saves (founder at 4) are renumbered once on load: faction members via the legacy roles
tag, structures via entity tag version 2. Only the founder (or a server admin) may change the count; the server
moves members of a removed rank up. `FactionRankDialog` sends the count with the rest of the roles rewrite.

## Config

`BFConfig`, a StarLoader `SimpleConfigContainer` named `config`, synced to clients. Getters clamp values; several
return 0 rather than the default if read before `initialize`.

| Key | Default | Page |
|-----|---------|------|
| `custom_emblems_enabled` | false | [Diplomacy](DIPLOMACY.md#emblems) |
| `propose_treaty_cost`, `break_treaty_cost`, `declare_war_cost`, `unjust_war_cost` | 100, 1000, 2000, 3000 | [Diplomacy](DIPLOMACY.md#costs-gifts-and-claims) |
| `gift_cost`, `gift_opinion` | 500, 200 | [Diplomacy](DIPLOMACY.md#costs-gifts-and-claims) |
| `fabricate_claim_cost`, `fabricate_claim_hours`, `claim_lasts_hours` | 1500, 12, 72 | [Diplomacy](DIPLOMACY.md#costs-gifts-and-claims) |
| `trade_agreement_shop_discount_percent`, `tariff_percent`, `tributary_income_percent` | 10, 20, 25 | [Diplomacy](DIPLOMACY.md) |
| `reputation_half_life_days` | 14 | [Diplomacy](DIPLOMACY.md#reputation) |
| `max_soft_loaded_battles` | 2 | [Battles](BATTLES.md) |
| `soft_loaded_ai_tick_divisor`, `soft_loaded_entity_tick_divisor` | 2, 2 | [Battles](BATTLES.md) |
| `battle_auto_resolve_after_seconds` | 300 | [Battles](BATTLES.md#auto-resolve) |
| `battle_start_below_tick_percent`, `battle_hold_above_tick_percent` | 60, 90 | [Battles](BATTLES.md#server-load) |
| `battle_encounter_timeout_minutes` | 20 | [Battles](BATTLES.md#encounters) |
| `civic_swap_cost`, `civic_swap_cooldown_days`, `civic_rush_cost` | 1000, 7, 3000 | [Civics](CIVICS.md#slots-picks-and-swaps) |
| `civic_slot_milestones`, `civics_disabled` | "500,1200,2500", "" | [Civics](CIVICS.md#slots-picks-and-swaps) |
| `institution_upkeep_points` | 10 | [Civics](CIVICS.md#institutions) |
| `spice_dealer_upkeep_percent` | 25 | [Civics](CIVICS.md#spice-dealer) |
| `senate_vote_hours`, `senate_vote_max_hours` | 24, 72 | [Civics](CIVICS.md#senate) |
| `corporation_shop_cut_percent`, `corporation_credits_per_fp`, `corporation_dividend_cap_points` | 20, 10000, 100 | [Civics](CIVICS.md#corporations) |
| `franchise_base_fee`, `franchise_cut_percent`, `franchise_royalty_percent`, `franchise_traffic_unit` | 100000000, 8, 20, 50000000 | [Civics](CIVICS.md#corporations) |
| `contract_posting_fee_percent`, `npc_bounty_kills`, `npc_bounty_reward` | 5, 3, 5000000 | [Civics](CIVICS.md#contracts) |
| `ambush_chance_percent` | 15 | [Fleets](FLEETS.md#trade-route-safety) |
| `fleet_upkeep_mass_per_point`, `station_income_points`, `station_income_min_mass` | 10000, 2, 10000 | [Fleets](FLEETS.md#faction-points-upkeep-and-income) |
| `fleet_speed_reference_thrust_mass_ratio` | 1.0 | [Fleets](FLEETS.md#faction-points-upkeep-and-income) |
| `claim_system_cost`, `mining_bonus_ramp_hours` | 500, 48 | [Fleets](FLEETS.md#system-claims-and-the-mining-ramp) |
| `wreck_lifetime_hours` | 48 | [Fleets](FLEETS.md#wrecks) |

Data files admins can drop in `moddata/BetterFactions/`: `npc/<preset>.xml` and `directives.xml` override the jar's
copies; `civics.xml` and `casus_belli.xml` are optional and have none.

## Persistence

`BFStore` writes `moddata/BetterFactions/<name>-<world>.tag` (zipped tag, written to `.tmp` and moved) on world
save and on disable, and loads each on server init. An unreadable file is renamed `.unreadable-<ts>` and that store
starts empty.

| Name | Holds |
|------|-------|
| `diplomacy` | Treaties, offers, ledgers, federations, emblem hashes, history, reputation |
| `battles` | Active and carried battles, the queue, reports |
| `civics`, `senate`, `council`, `corporations`, `contracts` | [CIVICS.md](CIVICS.md) |
| `intercepts`, `tradefleets`, `routesafety`, `repairs`, `designs` | [FLEETS.md](FLEETS.md) |
| `fleet-mass`, `claim-ages`, `point-mods`, `wrecks` | [FLEETS.md](FLEETS.md) |

Emblem images live in `moddata/BetterFactions/emblems/`; clients cache them in `./client-faction-emblems/<host>/`.

Not saved, so a restart clears them: skirmishes, NPC bounty kill counts, FAIR_KILL join times, raid prey and
seized-goods tallies, and the debug toggles.

## Networking

Everything goes over `BFPacket` (a kind byte plus a tag), dispatched in `DiplomacyManager.receive` on each side.
The server is authoritative; clients only mirror. Opinions, Senate, Council and Corporations go only to the faction
they are about; civics, contracts, reputation, skirmishes, route safety and military totals are public. A joining
player gets a snapshot of everything they may see.

Kinds are grouped by system in `BFPacket.Kind`: diplomacy (`PROPOSAL`, `FEDERATION_COMMAND`, `SNAPSHOT`, `TREATY`,
`OFFER`, `FEDERATION`, `OPINION`, `HISTORY`, `GIFT`, `FABRICATE_CLAIM`, `REPUTATION`, `SKIRMISHES`, `MILITARY`),
emblems, assets, battles (admin list and commands, watch, reports, detail, live battles), repairs and designs,
fleet orders (`INTERCEPT_ORDER`, `TRADE_FLEET_ORDER`, `JAMMED_FLEETS`, `ROUTE_SAFETY`), and civics (`CIVICS_PICK`,
`CIVICS`, `CIVIC_TYPES`, `INSTITUTION_SET`, Senate, Council, Corporations, contracts, `POINT_MODS`). A server-to-client
kind arriving at the server is logged and dropped.

## Commands

All admin-only (StarLoader's default permission).

| Command | Does |
|---------|------|
| `/faction_treaty_sign <a> <b> <type> [termHours]` | Sign a treaty (not federation) |
| `/faction_treaty_break <a> <b> <type>` | Break a treaty, even an unbreakable one |
| `/faction_treaty_list <faction>` | Treaty types and the faction's treaties |
| `/faction_emblem_clear <faction>` | Remove an emblem |
| `/faction_civic_grant <faction> <civicId> [on\|off]` | Grant an admin-only civic |
| `/faction_federation_list`, `/faction_federation_dissolve <id>` | Federations |
| `/directives_reload` | Reread `directives.xml` |
| `/battle_blueprints [filter]`, `/battle_spawn_fleet …` | Test fleets ([BATTLES.md](BATTLES.md#test-fleets)) |
| `/raid_status` | Why each raiding fleet has the prey it has |
| `/senate_pause`, `/council_next`, `/faction_npc_yield <id>`, `/yesman`, `/shipyard_repairs_free`, `/npc_fleets_frozen` | Debug toggles, not saved |

## Mod API

Events (`api/`), registered with `X.TYPE.register(listener, mod)`:

| Event | Fired | Cancel does |
|-------|-------|-------------|
| `DiplomacyActionListener` | Before an action is counted in a ledger | Drops the action |
| `ExecuteDiplomacyReactionListener` | Before an NPC reaction runs | Blocks the reaction |

Treaty types:

```java
TreatyTypeRegistry.register("mymod", new TreatyType.Builder("research_pact")
        .displayName("Research Pact")
        .importance(15)
        .names("The {from}-{to} Accord"));
```

A registered type appears in the propose dialog, inbox and treaty list with no GUI change, and can be named in
preset `<Treaty>` / `<Accept>` elements. Without `importance`, it only names a deal that signs no ranked type.

Civics: `CivicRegistry.register(namespace, new CivicType.Builder(...))`, or data civics in `civics.xml`.

Server-side helpers: `DiplomacyManager.server().signTreatyServer`, `breakTreatyServer`, `hasTreaty`,
`getTreaties`, `getFederationOf`.

## Guides

The engine's in-game guide viewer reads built-in pages from `data/guides/<section>/<nn-title>.md`, and mods add
pages in `StarMod.onRegisterGuides` with `registrar.registerFromResource(sectionKey, sectionLabel, title, path,
mod)`.

A mod page whose section key and title match a built-in page **replaces it in place**. The key is the built-in's
folder name and the title is its file name without the number prefix, so BetterFactions replaces
`data/guides/07-multiplayer/00-factions.md` with `("07-multiplayer", "Multiplayer", "Factions", ...)`. This
override needs the `GuidesRepository` change on `nf/faction-mod-hooks`; on an engine without it the page shows up
as a second "Factions" entry instead.

The rest go under a "BetterFactions" section: Treaties, Opinion and NPC Factions, Federations, Server
Administration. Guide pages cannot link to each other and mod pages cannot show images, so keep them
self-contained text. When a feature changes, update its page in `src/main/resources/guides/`.

Not yet covered by any guide: civics and institutions from the player's side, battles and reports, fleet orders,
raids, repairs, route safety, upkeep and income, wrecks, gifts and claims.

## Tests

- `./gradlew selfChecks` runs every `*_test` class's `main` with `-ea`. They are plain mains, not JUnit, and live
  in `src/main`, so they currently ship in the jar. `./gradlew test` runs nothing.
- `DiplomacyAcceptance_test` reads `src/main/resources/data/npc/` and checks every shipped preset parses, has
  reactions and acceptance rules, gives every rule a `<Refusal>`, has no rule for a demand, and differs from the
  others.
- In game, `/run_tests @videogoose.betterfactions` runs the self-checks (`InGameChecks`, which skips three that
  touch live state) and the world tests. See [BATTLES.md](BATTLES.md#world-tests).

## Roadmap

| Phase | Work | Status |
|-------|------|--------|
| 0 | Opinion ledger on every faction | done |
| 1 | Treaty model, string-keyed tags, storage, sync, derived relation | done |
| 2 | Proposal pipeline, server-side permission gate | done |
| 3 | NPC acceptance rules per archetype, strength | done |
| 4 | Diplomacy GUI, acceptance preview, inbox | done |
| 4.5 | A: wire trust (done). B: what a faction can hand over. C: military strength term (done) | in progress |
| 5 | Federations (done), vassalage and tributary behaviour, war goals, peace terms | in progress |
| 6 | Governance: civics, Senate, Council, Corporations (done); emblems (custom upload done, no atlas) | in progress |
| 7 | Modding API: more events, preset registration for mods | started (two events, treaty and civic registration) |

Beyond the original plan and done: battles and unloaded combat, fleet orders and raids, shipyard repairs, route
safety, upkeep and income, wrecks. What the civics and corporations plans still lack is listed in
[CIVICS.md](CIVICS.md#not-built-yet).

## Known issues

- `TERRITORY` is dropped (`DiplomacyAction.fromVanilla`) because the engine fires it for mere proximity. Nothing
  feeds a real trespass signal yet: the engine's intrusion check only covers player-owned space and only posts
  news. Preset rules on `TERRITORY` never fire.
- Unloaded planets can't be checked for a shipyard, so a Shipyards corporation can't seat its HQ on one.
- `FleetUpkeep`'s mass table is never pruned.
