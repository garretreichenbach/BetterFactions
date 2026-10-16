# Civics and Institutions

What a faction believes and how it governs itself: civics, doctrines, and the three institutions (Senate,
Council, Corporations) with the contract board. Package: `civics/`. Back to [DEVELOPER.md](DEVELOPER.md).

All picks and institution changes need the engine's `FACTION_EDIT_PERMISSION` ("Edit ranks"), not a mod right.
Each of these classes runs from `DiplomacyManager`'s update, in this order: Senate, Council, Corporations,
Contracts, then `Civics.update`.

## Civics

A `CivicType` is registry data (`CivicRegistry`, namespace `betterfactions`): id, name, description, `excludes`
(either side naming the other is enough), `requires` (another civic held), `impliesGovernment` (display only),
`choosesResource`, a `Kind` (CIVIC, DOCTRINE, INSTITUTION), unlock rules, payouts and an optional handler.

Unlock rules (`Unlock.Rule`): STRENGTH_AT_LEAST, HELD_TREATIES_AT_LEAST, WARS_WON_AT_LEAST,
CLAIMED_SYSTEMS_AT_LEAST, ADMIN_GRANTED. They are checked only when picking: a faction that later falls below one
keeps the civic. No built-in civic uses one yet.

### Built-in civics

Payouts are (event, FP, daily cap, filters).

| Id | Effect |
|----|--------|
| `holy_leader` | Government "Holy Empire". Requires `religious`. PLAYER_KILLED +100 (cap 500; by the leader, victim at war, fair kill); PLAYER_LOST −200 when the leader dies. Whoever kills the leader gets +50 (cap 150) |
| `warrior_code` | SHIP_DESTROYED +10 (cap 300) against factions at war, +10 more with a casus belli. Unlocks the Military Junta council style |
| `oathkeepers` | TREATY_KEPT +150 (cap 450), CALL_TO_ARMS_ANSWERED +100 (cap 300), CALL_TO_ARMS_REFUSED −200 |
| `expansionists` | SYSTEM_CLAIMED +100 (cap 500), SYSTEM_LOST −100 |
| `isolationists` | DAILY +50 while holding no treaty but non-aggression, war or sanctions. Excludes `diplomatic_corps`, `expansionists` |
| `diplomatic_corps` | TREATY_SIGNED +50 (cap 200). Opinion modifiers about the holder fade at half speed (`FactionDiplomacy`) |
| `raiders` | SHIP_DESTROYED +15 (cap 300) against factions *not* at war. The victim gets a ready casus belli against the raider |
| `spice_dealer` | Neighbours pay extra upkeep (below) |
| `corporate_chains` | Corporations: one more free charter, dividends ×1.15, franchise fees −25%, The Board council style |
| `religious` | Excludes `industrialist`. Unlocks the Dark Shadow Council style |
| `resource_specialist` | Picked with a raw resource (`FactionCivics.resource`) |
| `industrialist`, `scavengers`, `miner_guilds`, `pacifists` | Description only for now. `pacifists` excludes `raiders` and `warrior_code` and halves the Council's `declare_war` directive |
| `elite_crews`, `horde_tactics`, `famous_admirals` | Doctrines, held in the doctrine slot. No mechanical effect yet |
| `senate`, `council`, `corporations` | Institutions (below) |

### Payouts

`CivicPayouts.fire(CivicEvent)` pays every matching payout of the faction under key `id#index`, then runs both
factions' handlers.

- Events: PLAYER_KILLED, PLAYER_LOST, SHIP_DESTROYED, SYSTEM_CLAIMED, SYSTEM_LOST, TREATY_SIGNED, TREATY_KEPT,
  TREATY_BROKEN, WAR_DECLARED, CALL_TO_ARMS_ANSWERED, CALL_TO_ARMS_REFUSED, DAILY.
