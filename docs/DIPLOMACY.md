# Diplomacy

Treaties, proposals, opinion, NPC presets, federations and emblems. Package: `diplomacy/`, with the server logic in `DiplomacyManager`. Back to [DEVELOPER.md](DEVELOPER.md).

## Treaties

A `FactionTreaty` is `{typeId, from, to, signedAt, expiresAt, name, clause}`, stored per unordered pair in a
`FactionTreatySet`. Direction lives in `from`/`to`, so asymmetric types (vassalage, tributary) keep it.

`TreatyType` is registry data, not an enum. Builder flags: `asymmetric()`, `unilateral()` (no acceptance),
`unbreakable()`, `implies(RType)`, `names(templates...)`, `importance(int)`. The **kind** is derived from the flags:
unilateral is a declaration, symmetric and agreed is a pact, asymmetric and agreed is a demand. The built-in types are
seeded in `TreatyTypeRegistry` under the `starmade:` namespace.

**Sanctions** are declarations with no implied relation (`TreatyType.isSanction()`): `starmade:embargo` and
`starmade:tariff`. Declaring one records its own ledger action on the target (`EMBARGO` -200, `TARIFF` -75) instead of
`TREATY_SIGNED`, which an NPC would read as a truce. Only `from` can revoke one, free and not a broken treaty;
the target can ask for it as a `BREAK_TREATY` deal term given by `from` (`settlementBlocker` refuses any other giver).
Both are taken and lifted from the Actions dialog (`DiplomacyActionsDialog`).

- **Embargo.** `DiplomacyManager.tradeClosed(a, b)` is true either way round. `refusesTrade(seller, customer)` adds
  the secondary embargo: the embargoer also refuses anyone with an alliance, defensive pact or federation treaty
  with the side it embargoed (the allies refuse nobody). `ShopEmbargoMixin` makes `ShoppingAddOn.hasPermission`
  answer it (the gate for the client's shop screen and the server's buy and sell alike), and `BuyTradeListener` /
  `SellTradeListener` cancel trade orders it refuses either way. A trade agreement between the pair is broken on
  declaring it and refused while it holds.
- **Opinion pressure.** `tradedBetween(a, b)` runs on every shop price lookup and every trade order that goes ahead:
  whichever side traded with a faction somebody else has embargoed gets `TRADED_WITH_SANCTIONED` (-2 a time, to
  -300) from the embargoer and from every NPC whose opinion of the embargoer is at least 500.
- **Tariffs.** `shopPrice` applies `tariff_percent` (default 20) at the declarer's shops to the target's members, after
  any trade agreement discount: buying costs more, selling pays less.

**Importance** ranks types for naming a deal that signs several (see Proposals). Vanilla: vassalage 60, alliance 50,
tributary 40, defensive pact 30, non-aggression 20, trade agreement and intel sharing 10. Unset is 0.

**Derived relation.** `FactionTreatySet.getDerivedRelation()`: any ENEMY-implying treaty wins, then FRIEND
beats NEUTRAL, and no relation-implying treaty returns `null` so the engine's own relation byte stands. Signing
also writes the legacy relation via `setRelationServer`, so engine code that reads the byte directly agrees.

## Proposals

`DiplomacyProposal` actions: `PROPOSE`, `REVOKE`, `ACCEPT`, `DECLINE`, `COUNTER`, `QUERY`, `QUERY_RESULT`.

**A deal is its terms.** Only things that are not agreed keep a type id on the proposal: a declaration (war, with
its goals as terms), a `REVOKE`, a call to arms (`against != 0`) and a federation invite. Every agreement is a list
of `SettlementTerm`s; a treaty is a `SIGN_TREATY` term whose `giver` is the side it binds (for a pact, either side;
the dialog uses the target), carrying its own `expiresAt` and `name`. With no type id, a proposal between factions at
war is peace and otherwise a settlement (`DiplomacyManager.isSettlement`); a settlement needs at least one term.
Because the giver is explicit, either side can sign a one-sided treaty.

