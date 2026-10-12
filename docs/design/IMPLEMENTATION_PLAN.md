# Faction Diplomacy Overhaul — Stellaris-style treaties for player *and* NPC factions

> The original engine-side implementation plan, kept for its design decisions. See the note at the top of
> `DIPLOMACY_PLAN.md` for how it maps onto the mod.
>
> **History, not reference.** For how the mod works now and the current roadmap, see `../DEVELOPER.md`.


## Context

StarMade has **two disconnected halves of a diplomacy system**.

The half that works is NPC-only. `org.schema.game.server.data.simulation.npc.diplomacy` already contains
a genuine opinion engine: `NPCDiplomacyEntity` holds a signed point score per (NPC faction → other faction
*or* player), built from decaying `NPCDiplStaticModifier` (flat offset while a status holds) and
`NPCDiplTurnModifier` (per-turn drip that ramps up while an action repeats and walks back when it lapses).
`DiplomacyConfig` drives it from per-preset XML with an AND/OR/NOT `DiplomacyCondition` grammar and a
`DiplomacyReaction` list. That is most of a Stellaris opinion model already — it is just welded to
`NPCFaction` and invisible to player factions.

The half that doesn't is everything else. Actual *relations* are a single byte:

```java
// FactionRelation.java:20-22
public int a; public int b; public byte rel;   // RType{ NEUTRAL(0), ENEMY(1), FRIEND(2) }
```

keyed by a symmetric code, so relations cannot be asymmetric and cannot coexist. There is no treaty object
at all — "peace" is the *absence* of ENEMY, "non-aggression" is a derived NPC status that means "nobody has
shot at me lately", and trade agreements do not exist. Proposals ride `FactionRelationOffer`, which is
`FactionRelation` plus a message string, and the server pipeline that applies them is a hardcoded
`if (isEnemy) … else if (isFriend) … else if (isNeutral)` chain.

The goal: lift the opinion engine out of the NPC package onto `Faction` so every faction has one, and
replace the single relation byte with a set of first-class **treaties** that both players and NPCs sign,
break and reason about — federations, pacts, vassalage, casus belli — with NPC acceptance driven by the
existing per-preset XML, and the whole thing exposed to mods.

**Decisions already made (do not re-litigate):**
- `RType` stays as a **derived view**. Treaties become the real state; `getRelation()` collapses them to one
  of the three legacy values. The ~19 consumer files (turret targeting, beams, shops, fleets, HUD) are untouched.
- Player factions **do** accrue opinion toward each other, but opinion is **strictly informative/RP** for
  player↔player — it must never gate a player faction's available actions. Only NPC factions evaluate
  reactions and acceptance from opinion.
- **Fresh start on migration.** Existing `RType` rows load into the legacy field; no treaties are synthesised
  and old NPC opinion ledgers are dropped. This removes the hardest compatibility problem.
- Negotiation is gated by **faction role permissions** (§Phase 2). Voting is an opt-in charter flag, never
  the default.
- **No proximity-based opinion.** StarMade galaxies are too small for it; it would read as the map punishing
  you. See §New vocabulary.
- Scope includes a **lore/RP layer** — governance, voting, emblems (§Phase 6).
- Mods get **events + registration** (§Phase 7).

Write the design up as `docs/dev-docs/DIPLOMACY_PLAN.md` first, matching the house style of
`docs/dev-docs/PARTICLE_SYSTEM_PLAN.md` (a "Current state" table, then findings, then the roadmap). The
phases below are that roadmap.

---

## Core model

One new package, `org.schema.game.common.data.player.faction.diplomacy` (common — client and server both
need it, the client mirrors relations locally).

**`TreatyType`** — a **registry object, not an enum**, mirroring `PlanetTypeConfig` /
`UniversalRegistry.RegistryType`. Namespaced ids (`starmade:non_aggression`, `mymod:tribute`). Each type carries:
symmetric-vs-asymmetric, the `RType` it implies, its opinion modifier, whether signing needs acceptance,
duration/break rules, and which `DiplStatusType` it maps to for the NPC opinion ledger.

Vanilla set, chosen to match systems StarMade actually has:

| Type | Sym? | Implies | Effect hook |
|---|---|---|---|
| `war` | asym | ENEMY | carries a `WarGoal`; replaces today's ENEMY byte |
| `non_aggression` | sym | NEUTRAL | blocks `Faction.declareFactionWarOnHostileAction` for its duration |
| `defensive_pact` | sym | FRIEND | call-to-arms when a signatory is attacked (`Faction.onAttackOnServer`) |
| `alliance` | sym | FRIEND | today's FRIEND |
| `trade_agreement` | sym | — | `ShoppingAddOn.TradePerm`, trade-node visibility |
| `shared_vision` | sym | — | reuses `factionShareFowBuffer` / `canAllySeeStructure` |
| `vassalage` | **asym** | FRIEND | overlord/vassal; vassal cannot declare war independently |
| `tributary` | **asym** | NEUTRAL | periodic resource transfer via `NPCFactionInventory` / faction points |
| `federation` | sym | FRIEND | synthesised from `Federation` membership, not signed directly |

> **Persistence gotcha, fix it here:** every existing diplomacy tag writes `enum.ordinal()`
> (`DiplomacyAction.java:99-116`, `NPCDipl*Modifier`, `NPCDiplomacyEntity.toTag`), so reordering
> `DiplActionType` silently corrupts saves. Treaty types are a registry and **must** persist by string id.
> Convert the existing action/status tags to string keys in the same pass — it is the same serializer, and it
> is what unblocks §New vocabulary below.

**`FactionTreaty`** — `{int from; int to; String typeId; long signedAt; long expiresAt; byte flags; Tag payload}`.
Asymmetry lives in the record (`from`/`to`), not in the key, so there is **one** map:

```java
// FactionManager
private final Long2ObjectOpenHashMap<FactionTreatySet> treaties;  // key = FactionRelation.getCode(a,b)
```

`FactionTreatySet` holds a short `ObjectArrayList<FactionTreaty>` plus a **cached derived `RType`**. The cache
is not optional — `SeachingForTurretTarget.java:266` calls `isEnemy` per turret per tick; a per-call scan
would land in the combat hot path.

**`DiplomacyProposal`** replaces `FactionRelationOffer`: `{from, to, typeId, action(SIGN|BREAK|RENEW),
message, initiator, Tag payload, expiresAt}`, keyed by the directed `getOfferCode(a,b)` that
`FactionRelationOffer.java:16` already defines.