- Filters: VICTIM_AT_WAR, VICTIM_NOT_AT_WAR, IN_OUR_TERRITORY, BY_LEADER, WITH_CASUS_BELLI, FAIR_KILL (victim in
  their faction over 24 h and hasn't paid this killer out in the last hour; join times are in memory, so a
  restart makes everyone a veteran), ONLY_NON_AGGRESSION.
- Gains are capped per key per day; costs never are. Each credit goes into a 7-day ring (`FactionCivics.income`)
  shown on the Overview tab.
- Fired from the engine listeners in `BetterFactions` (death, overheat of a root entity, system claim) and from
  `DiplomacyManager` (treaties, wars, calls to arms). DAILY runs once a day per faction holding any civic.

### Slots, picks and swaps

- Three slots to start (`STARTING_SLOTS`), +1 per strength in `civic_slot_milestones`. Re-checked every 60 s;
  slots only ratchet up, and a new one sends faction mail. Slots fill in order and are never emptied.
- One doctrine slot (`FactionCivics.DOCTRINE = -1`). Institutions take no slot.
- `Civics.pick` refuses, in order: a civic in `civics_disabled`, a missing resource, no permission, then
  `FactionCivics.problem`: slot not open, wrong kind for the slot, duplicate, excluded, requirement not held,
  locked, cooldown or cost.
- Filling an empty slot is free. A swap costs `civic_swap_cost` (Council REFORM perk applies), plus
  `civic_rush_cost` inside the `civic_swap_cooldown_days` cooldown (rush cost 0 means no rushing). Doctrines share
  cost and cooldown.
- A swap drops every civic that (transitively) required the old one. Each change is posted as governance news
  and broadcast (`CIVICS`): civics are public.
- `/faction_civic_grant <faction> <civicId> [on|off]` grants an admin-only civic.

### civics.xml

`moddata/BetterFactions/civics.xml`, read on world load and sent to clients (`CIVIC_TYPES`). A `<Civic id=…>` that
exists has its payouts replaced; a new id registers a new civic (a bare id goes under `custom:`). Children:
`<Payout event where fp dailyCap>`, `<Description>`, `<Excludes>`,
`<Requires><Condition><Type/><Value/>|<Argument/></Condition></Requires>` (HOLDS_CIVIC becomes `requires`, the rest
unlock rules). Data civics cannot set kind, resource or a handler. The admin guide has an example.

### Spice Dealer

`SpiceDealerUpkeepMixin` (`Faction.handleFactionPointExpensesServer`, tail) recomputes each system's engine upkeep
and charges `spice_dealer_upkeep_percent` of it again for each distinct Spice Dealer faction owning a face-adjacent
system. Dealers' systems come from `Faction.lastSystems` (`FactionAccessor`), so they can lag a turn. Recorded as
the SPICE_DEALER point line.

## Institutions

Adopting or dropping one is free (`Civics.setInstitution`, `INSTITUTION_SET`); dropping one calls its
`onDissolved`. Upkeep is charged each income turn by `FleetUpkeepMixin`:
`institution_upkeep_points · n(n+1)/2` for n institutions, plus one step per extra Council seat and extra charter,
less the Council TREASURY perk.

### Senate

Active while the faction holds `senate` and is not an NPC (`Senate.gates`).

- **What is voted:** proposals (including war) and breaking a held treaty; accepting or countering an offer;
  Council and CEO appointments. Withdrawing an offer, declining and civic picks are not. With a Senate any member
  may put an act to a vote, bypassing the rank check. `DiplomacyManager.goesToSenate` mirrors this on the client
  for `SenateProposeDialog`.
- **Senators:** active members whose rank sits (`senateSeats` mask, 0 = every rank; the founder always sits). The
  proposer votes yes automatically.
- **Tally:** carried as soon as yes is a majority of seats, lost as soon as it can't be; at close, yes > no carries.
  Ballots of members who left or went inactive are dropped.
