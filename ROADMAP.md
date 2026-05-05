# BetterFactions Completion Roadmap

## Context
BetterFactions brings StarMade's NPC-only opinion/diplomacy system to player factions and expands it with Stellaris-inspired dynamic diplomacy. The core opinion point system, modifier framework, and condition/reaction engine are implemented but broken due to 3 stub methods and missing network sync. The mod needs bug fixes to get the existing system working, then completion of war/peace/pact flows, then expansion with casus belli, claims, rivalry, and full peace deal negotiation.

## Phase 1: Fix Critical Bugs [DONE]

**Goal**: Get opinion points, reactions, and network sync actually working.

### 1.1 Wire reaction system into FactionDiplomacyManager [DONE]
- Loaded `FactionDiplomacyConfig` during `initialize()`, returns `config.reactions` from `getReactions()`
- `getReaction()` looks up config elements by action type
- Replaced broken static `existsAction()` with instance method `hasActiveAction()` on `FactionDiplomacyEntity`
- Fixed structural bug: action expiry/dynamic modifiers now run regardless of whether a reaction exists

### 1.2 Implement FactionDiplomacyPacket processing [DONE]
- `processPacketOnClient()`: invalidates cache, notifies observers
- `processPacketOnServer()`: validates sender faction, marks changed for persistence
- `NetworkManager.sendToPlayer()`: sends `FactionDiplomacyPacket` via `PacketUtil`
- Registered `FactionDiplomacyPacket` in `BetterFactions.registerPackets()`

### 1.3 Fix PeaceDealPanel crash [DONE]
- Added `from`/`to` fields, populated from WarData participants
- Fixed `sendMessage()` to use `messageBar` instead of undefined `titleTextBar`
- `PeaceDealDialog` accepts `WarData` via `setWarData()` and passes to panel

---

## Phase 2: Complete Core Diplomatic Actions

**Goal**: War declaration, peace negotiation, non-aggression pacts, demands, counter-offers, and faction permissions all work end-to-end.

### 2.1 Consolidate duplicate WarData classes
- Delete `data/diplomacy/war/WarData.java` and `data/diplomacy/war/wargoal/WarGoalData.java` (older versions)
- Keep `data/serializeable/war/WarData.java` and `data/serializeable/war/WarGoalData.java` as canonical
- Implement their empty `serialize()`/`deserialize()` methods
- Migrate all references (`WarManager`, `NPCFactionUtils`, `ClientCacheManager`)

### 2.2 War Declaration flow
- **File**: `gui/faction/diplomacy/FactionActionsPanel.java` (line 311 — empty DECLARE WAR callback)
- **New**: `gui/faction/diplomacy/war/WarDeclarationDialog.java` + `WarDeclarationPanel.java`
- Panel shows available war goal types, optional message, DECLARE button
- Server-side: calls `faction.declareWarAgainstEntity()`, creates WarData, fires `DECLARATION_OF_WAR` action, creates news event

### 2.3 Full peace deal negotiation (Stellaris-style)
- **File**: `gui/faction/diplomacy/war/PeaceDealPanel.java` — rewrite from stub
- **File**: `data/serializeable/DiplomaticData.java` — implement empty serialize/deserialize
- Full demand/offer builder: each side can add demands (territory, credits, humiliation)
- War score determines what can be demanded
- `DiplomaticData.DiplomaticDataType` already has: WHITE_PEACE, OFFER_TERRITORY, OFFER_CREDITS, OFFER_RESOURCES, OFFER_DIPLO
- Server-side: handle `PEACE_OFFER` message type in `SendFactionMessagePacket`

### 2.4 Counter-offer system
- When a faction receives a peace deal, alliance offer, NAP, or demand, they can send back a **counter-offer** instead of just accept/reject
- Counter-offers modify the terms (change demands, add conditions) and send back to the original proposer
- New `MessageType.COUNTER_OFFER` in `FactionMessage`
- Counter-offers carry the original proposal plus modifications
- Chain terminates when one side accepts, rejects outright, or a configurable max rounds is reached

### 2.5 Non-Aggression Pact UI
- **File**: `gui/faction/diplomacy/FactionActionsPanel.java` — add NAP offer button
- Use existing `FactionMessageSendDialog` with new `MessageType`
- Server: create `FactionRelationOffer` with `rel = CustomRelationType.NON_AGGRESSION`
- Reaction types `OFFER/ACCEPT/REJECT_NON_AGGRESSION_PACT` already implemented in `FactionDiplomacyEntity.executeReaction()`