**`Federation`** — `{int id; String name; IntArrayList members; int leader; long charterFlags}` in
`FactionManager.federations`. Co-membership is exposed as a synthetic `federation` treaty so `getRelation`
has exactly one code path. Votes reuse the proposal pipeline with a federation-scoped proposal.

**`WarGoal` / casus belli** — a `WarGoal` rides the `war` treaty's payload (`{type, targetSystem, claimed}`).
A casus belli is just a `DiplomacyConditionGroup` that must evaluate true to unlock a goal — reuse the
existing grammar verbatim, do not invent a second one.

---

## New vocabulary

The existing opinion vocabulary was written for the three-state relation model and does not cover treaties,
federations, tribute or war goals. Extend all four enums. This spans phases, so it is listed once here.

**Blocked on the string-key conversion in Phase 1.** `DiplActionType` and `DiplStatusType` are persisted by
`enum.ordinal()` (`DiplomacyAction.java:99-116`, both `*Modifier` classes, `NPCDiplomacyEntity.toTag`), so
today a value can only be appended at the end and never reordered without corrupting saves. Once the tags are
string-keyed, the vocabulary can grow and be regrouped freely — do that conversion **first**.

### `DiplActionType` — discrete events that move opinion

Today: `ATTACK`, `ATTACK_ENEMY`, `MINING`, `TERRITORY`, `PEACE_OFFER`, `DECLARATION_OF_WAR`,
`ALLIANCE_REQUEST`, `ALLIANCE_CANCEL`, `TRADING_WITH_US`, `TRADING_WITH_ENEMY`, `ALLIANCE_WITH_ENEMY`.

The four `*_ALLIANCE_*` / `*_PEACE_*` entries are per-treaty-type special cases of one idea. Replace them with
generic treaty actions **sub-keyed by treaty id**, with per-type point values in XML:

| New action | Fired from |
|---|---|
| `TREATY_PROPOSED` / `TREATY_SIGNED` / `TREATY_REFUSED` / `TREATY_BROKEN` / `TREATY_EXPIRED` | the Phase 2 proposal pipeline |
| `CALL_TO_ARMS_HONORED` / `CALL_TO_ARMS_IGNORED` | defensive pact, `Faction.onAttackOnServer` |
| `TRIBUTE_PAID` / `TRIBUTE_MISSED` | tributary, on the faction-point income tick |
| `GIFT` | one-sided credit/resource transfer — the classic lever for buying opinion, needs a UI action |
| `FEDERATION_VOTE_WITH_US` / `FEDERATION_VOTE_AGAINST_US` | federation votes |
| `CLAIMED_OUR_SYSTEM` | `SystemClaimListener` already fires here; this is its opinion consequence |
| `WAR_GOAL_ACHIEVED` / `WHITE_PEACE` / `SURRENDERED` | war resolution |
| `VASSALIZED_THEM` / `RELEASED_VASSAL` | annexation vs liberation |

> The action map is `Byte2ObjectOpenHashMap<DiplomacyAction>` keyed on the type ordinal, so
> `TREATY_BROKEN(alliance)` and `TREATY_BROKEN(nap)` would collide on one counter. The string-key conversion
> fixes this too — key on `typeName` or `typeName + ":" + treatyId`. Same change, one place.

`ATTACK_ENEMY` already covers "helped us in a war"; no separate action needed.

**Drop proximity-based opinion entirely.** Stellaris can charge you for having borders near someone because its
galaxies are large enough that distance is a real choice. StarMade's are not — in a typical galaxy almost
every faction would sit permanently inside someone's penalty radius, and the resulting opinion hit reads as
the map punishing you rather than as anything you did. Concretely:

- Delete the `CLOSE_TERRITORY` status.
- **Repoint the `TERRITORY` action.** Despite its name ("Violated their territory") it fires today from the
  expansion scan finding an adjacent foreign system (`geo/NPCSystemStructure.java:314`) — it is proximity, not
  intrusion. Fire it instead from `FactionManager.checkTerritoryIntrusion()` (`:2803-2851`), which already
  detects a ship entering a claimed system and already carries a 5-minute per-owner/system/intruder cooldown,
  but currently only posts a news item and feeds no diplomacy action. That turns "you exist near me" into "you
  flew a warship into my system" — a player-legible act with a player-legible fix.
- `THREAT` (below) is scaled by recent conquests only, **not** by proximity, for the same reason.

### `DiplStatusType` — ongoing conditions

Today's list has three entries that do not work: `POWER` returns 0, `PIRATE` returns 1 but is skipped in the
default config, and `FACTION_MEMBER_AT_WAR_WITH_US` / `FACTION_MEMBER_WE_DONT_LIKE` have no `calculateStatus`
case at all.

**`POWER` must be implemented, not deleted** — relative strength is precisely what config-driven acceptance
needs ("they will not ally someone this weak, or pact with someone this frightening"). It needs a defined
strength metric; faction points + claimed system count + member count is the cheapest one available without
new bookkeeping. Decide the formula during Phase 3 and make it a config value, not a constant. `PIRATE` is
trivially derivable from the relation to `PIRATES_ID` — implement it or delete it, don't leave it returning a
value nothing reads.

New statuses:

| New status | Meaning |
|---|---|
| `TREATY_ACTIVE` (sub-keyed by treaty id) | replaces the hardcoded `ALLIANCE` and `NON_AGGRESSION` cases |
| `OVERLORD` / `VASSAL` | the two ends of vassalage — asymmetric, so they cannot share one value |
| `SAME_FEDERATION` | |
| `TRUST` | accumulates while a treaty is held unbroken and decays when one is broken; Stellaris's trust mechanic and the thing that makes long alliances feel earned |
| `RIVAL` | explicitly declared rivalry — the inverse of a treaty, and a casus belli source |
| `THREAT` | recent conquests and war declarations — **not** scaled by proximity, per the note above |

### `ConditionType` — what acceptance rules can test

Today: `EXISTS_ACTION`, `EXISTS_STATUS`, `STATUS_POINTS`, `STATUS_PERSISTED`, `ACTION_PERSISTED`,
`ACTION_COUNTER`, `TOTAL_POINTS`, `RAW_POINTS` — all of which only see the opinion ledger. Phase 3 acceptance
needs to see the world:

`HAS_TREATY(typeId)`, `TREATY_COUNT(typeId)` (for per-type caps), `RELATIVE_STRENGTH`,
`AT_WAR_COUNT`, `SHARES_ENEMY` / `SHARES_FRIEND` (common-cause weighting),
`IS_FACTION_TYPE` (player / NPC / pirate), `DISTANCE` (home-base separation).

`DISTANCE` is a one-off *acceptance* gate, not a passive penalty, so it does not carry the problem that sank
`CLOSE_TERRITORY` — but leave it unused in the shipped presets for the same reason, and let server owners and
modders opt into it.

### `ConditionReaction` — what an NPC can do about it

The six hardcoded `*_ALLIANCE_OFFER` / `*_PEACE_OFFER` entries collapse into
`PROPOSE_TREATY` / `ACCEPT_TREATY` / `REJECT_TREATY` / `BREAK_TREATY` (each taking a treaty id), a net
deletion. Add `DEMAND_TRIBUTE`, `OFFER_VASSALIZATION`, `CALL_ALLIES_TO_ARMS`, `JOIN_FEDERATION`,
`LEAVE_FEDERATION`, `DECLARE_RIVAL`, and `SEND_FACTION_MAIL` (distinct from the existing
`SEND_POPUP_MESSAGE` — it routes through `sendServerFactionMail`, which already exists and persists).

Every enum here is surfaced to config authors by `StringTools.listEnum` through `DiplomacyConfig.appendXML`'s
comment block, so additions document themselves in the regenerated XML.

---

## Phase 0 — Lift the opinion engine onto `Faction`

No behaviour change. This is a move plus a re-host, and it is what makes everything after it cheap.

Move `NPCDiplomacy`, `NPCDiplomacyEntity`, `NPCDipl{Modifier,StaticModifier,TurnModifier}`, `DiplomacyAction`,
`DiplomacyReaction`, `DiplomacyCondition`, `DiplomacyConditionGroup` from
`server/data/simulation/npc/diplomacy/` into the new common package, dropping the `NPC` prefix
(`FactionDiplomacy`, `DiplomacyEntity`, `OpinionModifier`, …).

- Field moves `NPCFaction` → `Faction`; drop it from `NPCFaction.toTagAdditionalInfo()` slot 8 and write it
  into a spare slot of `Faction`'s 32-slot array (`Faction.java:428-480`, which already does defensive
  length checks on load). Fresh start means old ledgers are simply not read.
- `Faction.checkActions()` (`Faction.java:141-150`) already drains `attackedBy` into `ATTACK` actions for
  every faction — it just had no ledger to write to for player factions. It now does.
- **Gate reaction evaluation, not accrual.** `FactionDiplomacy.checkReactions()` runs only when
  `faction.isNPC()`. Player↔player pairs accrue and display; nothing acts on the number.
- The scheduler (`NPCDiplomacy.initialize()`'s four `TimedExecution`s, driven from
  `NPCFactionManager.updateLocal` every ~3 s) needs a home that runs for player factions too — drive it from
  `FactionManager.updateLocal(Timer)`, which already runs on both sides.

Fix while here, since these are the lines being moved:
- `DiplStatusType.FACTION_MEMBER_AT_WAR_WITH_US` and `FACTION_MEMBER_WE_DONT_LIKE` are configured with values
  but have **no case** in `calculateStatus` — they fall through to `return 0` and never fire.
- `NPCDiplStaticModifier.totalTimeApplied` and `NPCDiplTurnModifier.elapsedTime` are **not persisted**, so
  every `STATUS_PERSISTED` / `ACTION_PERSISTED` condition resets to 0 on server restart.

**Files:** the 8 files above; `Faction.java`, `NPCFaction.java`, `FactionManager.java`,
`NPCFactionManager.java`, `RemoteNPCDiplomacy.java`, `api/event/faction/NPCDiplomacyActionListener.java`
(signature/package), `GUINPCDiplomacyScrollableList.java` (imports only — it reads `NPCDiplModifier` rows
through `getName()`/`getValue()` and keeps working unchanged once opinion lives on `Faction`).

---

## Phase 1 — Treaty model, storage, sync, derived relation

1. `TreatyType` + `TreatyTypeRegistry` (static map, namespaced ids, engine registers vanilla at startup).
   Follow `PlanetTypeConfig.registerType` (`.../world/planet/config/PlanetTypeConfig.java:217`) — it rejects a
   bare id with no namespace prefix and invalidates lazily rather than reloading.
2. `FactionTreaty` + `FactionTreatySet` with the cached `RType`.
3. `FactionManager.treaties` map, persisted as a **new trailing slot** in the `factions-v2` root struct.
   `fromTagStructure` already uses the optional-trailing-tag idiom at `subs[10]`
   (`if (subs[10].getType() != Type.FINISH)`); append at `subs[11]` with the same guard **plus** a
   `subs.length > 11` check, since files written before `currentFactionIdCreator` have the FinishTag at 10.
4. `FactionManager.getRelation(int,int)` (`:752-803`) gains one step between the faction-mode checks and the
   raw table lookup: consult the treaty set's cached `RType`. All other consumers untouched.
5. Network: new `RemoteFactionTreaty` + `RemoteFactionTreatyBuffer` pair — the established pattern, most
   recently exercised by `RemoteSystemRename`/`RemoteSystemRenameBuffer`. Add the buffer field to
   `NetworkGameState`; **keep** `factionRelationships` (the client still mirrors the derived `RType`).
   Include treaties in `updateToFullNetworkObject` (`:2656-2689`).

Fix while here — both are in the code being rewritten:
- `relations.put()` at `FactionManager.java:1902` happens **before** the `isCanceled()` check at `:1903`, so a
  cancelling `FactionRelationChangeListener` still mutates the map; it only suppresses the rule trigger and
  the broadcast.
- `isEnemy(int,int)` (`:898`) and `isNeutral(int,int)` (`:978`) ignore `MODE_SPECTATORS`, `MODE_FIGHTERS_FFA`
  and `allyNeutral`, while `getRelation(int,int)` honours all three — the two families disagree for the same
  pair. Route the predicates through `getRelation`.

---

## Phase 2 — Proposal pipeline + the permission gate

Replace `FactionRelationOffer` with `DiplomacyProposal` and turn the hardcoded chains into a dispatch over
`TreatyType`:

- `FactionManager.handleRelationShipOffers()` (`:2134-2236`) — the `if (isEnemy) … else if (isFriend) …` chain
  becomes "look up the type, apply its sign/accept/break rules". War's special case (applied unilaterally,
  never stored as an offer) becomes a `needsAcceptance = false` flag on the `war` type.
- `FactionManager.handleFactionOffersAccept()` (`:2082-2131`) — same.
- `PlayerFactionController.reorganizeFactionRelationShipOffers()` (`:439`) re-buckets in/out lists — retype.

> **Security fix, and the reason this phase exists:** relation offers and accepts are **not permission-checked
> on the server today.** `updateFromNetworkObject` (`:1351-1366`, `:1245-1257`) takes `initiator` straight off
> the wire into the staging queues, and neither drain consults `hasRelationshipPermission`. The only gates are
> client-side (`FactionControlManager.java:480`, `FactionScrollableListNew.java:375`) — a crafted packet can
> declare war on behalf of any faction. `PersonalEnemyMod` handling at `:1706` already does it right; copy
> that shape (`f.isNPC() || state.isAdmin(initiator) || factionPermission.hasRelationshipPermission(f)`) into
> both drains. **The same hole exists for role mods** at `:1947-1975`, which apply `f.getRoles().apply(roles)`
> with no `FACTION_EDIT_PERMISSION` check — fix it in the same pass.

Permissions: reuse `PermType.RELATIONSHIPS_EDIT` (value 1) as the base gate. `FactionPermission.PermType` has
two inactive spare slots — activate `UNDEF5` (512) as `DIPLOMACY_TREATIES` and `UNDEF6` (1024) as
`FEDERATION_VOTE`, so signing a pact can be delegated separately from declaring war. Both are already in
`ADMIN_PERMISSIONS`, so founders keep working with no migration.

---

## Phase 3 — NPC acceptance, config-driven per archetype

NPC archetypes live in `data/npcFactions/<Preset>/npcConfig.xml`, parsed by `NPCFactionConfig` +
`DiplomacyConfig`. Extend, don't replace:

- New `<Acceptance>` block in `DiplomacyConfig`: one `DiplomacyConditionGroup` per treaty type id, parsed with
  the **existing** `DiplomacyCondition` / `DiplomacyConditionGroup` code. Nothing new to write.
- Add the new `ConditionType` and `ConditionReaction` values from §New vocabulary, so acceptance can weigh
  relative strength, existing treaties, shared enemies and distance rather than raw opinion alone. The six
  hardcoded `*_ALLIANCE_*` / `*_PEACE_*` cases in `NPCDiplomacyEntity.executeReaction()` (`:739-905`) are
  deleted in favour of the generic treaty reactions.
- Settle the `POWER` strength formula here and make it a config value — it is the input every acceptance rule
  that isn't pure opinion depends on.
- An **acceptance preview** query (evaluate the group without executing) so the GUI can show Stellaris's
  "they would accept / they refuse, because —".
- Bump `DiplomacyConfig.VERSION` 2 → 3. `NPCFactionConfig.parse` (`:1026-1033`) already wipes reactions and
  calls `addDefaultActions()` on a version mismatch, so existing installs regenerate cleanly.
- Update the three shipped presets (`Outcasts`, `Scavengers`, `Trading Guild`) plus `npcConfigDefault.xml`
  with acceptance rules that give them distinct personalities.

Fix while here: `executeReaction` case `ACCEPT_PEACE_OFFER` returns `true` even when nothing was accepted
(`:778`), and `REJECT_PEACE_OFFER` matches `RType.FRIEND` offers (`:782`) instead of `NEUTRAL`.

---

## Phase 4 — GUI

`FactionPanelNew.recreateTabs()` (`:99-132`) currently gates the DIPLOMACY tab on
`areNPCsEnabled()` (`ServerConfig.NPC_FACTION_SPAWN_LIMIT > 0`) and it shows NPC opinion only. Ungate it and
make it the universal diplomacy screen:

- Left: all factions (player + NPC) — reuse `FactionScrollableListRelation`'s shape.
- Right: opinion breakdown (the existing `GUINPCDiplomacyScrollableList` works unchanged after Phase 0),
  active treaties, and a **Propose** button.
- The propose dialog is driven by `TreatyTypeRegistry`, replacing `FactionRelationEditPanel`'s
  `switch (relation)` over `RType` and its hard-coded pixel offsets. Show the acceptance preview for NPC targets.
- Move "CHANGE RELATION" off the LIST tab (`FactionScrollableListNew.java:354`) to here; keep the row button
  as a shortcut that opens this tab.
- The selected faction's **emblem goes in the top right of the info panel**, and the settings tab gets the
  control for changing your own faction's. Neither exists until §Phase 6; leave the corner clear for it.

> **The acceptance preview needs a wire op.** An NPC faction's preset never leaves the server -
> `NPCFaction.getConfig()` is null on a client - so a client calling `wouldAccept` would silently answer from
> `DefaultDiplomacySettings` and confidently give the wrong answer, which is worse than giving none. Add a
> `QUERY` / `QUERY_RESULT` pair to `DiplomacyProposal.Action`: the server evaluates the rule, replies with the
> verdict and the unmet conditions, and stages nothing. It rides the existing proposal buffer and the existing
> permission gate - if you may not negotiate a treaty you may not find out whether it would have worked - so
> it costs one enum value and no new channel.

**Federations get their own tab, not a sub-panel** (§Phase 5). The diplomacy tab is already carrying four
things at once; a federation is a different subject with its own members, leader, charter and open votes.

All lists follow the one `ScrollableTableList` recipe (ctor registers on `FactionManager` as an `Observer`,
`cleanUp()` deregisters, `initColumns()`, `updateListEntries()`); refresh is the existing
`notifyObservers()` → `flagDirty()` path. Add a `"TREATY_CHANGE"` notify argument alongside the existing
`"RELATIONSHIP_OFFER"`.

Watch two traps: `FactionPanelNew` preserves the selected tab by **matching tab name strings** after a rebuild
(`:100-131`), so renaming a tab silently resets selection; and `GUITextOverlay` elements do not report their
size before init, so lay out with passed-in sizes (see `api.gui.GUILayout`).

