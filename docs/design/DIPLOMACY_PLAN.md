# Faction Diplomacy — Audit, Status and Plan

> Written while this lived in the engine on `experimental/faction-diplomacy-rework`. It now lives in this mod;
> engine file and line references below describe that branch, not the mod. The mod reaches the engine only
> through the hooks on `nf/faction-mod-hooks` (relation query, faction delete, diplomacy action, faction
> panel, client disconnect, and the UNDEF5-7 permission bits).
>
> **History, not reference.** For how the mod works now and the current roadmap, see `../DEVELOPER.md`. Where
> this disagrees with it (emblems shipped as uploads with no atlas, config lives in `moddata/BetterFactions/`), the
> developer guide is right.


Audited 2026-09-14 on `experimental/faction-diplomacy-rework`, branched off the modding-API work.
Phases 0-3 are implemented; findings are marked with their status as they are dealt with.

The goal is a Stellaris-like diplomacy layer — treaties, federations, vassalage, casus belli — that applies
uniformly to player factions and NPC factions, driven by config and extensible by mods.

---

## Current state

**There are two disconnected halves of a diplomacy system, and only one of them works.**
This is the single most important fact about this area and it is not obvious from either package.

|            | `faction` (relations)                        | `npc.diplomacy` (opinion)                                         |
|------------|----------------------------------------------|-------------------------------------------------------------------|
| Location   | `org.schema.game.common.data.player.faction` | `org.schema.game.server.data.simulation.npc.diplomacy`            |
| Applies to | every faction                                | **NPC factions only**                                             |
| State      | one `byte` per unordered faction pair        | signed point score per (NPC faction → faction *or* player)        |
| Authoring  | hardcoded                                    | per-preset XML with an AND/OR/NOT condition grammar               |
| Runs on    | client and server (client mirrors)           | server only                                                       |
| Vocabulary | `RType { NEUTRAL, ENEMY, FRIEND }`           | 11 action types, 12 status types, 8 condition types, 10 reactions |
| Decay      | none                                         | per-turn ramp up/down, static-modifier timeout, action timeout    |

### The NPC half is most of an opinion model already

`NPCDiplomacyEntity` holds `points` (accumulated from per-turn modifiers) plus `pointCached`
(`points` + the sum of all static modifiers, clamped to the configured range). Two modifier kinds:

- **`NPCDiplStaticModifier`** — a flat offset while a *status* holds. Statuses are **derived, not stored**:
  `calculateStatus()` recomputes each one from current relations every 60 s.
- **`NPCDiplTurnModifier`** — a per-turn drip. While an action keeps repeating the value ramps toward a
  configured limit; when the action lapses it walks back toward zero and is removed at the origin.

Four `TimedExecution`s drive it from `NPCFactionManager.updateLocal`, which ticks every ~3 s.
`DiplomacyConfig` supplies the point tables and a list of `DiplomacyReaction`s, each guarded by a
`DiplomacyConditionGroup` tree parsed from `data/npcFactions/<Preset>/npcConfig.xml`.

That is the substrate a Stellaris-style system needs. It is just welded to `NPCFaction`.

### The relation half is one byte

```java
// FactionRelation.java:20-22
public int a;
public int b;
public byte rel;   // RType{ NEUTRAL(0), ENEMY(1), FRIEND(2) }
```

Stored in `FactionManager.relations`, keyed by `FactionRelation.getCode(a,b)` = `min*MAX_VALUE + max` —
**order-independent**, so a relation cannot be asymmetric. Absence means NEUTRAL, and neutral rows are
deliberately not sent on full sync to save bandwidth.

Consequences:

- **No treaty object exists.** "Peace" is the absence of ENEMY. "Non-aggression" is a derived NPC status
  meaning "nobody has shot at me lately", not a pact. Trade agreements do not exist at all.
- **Relations cannot coexist.** A pair is neutral, enemy or friend — never "non-aggression pact plus trade
  agreement".
- **Relations cannot be asymmetric**, so vassalage and tributaries have nowhere to live.
- Proposals ride `FactionRelationOffer extends FactionRelation`, adding only `message`, `initiator` and
  `revoke`. The server pipeline that applies them is a hardcoded
  `if (isEnemy) ... else if (isFriend) ... else if (isNeutral)` chain in `handleRelationShipOffers()`.

### Everything funnels through one method

`FactionManager.getRelation(int a, int b)` is the single source of truth, and it already derives rather than
just looking up — it special-cases faction 0 via `isAttackNeutral`/`isAllyNeutral`, forces NEUTRAL for
`MODE_SPECTATORS` and ENEMY for `MODE_FIGHTERS_FFA`, before falling through to the table.