`DiplomacyProposal.foldTreatyIntoTerms` turns the old shape (an agreed type id on the proposal) into a leading
`SIGN_TREATY` term binding `b`, with the proposal's expiry and name. It runs on every incoming proposal, on offers
loaded from a save, and in `asCounterProposal` after the pair is turned around, so NPC reactions, older clients and
old saves all arrive as terms.

Server flow (`DiplomacyManager.handleRelationShipOffers`):

1. The initiator is overwritten with the packet sender's name; clients cannot claim to be someone else.
2. `mayNegotiate`: server admins pass; the engine relationship permission passes everything; `Perms.TREATIES`
   passes everything but war and peace.
3. `QUERY` is answered with the acceptance verdict and unmet conditions, nothing else changes.
4. Unilateral types apply at once. Everything else is stored, broadcast, and answered immediately by
   `answerAsNPC` if the target is an NPC.
5. Answers are permission-checked against the offer's target before the offer is consumed. `COUNTER` declines
   and proposes back the other way.

On acceptance `applyTerms` applies every term; each `SIGN_TREATY` goes through `signTreatyServer`, so it gets the
treaty record, news, relation change and ledger actions of a treaty signed on its own, with its term's expiry and
the deal's message as its clause. A settlement has no proposal-level effect of its own; peace clears the treaties
forcing the pair apart. A settlement costs `proposeTreatyCost` like any offer, needs only `Perms.TREATIES` (the
treaties in it are each checked), and is blocked by debt; peace is free and open to a faction in debt.

Signing a type the pair already holds replaces it; that is how renames and renewals work. A one-sided treaty can only
be re-signed the way round it already stands ("Only X can renew"), checked per term in `settlementBlocker`.

**Naming.** A deal is one agreement, so every treaty it signs gets the same name. The deal is called after its
**headline**: `SettlementTerm.headline(terms)`, the `SIGN_TREATY` of the highest-importance type (first on a tie).
The name is the headline's `name` if `TreatyNames.sanitize` accepts it, else any other treaty term's name, else one
generated from the headline type's templates with the headline's receiver as `{from}` and giver as `{to}`
(`{from}`, `{to}`, `{fromHome}`, `{toHome}`, `{ceded}`, `{type}`; a template whose token has no value is skipped).
News, history and the inbox also go by the headline: an alliance with a trade agreement on the side is offered,
announced and listed as an alliance. A declaration still takes its name from the proposal.

Expired treaties are removed on the server tick. Intel sharing re-merges both maps every 60 s.

## Opinion

`DiplomacyEntity` is one faction's ledger: an entry per other faction id or player db id (db id ≥
`Integer.MAX_VALUE` is a player). Score = raw points + static modifiers, clamped to the configured range.

- **Actions** (`DiplomacyAction.DiplActionType`) are events. A live action drives an `OpinionTurnModifier` that
  ramps toward its limit while the action repeats and walks back once it lapses.
- **Statuses** (`DiplStatusType`) are derived each status tick and give an `OpinionStaticModifier`.
- Four timers in `FactionDiplomacy`: status calc, turn apply, turn change, and an 11 s re-check after an action.
- Tags are string-keyed by enum name, so the enums can be reordered; unknown names are dropped on load.
- The engine reports `ATTACK` / `ATTACK_ENEMY` on every update a hit lands. Reports under 2 min apart
  (`DiplomacyAction.ENGAGEMENT_GAP_MS`) are one engagement and count once (`newOccurrence`; not saved).
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
  (`SettlementPricing`). Each term is worth its faction points, or `TreatyValue` per one-sided treaty signed or any
  treaty broken (5x for a vassalage or tributary, and never for the NPC to submit). A pact is worth nothing either
  way: both sides take on the same thing, so whether the NPC wants it is its `<Accept>` rule's call, and it is never
  priced in points. For a settlement, the rule check is every pact in it (`acceptsPacts`); one-sided treaties are
  left to the pricing. Peace is worth `PeaceValue·2^lead` to an NPC whose peace
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

