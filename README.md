# BetterFactions

A faction overhaul for StarMade (New Foundations engine, StarLoader 2.0): real diplomacy, governments with
consequences, and wars that keep going when nobody is watching. For player and NPC factions alike.

## Features

**Diplomacy**
- Two factions can hold several treaties at once: war, non-aggression, defensive pact, alliance, trade agreement,
  intel sharing, vassalage, tributary. The treaties decide who shoots whom.
- Deals bundle treaties, faction points and concessions. NPCs counter offers with the terms they would sign.
- Embargoes and tariffs reach into shops and the trade network.
- Every faction keeps an opinion ledger. NPC factions answer offers and act on their own, from per-archetype XML
  rules.
- Gifts, fabricated claims and casus belli. A war without a just cause costs more.
- Undeclared fights become skirmishes, and galaxy-wide reputation remembers who breaks treaties.
- Federations: blocs with a chair, a charter and votes on war, peace and embargoes.

**Governance**
- Civics such as Warrior Code, Oathkeepers, Raiders, Expansionists and Spice Dealer. They pay out faction points
  for the right deeds, and a stronger faction earns more slots.
- Institutions:
  - A **Senate** that votes on war, treaties and appointments.
  - A **Council** of councilors who set agendas and grant perks while they are pleased.
  - **Corporations** that take a cut of shop sales, buy Trading Guild franchises and post contracts.

**War**
- Battles between fleets in unloaded sectors are fought for real, with the sector loaded on its own and throttled
  so the server stays responsive. Fights that wait too long are settled on paper.
- Every faction gets a battle report: who held the field, what each side lost, and a list of every ship.
- Admins get a battles profiler tab and can pause, end, visit or spectate fights.
- Destroyed ships stay behind as wrecks for a while.

**Fleets and economy**
- New fleet orders:
  - **Intercept** enemies entering a system.
  - **Escort** your convoys.
  - **Raid** others' trade routes and miners, and seize their cargo.
  - **Shipyard Repair**: rebuild ships from their blueprint, paid in components.
- Trade routes have a safety rating from traffic, protection and raids. Unsafe ones draw pirate ambushes.
- Faction points are spent on fleet upkeep and institutions, and earned from stations and corporation dividends.
- The mining bonus for a newly claimed system ramps up over time.

**Interface**
- A reworked faction panel with Overview, Assets, Governance, Diplomacy, Federation and Settings tabs.
- A rank editor grid with 2 to 10 ranks.
- Custom faction emblems, off by default.

Players: the in-game guides (**Multiplayer → Factions** and the **BetterFactions** section) explain how to play.
Their source is in [src/main/resources/guides](src/main/resources/guides).

Server admins: every number above is a key in the mod config. The **Server Administration** guide and
[docs/DEVELOPER.md](docs/DEVELOPER.md#config) list the keys and the data files you can override.

## Requirements

- StarMade 0.4.509 with StarLoader 2.0.
- An engine built from the `nf/faction-mod-hooks` branch. A stock build does not work. The mod needs these hooks
  from that branch:
  - the relation query, faction delete, diplomacy action, faction panel and client disconnect events;
  - the spare `UNDEF5-7` permission bits (`PermType.define`);
  - 2 to 10 ranks per faction;
  - guide overrides.
- Optional: Resources Reorganized, for fleet trade routes in the holdings
  list and offline repair payments from station storage.

## Building

Set `starmade_root` in [gradle.properties](gradle.properties) to your install (trailing `/`). Then:

```bash
./gradlew jar
```

The jar lands in `<starmade_root>/mods`. To compile against an engine checkout instead of the installed jar, pass
`-Pstarmade_classes=<engine>/build/classes/java/main`.

```bash
./gradlew selfChecks
```

runs every `*_test` class's `main` with `-ea`. In game, `/run_tests @videogoose.betterfactions` runs those plus the
world tests.

## Docs

- [docs/DEVELOPER.md](docs/DEVELOPER.md): how the mod is put together, with config, saves, packets, commands and
  the mod API. Start here.
  - [docs/DIPLOMACY.md](docs/DIPLOMACY.md): treaties, opinion, NPC presets, federations.
  - [docs/BATTLES.md](docs/BATTLES.md): battle tracking, soft-loading, reports, admin tools.
  - [docs/CIVICS.md](docs/CIVICS.md): civics, Senate, Council, Corporations, contracts.
  - [docs/FLEETS.md](docs/FLEETS.md): fleet orders, raids, repairs, route safety, upkeep.
- [docs/design/](docs/design): the original design and audit, written while this was engine work. Kept for its
  decisions.