`AdminCommands` needs treaty equivalents of `FACTION_MOD_RELATION` (`:831`) and `FACTION_SET_ALL_RELATIONS`
(`:798`) — follow the `AdminCommandGroups` `FACTION` prefix convention so `/faction treaty …` works.
Deferred to §Phase 4.5, with the two legacy commands they have to reconcile with.

---

## Phase 4.5 — Close the open holes

Everything the audit left open, in one place. Three groups: each is either a blocker for §Phase 5's peace
settlements or a hole §Phase 5 widens by stacking new packets on top of it. It is numbered 4.5 rather than
renumbering 5-7 because the cross-references in both documents outnumber the work.

### A. Three paths still trust the client — **done**

The proposal pipeline was fixed in §Phase 2 by reading the sender the network layer stamps on rather than the
initiator the client wrote. Three neighbours never got the same treatment, and they are the reason this group
goes **first**: §Phase 5's federation and vote packets need the same sender-stamped type, so building it once
here is strictly less work than building it there and retrofitting afterwards.

- **`PersonalEnemyMod`, faction kicks and member modifications ride `RemoteStringArray`, which carries no
  sender.** The permission checks they do perform can be passed by naming somebody else. Add the `Remote*`
  type that stamps `updateSenderStateId` - the shape proposals already use - and route all three through it.
- **Rank escalation via the roles blob.** `FACTION_EDIT_PERMISSION` now gates who may rewrite roles, but
  nothing stops a permitted member granting their own rank rights the founder holds. The kick path already
  has the comparison this wants (`targetPermission.role >= kickerPermission.role`); apply it per granted
  right, so a rewrite can never hand a rank something the writer's own rank lacks.
- **Admin relation commands bypass the treaty model.** `FACTION_MOD_RELATION` and `FACTION_SET_ALL_RELATIONS`
  write the legacy relation directly, behind a treaty set that then disagrees with it. §Phase 4 deferred the
  `/faction treaty ...` equivalents until the treaty paths had been exercised by hand; they land here, and
  the two legacy commands route through the same pipeline so an admin-set relation is a treaty like any
  other.

### B. A faction has nothing it can hand over

§Phase 5 wants settlement terms that transfer something. Findings 15-18 say what is actually transferable
today, and the answer is: faction points, treaties, and vassalage. Everything else needs a feature that is
not a diplomacy feature, so the lazy reading of those four findings is mostly **rulings, not code**:

- **Credits stay out of settlements.** `Faction.getMoney()` is the homebase shop's till (finding 15). Giving
  `Faction` a second, real balance would mean two faction moneys and a UI to explain the difference - that is
  faction banking, its own feature, not a peace term. One line of code is still worth taking: the null or
  unloaded homebase case prints a stack trace for a perfectly normal condition. Return 0 quietly.
- **Resources stay out of settlements.** `NPCFactionInventory` is NPC-only (finding 18). Lifting it to
  `Faction` means shared storage with access rules, permissions and a screen - again a feature of its own.
  If faction storage ever ships, `TRANSFER_RESOURCES` is a few lines on top of it.
- **`CEDE_SYSTEM` needs a ceded-to override** (finding 16), and it is the only real code in this group. A
  claim is anchored to a station, so flipping the faction id leaves the loser's station in place to re-claim
  it. Give `StellarSystem` an explicit ceded-to field that outranks the station claim until that station
  leaves the system: you agreed to give it up, so you pull your station out.
- **Fleet leasing is a feature with a UI and no wire** (finding 17). `FleetOptionButtons` builds a
  `FactionLeaseFleetOffer` whose three fields never serialize and which is then added to the local client's
  own staging queue instead of being sent, so the button has never done anything. It is not dead code to
  delete - it is the same idea as a settlement term, badly. Re-express it as a proposal payload (a
  `fleet_lease` treaty, or a term), and the subclass-on-the-wire goes with it.

The ruling this group exists to make: **a settlement term may only move something the server already owns
and persists.** Faction points qualify, treaties qualify, vassalage qualifies, a system qualifies once the
override exists. Nothing else does, and no term may be left as a promise.

### C. Strength has no military term

See §Open: what makes a faction strong in `DIPLOMACY_PLAN.md` for why this is an investigation rather than a
formula. The work, once it is decided:

- A **persisted per-entity combat rating**, recomputed when a ship is edited rather than when it is asked
  about, so the diplomacy tick never loads anything. `FleetMember.lastMass` is the precedent for the shape.
- It must count **ships outside fleets**, or it systematically under-rates player factions - whose main
  combat ship usually belongs to no fleet - while flattering NPC factions, which are entirely fleet-driven.
  A metric that is wrong in a direction is worse than no military term at all.
- Not mass. Mass is trivially inflated by heavy filler; the rating reads what a ship can do (reactor output,
  shields, weapon output, thrust, armour), which `client/view/gui/advancedstats/` already computes per entity
  on the wrong side of the wire.

This group is the only one §Phase 5 does not block on: the settlement *structure* can be built and tested
against player factions before NPCs can price it. Land A and B first; C can run beside §Phase 5 and lands
when it lands.

**Out of scope for this phase:** faction banking, shared faction storage, and any rework of the advanced
stats panel. Each is named above as the reason a ruling went the way it did, not as work this phase takes on.

---

## Phase 5 — Federations, vassalage, war goals

Built on Phases 1–3; no new substrate.

- `Federation` object + persistence slot + `RemoteFederation`/`RemoteFederationBuffer`. Co-membership
  synthesises a `federation` treaty so `getRelation` is unchanged.
- A **FEDERATION tab** on the faction panel: members, leader, charter flags and open votes. Its own tab
  rather than a corner of the diplomacy one - see §Phase 4.
- Federation votes = proposals scoped to a federation id, gated by `FEDERATION_VOTE`.
- `vassalage` / `tributary` exercise the asymmetric path: vassals cannot sign `war` independently; tribute
  transfers on the existing faction-point income tick (`FactionPointIncomeConfig`,
  `IncomeExpensePeriodMinutes` in `FactionConfig.xml`).
- **NPC factions are not vassalizable, and that stays true.** They refuse every `DEMAND`-kind treaty before
  their archetype's rules are consulted. An NPC faction is run by the engine rather than by anyone who could
  be made to answer to a player, so there is nobody there for the arrangement to bind - implementing
  vassalage does not change that. Player-to-player vassalage is the whole of this bullet.