## Costs, gifts and claims

Diplomacy spends faction points (FP). All amounts are mod config keys:

| Key | Default | Pays for |
|-----|---------|----------|
| `propose_treaty_cost` | 100 | Any offer but peace |
| `break_treaty_cost` | 1000 | Breaking a treaty |
| `declare_war_cost` | 2000 | Declaring war |
| `unjust_war_cost` | 3000 | On top, for a war with no casus belli |
| `gift_cost` / `gift_opinion` | 500 / 200 | A gift (`GIFT` packet). Repeat gifts within an hour earn less. 0 cost turns gifts off |
| `fabricate_claim_cost` | 1500 | A casus belli on another faction (`FABRICATE_CLAIM`), ready after `fabricate_claim_hours` (12) of uptime |
| `claim_lasts_hours` | 72 | How long a ready claim stays usable |
| `trade_agreement_shop_discount_percent` | 10 | Shop discount (or better sale price) between trade partners |
| `tributary_income_percent` | 25 | Share of a tributary's FP income its overlord takes (`TributeTurnMixin`) |

Gifts and claims are taken from the Diplomacy tab's opinion list (`DiplomacyOpinionList`). Casus belli are also
granted, free and ready at once, to the victim of a skirmish or a trade raid. An optional
`moddata/BetterFactions/casus_belli.xml` (`<CasusBelli>`, see `DiplomacyConfig.parseCasusBelli`) tunes them.

## Skirmishes

`DiplomacyManager.onAttacked`: a hit between factions not at war opens a `Skirmish`. While it lasts (2 min since
the last hit) `derivedRelation` makes the pair ENEMY, unless their treaties say FRIEND. Return fire is not
aggression (`Skirmish.hit`). The first hit writes a history line, gives the victim a casus belli and mail (not
against pirates), and runs a call to arms. Vanilla's auto-declare-war is switched off every tick. A raid hit
(`TradeFleetOrders.raiding`) only records the grievance, with no skirmish or call to arms. Skirmishes are synced
with `SKIRMISHES` and are not saved.

## Reputation

`Reputation` is galaxy-wide infamy and aggression per faction: +1 infamy per broken treaty, +1 aggression per
war declared (+2 more if unjust), +0.25 per engagement attacked (10 min engagements; return fire doesn't count).
Raids count as attacks. Both decay with half-life `reputation_half_life_days` (14, 0 = never), counted in server
uptime. Read by the `REPUTATION` preset condition, route safety (see [FLEETS.md](FLEETS.md)) and the faction
card. Saved inside the diplomacy file, broadcast with `REPUTATION`.

## Federations

`Federation` holds members, chair, invites, open votes and `charterFlags` (one long: vote-on bits, threshold,
weighting, binding, chair rotation days). Commands are `FederationCommand`s over `BFPacket`, all gated by
`Perms.FEDERATION_VOTE`, and the chair-or-vote rule per command lives in `DiplomacyManager`. Membership writes a
`starmade:federation` treaty with every other member; a federation war signs a `starmade:war` treaty from each
bound member, and a federation peace files a server-authored white peace offer from each bound member at
war with the target (war and peace share `CHARTER_VOTE_WAR`). `EMBARGO` / `LIFT_EMBARGO` commands and votes, also
under `CHARTER_VOTE_WAR` and its binding rule, file a server-authored embargo (or its revocation) from each bound
member; a member only lifts one it declared itself. Votes and ballots are only sent to members.

## Emblems

Off unless `custom_emblems_enabled`. The server checks the PNG header (≤ 64 KB, ≤ 2048²), decodes, re-encodes to
128×128 and stores `moddata/BetterFactions/emblems/emblem_<sha256-16>.png`. Clients fetch by hash into
`./client-faction-emblems/<host>/`. `/faction_emblem_clear` removes one.