- **Length:** the faction's own hours, capped at `senate_vote_max_hours`, else `senate_vote_hours`.
- **Veto:** with `senateVeto` on, a vote the leader didn't propose is held after passing until
  `max(close, now + 1 h)`; the leader assents or vetoes (`SENATE_LEADER`).
- **Carrying:** `DiplomacyManager.senateCarrier` hands the act to `Council.carry`, `Corporations.carry`, or
  re-injects the proposal with `senateApproved`.
- Rules (`SENATE_RULES`, `SenateRulesDialog`): seat mask, hours, veto, a name up to 32 characters.
- `/senate_pause [on|off]` holds passed votes (debug, not saved).
- Private to the faction (`SENATE`); saved in `senate`, acts included.

### Council

AI or member councilors who set the faction agendas and give perks while they are pleased.

- **Styles** (`CouncilLore.Flavour`): The Council (always), The Dark Shadow Council (Religious), The Board
  (Corporate Chains), Military Junta (Warrior Code). Each has its own lore, seat titles, quotes and name generator.
  A chosen style that is no longer allowed falls back, with news.
- **Seats:** roles war, peace, intrigue, calm, treasury, glory, diplomacy, expansion, reform, zeal. War, peace and
  intrigue are seated automatically (`BASE_SEATS = 3`), up to `MAX_SEATS = 7`; each extra seat costs one upkeep
  step.
- **Favour:** starts at 50. Agenda met +15, failed −15, refused −20, clashed −10 (AI seats only). At 0 the
  councilor resigns and is replaced. At 75+ (`DEVOTED`), or held by a member, the seat gives its perk:

  | Role | Perk |
  |------|------|
  | war | Declaring war −25% |
  | peace | Proposing a treaty −25% |
  | intrigue | Breaking a treaty −50% |
  | calm | Station income +10% |
  | treasury | Institution upkeep −15% |
  | glory | Fleet upkeep −10% |
  | diplomacy | Gifts −25% |
  | expansion | Fabricated claims and system claims −25% |
  | reform | Civic and doctrine swaps −25% |
  | zeal | Unjust-war surcharge −50% |

- **Agendas:** an empty seat asks within 6 h, then 1–2 days after each one settles. An AI seat draws one; a member's
  seat is offered three (its role and the next two) and picks. Objectives: DESTROY_SHIPS, DECLARE_WAR, SIGN_TREATY,
  BREAK_TREATY, KEEP_PEACE, STAY_SOLVENT, SANCTION, CLAIM_SYSTEMS, KILL_PLAYERS. Met pays the reward, failed costs
  the penalty, refused costs 2/5 of the penalty. Meeting one drops agendas that clash with it. KEEP_PEACE and
  STAY_SOLVENT are timed; the rest fail at the deadline.
- **Targets** (`Council.choose`): other factions shown in the hub, excluding the Guild and pirates, by mutual
  opinion: war below −500, sanction below −200, break below 0, sign treaties at 0 and up (thresholds in
  `Council`).
- **Directives:** `data/directives.xml`, overridden whole by `moddata/BetterFactions/directives.xml`
  (`/directives_reload`). Each sets roles, objective, count and day ranges, reward (penalty defaults to 3/5), how
  targets are weighted (OPINION, STRENGTH, RANDOM) and `<Weight>` / `<When>` modifiers on civics either side holds.
- **Pool:** four candidates are drawn daily; one can replace an AI seat (an unfinished agenda costs its refuse cost).
- **Suspension:** with no active member the Council stops; on return every deadline shifts by the time away.
- **Actions** (`COUNCIL_ACTION`): back, refuse, choose, rename council or councilor, replace from pool, style, add
  seat, retitle. Everything but back and choose needs edit rights. With a Senate, appointing, replacing and removing
  member councilors are votes (`COUNCIL_PROPOSE`).
- `/council_next` makes empty seats ask now (debug). Private (`COUNCIL`); saved in `council`.

### Corporations

Faction-chartered companies that take a cut of shop sales and pay it back as FP dividends.