- Casus belli: a `DiplomacyConditionGroup` per war-goal type, evaluated when a war is proposed.
- **Peace settlements carry terms** — see below.
- Defensive pact call-to-arms fires from `Faction.onAttackOnServer` (`:701`), next to the existing auto-war
  logic and its 5 s `sentWarDeclaration` debounce.


### Making peace

A war ends on terms, and today a proposal can only carry a treaty type and a term length. That is enough to
say "stop shooting" and nothing else — no indemnity, no ceded system, no "I will stop if you drop your pact
with them". This is the part of the war model that makes a war worth winning rather than worth abandoning.

**A settlement is a list of terms, not a treaty type.** Add a `SettlementTerm` — `{kind, giver, Tag payload}`
— and a list of them on `DiplomacyProposal`. The original core model above gave proposals a `Tag payload`
and the implementation dropped it because nothing needed one; this is what needs it. Rules:

- **Either side can be charged.** A term names its giver, so "you pay me" and "I will pay you to leave" are
  the same structure. A demand for unconditional surrender and an offer of tribute to go away differ only in
  which id is in `giver`.
- **A settlement applies atomically or not at all**, server-side, at the moment it is signed. Half a peace
  deal is worse than no peace deal, and nothing may be left as a promise the engine then has to police.
- **White peace is a settlement with no terms**, so there is one code path rather than a special case.
- **A war goal is a term declared up front.** Phase 5 already puts a `WarGoal` on the war treaty's payload;
  rather than a second vocabulary, a war goal *is* a `SettlementTerm` the aggressor named when declaring, and
  "surrender" is accepting exactly that list. Casus belli, war goals and peace terms collapse into one type.
- **Anything recurring is a treaty, not a term.** "Pay me 500 a period for ten periods" is a `tributary`
  treaty with an expiry, which already exists and already transfers on the faction-point income tick. Terms
  are for what happens once, at signing.
- **A home system can never be taken.** `CEDE_SYSTEM` must refuse the giver's home system outright - not
  price it high, refuse it - and so must any future term that moves territory. A faction's home is where it
  respawns, where its shop till lives and what its whole point economy is measured from; a settlement that
  takes it does not weaken a faction, it deletes one, and no peace deal should be able to end the game for
  somebody who agreed to it. It is also what makes the naming rule honest: §Phase 6's `{ceded}` token is
  documented as never being a home system, and this is the invariant that has to hold for that to be true.
  Enforce it server-side where the term applies, not in the GUI that offers it.

> Nothing today can reach a system through a treaty - the only path to `setOwnerFaction` is the station
> claim - so there is nothing to guard yet. The invariant is written here because the guard belongs in the
> term, at the moment it is built, and this is the paragraph whoever builds it will be reading.

**Term kinds, in the order they should be built** — the ordering is by what the engine can already do:

| Kind | Mechanism | State |
|---|---|---|
| `SIGN_TREATY` / `BREAK_TREATY` | the phase 2 pipeline | works today; a settlement that includes a NAP is two treaties signed together |
| `VASSALIZE` / `RELEASE_VASSAL` | asymmetric types, already registered | works once phase 5 wires vassalage |
| `TRANSFER_FACTION_POINTS` | `Faction.factionPoints`, a float the server owns and persists (slot 18) | cleanest transferable in the game |
| `TRANSFER_CREDITS` | `Faction.getMoney()` / `setMoney()` | **blocked, see finding 15** |
| `TRANSFER_RESOURCES` | `NPCFactionInventory` | NPC-only; player factions have no faction inventory at all |
| `CEDE_SYSTEM` | `StellarSystem.setOwnerFaction/setOwnerUID/getOwnerPos` | **hard, see finding 16** — do this last |

A `CEDE_SYSTEM` term also fills the `{ceded}` naming token (§Phase 6, Flavour text), which is what turns a
settlement's treaties into "The Tarsis Concession" rather than a second Non-Aggression Pact. A home system
is never cedeable, so that token is never a home system.

**NPC acceptance needs terms to have a price.** `wouldAccept(typeId, unmet)` answers yes or no about one
treaty; a settlement needs to be weighed. Each term kind declares a value, the archetype config declares what
it will pay for peace, and the two are compared — scaled by how the war is actually going, which is the
`RELATIVE_STRENGTH` number phase 3 built. That makes the settlement design dependent on
§Open: what makes a faction strong: a faction losing a war it is winning on paper will haggle wrongly, and
right now the strength number has no military term at all. Build the term structure first and the NPC
valuation after that investigation lands.

**GUI**: the propose dialog from phase 4 grows a terms list — add term, pick kind, pick giver, pick amount —
and the inbox row renders the terms so an offer can be read before it is answered. The acceptance preview
already round-trips through `QUERY`, so previewing a settlement is the same call with a longer payload.

<!-- ponytail: terms are a list on the proposal and nothing else. Do not build an escrow, a staged-payment
     system, or a treaty-enforcement tick — everything applies at signing, and anything that genuinely
     recurs is a tributary treaty, which already exists. -->

**New tuning config:** put diplomacy constants in a **new `DiplomacyConfig.xml`**, not in
`FactionConfig.xml`. `FactionConfig`'s reflective parser *throws* when an `@ConfigurationElement` field has
no matching tag (`FactionConfig.java:236-238`), so every added field breaks every existing
`customFactionConfig/FactionConfig.xml` — and that file is checksummed and shipped to clients
(`GameServerState.setFactionConfigCheckSum`). A separate file costs one loader and breaks nothing.

---

## Phase 6 — Governance, voting and identity (lore/RP layer)

None of this changes combat or economy — it exists so a faction reads as a polity rather than a permission
bitmask, and so treaties have a story attached. Build it on the Phase 2 proposal pipeline; no new substrate.

### Governance

A `GovernmentType` registry object (same shape as `TreatyType`, so it gets mod registration for free):
id, display name, description, a leader title and a member-rank title set (Empire → Emperor/Baron,
Republic → Consul/Senator, Syndicate → Boss/Made, …). Mostly flavour, with two mechanical hooks worth having:

- **Succession** — what happens when the founder goes inactive. `FactionPermission.lastSeenTime` and
  `isOverInactiveLimit()` already exist and `FactionActivityConfig.SetInactiveAfterHours` already ships;
  today an abandoned faction just rots. Options: none (current behaviour), most-senior member, or a vote.