### 2.6 Wire diplomatic offer acceptance/rejection
- **File**: `network/client/ModifyFactionMessagePacket.java`
- When player accepts ALLIANCE_OFFER: set relation to FRIEND
- When player accepts NON_AGGRESSION_PACT: set relation to NON_AGGRESSION
- When player accepts PEACE_OFFER: set relation to NEUTRAL, apply truce modifier
- Fire corresponding `DiploActionType` for all accepts/rejects

### 2.7 Faction permissions hookup for diplomacy
- Hook into StarMade's `FactionPermission` system to gate diplomatic actions
- New permissions: `DIPLOMACY_DECLARE_WAR`, `DIPLOMACY_OFFER_PEACE`, `DIPLOMACY_OFFER_ALLIANCE`, `DIPLOMACY_OFFER_NAP`, `DIPLOMACY_SEND_DEMAND`, `DIPLOMACY_MANAGE_CLAIMS`
- Default: faction leader has all permissions, officers have peace/alliance/NAP, members have none
- Check permissions before showing buttons in `FactionActionsPanel`
- Server-side validation in packet handlers — reject actions from players without permission

### 2.8 Demand system (non-hostile)
- **New**: `data/diplomacy/DemandData.java` — represents a demand (territory, credits, diplomatic concession) without declaring war
- **New**: `gui/faction/diplomacy/DemandDialog.java` + `DemandPanel.java` — UI to compose demands
- Demands are sent as diplomatic messages; recipient can accept, reject, or counter-offer
- Rejected demands can escalate: the demanding faction gains a CB against the rejecting faction
- Accepted demands execute their terms (transfer territory, credits, set relation)
- New `DiploActionType`: `SEND_DEMAND`, `ACCEPT_DEMAND`, `REJECT_DEMAND` with configurable opinion modifiers

**Testable after Phase 2**: Players can declare war with war goals, negotiate Stellaris-style peace deals with demands and counter-offers, offer/accept non-aggression pacts, send non-hostile demands that can escalate, and all actions are gated by faction permissions.

---

## Phase 3: Stellaris-Inspired Expansion

**Goal**: Casus belli, claims, rivalry, containment CB mechanics, and (optional) war exhaustion.

### 3.1 Casus Belli system (HIGH PRIORITY)
- **New**: `data/diplomacy/war/CasusBelli.java` — CB types: RIVALRY, BORDER_FRICTION, ALLIANCE_THREAT, SUBJUGATION, FABRICATED, CONTAINMENT, REJECTED_DEMAND
- **New**: `manager/CasusBelliManager.java` — tracks available CBs per faction pair, fabrication timers
- Each CB has: generation conditions, fabrication time, war goals it unlocks, demand cost modifiers
- Declaring war without CB = large opinion penalty with ALL factions (new `UNJUSTIFIED_WAR` action type, configurable)
- **Containment CB**: When a faction declares an unjustified war, all factions with opinion below a configurable threshold toward the aggressor automatically receive a free `CONTAINMENT` CB against them. Containment CB gives discounts on certain peace deal demands (e.g., territory demands cost less war score).
- **Rejected Demand CB**: When a demand is rejected, the demanding faction gains a CB tied to the demand's terms
- `HAS_WAR_GOAL` and `TARGET_OF_WAR_GOAL` DiploStatusTypes already exist

### 3.2 Claims & Territory (HIGH PRIORITY)
- **New**: `data/diplomacy/claims/ClaimData.java` — faction → system claims with strength
- **New**: `manager/ClaimsManager.java` — claim management, generates BORDER_FRICTION CB
- Add `CONTESTED_CLAIMS` DiploStatusType (opinion penalty)
- "Fabricate Claim" button in FactionActionsPanel (gated by `DIPLOMACY_MANAGE_CLAIMS` permission)
- Claims provide TERRITORY war goal to take systems

### 3.3 Rivalry & Guarantee (HIGH PRIORITY)
- Add `RIVAL` and `GUARANTEED_BY` DiploStatusTypes
- Add `DECLARE_RIVALRY`, `GUARANTEE_INDEPENDENCE` DiploActionTypes
- Max 3 rivals per faction. Rivalry grants CB + prestige, applies negative opinion
- Guarantee: promise to defend if attacked, auto-join defensive wars
- Buttons in FactionActionsPanel

### 3.4 War Exhaustion (OPTIONAL — config toggle)
- **File**: `manager/ConfigManager.java` — add `SimpleConfigBool warExhaustionEnabled`
- Track exhaustion per participant in WarData
- Increases from: ship/station losses, time at war, failing war goals
- At 100%: forced to accept status-quo peace
- All logic gated behind the config toggle

