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
  <PowerWeights><PointSurplus/><ClaimedSystems/><Members/></PowerWeights>
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

`FactionStrength`: `max(0, surplus)·wS + claimedSystems·wSys + activeMembers·wM` (inactive members count 0.25).
`RELATIVE_STRENGTH` / `POWER` use `clamp(log2(theirs/ours), ±6)`. There is no military term yet.

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
`EMBLEM_HASH`, `EMBLEM_DATA`, `ASSETS`. The server is authoritative; clients only mirror. Opinions go only to
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
| 4.5 | A: wire trust (done). B: what a faction can hand over. C: military strength term | in progress |
| 5 | Federations (done), vassalage and tributary behaviour, war goals, peace terms | in progress |
| 6 | Governance types, faction-internal voting, emblems (custom upload done, no atlas) | in progress |
| 7 | Modding API: more events, preset registration for mods | started (two events, treaty registration) |

## Known issues

- `TERRITORY` is dropped (`DiplomacyAction.fromVanilla`) because the engine fires it for mere proximity.
  Nothing feeds a real trespass signal yet: the engine's intrusion check only covers player-owned space and
  only posts news. Preset rules on `TERRITORY` never fire.