- **Charter flags** — a bitmask of self-imposed rules the faction advertises and the UI enforces for
  flavour: "treaties require a vote", "war requires a vote", "open borders", "no first strike".

Rank titles flow into every place that renders a role name today (`FactionRoles`, the member list,
`FactionRoleSettingPanel`), so this is one accessor change plus defaults, not a UI rewrite.

### Voting

`FactionVote {int id; int scope(FACTION|FEDERATION); String kind; Tag payload; long opened; long closes;
Int2ByteMap ballots; byte rule}` — one object, used for both faction-internal and federation votes. Tally
rules: simple majority, supermajority, leader-veto, weighted by rank.

> The earlier decision stands: **role permissions are the default gate for negotiation, not voting.** A vote
> is an opt-in charter flag a faction switches on for itself. Never make a player faction wait on a quorum it
> did not ask for.

Federation votes (admission, expulsion, declaring war, leader rotation, charter changes) reuse the same
object with `scope = FEDERATION`, gated by the `FEDERATION_VOTE` permission activated in Phase 2.

Sync follows the established `Remote<X>` + `Remote<X>Buffer` pair; persistence is a new trailing slot, and
open votes belong in the DIPLOMACY tab's federation sub-panel from Phase 4.

### Emblems

Faction identity today is exactly one field — `Vector3f color`, edited through `FactionColorPickerDialog`,
pushed as a comma-joined string under `NT_CODE_COLOR`. There is no emblem, icon, banner or insignia support
anywhere in the tree.

Ship an **emblem atlas**, not image upload: a `FactionEmblems-8x8-gui-.png` sheet under
`data/image-resource/` (the `-8x8-` in the filename is the existing parsed grid convention), and one
`short emblemIndex` on `Faction`, tinted by the existing colour. That is one field, one `NT_CODE_EMB` string
alongside `NT_CODE_COLOR`, and zero new sync machinery — the emblem rides the `factionMod` buffer and the
`RemoteFaction` blob that already carry the colour.

Player-uploaded images are **in scope, behind a server opt-in that ships off**. The atlas is what a
faction gets by default and on any server that never touches the setting; uploads are a thing a server owner
turns on for their community, having decided they are willing to moderate it.

- `ServerConfig.FACTION_CUSTOM_EMBLEMS_ENABLED`, default **false**. With it off the upload control does not
  exist in the GUI *and* the server rejects an upload packet outright. The client gate is not the gate — the
  same lesson as the relation offers in Phase 2.
- Transfer rides the existing `ClientFileDownloadController` path rather than a new one. Hard byte cap, PNG
  only, fixed dimensions; the server **decodes and re-encodes** the image rather than storing what arrived,
  so a malformed or hostile file never reaches another client verbatim.
- Stored server-side per faction and fetched on demand, keyed by content hash and cached per client. The
  image does not go in the faction sync blob — `Faction` keeps the atlas `emblemIndex` plus a nullable
  custom-emblem hash, and the atlas index stays as the fallback a faction reverts to.
- **Admin moderation is part of the feature, not a follow-up.** Right-clicking a faction in the faction list
  or the diplomacy panel gives an admin *Remove emblem*: it reverts that faction to its atlas fallback,
  purges the stored image and the cached copies, records who removed it in the faction log, and tells the
  faction it happened. An `/faction emblem clear <faction>` admin command does the same from a console, so a
  headless server is not stuck. Federations get the identical control.
- A removal is recorded rather than merely applied, so a faction that keeps re-uploading is visible to the
  existing admin tooling instead of being an invisible game of whack-a-mole.

Shipping the atlas first is still right: it makes the feature work with no moderation burden at all, and it
is what every faction falls back to when an upload is removed or the setting is switched off.

<!-- ponytail: the atlas is the whole feature; the upload path is an opt-in extra. Build the atlas, the
     server flag and the admin removal together — an upload path without the removal is the part that
     becomes somebody's weekend. -->

Emblems render wherever faction colour already does: `GUIFactionListElement`, the member and faction lists,
`HudIndicatorOverlay`, `GameMapDrawer`, and the new diplomacy panel. Two placements are settled:

- **Diplomacy tab, top right of the info panel** - beside the selected faction's name, relation and opinion.
  §Phase 4 leaves that corner clear for it.
- **Settings tab**, for your own faction, next to the colour picker: this is where you change it, and where
  the upload control appears when the server has opted in.

### Flavour text

Federations get a name, emblem and charter description; treaties get an optional free-text clause the
proposer writes, carried in the `DiplomacyProposal.message` field that already exists and persisted onto the
signed `FactionTreaty`. Costs nothing and is most of what RP servers actually want.

**Treaty names shipped early** (they were cheap and they are what a treaty list actually reads as). Two
decisions worth keeping:

- **A name is agreed at signing, not set afterwards.** It rides the proposal, so the other side reads it in
  the inbox before agreeing; renaming is re-offering the same type on the same term under a new name, which
  the other side accepts. Signing a type already in force replaces it, so that needed no new protocol at
  all - and a name both signatories see is only worth having if neither can change it alone.
- **The default is generated from what the agreement actually involved.** Each `TreatyType` carries a few
  templates; `TreatyNames` fills `{from}`, `{to}`, `{fromHome}`, `{toHome}`, `{ceded}` and `{type}`. A
  template whose required token has nothing behind it is **skipped rather than filled** - which is the whole
  reason `{ceded}` is not `{toHome}`: a faction never concedes its capital, so "The Berenike Concession"
  naming the loser's home system would be a lie on every row. Home tokens are softer and fall back to the
  faction's own name.

§Phase 5 is what makes `{ceded}` mean anything: a `CEDE_SYSTEM` settlement term names the system, and the
treaties that settlement signs get named after it.

---

## Phase 7 — Modding API

Follows the `nf/modding-api-improvements` conventions exactly: one interface per event with a
`static EventType TYPE`, fired by an inline loop that merges `EventResult`.

New listeners in `src/main/java/api/event/faction/`: `TreatySignListener`, `TreatyBreakListener`,
`DiplomacyProposalListener` (pre-send, cancellable), `DiplomacyProposalRespondListener`, `WarDeclareListener`,
`FederationCreateListener`, `FederationMemberChangeListener`, `OpinionModifierListener`,
`VoteOpenListener`, `VoteCastListener`, `VoteResolveListener`, `FactionGovernmentChangeListener`.