- **Charters:** up to 6 (`MAX_CORPS`). The first is free (two with Corporate Chains); each further one costs an
  upkeep step. Types: Logistics, Consumer Goods, Resources, Shipyards, Scrappers, Mercenaries.
- **Holdings:** up to 8 own stations, planets and franchises. A CEO is an active member (CEO of one corp only) or an
  NPC; with a Senate, appointing one is a vote.
- **HQ:** one per sector across all factions, active after 2 days. Holdings within 3 systems earn +10% on top. A
  Shipyards corp's HQ must have a shipyard: loaded, it is checked directly; unloaded, only a station
  `ShipyardRepairs` remembers a yard on passes. Unloaded planets fail.
- **Revenue:** `corporation_shop_cut_percent` of each sale at a held shop (`ShopSaleMixin`) and of trade-network
  orders (`BuyTradeListener` / `SellTradeListener`), taken from the shop into the corp treasury.
- **Franchises:** a corp buys into a Trading Guild shop from the market (the 12 unheld Guild shops nearest home,
  outside enemy space). Fee: `franchise_base_fee`, ×2 for a Trade Station, up to ×4 more with traffic
  (`franchise_traffic_unit` of recent sales per step), ±50% by the system owner's opinion, −10% for a Consumer
  Goods corp with an HQ within 5 systems, −25% with Corporate Chains. It then earns `franchise_cut_percent` of
  each Guild sale after the Guild's `franchise_royalty_percent` royalty, minted. Giving one up refunds nothing.
- **Dividends** (each income turn, from `FleetUpkeepMixin`): the corp pays `rate`% of its revenue (from the
  treasury) as FP at `corporation_credits_per_fp`, capped at `corporation_dividend_cap_points` per corp (−1 = no
  cap).
- Holdings, HQs and franchises that vanish from the asset index are dropped every 30 s.
- Actions are `CORP_ACTION` ops 0–11 (charter, dissolve, rename, rate, assign, release, HQ, CEO, description, NPC
  CEO name, buy and give up a franchise). Charter and dissolve need edit rights, the rest edit rights or the CEO.
- Private (`CORPORATIONS`); saved in `corporations`.

### Contracts

A public board (`CONTRACTS`, saved in `contracts`). Every action needs the Corporations institution.

- Postable types: BOUNTY (on an online player of another faction), HAUL (from one own station to another) and
  SUPPLY (to an own station). ESCORT, MINE, BUILD and SALVAGE exist but cannot be posted yet.
- Each type is taken by one corp type: bounty Mercenaries, haul Logistics, supply Consumer Goods.
- A posting escrows its reward (at least 100,000) from the poster corp's treasury, runs 1–7 days, and a corp may
  have 5 posted. A haul's taker escrows the goods' list value as collateral.
- Goods are loaded and delivered by any member of the taker's faction standing at the loaded station; partial
  moves count. A bounty pays when any member of the taker's faction kills the mark.
- Lapsing returns the reward less `contract_posting_fee_percent`, taken or not. Haul collateral goes to the poster
  if goods went missing, else back to the taker.
- **NPC bounties:** `npc_bounty_kills` kills of one NPC faction's ships by one pilot within 6 h post a minted
  bounty of `npc_bounty_reward` × (standing bounties on them + 1), for 3 days.

## Not built yet

Planned in [design/CIVICS_PLAN.md](design/CIVICS_PLAN.md) and
[design/CORPORATIONS_PLAN.md](design/CORPORATIONS_PLAN.md) but absent from the code:

- Government rules (Holy Empire is a name only), a "Choose civics" rank right, Council refusal escalation,
  data-driven council styles, civic evolution, NPC presets choosing civics.
- Effects for Pacifists, Industrialist, Religious, Scavengers, Miner Guilds, Resource Specialist and the doctrines.
- Corporation market share and rivalry, Mercenaries fleet leasing, every HQ bonus but Consumer Goods', Logistics
  fees, escort/mine/build/salvage contracts, NPCs spending real money on bounties.
