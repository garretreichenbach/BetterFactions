# BetterFactions Completion Roadmap

## Context
BetterFactions brings StarMade's NPC-only opinion/diplomacy system to player factions and expands it with Stellaris-inspired dynamic diplomacy. The core opinion point system, modifier framework, and condition/reaction engine are implemented but broken due to 3 stub methods and missing network sync. The mod needs bug fixes to get the existing system working, then completion of war/peace/pact flows, then expansion with casus belli, claims, rivalry, and full peace deal negotiation.

## Phase 1: Fix Critical Bugs

**Goal**: Get opinion points, reactions, and network sync actually working.

### 1.1 Wire reaction system into FactionDiplomacyManager
- **File**: `manager/FactionDiplomacyManager.java`
- `getReactions()` (line 113) returns empty list — reactions never fire
- Fix: Load `FactionDiplomacyConfig` during `initialize()`, return `config.reactions` from `getReactions()`
- `getReaction()` (line 164) returns null — action-triggered reactions don't work
- Fix: Look up config element by `action.type`, return its attached `reaction`
- `existsAction()` (line 168) returns false — dynamic modifier decay broken
- Fix: Move to `FactionDiplomacyEntity` as instance method checking its `actions` map, or accept entity context param

### 1.2 Implement FactionDiplomacyPacket processing
- **File**: `network/FactionDiplomacyPacket.java` — both `processPacketOnClient/Server()` are empty
- Fix `processPacketOnClient()`: update `diplomacyCache`, call `onClientChanged()`
- Fix `processPacketOnServer()`: validate sender, update cache, mark changed
- **File**: `manager/NetworkManager.java` — `sendToPlayer()` is empty
- Fix: call `PacketUtil.sendPacket(playerState, new FactionDiplomacyPacket(...))`
- **File**: `BetterFactions.java` — register `FactionDiplomacyPacket` in `registerPackets()`

### 1.3 Fix PeaceDealPanel crash
- **File**: `gui/faction/diplomacy/war/PeaceDealPanel.java` — references undefined `titleTextBar`, `from`, `to`
- Add `from`/`to` fields, set from WarData participants in `createPanel()`
- Fix `PeaceDealDialog` to pass WarData through

**Testable after Phase 1**: Opinion points change from actions (attacks, trades), reactions fire (auto-war at low opinion, auto-peace offers), diplomacy data syncs to clients in multiplayer.

---

## Phase 2: Complete Core Diplomatic Actions

**Goal**: War declaration, peace negotiation, and non-aggression pacts work end-to-end.

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
- **File**: `gui/faction/diplomacy/war/PeaceDealPanel.java` — rewrite from 20% stub
- **File**: `data/serializeable/DiplomaticData.java` — implement empty serialize/deserialize
- Full demand/offer builder: each side can add demands (territory, credits, humiliation)
- War score determines what can be demanded
- `DiplomaticData.DiplomaticDataType` already has: WHITE_PEACE, OFFER_TERRITORY, OFFER_CREDITS, OFFER_RESOURCES, OFFER_DIPLO
- Server-side: handle `PEACE_OFFER` message type in `SendFactionMessagePacket`, process accept/reject with `FactionDiplomacyAction` types

### 2.4 Non-Aggression Pact UI
- **File**: `gui/faction/diplomacy/FactionActionsPanel.java` — add NAP offer button
- Use existing `FactionMessageSendDialog` with new `MessageType`
- Server: create `FactionRelationOffer` with `rel = CustomRelationType.NON_AGGRESSION`
- Reaction types `OFFER/ACCEPT/REJECT_NON_AGGRESSION_PACT` already implemented in `FactionDiplomacyEntity.executeReaction()`

### 2.5 Wire diplomatic offer acceptance/rejection
- **File**: `network/client/ModifyFactionMessagePacket.java`
- When player accepts ALLIANCE_OFFER: set relation to FRIEND
- When player accepts NON_AGGRESSION_PACT: set relation to NON_AGGRESSION
- When player accepts PEACE_OFFER: set relation to NEUTRAL, apply truce modifier
- Fire corresponding `DiploActionType` for all accepts/rejects

**Testable after Phase 2**: Players can declare war with war goals, negotiate Stellaris-style peace deals with demands, offer/accept non-aggression pacts, and all actions properly modify opinion.

---

## Phase 3: Stellaris-Inspired Expansion

**Goal**: Casus belli, claims, rivalry, and (optional) war exhaustion.

### 3.1 Casus Belli system (HIGH PRIORITY)
- **New**: `data/diplomacy/war/CasusBelli.java` — CB types: RIVALRY, BORDER_FRICTION, ALLIANCE_THREAT, SUBJUGATION, FABRICATED
- **New**: `manager/CasusBelliManager.java` — tracks available CBs per faction pair, fabrication timers
- Each CB has: generation conditions, fabrication time, war goals it unlocks
- Declaring war without CB = large opinion penalty with ALL factions (new `UNJUSTIFIED_WAR` action type, configurable)
- `HAS_WAR_GOAL` and `TARGET_OF_WAR_GOAL` DiploStatusTypes already exist

### 3.2 Claims & Territory (HIGH PRIORITY)
- **New**: `data/diplomacy/claims/ClaimData.java` — faction → system claims with strength
- **New**: `manager/ClaimsManager.java` — claim management, generates BORDER_FRICTION CB
- Add `CONTESTED_CLAIMS` DiploStatusType (opinion penalty)
- "Fabricate Claim" button in FactionActionsPanel
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

**Testable after Phase 3**: CBs generate from rivalry/border friction/claims. Unjustified wars have consequences. Claims create territorial disputes. Rivals provide structured competition. War exhaustion (if enabled) forces prolonged wars to end.

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

## Key Files Reference

| File | Role |
|------|------|
| `manager/FactionDiplomacyManager.java` | 3 broken stubs to fix (Phase 1) |
| `network/FactionDiplomacyPacket.java` | Empty packet processing (Phase 1) |
| `manager/NetworkManager.java` | Empty sendToPlayer (Phase 1) |
| `data/diplomacy/FactionDiplomacyEntity.java` | Core opinion engine, reaction execution |
| `data/diplomacy/FactionDiplomacyConfig.java` | Default reactions, config elements |
| `gui/faction/diplomacy/FactionActionsPanel.java` | Main UI entry point for all diplomatic actions |
| `gui/faction/diplomacy/war/PeaceDealPanel.java` | Peace deal UI (rewrite in Phase 2) |
| `data/serializeable/war/WarData.java` | Canonical war data (consolidate in Phase 2) |
| `data/serializeable/DiplomaticData.java` | Peace demand types (implement serialize in Phase 2) |

## Verification
- **Phase 1**: Start a test server, have two factions attack each other, verify opinion points change in diplomacy tab and reactions fire (auto-war declaration at low opinion)
- **Phase 2**: Declare war via UI, negotiate peace with demands, offer/accept NAP — verify all state changes persist and sync
- **Phase 3**: Verify CBs generate over time, claims appear on systems, rivalry/guarantee buttons work
- **Phase 4**: Hover over opinion scores to see tooltip breakdown