About 19 files consume it (turret target selection, ship AI engage/disengage, beams, mines, shop
`TradePerm`, trade nodes, fleet missions, HUD colours, map drawing, scanner sharing). **That makes it the
natural insertion point**: if treaties become the real state and `getRelation` collapses them into the same
three legacy values, none of those consumers change.

---

## Findings

### 1. Nothing on the faction wire was permission-checked on the server — *fixed, phase 2*

Serious enough that it has its own section: see [Security](#security).

### 2. Every diplomacy tag is keyed by `enum.ordinal()` — *fixed, phase 1*

`DiplomacyAction`, both `NPCDipl*Modifier` classes and `NPCDiplomacyEntity` all persist
`(byte) type.ordinal()` and read back `values()[b]`. Each writes a version byte that is read and
immediately discarded.

So reordering or inserting into `DiplActionType` / `DiplStatusType` silently corrupts existing saves — new
values may only be appended. Since the whole point of this work is to grow that vocabulary, the tags must
move to string keys first.

The action map compounds this: it is a `Byte2ObjectOpenHashMap` keyed on the ordinal, so a generic
`TREATY_BROKEN` action could not distinguish which treaty broke. String keys fix both problems at once.

### 3. Statuses that are configured but unreachable — *fixed, phases 0 and 3*

- `DiplStatusType.FACTION_MEMBER_AT_WAR_WITH_US` and `FACTION_MEMBER_WE_DONT_LIKE` have configured point
  values (-500 / -50) but **no case in `calculateStatus`** — they fall through to `return 0`.
- `POWER` has a case that returns 0. Relative strength is exactly what config-driven treaty acceptance
  needs, so this has to be implemented rather than removed.
- `PIRATE` returns 1 but is skipped in the default config.

### 4. Persisted-time fields are not persisted — *fixed, phase 0*

`NPCDiplStaticModifier.totalTimeApplied` and `NPCDiplTurnModifier.elapsedTime` are absent from their tags,
so every `STATUS_PERSISTED` / `ACTION_PERSISTED` condition resets to zero on server restart. The default
config leans on these ("has been peaceful for 15 minutes" gates a peace offer, 120 minutes gates an
alliance), so restarts quietly reset NPC diplomatic progress.

### 5. `TERRITORY` measures proximity, not intrusion — *fixed, phase 3*

Despite its label ("Violated their territory") the `TERRITORY` action fires from the expansion scan finding
an *adjacent* foreign system, and feeds the `CLOSE_TERRITORY` status at -100.

Meanwhile `FactionManager.checkTerritoryIntrusion()` detects an actual ship entering a claimed system,
already carries a five-minute per-owner/system/intruder cooldown, and feeds **no** diplomacy action at all —
it only posts a news item.

Proximity-based opinion is also a poor fit for StarMade specifically: its galaxies are far smaller than
Stellaris's, so nearly every faction would sit permanently inside someone's penalty radius and the hit would
read as the map punishing you rather than as anything you did.

### 6. The relation-change listener can be cancelled after the fact — *fixed, phase 1*

In the `relationsToAdd` drain, `relations.put(fk.getCode(), fk)` executes **before** the
`isCanceled()` check. A cancelling `FactionRelationChangeListener` therefore still mutates the map; it only
suppresses the rule trigger and the network broadcast.

### 7. The predicate family disagrees with `getRelation` — *fixed, phase 1*

`isEnemy(int,int)` and `isNeutral(int,int)` ignore `MODE_SPECTATORS`, `MODE_FIGHTERS_FFA` and
`allyNeutral`, while `getRelation(int,int)` honours all three. The two can return contradictory answers for
the same pair.

### 8. Reaction bugs — *fixed, phase 2*

`executeReaction` case `ACCEPT_PEACE_OFFER` returns `true` even when nothing was accepted, and
`REJECT_PEACE_OFFER` matches `RType.FRIEND` offers rather than `NEUTRAL`.

### 9. There is no faction identity beyond a colour

`Faction` carries exactly one presentation field, `Vector3f color`, pushed to clients as a comma-joined
string under `NT_CODE_COLOR`. There is no emblem, icon, banner or insignia support anywhere in the tree,
and no government/charter concept — a faction is a permission bitmask with a name.

### 10. `<DiplStatus>` config entries were never read — *fixed, phase 3*

`DiplomacyConfig.parse` tested the wrong node:

```java
if(item.getNodeName()...

equals("diplaction") ||
        node.

getNodeName()...

equals("diplstatus")){   // `node` is the enclosing DiplomacyElement
```

`node` is the parent, so the second arm was never true and **every `<DiplStatus>` entry in every config
has been silently discarded**, leaving the hardcoded constructor defaults in force. Server owners have been
tuning status point values that never did anything. Three shipped presets set `IN_WAR` to -16000; the game
has always used -10000.

### 11. Config parse failures were reported as the wrong thing — *fixed, phase 3*

Three separate cases, each of which made a recoverable situation look like a crash or, worse, reached the
player:

- An out-of-date `Diplomacy` block set `found = false`, which is the reflective parser's signal for *no such
  field*. It printed a `ConfigParserException` stack trace and announced the tag as missing, for a file that
  had it all along. (The rewrite it triggered was the desired behaviour and is kept.)
- An unknown `DiplActionType` / `DiplStatusType` name reached `Enum.valueOf`, whose `IllegalArgumentException`
  escaped and took the whole preset with it. Retiring any vocabulary value would have broken every config
  written while it existed — which is exactly what removing `CLOSE_TERRITORY` does.
- An unknown reaction name was turned into a `SEND_POPUP_MESSAGE` reading `UNKNOWN REACTION TYPE IN CONFIG:
  X`, i.e. the server's config error was shown to players as an in-game diplomatic message.

### 12. NPC offer lookups used the wrong key — *fixed, phase 2*

`NPCDiplomacyEntity` looked its own standing offers up with `FactionRelation.getCode(a, b)`, the *order-independent*
relation key, while the offer map is keyed by the *directed* `getOfferCode(a, b)`. The
lookup could only ever miss, so the "withdraw our offer because they attacked us" path never ran.

### 13. Only NPC factions recorded diplomacy actions — *fixed, phase 3*

`FactionManager.diplomacyAction` routed to `NPCFaction` only. Phase 0 gave every faction a ledger, but
everything except `Faction.checkActions()` still wrote to NPC ones exclusively, so player-faction ledgers
stayed almost empty.

### 14. NPC archetypes cannot be added by mods

`NPCFactionConfig` scans `./data/npcFactions/` for directories containing `npcConfig.xml`. There is no
registration entry point, so a mod cannot contribute an NPC faction archetype — which contradicts the
established rule elsewhere in the API that the mod pushes and the engine never scans jars.

### 15. `Faction.getMoney()` is the homebase shop's till, not a faction balance

`getShop()` resolves `getHomebaseUID()` through `getSegmentControllersByName()`, so a faction with no homebase — or one
whose
homebase is not currently loaded — gets 0 and a printed stack trace, and `setMoney` silently does nothing.
Any peace term that transfers credits on this would appear to work and move nothing, which is worse than
refusing. Either give `Faction` a real persisted balance the way `factionPoints` already is, or leave credits
out of settlements; `factionPoints` is the transferable that needs no new bookkeeping. Phase 4.5 rules the
second way: a second balance means two faction moneys and a screen to explain the difference, which is
faction banking rather than a peace term.

### 16. System ownership is anchored to a claiming station, not held as a faction id

A claim writes `setOwnerUID(baseUID)`, `getOwnerPos().set(...)` and `setOwnerFaction(...)` together, from a faction
block on
a station in that system. So "cede this system" cannot just flip the faction id: the loser's station is still
sitting there and re-claims it. Two ways out — transfer the station itself with the system (large, and hands
over whatever is built on it), or give the system an explicit ceded-to override that outranks the station
claim until that station is removed. The second is smaller and reads correctly: you agreed to give the system
up, so you pull your station out. Either way, territory is the last term kind to build, not the first.

### 17. `FactionLeaseFleetOffer` is a fleet-leasing feature with a UI and no wire

It is broken in two independent ways. It extends `DiplomacyProposal` with `price`, `duration` and
`fleetId`, but inherits
`DiplomacyProposal.serialize`, so those three fields never cross the wire; and `FleetOptionButtons` adds the
offer straight into the local client's `getRelationOffersToAdd()` staging queue instead of sending it, so it
never reaches the server either. The button has therefore never done anything. It is not dead code but a
missing feature, and it is the same idea as a settlement term done badly: terms belong in a serialized
payload on the proposal, not in a subclass the wire does not know about. Phase 4.5 re-expresses it as one.

### 18. Player factions have no shared inventory

`NPCFactionInventory` exists and is NPC-only. A resource term in a settlement therefore has nowhere to take resources
from or put them for a player faction, which is
why the term table orders resources behind faction points.

---

## Security

Faction state is client-driven: the client sends what it wants done and the server applies it. Almost none
of that was checked. This section is the running record — what was wrong, what was done, and what is still
open.

### Fixed

**Nothing on the faction wire was permission-checked.** `updateFromNetworkObject` staged relation offers,
accepts and role blobs from the wire, and no drain consulted `hasRelationshipPermission`. A crafted packet
could declare war, break an alliance, or accept one on behalf of any faction in the game. The role blob was
worse: `f.getRoles().apply(roles)` took a faction id and a permission bitmask and applied both verbatim, so
any client could grant itself every permission in a faction it had never joined.

**The initiator name was a claim, not an identity.** This is why the obvious fix would not have worked.
`PersonalEnemyMod` does check permissions — against `f.getMembersUID().get(m.initiator)`, where `initiator`
is a string the sending client chose. Copying that shape into the offer drains would have produced a check
anyone could pass by typing a different name. Proposals moved onto a `Remote*` type whose `deserialize`
stamps the network layer's `updateSenderStateId`, the pattern `FactionSystemRename` already used; the server
resolves that to a client and overwrites the claimed name before any check reads it. Role blobs already
carried a stamped `senderId` that nothing had ever looked at.

**The engine needs a bypass; the wire must not be able to claim it.** Auto-declared wars and NPC reactions
are proposals the engine builds for itself, with no member to check them against. They carry a
`serverAuthored` flag that no `deserialize` sets — a self-check pins that a round trip through the wire
cannot turn it on.

**NPC factions get no permission bypass.** The existing `f.isNPC() || ...` shape would, with identity now
resolved, have let any player set an NPC faction's foreign policy by addressing a proposal as that faction.
An NPC's own decisions are server-authored and never reach the check, so the bypass was dropped rather than
copied.

**A refused answer consumed the offer.** The accept drain removed the proposal from the map before doing
anything else, so a player without permission could still destroy a pending offer by answering it. The
lookup, the check and the removal are now in that order.

**A client could have forged treaties.** Found and fixed within phase 1 rather than shipped:
`updateFromNetworkObject` runs on both client and server, so the new treaty receive loop would have let a
client push a treaty at the server and have it applied verbatim. Treaties are server-authored, so inbound
changes are client-side only.

**A counter-offer is two things at once.** It settles the proposal it answers and makes one of its own, so
it is checked against both the terms it turns down and the terms it asks for.

**The acceptance query asks nothing it may not ask.** Phase 4's "would they accept?" preview is a proposal
with an action of its own, so it arrives on the same buffer and passes the same `mayNegotiate` check before
it is answered: a client cannot probe an NPC's acceptance rules for a treaty it has no standing to offer.
The answer is broadcast like every other proposal, which makes what an NPC would agree to visible to
everyone - a disclosure, not an authority, and the same broadcast proposals have always used.

**Every faction request now names its sender, not itself.** `PersonalEnemyMod`, faction kicks, member
modifications and homebase changes rode `RemoteStringArray`, which carried no sender, so the permission
checks they did perform could be passed by naming somebody else - each of them looks the initiator up in
the faction's member list, which makes a forged name a forged permission. `RemoteArray` keeps the
`updateSenderStateId` the network layer already handed it, and on the server the initiator is resolved from
that rather than read out of the payload. A request whose sender cannot be resolved is dropped rather than
attributed to anyone. Two neighbouring holes went with it: `factionMod` is a server broadcast that the
server no longer accepts inbound at all, and a member modification naming an initiator who is not a member
is refused instead of falling through every check to the apply.

**A roles rewrite is clamped to what the editor holds.** `FACTION_EDIT_PERMISSION` gates who may rewrite
roles; `applyLimited` bounds them once they are in. A rank above the editor's own is untouchable, and no
rank may be granted a right the editor's own rank lacks - so the one right such an officer holds is no
longer enough to write every other right onto their own rank, or to disarm the founder. Only the rights in
`ADMIN_PERMISSIONS` are clamped: a bit outside it is a faction policy toggle rather than a capability.

**Admin treaty commands exist, and go through the pipeline.** `/faction_treaty_sign`, `/faction_treaty_break` and
`/faction_treaty_list` end at the same apply a negotiated proposal does, so an admin-made agreement is a treaty like
any other rather than a relation written behind the model's back.

### Still open

- **Nothing from the original audit.** Group A of phase 4.5 closed the three items that stood here; what
  remains open is scheduled work rather than an unaddressed hole - see groups B and C of phase 4.5, and
  §Open: what makes a faction strong.

---

## Open: what makes a faction strong

*Scheduled as group C of phase 4.5.*

Phase 3 needed a strength number for `RELATIVE_STRENGTH` and `POWER`, and built one from what was already
cached:

```java
strength(f) = max(0, pointSurplusPerPeriod) * surplusWeight
            + claimedSystems                * systemWeight
            + activeMemberStrength          * memberWeight
```

Three quantities that do not overlap: whether a faction can afford what it holds, how much it holds, and how
many people it can actually field. Weights are per archetype. The comparison is
`clamp(log2(theirs/ours), ±6)`.

The first attempt used the faction-point **balance** and a **flat member count**, and both were wrong in
ways that cancelled out:

- Territory *drains* the point balance (per-system base rate, distance to galaxy centre, distance from
  homebase) while counting positively in the formula, so the two terms fought. A sprawling empire running
  near zero points scored lower on the points term than a two-system faction that had been hoarding.
- The balance grows without bound for as long as a faction stays solvent, so on a long-lived server the
  metric quietly became *how long ago somebody was founded*.
- A flat member count is the easiest number in the game to inflate: forty alts read as an empire. The income
  tick already distinguishes active from dormant members; the strength formula did not.

**None of it is military.** Two factions with identical surplus, territory and membership rate as equals
whether one fields capital ships or three mining drones — for a number whose entire job is "can they take
us", that is the hole in the middle.

### Why this is its own investigation

The obvious input is **mass**, and mass is the wrong answer. Players do reach for it when comparing ships,
but it is trivially inflated: a hull packed with heavy filler blocks outweighs a smaller, better-armed one
without being more dangerous. Anything built on mass alone rewards the wrong building.

A real metric has to read what a ship can *do*, and the vocabulary for that already exists per entity —
reactor output and chamber configuration, shield capacity and regeneration, weapon output, thrust, armour.
The advanced structure stats panel (`client/view/gui/advancedstats/`) computes most of it, but it computes
it **client-side for one loaded entity**, which is the wrong shape for a server-side question about a whole
faction. Beyond single ships, a fair account also has to include fleet count and composition, deployed
stations and their defences, and shipyard capacity.

Two constraints make this harder than it looks and are the reason it is not folded into phase 3:

- **It must not require loading anything.** Faction strength is asked on the diplomacy tick, for factions
  whose sectors are nowhere near a player. `Fleet.getMass()` works unloaded because `FleetMember.lastMass`
  is cached; any richer metric needs the same treatment — a persisted per-entity rating, recomputed when a
  ship is edited rather than when it is asked about.
- **Fleets undercount players and flatter NPCs.** NPC factions are entirely fleet-driven, while a player's
  main combat ship usually belongs to no fleet at all. A fleet-based metric would systematically rate player
  factions weaker than they are, which is worse than having no military term.

Until that exists, strength deliberately measures presence and territory rather than force, and the shipped
acceptance rules lean on opinion and shared enemies rather than on `RELATIVE_STRENGTH`. The one preset that
uses it (Scavengers attaching themselves to whoever is stronger) reads as a crude judgement because it is
one.

---

## Roadmap

The full phase breakdown, decisions and verification steps live in the approved plan. Summary:

| Phase | Work                                                                                                     | Status      |
|-------|----------------------------------------------------------------------------------------------------------|-------------|
| 0     | Lift the opinion engine out of `npc.diplomacy` onto `Faction`; every faction gets a ledger               | done        |
| 1     | Treaty model, string-keyed tags, storage, sync, derived `RType`                                          | done        |
| 2     | Generalise the proposal pipeline; **fix the server-side permission hole**                                | done        |
| 3     | NPC acceptance rules, config-driven per archetype; implement `POWER`                                     | done        |
| 4     | Universal diplomacy GUI: ungated tab, treaty list, propose dialog, acceptance preview                    | done        |
| 4.5   | Close the open holes: wire trust (**A, done**), what a faction can transfer (B), the strength metric (C) | in progress |
| 5     | Federations (own tab, **done**), vassalage, tributaries, war goals                                       | in progress |
| 6     | Governance, voting, emblems — the lore/RP layer                                                          |             |
| 7     | Modding API: events, treaty/government/preset registration, docs                                         |             |

### What phase 3 settled

- **Acceptance is a config block, not Java.** `<Acceptance>` holds one condition group per treaty type id,
  reusing the existing `DiplomacyCondition` grammar. A type with no rule is refused, so an archetype opts in
  to each kind of agreement. It is evaluated the moment a proposal arrives rather than on the diplomacy
  turn, which is both what a player expects and what lets the same query answer the GUI's "would they
  accept?" preview in phase 4. The four hardcoded `ACCEPT_*`/`REJECT_*` reactions are gone.
- **The reaction vocabulary shrank from ten to five.** Offer, remove and accept/reject existed separately
  for alliances and for peace; they are now `PROPOSE_TREATY` / `WITHDRAW_PROPOSAL` / `BREAK_TREATY`, each
  naming a treaty type in a `<Treaty>` element, plus `SEND_POPUP_MESSAGE` and `SEND_FACTION_MAIL`.
- **`POWER` compares strength as a ratio, in doublings.** `log2(theirs/ours)`, clamped, because the question
  is "can they take us" rather than "how many more systems". The weights that make up a strength total are
  per archetype, not global: what counts as formidable is a judgement, and a raider counting crew while a
  merchant counts territory is most of what makes them feel different. `RELATIVE_STRENGTH` reads the same
  number for acceptance rules. What goes *into* the total is unfinished — see
  [Open: what makes a faction strong](#open-what-makes-a-faction-strong).
- **Renegotiation.** A proposal can be answered with `COUNTER`, which settles the offer and makes one of its
  own back the other way; proposals carry a term, and signing a type already in force replaces it. Treaty
  expiry existed from phase 1 but nothing could produce a treaty that ever lapsed until proposals could name
  a term.
- **Intel sharing became a standing arrangement.** It was registered in phase 1 and consumed by nothing.
  Fog of war could only ever be handed over as a one-off - a member with the right permission pushed their
  faction's map across once and nothing kept it current - so there was no way to express an ongoing deal.
  The treaty now re-merges both maps on a timer while it holds. (Named `shared_vision` at first; renamed to
  `intel_sharing`, which is what it actually does.)
- **Every shipped reaction carries its own text.** Proposals, warnings and faction mail all surface the
  reaction's message, and an archetype that leaves them blank gets one generic engine line for all of them -
  so a self-check fails a shipped preset with an empty message.
- **Tolerance is a feature of the config, not a nicety.** Growing and shrinking this vocabulary is the point
  of the work, so an unknown action, status or reaction name now costs one skipped entry and a log line
  instead of the file.

### What phase 4 settled

- **The diplomacy tab was NPC- *typed*, not merely NPC-gated.** Its list, its selection handler and its
  opinion tables all took an `NPCFaction`, so ungating it would have produced an empty tab on a server with
  NPCs turned off. One list of every faction replaces it; the gate on `NPC_FACTION_SPAWN_LIMIT` is gone.
- **Treaties have a screen.** They had been syncing to clients since phase 1 with nothing rendering them, so
  the only way to discover you held one was that turrets stopped shooting. The left list carries a treaties
  column - the one at-a-glance answer to who you have what with - and the right column lists the selected
  pair's agreements with their age, remaining term, clause and a way out.
- **The acceptance preview is a wire op, not a local call.** An NPC's preset never leaves the server, so a
  client evaluating `wouldAccept` itself would answer from the default settings and be wrong. `QUERY` and
  `QUERY_RESULT` join `DiplomacyProposal.Action`: the server evaluates, replies with the verdict and the
  unmet conditions, stages nothing and moves no opinion. It reuses the proposal buffer and sits behind the
  same permission gate - if you may not negotiate it, you may not find out whether it would have worked.
- **A refusal now says why.** `answerAsNPC` computed the unmet conditions and printed them to stderr, which
  left the player with a "no" and no idea what would change it. They go out as faction mail as well.
- **The two opinion tables became one with a toggle.** Personal and faction ledgers were rendered side by
  side as near-identical tables taking half the panel; merging them is what freed the room for the treaty
  list.
- **`COUNTER` became reachable.** The protocol has carried it since phase 2 and no UI could produce one; the
  inbox has a Counter button that opens the propose dialog against the offer being answered.
- **The inbox stopped being a secret.** It lived two clicks deep under OPTIONS with nothing anywhere saying
  an offer was waiting. It is a counted button on the diplomacy tab, with the OPTIONS entry kept as a
  shortcut.
- **Net deletion.** `FactionRelationEditPanel`, `FactionRelationDialog` and six `FactionControlManager`
  methods go; the propose dialog is driven by `TreatyTypeRegistry`, so a modded treaty type appears in it
  with no GUI change. SHARE FOW leaves the faction list, superseded by the intel-sharing treaty.
- **The default window grew to 960x620.** Four things share the tab now and 850x550 clipped the treaty list.
  Window size is saved per window id, so the id changed with it - otherwise the saved size wins and the new
  default never applies.
- **No `"TREATY_CHANGE"` notify argument**, which the plan had asked for: a treaty change already sets
  `changedFactionAspect`, which fires the no-arg `notifyObservers()` that every `ScrollableTableList` flags
  dirty on. A second signal would have bought nothing.

### What phase 4.5 group A settled

- **The sender stamp went on `RemoteArray`, not a new type.** `fromByteStream` has always been handed
  `updateSenderStateId` and always thrown it away, so keeping it is two lines in the base class and every
  array-backed buffer in the game gets it - the five faction paths that needed it and whatever needs it
  next. A parallel sender-carrying subclass would have been a bigger diff that covered less.
- **Faction settings are a broadcast, not a request.** Every send of `factionMod` - name, colour,
  description, open-to-join, attack-neutral, auto-declare-war - is the server announcing a change it has
  already made and permission-checked, and the client requests that ask for one arrive on the player's own
  network object instead. The drain applied them on both sides and checked nothing at all, so any client
  could rename or recolour any faction whose id it could name. The server now ignores the buffer entirely,
  which is a deletion rather than a permission check.
- **An unknown initiator was not a refusal.** The member-modification drain hung every check off looking
  the initiator up in the faction's member list, and when that lookup failed it logged, asserted, and then
  applied the change anyway. Naming yourself as the initiator of your own promotion into a faction you were
  not in therefore skipped every check there was. It refuses now.
- **The roles clamp is two limits, not one.** Holding the permission that governs permissions gets a member
  into the roles blob; what stops them there is that a rank above their own is untouchable and that no rank
  may be granted a right their own rank does not hold. Without the second, the one right such an officer
  holds was enough to write every other right onto their own rank.
- **Only the rights that confer authority are clamped.** `KICK_ON_FRIENDLY_FIRE` is a rule a faction
  imposes on itself rather than a capability anyone wields, and it is not in `ADMIN_PERMISSIONS` - so
  clamping it to what the editor holds would have made it unsettable by anyone, the founder included.
- **Admin treaty commands go through the pipeline.** `/faction_treaty_sign`, `/faction_treaty_break` and
  `/faction_treaty_list` end at the same apply a negotiated proposal does, so an admin-made agreement is the same
  record a player can then see, break or renegotiate. The other half of that finding was already closed:
  `setRelationServer` clears the treaties that were forcing a relation, so `FACTION_MOD_RELATION` has not
  left the relation and the treaty set disagreeing since phase 2.

### What treaty naming settled

- **A treaty has a name, and both signatories see the same one.** Every non-aggression pact in the galaxy
  rendered as "Non-Aggression Pact", which stops being a name the moment a faction holds two of anything.
  The name rides the proposal, so the other side reads it in the inbox before agreeing, and renaming is
  re-offering the same agreement under a new name. Signing a type already in force replaces it, so the
  rename path needed no new protocol - just a button that opens the propose dialog on a held type.
- **The default name is generated from what the agreement involved**, not from the type. `TreatyType`
  carries templates; `TreatyNames` fills `{from}`, `{to}`, `{fromHome}`, `{toHome}`, `{ceded}`, `{type}`.
- **A template whose token has nothing behind it is skipped, not filled.** A concession is named after the
  system that was conceded, and a faction never concedes its capital - so `{ceded}` is a token of its own
  rather than a reuse of `{toHome}`, and a concession template simply does not come up until a settlement
  term says a system changed hands. Filling it from a home system would have put a lie on every row.
- **The generated name is decided server-side, at signing.** A client cannot build it: it does not know what
  the galaxy calls a system it has never visited, and a name that differed between the two signatories would
  defeat the point. The propose dialog offers to name the agreement and says nothing about the default.

### What in-game testing of phase 4 turned up

- **A treaty, a demand and a declaration are three different things to a player.** The engine stores all
  three the same way - a record between two factions, which is what lets `getRelation` keep one code path -
  but calling a war declaration a treaty reads as nonsense. `TreatyType.getKind()` derives the distinction
  from flags the type already sets rather than adding a field: unilateral is a **declaration**, asymmetric
  and agreed is a **demand**, symmetric and agreed is a **pact**. The propose dialog labels each offering
  with its verb ("Propose: Alliance", "Demand: Vassalage", "Declare: War") and sorts agreements first,
  declarations last; the acceptance preview stops asking whether a declaration would be accepted, because
  nobody is being asked. Both list columns became **Standing** rather than "Treaties", since a war sits in
  them too.
- **A bottom filter row holding only a LEFT element reserved no height.** `FilterController.calcInit`
  skipped LEFT elements when measuring - correct for the usual LEFT/RIGHT pair, where counting both would
  reserve the row twice, but a row holding nothing else still exists. The opinion list's toggle was drawn
  one row below the list's own bottom edge, outside its box. Fixed where it is measured rather than by
  restyling the one caller that tripped over it.

### What federation governance settled

- **The two open questions from the design became charter rules, not decisions made for everyone.**
  Whether a carried war binds members who voted against it, and whether ballots weigh by power, are rows
  on the bloc's Governance pane. The defaults are "binds everyone" and "the same", so a bloc is a polity
  unless it chooses to be a mailing list; leaving stays free, which is the check on both.
- **The whole charter is one long.** Vote-on flags (admit, expel, war, charter), weighting, binding, the
  threshold (simple majority, two thirds, everyone) and the chair's term in days are packed into
  `charterFlags`, so a rule change is one number on the wire, in the save and in a charter vote's payload.
  The server sanitises it, since the client names it.
- **Charter changes can go to a vote**, and that flag is the one that matters: without it the chair could
  switch every other vote off alone.
- **Several votes at once**, at most one per question. One open vote at a time made a bloc that wanted to
  admit two factions wait on the first.
- **A federation war is each member's own war.** A carried war vote (or the chair's call, when war is not
  put to a vote) signs a `war` treaty from every bound member to the target, so the existing treaty model,
  peace proposals and combat consumers needed no change, and each member makes its own peace.
- **The chair can rotate** to the next member on a term set in the charter; handing it over or the chair
  leaving starts a new term.
- **The tab has two layouts.** A member sees the design's bloc card and members beside governance and open
  votes; a faction in no bloc sees every bloc (where invites are answered) with the one clicked beside it.
  Joining or leaving rebuilds the tab through the engine panel's own rebuild flag.

### Power, and what a refusal says

- **Faction power is on screen**, on the diplomacy info panel against the viewer's own ("Power: 1,240 vs
  your 620") and on the faction log for your own faction, both with the breakdown that makes it
  readable - systems, active members, surplus per period. The formula moved out of `DiplomacyEntity`
  into `FactionStrength`, because the moment a player can see the number it has to be the number the
  acceptance rules use or the panel is describing a different game. The GUI reads it on the neutral
  default weights; an archetype still sizes people up on its own.
- **It needed one line to work client-side.** The three inputs were all on the wire except the claimed
  system list, which is server-only - but the owning sector of each system rides the faction blob and
  the database query fills both lists from the same rows, so the sector count is exact on a client.
- **A refusal stopped reading like a config file.** "no acceptance rule configured for
  starmade:vassalage" described the server's XML rather than anything happening in the game, and the
  conditions behind a real refusal printed as `RAW_POINTS >= 1000`. Conditions now describe themselves
  in the terms a player sees elsewhere, and an acceptance rule can carry its own `<Refusal>` line that
  replaces the generated list entirely - the three shipped presets each refuse in their own voice, and
  a self-check fails the build on a preset that forgets one or pastes a type id into it.
- **No rule is an answer, not an oversight.** An archetype opts in to each kind of agreement by writing
  a rule for it, so the absence of one is a settled "we do not do that" - phrased by kind now, so a
  demand gets "they do not submit to anyone" rather than a missing-config notice.

### Two standing rules

- **A home system can never be taken by a treaty.** `CEDE_SYSTEM` must refuse the giver's home outright
  rather than price it, and so must anything else that moves territory. A faction's home is its respawn
  point, its shop till and the origin its whole point economy is measured from - a settlement that takes it
  deletes a faction rather than weakening one. It is also the invariant behind the `{ceded}` naming token,
  which is documented as never being a home system. Nothing today can reach a system through a treaty (the
  only path to `setOwnerFaction` is the station claim), so the guard belongs in the cede term when phase 5
  builds it.
- **NPC factions refuse every demand.** Vassalage and tribute are turned down before the archetype's
  acceptance rules are even consulted, so no config can make an NPC somebody's vassal. Partly because
  nothing implements what being one would mean yet, but mostly because an NPC faction is run by the engine
  rather than by anyone who could be held to the arrangement. A self-check pins which types count as demands
  - the classification is derived from "agreed, but asymmetric" rather than declared - and fails the build
    if a shipped preset carries an acceptance rule for one, since that would be config doing nothing.

### Decisions

- **`RType` stays as a derived view.** Treaties are the real state; `getRelation()` collapses them to one of
  the three legacy values, leaving all ~19 consumers untouched.
- **Player factions accrue opinion, but it never gates them.** Opinion is strictly informative/RP for
  player-to-player pairs; only NPC factions evaluate reactions and acceptance from it.
- **Fresh start on migration.** Existing `RType` rows load into the legacy field; no treaties are
  synthesised and old NPC ledgers are dropped.
- **Role permissions gate negotiation.** Voting exists but is an opt-in charter flag, never the default.
- **No proximity-based opinion** — see finding 5.
- **Emblems ship as a fixed atlas**, and that is what every faction gets by default. Player uploads are in
  scope but sit behind `ServerConfig.FACTION_CUSTOM_EMBLEMS_ENABLED`, which ships **off** — a server owner
  opts in having decided they will moderate it. Admins can remove an emblem from the faction list or the
  diplomacy panel, which reverts the faction to its atlas fallback, purges the stored image and logs who did
  it. The removal control is part of the upload feature, not a follow-up to it.