**Wire `NPCExecuteDiplomacyReactionListener`** — it is declared but has no fire site. Per the standing rule,
an unfired event is a missing feature, not dead code; `executeReaction()` is its fire site.

New `src/main/java/api/faction/DiplomacyAPI.java`, mirroring `api/world/PlanetTypeAPI.java` one-for-one:
`register(StarMod, String xml)`, `registerFromJar`, `unregister`, `registered()`, `getById`, `qualify`,
`dump` — covering treaty types and government types. Same shape for `NPCPresetAPI`: NPC archetypes are
currently **directory-scanned only** (`NPCFactionConfig.java:615-620`), so mods have no way to add one, and
the engine-never-scans-jars rule says the mod must push. Emblems go through the existing texture/resource
registration path rather than a new API.

**Teardown is mandatory:** add `TreatyTypeRegistry.unregisterAll()` and the government-type and preset
equivalents to `ModStarter.resetModState()`, alongside `PlanetTypeConfig.unregisterAll()` /
`RecipeAPI.unregisterAll()`. Without it, relogging stacks duplicate types.

Docs: run `gradlew eventCatalogue` after adding the listeners (it asserts ≥150 and regenerates
`docs/source/developer-reference/event-catalogue.md`), and add a `docs/source/core-concepts/` page for treaty
XML next to `planet-types.md`.

---

## Verification

Build (`gradlew` fails while the game is running — close the client first):

```bash
./gradlew quickCompile
```

`quickCompile` is Java-only and deploys straight to the game folder. Phases 3, 5, 6 and 7 add or change XML
under `src/main/resources/data/`, and Phase 6 adds the emblem atlas PNG — `quickCompile` copies **no**
resources, so those need a full `./gradlew build` or they silently will not reach the game.

Self-checks — add `TreatyTypeRegistry_test` and `FactionTreaty_test` (tag round-trip, asymmetric
from/to survives, unknown type id skipped cleanly) beside their subjects. The string-key conversion needs one
of its own: assert that an action/status tag written with one enum ordering reads back correctly after the
enum is reordered — that is the whole point of the conversion, and nothing else catches a regression on it.
Then:

```bash
./gradlew apiSelfChecks
```

End-to-end, in one client + one dedicated server:

1. **Two player factions.** Create A and B, propose a non-aggression pact from A, accept as B. Confirm the
   treaty appears on both clients' DIPLOMACY tab, that A's "declare war" is refused while it holds, and that
   it survives a server restart.
2. **Permission gate.** As a member whose role lacks `RELATIONSHIPS_EDIT`, confirm the propose button is
   hidden *and* that a replayed proposal packet is rejected server-side — the client gate is not the fix.
2b. **Wire trust.** Replay a captured personal-enemy, kick and role-change packet naming another member
   as the initiator; confirm each is refused because the sender stamp disagrees with the name. Then, as a
   member holding `FACTION_EDIT_PERMISSION`, attempt to grant your own rank a right your rank does not hold
   and confirm the rewrite is refused rather than partially applied.
2c. **Admin treaty commands.** `/faction treaty …` signs and breaks a treaty that reads identically to a
   negotiated one on both clients. Confirm `FACTION_MOD_RELATION` no longer leaves the legacy relation and
   the treaty set disagreeing.
3. **Asymmetry.** Sign `vassalage` A→B; confirm the record keeps its direction through save/load and that the
   vassal cannot declare war independently.
3b. **Peace terms.** Declare war A→B with a war goal, then settle it with a multi-term offer (faction points
   one way, a non-aggression pact both ways). Confirm the terms are visible in the inbox before answering,
   that accepting applies every term or none, that a settlement whose giver cannot pay is refused rather
   than partially applied, and that white peace still works as a settlement with no terms at all.
4. **NPC acceptance.** Against each of the three shipped presets, propose a treaty below and above its
   configured threshold; confirm the preview matches the outcome and that the three presets differ. Raise
   opinion via `/faction …` admin commands rather than waiting on the 60 s turn timers.
4b. **Strength.** With the combat rating in, confirm a faction whose ships are docked and unloaded scores
   the same as when they were loaded, that a hull padded with filler blocks does not out-score a smaller
   armed ship, and that a player faction whose main combat ship is in no fleet is not rated as unarmed.
5. **Derived relation, no regressions.** With an alliance active, confirm turrets hold fire
   (`SeachingForTurretTarget`), the HUD shows friendly colours, shop `TradePerm.ALLY` opens, and the AI
   disengages mid-combat when a peace treaty is signed (`ShipGameState.isHostileCombatTarget` re-checks each
   tick).
6. **Fresh start.** Load a pre-change world: existing ENEMY/FRIEND rows still resolve, no treaties, no crash,
   no dropped factions.
7. **No proximity penalty.** Found two factions in adjacent systems, leave them alone, and confirm opinion
   does not drift down on its own. Then fly a ship into a claimed system and confirm `TERRITORY` fires once
   and respects its 5-minute cooldown.
8. **Governance and voting.** Set a charter flag requiring a vote for treaties, confirm a proposal opens a
   vote instead of sending, that it tallies and resolves, and that a faction *without* the flag still signs
   immediately on role permission alone — the vote must stay opt-in.
9. **Emblems.** Pick an emblem from the atlas, confirm it reaches other clients and renders in the faction
   list, HUD indicator and map, survives a restart, and that an out-of-range index falls back rather than
   crashing the atlas lookup. Then with `FACTION_CUSTOM_EMBLEMS_ENABLED` **off**, confirm the upload control
   is absent *and* that a replayed upload packet is refused server-side. Turn it on, upload an image, confirm
   it reaches a second client and survives a restart; then as an admin right-click the faction and remove it,
   and confirm the faction reverts to its atlas emblem on both clients, the stored file is gone, and the
   removal is in the faction log.
10. **Mod teardown.** With the `ModTemplate` mod registering a custom treaty type and a custom government
    type, disable and re-enable it and confirm `registered()` does not grow.

## Out of scope

Research/migration treaty types (no matching game system), espionage, and any rework of the dead legacy GUI
island rooted at
`client/view/gui/faction/FactionPanel.java` (zero inbound references) — leave it alone rather than porting it.

Adding further action/status/condition/reaction values beyond §New vocabulary is explicitly *in* scope as
needs surface — once the tags are string-keyed the enums are safe to extend, and each one is a few lines plus
a config default.