**Testable after Phase 3**: CBs generate from rivalry/border friction/claims/rejected demands. Unjustified wars trigger containment CBs for opposing factions. Claims create territorial disputes. Rivals provide structured competition. War exhaustion (if enabled) forces prolonged wars to end.

---

## Phase 4: UI Polish & Quality of Life

### 4.1 Opinion tooltip breakdown
- Hover over opinion score → Stellaris-style tooltip listing every modifier with name, value, color
- Uses existing `staticMap`/`dynamicMap` data from `FactionDiplomacyEntity`

### 4.2 Diplomatic history log
- Per-faction diplomatic event history filtered from news system
- Timestamps + opinion point changes per event

### 4.3 Trade agreements
- Formalized trade pacts with mutual opinion boost + resource income bonus
- `TRADING_WITH_US`/`TRADING_WITH_ENEMY` action types already exist
- Button in FactionActionsPanel (currently empty block at lines 346-348)

### 4.4 Protection pacts
- `PROTECTING`/`BEING_PROTECTED` DiploStatusTypes already exist with config values
- Auto-call-to-war when protected faction is attacked
- Buttons exist in old commented-out code (FactionActionsPanel lines 468-501)

---

## Phase 5: Ethics & Government System (DESIGN TBD)

**Goal**: Factions have player-chosen ethics and government types that create unique gameplay through bonuses, penalties, and exclusive CBs.

### 5.1 Core ethics/government framework
- **New**: `data/faction/FactionEthics.java` — ethics axes (e.g., Militarist/Pacifist, Spiritualist/Materialist, Authoritarian/Egalitarian, Xenophobe/Xenophile)
- **New**: `data/faction/GovernmentType.java` — government types determined by ethic combinations (Democracy, Oligarchy, Dictatorship, Theocracy, etc.)
- Factions choose ethics at creation or via reform (with cooldown)
- Each ethic/government combo provides pros and cons (opinion modifiers, build bonuses, diplomatic options)

### 5.2 Ethics-based diplomacy
- Factions with similar ethics get opinion bonuses; opposing ethics get penalties
- New DiploStatusTypes: `SIMILAR_ETHICS`, `OPPOSING_ETHICS`
- Ethics-exclusive CBs (e.g., Spiritualist Theocracy gets "Holy War" CB against opposing government types; Militarist gets reduced war declaration opinion penalties)
- Some diplomatic actions gated by ethics (e.g., Xenophobe can't guarantee independence of other species/factions)

### 5.3 Government mechanics
- Government type affects internal faction mechanics (election cycles, succession, reform costs)
- Some government types unlock unique diplomatic options (Federations only for certain types, etc.)

**Note**: This phase needs further design discussion before implementation. Key questions:
- How many ethic axes? Stellaris has 4 axes with 3 tiers — may be too complex for StarMade
- Should ethics affect gameplay beyond diplomacy (build costs, research, etc.)?
- How does government type interact with StarMade's existing faction rank/permission system?
- Should ethics be visible to other factions or partially hidden?

---

## Key Files Reference

| File | Role |
|------|------|
| `manager/FactionDiplomacyManager.java` | Core manager, reactions, config (Phase 1 DONE) |
| `network/FactionDiplomacyPacket.java` | Diplomacy network sync (Phase 1 DONE) |
| `manager/NetworkManager.java` | Packet sending (Phase 1 DONE) |
| `data/diplomacy/FactionDiplomacyEntity.java` | Core opinion engine, reaction execution |
| `data/diplomacy/FactionDiplomacyConfig.java` | Default reactions, config elements |
| `gui/faction/diplomacy/FactionActionsPanel.java` | Main UI entry point for all diplomatic actions |
| `gui/faction/diplomacy/war/PeaceDealPanel.java` | Peace deal UI (Phase 1 crash fixed, Phase 2 full rewrite) |
| `data/serializeable/war/WarData.java` | Canonical war data (consolidate in Phase 2) |
| `data/serializeable/DiplomaticData.java` | Peace demand types (implement serialize in Phase 2) |

## Verification
- **Phase 1** [DONE]: Opinion points change from actions, reactions fire, diplomacy syncs to clients
- **Phase 2**: Declare war via UI, negotiate peace with demands + counter-offers, offer/accept NAP, send demands, permissions gate actions
- **Phase 3**: CBs generate over time, unjustified wars trigger containment CB, claims work, rivalry/guarantee buttons work
- **Phase 4**: Hover over opinion scores to see tooltip breakdown
- **Phase 5**: Ethics/government chosen at faction creation, affect opinion and available CBs
