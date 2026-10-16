# Fleets, Trade and Economy

Mod fleet orders (intercept, escort, raid, shipyard repair), trade route safety, faction point upkeep and income,
wrecks and the mining ramp. Packages: `fleet/`, `trade/`, `repair/`, `assets/`, plus top-level `FleetUpkeep`,
`FactionPointMods`, `ClaimAges` and `Wrecks`. Back to [DEVELOPER.md](DEVELOPER.md).

## Fleet orders

`FleetRepairOrderMixin` adds four tiles to the fleet Order dialog: Shipyard Repair, Intercept, Escort Trade and
Raid Trade Routes. `MapInterceptMixin` adds Intercept and Raid Trade to the map's radial menu for selected fleets.

Common rules for the mod's orders (`FleetIntercepts`, `TradeFleetOrders`):
- Commands go out through `FleetManager.executeCommand`, re-sent only when they change.
- Any order a player sends ends the post (`FleetCommandReceiveMixin`). So does the fleet's command changing under
  it after a 5 s grace.
- Checked every 2 s on the server tick.
- `FleetLabels` puts the mod's state in the fleet's mission text ("INTERCEPTING", "RAIDING CONVOY", "SHIPYARD
  REPAIR: DOCKING"…), through `FleetMissionLabelMixin`.

### Intercept

`FleetIntercepts`, packet `INTERCEPT_ORDER`, saved in `intercepts`. The fleet defends a sector. When an enemy
fleet's flagship is anywhere in that system, it attacks the nearest one's sector, follows it while it stays in the
system, then returns to its post.

**Jamming.** A fleet whose loaded flagship is jamming is hidden on the map from any faction whose recon in that
system (its own, or a friend's shared scanner) doesn't beat the jammer's stealth (`JAMMED_FLEETS`,
`MapJammedFleetMixin`). Owners always see their own. Unloaded flagships never jam, and recon covers the whole
system with no falloff.

### Escort Trade

`TradeFleetOrders`, kind ESCORT. The fleet waits ("AWAITING CONVOY"), then defends the nearest active delivery
bought or sold by its own faction that no other escort has taken ("ESCORTING CONVOY"). Faction fleets only.

### Raid Trade Routes

`TradeFleetOrders`, kind RAID, ordered through `RaidOrderDialog` (route sectors, target factions, "Mining fleets
too") or the map radial (adds the clicked system to the route). Packet `TRADE_FLEET_ORDER`, saved in
`tradefleets`.

- **Patrol.** Up to 16 route systems, defended in turn ("RAIDING TRADE ROUTES"). The fleet attacks the nearest
  raidable delivery in any route system ("RAIDING CONVOY") and lets it go once it leaves the route or stops
  trading. With miners on, it also hunts fleets on MINE_IN_SECTOR and NPC mining fleets.
- **Who is raidable** (`raidable`). With no targets named: anyone but the Trading Guild, the raider's own faction
  and its friends. With targets: deliveries where a named faction is a party, never the raider's own. No war is
  needed.
- **Hostility.** While any raid runs, `RaidHostilityMixin` makes `FactionManager.isEnemy` true between a raider and
  its prey, and between a raider and any ship of a faction it is raiding. Turrets follow their root; stations are
  left out. `BattleTracker` treats a raider sharing a sector with its prey as a battle, and auto-resolve counts
  them as enemies.
- **Seizure** (`onOverheat`). When a convoy ship overheats, the raider's loaded ships in the sector take whatever
  fits from its inventories, plus a share of an NPC convoy's on-paper Guild goods. The sector is a wreck site for
  3 min, and floating stacks there are swept into raider holds. Drops in a soft-loaded battle sector go to the
  raiders there too (`SoftLootMixin` → `catchDrop`).
- **Consequences** (`raided`), for each victim that isn't the Guild, pirates or already at war with the raider:
  a reputation attack, the `RAIDED` opinion action (−150), and a ready casus belli against the raider (with mail if
  they didn't hold one). The route's safety drops.
- **Reports.** The fleet owner gets chat ("seized N blocks … worth about X credits"). After 60 s without a hit, or
  when the raid ends, victims get a "Trade Raided" news post and the raider's faction a "Raid Report" listing the
  top 15 block types taken.
- `/raid_status` explains each raiding fleet's prey and why each candidate is or isn't taken.

### Shipyard Repair

`ShipyardRepairs`, ordered from the Order tile, an Assets fleet row, or REPAIR SHIP in the Designs list. Packets
`REPAIR_ORDER`, `REPAIR_CANCEL`, `REPAIR_QUEUE_REQUEST` / `REPAIR_QUEUE`; saved in `repairs`.

- The player picks one of the faction's stations. The fleet moves there and each undocked member is queued with
  its design (below); a member without one is skipped.
- At the yard ships go one at a time: a "Repair: <blueprint>" design is made if missing, the ship is flown in
  (`ShipyardDockingMixin`, then `Approach.lead`), rebuilt with REPAIR_FROM_DESIGN, put back in its fleet slot and
  led out the far end. Other fleets park beside the yard. The fleet's previous order comes back when it's done.
- Steps: EN_ROUTE, HOLDING, DESIGN, DOCKING, REPAIRING, LEAVING, OFFLINE. A ship longer than the yard is skipped. A
  ship stuck leaving for 45 s is warped out.
- **Paid in components** (`RepairComponents`): a factory-made block costs its recipe, other blocks themselves,
  shapes their base block. Cost is the blueprint's components minus the old ship's; the yard pulls from connected
  chests, and deconstruction refunds the rest (`ShipyardRepairComponentsMixin`, `ShipyardRepairRefundMixin`,
  `ShipyardRepairGoalMixin`). The shipyard's Inputs Needed shows what the queue still needs
  (`ShipyardInputsButtonMixin`); Repair Queue and Cancel Repairs buttons sit on the shipyard panel
  (`ShipyardQueueButtonMixin`).
- **Unloaded** (`UnloadedRepairs`): with the station unloaded and the ship in its sector, the repair runs on paper
  for the engine's deconstruct-and-build time. It pays from the station's saved inventories (through Resources
  Reorganized's `OfflineProcessing` when installed), then replaces the old entity with the blueprint spawned into
  the database. If the area loads first, nothing has been paid and the ship goes back to the loaded path.
- `/shipyard_repairs_free` makes repairs cost nothing (debug, not saved).
- After a restart, LEAVING entries are dropped and the rest restart from EN_ROUTE (REPAIRING stays). The previous
  orders aren't saved.

**Designs** (`FactionDesigns`, `FactionDesignsList` under Assets → Designs; packets `DESIGNS_REQUEST`, `DESIGN_SET`,
`DESIGNS`; saved in `designs`). Each ship maps to the catalog blueprint it is rebuilt to. Unset, it is guessed from
the ship's name (longest blueprint it starts with, else the longest shared prefix of 4+ characters). Only blueprints
in the faction's own catalog are accepted.

## Trade route safety

`RouteSafety`, saved in `routesafety`, synced as `ROUTE_SAFETY`.

- Each system's safety is `min(1, 0.15 + 0.45·t + 0.4·p) · (1 − 0.8·r)`, where `t` is traffic (half at 200), `p`
  protection (1 inside a reputable faction's system, fading to 0 at 4 systems), `r` raids (half at 3). Reputable
  means infamy and aggression below 3, not pirates. A route is as safe as its least safe system on a straight line.
- Every 30 s each active delivery adds traffic where its convoy is. Traffic and raids fade 10% a day.
- A fleet-carried delivery entering a new system is ambushed with chance `ambush_chance_percent · (1 − safety)²`:
  a pirate patrol of 1–4 ships spawns about 3 sectors away.
- Shown as "NN% safe" in the trade network list (`TradeNodeSafetyMixin`), on corporation routes, and on the galaxy
  map (`SafetyMapOverlay`, the 27 systems around the viewed one, red to green; toggled by the Trade Safety
  checkbox from `MapSafetyToggleMixin`).

## Faction points: upkeep and income

Each income turn `FleetUpkeepMixin` (`Faction.handleFactionPointExpensesServer`, tail) adjusts player factions'
FP, and `FactionPointMods` records each line (FLEET, INSTITUTIONS, SPICE_DEALER, STATIONS, TRIBUTE, DIVIDENDS)
for the faction point stats panel (`FactionPointStatsMixin`, `POINT_MODS`, saved in `point-mods`).

| Line | Rule |
|------|------|
| Fleet upkeep | −1 FP per `fleet_upkeep_mass_per_point` of undocked fleet mass (Council GLORY −10%) |
| Institutions | See [CIVICS.md](CIVICS.md#institutions) |
| Station income | `station_income_points · (1 + log10(mass / station_income_min_mass))` per station at or over the minimum mass (Council CALM +10%) |
| Dividends | Corporation dividends, see [CIVICS.md](CIVICS.md#corporations) |
| Tribute | `tributary_income_percent` of a tributary's income to its overlord (`TributeTurnMixin`) |
| Spice Dealer | See [CIVICS.md](CIVICS.md#spice-dealer) |

`FactionIncomeMixin` sets the engine's per-member income to 300 online / 80 offline.

`FleetUpkeep` remembers every entity's last loaded mass and thrust-to-mass ratio (saved in `fleet-mass`, never
pruned). Fleet mass shows as a column on the fleet and fleet member lists (`FleetMassColumnMixin`,
`FleetMemberMassColumnMixin`, `FleetMemberMassMixin`). Unloaded fleets travel at a speed scaled by their slowest
member's ratio against `fleet_speed_reference_thrust_mass_ratio`, clamped to 0.25–4× (`FleetUnloadedMoveMixin`).

## System claims and the mining ramp

`SystemClaimListener` stops players claiming another faction's system, charges `claim_system_cost` (a faction's
first claim is free; Council EXPANSION −25%), and records the claim time in `ClaimAges` (saved in `claim-ages`).

The owner's mining bonus ramps from the unowned figure to the owned one over `mining_bonus_ramp_hours`. It applies
to salvage and fleet mining (`MiningBonusRampMixin`) and extractors (`ExtractorMiningBonusCalculateListener`). A
system with no recorded claim counts as fully ramped.

## Wrecks

`Wrecks` (saved in `wrecks`). A root ship whose overheat timer runs out becomes an ownerless wreck for
`wreck_lifetime_hours` instead of exploding (`WreckOverheatMixin`). It leaves its fleet, players aboard die as in
vanilla, and turrets knocked off it go with it (`TurretKnockedOffMixin`). Rebooting it un-wrecks it; a second
expiry destroys it. 0 hours keeps vanilla behaviour.

## Freezing NPC fleets

`/npc_fleets_frozen [true|false]` (debug, not saved) stops NPC fleets moving: unloaded moves are skipped
(`FleetUnloadedMoveMixin`), loaded members are not sent on or unstuck (`FleetLoadedFreezeMixin`), and ships in
flight stop (`FleetMoveFreezeMixin`). Target search still runs, so frozen fleets still fight.

## Holdings

`FactionAssetIndex` caches each faction's stations, shops and planets from the entity table (30 s, at most 2000
rows). `ASSETS_REQUEST` (needs `Perms.ASSETS_VIEW`) answers with `FactionAssets`, shown under Assets → Holdings.
With Resources Reorganized installed, each fleet's trade route is shown too (`FleetRoutes`, by reflection).
