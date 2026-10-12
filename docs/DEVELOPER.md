# BetterFactions — Developer Guide

How the mod works today. For why it works that way, see [design/DIPLOMACY_PLAN.md](design/DIPLOMACY_PLAN.md)
(audit, findings, decisions) and [design/IMPLEMENTATION_PLAN.md](design/IMPLEMENTATION_PLAN.md) (the phase plan).
Both were written against the engine branch this started on, so their file and line references are to that
branch.

Package root: `src/main/java/videogoose/betterfactions/`.

---

## Layout

| Path | What |
|------|------|
| `BetterFactions.java` | Mod entry. Registers the packet, engine listeners, commands and guides |
| `DiplomacyManager.java` | All diplomacy state and server logic. One instance per side: `server()` / `client()` |
| `BFStore.java` | Save/load of the server manager |
| `BFConfig.java` | Mod config (`custom_emblems_enabled`) |
| `Perms.java` | The mod's rank rights, mapped onto spare engine bits |
| `diplomacy/` | Treaties, proposals, opinion ledgers, NPC config, federations, faction strength |
| `emblem/` | Emblem validation, client cache, GUI element |
| `assets/` | Faction holdings query (stations, shops) |
| `gui/` | Faction panel tabs and dialogs. Ranks are edited in `FactionRankDialog` (the `FactionRankMatrix` grid), not the engine's dialog |
| `net/BFPacket.java` | The one packet: a kind byte plus a tag |
| `commands/` | Admin chat commands |
| `api/` | Events other mods can listen to |
| `src/main/resources/data/npc/` | Shipped NPC archetype presets |
| `src/main/resources/guides/` | In-game guide pages |

## Engine hooks

The mod touches the engine only through these listeners (`BetterFactions.registerEvents`):

| Listener | Use |
|----------|-----|
| `FactionRelationQueryListener` | Returns the treaty-derived relation. This is what makes a treaty change combat |
| `FactionDeleteListener` | Drops the faction's treaties, federation seat, emblem, ledgers and offers |
| `FactionDiplomacyActionListener` | Feeds engine opinion events into the mod's ledgers |
| `NPCExecuteDiplomacyReactionListener` | Always cancels, so only the mod's ledger acts for NPCs |
| `ServerInitializeListener` | Reset, clear preset cache, build asset index, load save |
| `WorldSaveListener` | Save |
| `ServerUpdateListener` | Server tick |
| `PlayerJoinWorldListener` | Queue the join snapshot |
| `SegmentControllerChangeFactionListener` | Invalidate the asset cache |
| `FactionPanelCreateListener` | Build the faction panel tabs |
| `ClientDisconnectListener` | Reset the client mirror |
| `SegmentControllerOverheatListener` | Count a ship lost in the battle it was in |

Mixins (`mixin/`, listed in `betterfactions.mixins.json`), all for `battle/BattleTracker`:

| Mixin | Target | Use |
|-------|--------|-----|
| `CheckAttackMixin` | `EditableSendableSegmentController.checkAttack` (return) | Every hit the engine accepts starts or extends a battle |
| `SoftRailCollisionMixin` | `RailController` turret and rail movement checks | No rail collision checks in a soft-loaded battle sector. Docking keeps its check |
| `SoftShipCollisionMixin` | `CollisionDispatcherExt.needsCollision` HEAD | Ships and stations pass through each other in a soft-loaded battle sector. Hits use ray tests, so they still land. A watching client skips them too: its own physics on the same ships is otherwise most of its frame |
| `WatchCcdMixin` | `ModifiedDynamicsWorld.integrateTransforms`, its `getCcdSquareMotionThreshold` calls | A watching client skips anti-tunnelling sweeps. Debris shards sweep a sphere every physics step, and the engine's sphere sweep skips the bounding-box test, so each was tested against every ship's blocks |
| `TimerAccessor` | `Timer.delta` (accessor) | Lets a ship updated one tick in N be given N ticks' elapsed time |
| `SoftAiTickMixin` | `ShipAIEntity.updateOnActive` | Ship AI runs one tick in `soft_loaded_ai_tick_divisor` in a soft-loaded sector |
| `BattleLoadMixin` | `GameServerController.update`, its `Sendable.updateLocal` calls | Times entity updates in battle sectors. In a soft-loaded one, each ship is updated one tick in `soft_loaded_entity_tick_divisor` (default 2), staggered by the root of its dock chain, with the tick's elapsed time multiplied to match. Ship AI is divided further on top |
| `BattleSectorLoadMixin` | `Universe.updateSector`, its `Sector.update` call | Times a battle sector's physics and projectiles |
| `ProfilerBattlesTabMixin` | `GUINetworkStatsPanelNew.addDrawnTab` (tail) | Adds an admin-only BATTLES tab to the profiler |
| `SoftLootMixin` | `RemoteSector.addItem` (head) | Nothing drops into a soft-loaded battle sector |
| `NPCEntityAccessor` | `NPCEntityContainer$NPCEntity.entityId` (accessor) | Lets the NPC rating backfill read which entity a record points at |
| `SpectatorPingMixin` | `Universe.pingSectorBlock` HEAD | A sector holding only spectators loads no neighbours |
| `SpectatorCameraMixin` | `FreeRoamController.update` | Banner, exit key and battle-scale speeds for the free-roam camera while watching |
| `ServerTickMixin` | `GameServerController.update` HEAD and RETURN | Times every server tick, for the load gate below |
| `TableAccessor` | `Table.c` (accessor) | The world database connection, to ask whether a sector was ever generated |
| `BlueprintRatingMixin` | `ShipOutline` / `SpaceStationOutline.spawnInDatabase` (return) | Rates an entity spawned straight into the database from its blueprint, until it first loads |

## Battles

`BattleTracker` treats a sector where one faction's entity took a hit from another's as a battle. Unloaded
fleets never shoot, so every 5 s it also scans cached fleets whose flagship is unloaded. If one shares an
unloaded sector with a faction it is at war with (other fleets, plus ships and stations from the entity table
for sectors a fleet just moved into), and something there would shoot, that sector is queued. Something that
shoots means a fleet on SENTRY, SENTRY_FORMATION, FLEET_ATTACK, FLEET_DEFEND, PATROL_FLEET or RECON_FLEET, or a
station. Idle and moving fleets don't look for targets even when loaded, so two of them passing is not a battle. Up to `max_soft_loaded_battles` queued sectors are
loaded with `Universe.getSector` and kept alive with `Sector.ping()`. Only player pings load neighbours
(`Universe.pingSectorBlock`, a 3x3x3 cube), so this loads the battle sector alone.

A battle sector no player is watching is soft-loaded, and the mixins above apply. Watching means a player in
the sector or a neighbouring one. A player keeps those 26 neighbours loaded and can see into them. The exception
is a sector in `Universe.notKeptLoaded`, one players have let go, where only a player inside it counts. This is decided every tick, so
a player arriving turns everything back on. A battle a player leaves keeps being pinged until it ends. A battle
ends after 5 min without a hit (`QUIET_MS`), or after 15 min if soft-loaded. The 5 min is so reinforcements a
faction calls in land in the same fight and report rather than a second battle (or, after a break-off, in the
sector's 10 min hit hold, uncounted). A loaded sector that never sees a hit is released after 60 s (`START_MS`). How long its sector is then left alone depends on how it ended (`BattleTracker.Ending`; table below). Every faction in a battle that saw
hits gets a short `WAR` news post: the verdict, ships lost per side, and a pointer to the full report. Losses
count overheats and exclude docked entities. The full report goes to `BattleReports`:
- The server keeps the newest 400 across all factions and saves them with the battles (`battles-<world>.tag`).
- A member asks with `BATTLE_REPORTS_REQUEST`, at most once a second per player, and gets their faction's
  newest 100.
- They show under Assets, in the Battle Reports sub-tab (`BattleReportList`). Each row is coloured by whether
  we held the field (green/red), came off better or worse (lighter green/red) or it was even (grey), and expands
  to the full report with a NAV TO button. The bar above counts held, came off better, came off worse, lost and
  even.
- `Report.winner` and `Report.favoured` are saved as two ints; a report saved before `favoured` existed reads as
  no edge.

The engine's faction news row used to be a fixed 100 px tall with DELETE pinned at its foot, so any post longer
than about four lines ran under the button. It now sizes itself to the post (`FactionNewsScrollableListNew`, an
engine fix). One line also
goes to the galaxy news everyone reads (`NPCFactionNews.post`, an engine addition: a `MOD` event carrying its
own text and category).

**Verdict.** In order (`BattleTracker.summarize`):
- An admin stop names nobody: "Ended by an admin."
- The one side with ships left in the sector that did not retreat held the field (`holder`). A side retreated
  (`Side.withdrew`, "withdrew" on its report line) if auto-resolve sent its fleets home, or if every one of its
  fleets in the fight was ordered away: `noteRetreats` records each fleet's order when it is first seen in the
  battle and checks every second whether it has changed to one bound elsewhere. Pulling one squadron out while
  another fights on is not a retreat. A fleet that arrived already bound elsewhere (an encounter) and a patrol
  moving to its next waypoint are not retreating.
- Otherwise (several sides standing, none, or all broke off) the side that lost clearly less came off better
  (`favoured`, `edge`): under two thirds of the runner-up's combat rating lost, or ships lost when no rating was
  lost, or hits taken when nothing was lost. Only the first tally anyone has decides. Losses are weighed as a
  share of what each side committed (`Side.committedRating`/`committedShips`: the most it had in the sector at
  once, counting what it had already lost, updated every second by the same check), so a side that brought
  reinforcements is not marked down for losing more in absolute terms: "lost 5 of 25 ships to Traders' 4 of 10".
  A battle the tracker never looked at (auto-resolved, ended before its first check, or from before a restart)
  falls back to raw losses. "Inconclusive; Dovan Empire
  came off better (lost 1 ship to Traders' 4)." or "Broke off; ...".
- Otherwise "The battle was inconclusive." or "The fleets broke off and went on their way."

**Soft-load cap.** `max_soft_loaded_battles` counts every battle with no player in it, not only the ones the
tracker loaded. A battle a player leaves while every slot is taken is *carried*: it stops being pinged, and its
tallies wait in `carried` for the sector to fight again. `open()` picks them up whether the fight resumes by a
hit or by a soft load, so one report covers the whole fight. Paused battles take no slot. A carried battle that
doesn't resume within an hour is reported as it stands.

**Test fleets.** Two admin commands, in `BattleCommands`:
- `/battle_blueprints [filter]` lists catalog ship blueprints, heaviest first.
- `/battle_spawn_fleet <factionId> <count> <x> <y> <z> <blueprint name>` spawns `count` ships of one blueprint
  for any faction, straight into an unloaded sector's database, the way NPC fleets are.
  - The ships go in a centred cube, spaced by the blueprint's size plus 40 m. At most 50 ships.
  - The fleet is put on sentry, which also keeps it cached.
  - The fleet belongs to the admin who ran the command, so it can be ordered from their fleet menu. Its ships
    are the faction's, and a fleet takes its flagship's faction, so it fights as that faction.

**Encounters.** A battle where no fleet has business in the sector is an encounter: every fleet there is
bound elsewhere, as patrols, moves and attacks on other sectors are.
- After `battle_encounter_timeout_minutes` (default 20) of combat, it breaks off (`Ending.DISENGAGED`). A soft-loaded
  one breaks off at the 15 min soft-load cap instead, if that comes first.
- Every fleet there is sent its current order again, so ships pulled into the fight go back to it.
- The battle is reported ("Broke off; X came off better (...)." or, when even, "The fleets broke off and went on
  their way."), with each side's losses. If a side was ordered away before that, the side that stayed held the
  field.
- Sentries, defenders and a fleet sent to attack the sector fight on as before. The fleets are checked at most
  every 5 s, and only once the time is up.

**Passing fleets.** The scan only counts a fleet in a sector it has stopped in: no move target, or this
sector is its target. A patrol counts all along its route. A fleet passing through would be gone before the
sector loaded, leaving an empty battle on whatever station it flew past. A queued sector is dropped if no fleet
is stopped there when its turn comes. A battle nobody fired in is never carried or saved, unless an admin
paused it.

**Undiscovered stations.** The galaxy marks some sectors as station sectors, and the map shows them as an
"Undiscovered Station". The station itself (pirate, NPC, empty or random, by system) is only generated when
the sector first loads, so until then the database has nothing there to be at war with:
- A fleet under fighting orders in such a sector is queued anyway. "Never generated" means no row in
  `SECTORS`, read through `TableAccessor`.
- Loading the sector makes the station, and the first shot opens the battle.
- If nobody fires, the sector is released and backs off like any other sector that never fought.
- An armed fleet whose sector isn't a battle logs why, once per sector: who the scan saw there.

In every tracked battle, watched or not, a ship that starts overheating is destroyed at once, with its turrets,
instead of waiting out its timer and undocking them (`BattleTracker.scuttle`). Its loss was already counted when
it started overheating. Ships with a player aboard are left to the engine.

**Server load.** Battles are the lowest priority on the server thread. `ServerTickMixin` times each tick against
the 37 ms budget (`NetUtil.UPDATE_RATE_SERVER`), and the load mixins time the battle sectors within it, so the
gate sees what the rest of the server costs on its own:
- Each tick adds to a credit what the rest of the server left under `battle_hold_above_tick_percent` (default 90)
  of the budget, and takes off what battles spent (`BattleTracker.credit`). Soft-loaded battles run on ticks the
  credit is above 0 and are frozen for the others, with their clocks shifted. A battle that costs three times what
  is left runs about one tick in four: slowed down, not switched off and on. Measuring the whole tick instead
  made a running battle hold itself, and the held battle release itself, every 30 s.
- The credit is capped at one budget and floored at minus four, so a quiet spell is not banked for a burst and one
  hitch does not starve battles for long.
- Queued battles only start while running ones get over 90% of ticks and the rest of the server is under
  `battle_start_below_tick_percent` (default 60).
- A battle a player can see always runs. Its cost still comes out of the credit.
- Soft battles that have not run for 2 s count as held, for the spectator banner and the profiler.
- A held or paused battle with a spectator in it is not frozen outright. With no updates, the spectator's client
  would carry on alone, flying ships on their last velocity and drawing their last shots. Instead it keeps
  updating with AI off, and `holdStill` stops every AI-flown ship each tick. It also clears each ship's and turret's
  synced target and aim: clients run AI firing themselves, for show, while a target is set
  (`ShipAIEntity.updateAIClient`).
- Queued battles that wait through a long crunch settle on paper (auto-resolve, below).
- The profiler header shows the tick, the part of it that is not battles, and the share of ticks soft battles
  run on. Held rows show `HELD`.
- A watching admin's banner switches between SOFT-LOADED BATTLE and BATTLE ON HOLD.

**Auto-resolve.** A queued sector that waits more than `battle_auto_resolve_after_seconds` (default 300, 0 =
never) is settled without loading. Each faction's strength is the sum of the stored `CombatRatings` of what it
has there (fleet members, plus ships and stations from the entity table), with stations ×1.5. Each strength is
rolled at 0.75–1.25, and the highest roll among factions at war with someone present wins. Fleets at war with
the winner are sent off with `MOVE_FLEET`: near their faction's home sector, or 3 sectors away if they have no
home. Nothing is destroyed. Don't use `Fleet.moveToNearHB`: it mutates the faction's home-sector vector in place
and sends homeless factions to the origin.

An entity spawned straight into the database (NPC fleets and stations, mostly) is rated from its blueprint at
spawn (`CombatRatings.putIfUnrated`). It uses the same block counts as a loaded rating, with shields read as
capacitor and recharger blocks, and no reactor gate. The first loaded rating replaces it, and a blueprint rating
never replaces a loaded one.

NPC entities spawned before the spawn-time rating existed are backfilled once per world, at the first military
sweep (`NpcRatingBackfill`). Each NPC system's contingent records blueprint name → entity id
(`NPCEntityContainer.getCounts()`; the id is read through `NPCEntityAccessor`). Each recorded entity with no
rating gets its faction preset's blueprint rating, children included, since the record names the root and its
turrets were spawned unrecorded. A missing blueprint rates nothing, rather than 0. Player ships spawned into the
database before then, and never loaded since, still count 0; that combination is rare.

**Ratings.** `CombatRatings.rate` scales offence by the reactor gate: configured reactor recharge divided by what
every weapon `FiringUnit` (cannons, beams, missiles) draws while reloading, capped at 1. Guns the reactor can't
feed rate nothing.

**Restarts.** Battles (active and carried) and the queue are saved to `moddata/BetterFactions/battles-<world>.tag`
next to the diplomacy file. On load every battle comes back carried, with its sector queued. Ages are saved as
durations, so downtime doesn't count, and each one gets a fresh 5 min quiet window when its sector loads. Each side's commitment (below) is saved with
its tallies; a file from before it reads as 0, and that battle is judged on raw losses.

**World tests.** The battle system has in-world tests on the engine's JSON test framework
(`org.schema.game.server.test.world`). They are `src/main/resources/tests/*.json`, which ship in the jar under
`tests/`. Results go to chat and `logs/tests.0.log`. A player has to be online.

- `/run_tests @videogoose.betterfactions` runs everything the mod has: these world tests, and the `*_test`
  self-checks, which `onRegisterTests` registers through `InGameChecks`.
- `/run_tests betterfactions/*` runs just the world tests; `/run_tests betterfactions/battle_pause_holds` runs
  one.

`InGameChecks` runs each self-check's `main` with assertions switched on for its class, since the game runs
without `-ea`. It skips three that touch state a live server shares: `FactionTreaty_test` (unregisters every
mod's treaty types), `FactionEmblem_test` (writes to the world's emblem store) and `DiplomacyAcceptance_test`
(reads presets from the source tree). Those run only under `./gradlew test`, which runs all of them. `BattleWorldTests`
(registered in `onEnable`) adds a `"battle"` setup list and expect key; its class comment lists the actions and
measures. Docked children spawn in their parent's faction. That is an engine fix in `WorldTestSession`: before
it they spawned factionless, and so were refused a rail on a factioned station.

| Test | Checks |
|------|--------|
| `battle_watched_next_door` | A Trading Guild station's turret fires on a pirate ship in the sector next to the player: a battle is tracked, takes hits, and is never soft-loaded |
| `battle_soft_far_sector` | The same fight three sectors away, held loaded by the test but out of the player's range: soft-loaded from the start, it still fights, and its sector stays loaded |
| `battle_soft_after_unload` | The fight next door, then the test stops keeping that sector loaded: the watched battle hands over to soft-loading and the sector stays loaded |
| `battle_end_releases_sector` | Ending that soft battle reports it, and the sector then unloads |
| `battle_pause_holds` | A paused battle outlives its 5 min quiet window |
| `battle_unloaded_fleet_sentry` | The invoker's fleet sits unloaded beside an unarmed pirate. Ordered to SENTRY while unloaded, it is found by the scan, soft-loaded, and fights. Run it unfactioned or at war with pirates |
| `battle_fleet_attacks_station` | A raider fleet two sectors from an unmanned, non-pirate enemy station is unloaded, then ordered with FLEET_ATTACK onto the station's sector. It travels unloaded, is found by the scan, is soft-loaded, and fires on the station |
| `rating_reactor_gate` | 80 barrels on one reactor block gate to 0.5 or less and rate below the ungated figure; a well-fed ship gates to 1 |

Every test fights outside the player's sector. `main`, the one thing the framework always puts 40 m in front of
the player, is a one-block dummy, and the combatants are `entities` with a `sector` offset. Two tests fight next
door (`[1,0,0]`) because the watched case is what they test: `battle_watched_next_door` and
`battle_soft_after_unload`. The rest are 3 sectors out, beyond the player's loaded range. Each test has a sector of
its own. When three ran at once in one distant sector, none of them got through setup.

Stations are always targets. AI target search (`GameState.checkTarget`, and the turret search's copy of it)
used to skip any unmanned target with its AI off, unless it was a pirate. That covered stations too, so an
attack order on a station did nothing. Engine fix: the rule now covers ships only, so parked and derelict ships
are still left alone.

The ship targets are pirates for a reason. Turret target search (`SeachingForTurretTarget`) skips a ship with no
pilot and its AI off unless it belongs to the pirates, so an unattended target of any other faction is never
shot at.

**Cuts considered and not made.** Each was checked against the engine source:
- **Network sync:** the engine only sends to clients that can see the entity. The encode runs for every changed
  entity anyway, and skipping `updateToNetworkObject` would freeze the view for faction-mates, who see an entity
  at any range.
- **System recalculation:** the engine already debounces it to once a second per collection on damaged entities
  (`ELEMENT_COLLECTION_RECALC_DEBOUNCE_MS`), and the heavy part runs on a worker thread.
- **Autosave:** skipping it would lose a crash's worth of battle, and would leave the entity table's positions
  stale. The battle scan reads those positions.
- **Factories:** production doesn't catch up, so skipping it would cost station owners real output.

**Admin view.** The profiler (`GUINetworkStatsPanelNew`) gets a BATTLES tab when the player is an admin. Its
SERVER tab reads the server's statics directly, which only works when the server runs in the same JVM, so the
battles tab asks instead. While the tab is drawn it sends `BATTLES_REQUEST` about once a second, and the
server (checking `isAdmin` again) answers with `BATTLES`: the report from `BattleTracker.adminReport`, built
as text on the server. It lists each battle's sector, watched or soft-loaded, update cost in ms/tick
(smoothed), entity count, age, time since the last hit, and each side's losses. It also shows the total battle
cost against the server's average tick (`ServerTickProfiler.getAvgTickMs`), the queue, and the last 8 battles
that ended. Rail collision checks run on a thread pool and are not in the figure.

Below the header, `BattleAdminList` has one row per battle with three buttons, sent as `BATTLE_COMMAND`
{action, sector id} and checked for admin rights on the server:

- **Pause / Resume.** A paused battle runs no ship AI anywhere in its sector, so nothing fires. With no player
  there it is frozen: `BattleLoadMixin` and `BattleSectorLoadMixin` skip its entity and sector updates. A
  player arriving unfreezes it, but the AI stays off until it is resumed. A paused battle's clocks stop, and
  resuming gives back the time it had.
- **End.** Asks for confirmation, then reports the battle ("Ended by an admin.") and releases it. The sector is not queued or tracked
  again for 30 min unless a player is there.
- **Go to.** Moves whatever the admin is controlling to the sector (its rail root), the same way
  `/change_sector` does.
- **Watch.** Takes the admin, on foot only, to the battle as a spectator, without making it a watched battle:
  - It stays soft-loaded. `watched()` skips spectators.
  - Invisibility is turned on, which the AI already ignores as a target.
  - `SpectatorPingMixin` stops their 26 neighbour sectors loading.
  - On arrival the server sends `BATTLE_WATCH`, and the client switches to the engine's free-roam
    camera. Only the character's movement is suspended, so N (nav list), F (select what the camera
    looks at) and the target panel still work. The camera holds still and frees the mouse while a menu
    or chat is open. `SpectatorCameraMixin` speeds it up to 40 m/s, or 400 m/s with Shift, and polls the
    exit-ship key (R).
  - Pressing R, or WATCH again, or the battle ending, sends them back to where they were and restores their
    invisibility. Leaving the sector on their own just ends it.
  - Getting a player out of their ship is up to their client, so a ship is refused.

How long a sector is left alone after a battle ends (`BattleTracker.Ending`):

| Ended because | Scan won't queue it for | New hits ignored, unless watched, for |
|---------------|-------------------------|----------------------------------------|
| It fought, then went quiet | 5 min | 0 |
| It loaded and nobody fired | 5 min | 0 |
| It reached the 15 min soft-load cap | 5 min | 5 min |
| It was released over the soft-load cap | 5 min | 5 min |
| An admin ended it | 30 min | 30 min |
| An encounter broke off | 10 min | 10 min |

Hits are never ignored after a fight that went quiet. Fleets still in the loaded sector, or arriving, start a
new battle at their next shot. A blanket hit block once meant two fleets left in a sector after a battle never
fought again: their hits were ignored, the sector unloaded, and the scan then skipped it.

Each soft-loaded round in a row that ends with nothing lost doubles the scan wait: 5, 10, 20, then 40 minutes.
Without that, fleets that cannot hurt each other, or will not fire, get their sector reloaded forever. A round
where something is lost, or one a player watched, resets the count.

A round that fought but lost nothing is carried rather than reported, so the next round adds to the same
report. Its clocks (quiet, soft-load cap) restart when it reloads, and its reported length runs from the first
shot. A carried round no one picks up is reported after an hour.

Every ship and station in a battle sector is rated as the battle ends (`rateAll`). Otherwise a fleet never
loaded since ratings began counts as nothing until a military sweep happens to catch it loaded.

Plus `StarMod.onRegisterGuides` (see [Guides](#guides)) and the `UNDEF5-7` permission bits. The engine's
founder rank already holds them; `Perms.define()` names them with `PermType.define` on every enable, which is
what makes them show up in the engine's rank editor. The engine hides them again on mod reset.

| Right | Bit | Rank editor label | Gates |
|-------|-----|-------------------|-------|
| `Perms.TREATIES` | `UNDEF5` | Sign treaties | Every treaty but war and peace |
| `Perms.FEDERATION_VOTE` | `UNDEF6` | Speak for us in the federation | Every federation command |
| `Perms.ASSETS_VIEW` | `UNDEF7` | View holdings | The holdings query |

**Ranks.** The engine branch gives each faction 2 to 10 ranks (`FactionRoles.MIN_ROLES`/`MAX_ROLES`). Rank
indices are a fixed space with the founder always at 9 (`INDEX_ADMIN_ROLE`); a faction with *n* ranks uses
slots `10 - n` to 9, and ranks are only added or removed at the bottom. That is what keeps structure rank
requirements (`player rank >= required`, stored on possibly unloaded entities) and fleet access (counted down
from the founder) meaning the same through any change. Old saves (founder at 4) are renumbered once on load:
faction members via the legacy roles tag, structures via entity tag version 2. Only the founder (or a server
admin) may change the count; the server moves members of a removed rank up. `FactionRankDialog` sends the
count with the rest of the roles rewrite.

## Treaties

A `FactionTreaty` is `{typeId, from, to, signedAt, expiresAt, name, clause}`, stored per unordered pair in a
`FactionTreatySet`. Direction lives in `from`/`to`, so asymmetric types (vassalage, tributary) keep it.

`TreatyType` is registry data, not an enum. Builder flags: `asymmetric()`, `unilateral()` (no acceptance),
`unbreakable()`, `implies(RType)`, `names(templates...)`. The **kind** is derived from the flags: unilateral is a
declaration, symmetric and agreed is a pact, asymmetric and agreed is a demand. The built-in types are seeded in
`TreatyTypeRegistry` under the `starmade:` namespace.

**Derived relation.** `FactionTreatySet.getDerivedRelation()`: any ENEMY-implying treaty wins, then FRIEND
beats NEUTRAL, and no relation-implying treaty returns `null` so the engine's own relation byte stands. Signing
also writes the legacy relation via `setRelationServer`, so engine code that reads the byte directly agrees.

## Proposals

`DiplomacyProposal` actions: `PROPOSE`, `REVOKE`, `ACCEPT`, `DECLINE`, `COUNTER`, `QUERY`, `QUERY_RESULT`. A
null type id is white peace.

Server flow (`DiplomacyManager.handleRelationShipOffers`):

1. The initiator is overwritten with the packet sender's name; clients cannot claim to be someone else.
2. `mayNegotiate`: server admins pass; the engine relationship permission passes everything; `Perms.TREATIES`
   passes everything but war and peace.
3. `QUERY` is answered with the acceptance verdict and unmet conditions, nothing else changes.
4. Unilateral types apply at once. Everything else is stored, broadcast, and answered immediately by
   `answerAsNPC` if the target is an NPC.
5. Answers are permission-checked against the offer's target before the offer is consumed. `COUNTER` declines
   and proposes back the other way.

Signing a type the pair already holds replaces it; that is how renames and renewals work. Names come from the
proposal if `TreatyNames.sanitize` accepts them, else from the type's templates (`{from}`, `{to}`,
`{fromHome}`, `{toHome}`, `{ceded}`, `{type}`; a template whose token has no value is skipped).

Expired treaties are removed on the server tick. Intel sharing re-merges both maps every 60 s.

## Opinion

`DiplomacyEntity` is one faction's ledger: an entry per other faction id or player db id (db id ≥
`Integer.MAX_VALUE` is a player). Score = raw points + static modifiers, clamped to the configured range.

- **Actions** (`DiplomacyAction.DiplActionType`) are events. A live action drives an `OpinionTurnModifier` that
  ramps toward its limit while the action repeats and walks back once it lapses.
- **Statuses** (`DiplStatusType`) are derived each status tick and give an `OpinionStaticModifier`.
- Four timers in `FactionDiplomacy`: status calc, turn apply, turn change, and an 11 s re-check after an action.
- Tags are string-keyed by enum name, so the enums can be reordered; unknown names are dropped on load.
- Treaty actions carry the treaty id as their subject. `getActionCount(type)` sums every subject, which is
  what conditions, turn modifiers and trust read; `getActionCount(type, subject)` asks about one treaty.

Player factions use the Java defaults in `DefaultDiplomacySettings` and never act on opinion. NPC factions run
reactions and acceptance from their preset.

## NPC presets

`DefaultDiplomacySettings.forPreset(name)` looks up, by lowercased engine preset name:

1. `moddata/BetterFactions/npc/<name>.xml`
2. jar `data/npc/<name>.xml`
3. moddata `default.xml`, then jar `default.xml`
4. built-in Java defaults

Cached per name, cleared on server init. Format:

```xml
<Diplomacy>
  <Version>4</Version>
  <ActionTimeouts><Attack>300000</Attack></ActionTimeouts>
  <DiplomacyElement>
    <DiplAction><Name/><UpperLimit/><LowerLimit/><ExistingModifier/><NonExistingModifier/>
                <ActionTimeoutDuration/><Value/></DiplAction>
    <DiplStatus><Name/><Timeout/><Value/></DiplStatus>
  </DiplomacyElement>
  <PowerWeights><PointSurplus/><ClaimedSystems/><Members/><Military/></PowerWeights>
  <Reactions>
    <Reaction><Name/><Message/><Action>PROPOSE_TREATY</Action><Treaty>starmade:alliance</Treaty>
      <AND><Condition><Type>RAW_POINTS</Type><Value>1000</Value></Condition></AND>
    </Reaction>
  </Reactions>
  <Acceptance>
    <Accept><Treaty>starmade:alliance</Treaty><Refusal>They will not stand beside you.</Refusal>
      <AND>...</AND>
    </Accept>
  </Acceptance>
</Diplomacy>
```

- Reactions: `PROPOSE_TREATY` (empty `<Treaty>` = peace), `WITHDRAW_PROPOSAL`, `BREAK_TREATY`,
  `SEND_POPUP_MESSAGE`, `SEND_FACTION_MAIL`.
- Condition types: `EXISTS_ACTION`, `EXISTS_STATUS`, `STATUS_POINTS`, `STATUS_PERSISTED`, `ACTION_PERSISTED`,
  `ACTION_COUNTER`, `TOTAL_POINTS`, `RAW_POINTS`, `HAS_TREATY`, `TREATY_COUNT`, `RELATIVE_STRENGTH`,
  `AT_WAR_COUNT`, `SHARES_ENEMY`, `SHARES_FRIEND`, `IS_FACTION_TYPE`. Groups are `AND` / `OR` / `NOT` (one child).
- A type with no `<Accept>` rule is refused. Demands are refused before rules are read.
- Unknown action, status, reaction or treaty names skip that entry, not the file.
- A missing `<Reactions>` block means no reactions; a missing `<Acceptance>` keeps the built-in rules.
- `<Settlement><PeaceValue>500</PeaceValue><TreatyValue>1000</TreatyValue></Settlement>` prices settlement terms
  (`SettlementPricing`). Each term is worth its faction points, or `TreatyValue` per treaty signed or broken (5x for a
  vassalage or tributary, and never for the NPC to submit). Peace is worth `PeaceValue·2^lead` to an NPC whose peace
  rule accepts (it gives up to that), and costs `PeaceValue·2^-lead` when the rule refuses (it wants paying), where
  `lead` is the other side's strength advantage in doublings, clamped to ±4. Other agreements are not for sale. Short
  of the price, an NPC counters a player with the terms it would sign: what it gives talked down or dropped, then any
  shortfall asked for in faction points.

`FactionStrength`: `max(0, surplus)·wS + claimedSystems·wSys + activeMembers·wM + sqrt(combatRating)·wMil` (inactive members count 0.25).
`RELATIVE_STRENGTH` / `POWER` use `clamp(log2(theirs/ours), ±6)`.

`CombatRatings`: per ship/station, `sqrt(weaponOutputBlocks · (shieldBlocks + 0.1·totalBlocks))`. The server rates every
loaded faction-owned ship and station once a minute, keeps the rating (saved) while it is unloaded, and drops rows the
entity table says the faction no longer owns. Clients get per-faction totals (`MILITARY` packet, and the snapshot).
Reactor output and thrust are not read.

## Federations

`Federation` holds members, chair, invites, open votes and `charterFlags` (one long: vote-on bits, threshold,
weighting, binding, chair rotation days). Commands are `FederationCommand`s over `BFPacket`, all gated by
`Perms.FEDERATION_VOTE`, and the chair-or-vote rule per command lives in `DiplomacyManager`. Membership writes a
`starmade:federation` treaty with every other member; a federation war signs a `starmade:war` treaty from each
bound member. Votes and ballots are only sent to members.

## Emblems

Off unless `custom_emblems_enabled`. The server checks the PNG header (≤ 64 KB, ≤ 2048²), decodes, re-encodes to
128×128 and stores `moddata/BetterFactions/emblems/emblem_<sha256-16>.png`. Clients fetch by hash into
`./client-faction-emblems/<host>/`. `/faction_emblem_clear` removes one.

## Persistence and networking

`BFStore` writes `moddata/BetterFactions/diplomacy-<world>.tag` (zipped tag, written to `.tmp` and moved) on
world save and on disable. An unreadable file is renamed `.unreadable-<ts>` and the world starts empty.

`BFPacket` kinds — client to server: `PROPOSAL`, `FEDERATION_COMMAND`, `EMBLEM_UPLOAD`, `EMBLEM_CLEAR`,
`EMBLEM_REQUEST`, `ASSETS_REQUEST`; server to client: `SNAPSHOT`, `TREATY`, `OFFER`, `FEDERATION`, `OPINION`,
`EMBLEM_HASH`, `EMBLEM_DATA`, `ASSETS`, `MILITARY`. The server is authoritative; clients only mirror. Opinions go only to
the player or faction they are about.

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
        .names("The {from}-{to} Accord"));
```

A registered type appears in the propose dialog, inbox and treaty list with no GUI change, and can be named
in preset `<Treaty>` / `<Accept>` elements. Server-side helpers: `DiplomacyManager.server().signTreatyServer`,
`breakTreatyServer`, `hasTreaty`, `getTreaties`, `getFederationOf`.

## Guides

The engine's in-game guide viewer reads built-in pages from `data/guides/<section>/<nn-title>.md` and mods add
pages in `StarMod.onRegisterGuides` with `registrar.registerFromResource(sectionKey, sectionLabel, title, path,
mod)`.

A mod page whose section key and title match a built-in page **replaces it in place**. The key is the
built-in's folder name and the title is its file name without the number prefix, so BetterFactions replaces
`data/guides/07-multiplayer/00-factions.md` with `("07-multiplayer", "Multiplayer", "Factions", ...)`. This
override needs the `GuidesRepository` change on `nf/faction-mod-hooks`; on an engine without it the page shows
up as a second "Factions" entry instead.

Guide pages cannot link to each other and mod pages cannot show images (they have no file path to resolve
against), so keep them self-contained text. When a feature changes, update its page in
`src/main/resources/guides/`.

## Tests

`./gradlew selfChecks` runs each `*_test` class's `main` with `-ea`. `DiplomacyAcceptance_test` reads
`src/main/resources/data/npc/` and checks every shipped preset parses, has reactions and acceptance rules, gives
every rule a `<Refusal>`, has no rule for a demand, and differs from the others. The tests live in `src/main`
and currently ship in the jar.

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
| 6 | Governance types, faction-internal voting, emblems (custom upload done, no atlas) | in progress |
| 7 | Modding API: more events, preset registration for mods | started (two events, treaty registration) |

## Known issues

- `TERRITORY` is dropped (`DiplomacyAction.fromVanilla`) because the engine fires it for mere proximity.
  Nothing feeds a real trespass signal yet: the engine's intrusion check only covers player-owned space and
  only posts news. Preset rules on `TERRITORY` never fire.
