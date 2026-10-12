# BetterFactions

Treaties, opinion, federations and emblems for player and NPC factions alike, for StarMade (New Foundations engine, StarLoader 2.0).

- Two factions can hold several treaties at once (war, non-aggression, defensive pact, alliance, trade, intel sharing, vassalage, tributary), and the treaties decide who shoots whom.
- Every faction keeps an opinion ledger. NPC factions accept or refuse offers, and act on their own, from per-archetype XML rules.
- Federations: blocs with a chair, a charter and votes.
- Custom faction emblems, off by default.
- Reworked faction panel: Overview, Assets, Members, Diplomacy, Federation, Settings.

Players: the in-game guides (**Multiplayer → Factions** and the **BetterFactions** section) cover everything. Their source is in [src/main/resources/guides](src/main/resources/guides).

## Requirements

The mod reaches the engine only through hooks on the engine's `nf/faction-mod-hooks` branch: the relation query, faction delete, diplomacy action, faction panel and client disconnect events, the spare `UNDEF5-7` permission bits a mod can name (`PermType.define`), 2 to 10 ranks per faction, and guide overrides. A stock build does not have them.

## Building

Set `starmade_root` in [gradle.properties](gradle.properties) to your install (trailing `/`). Then:

```bash
./gradlew jar
```

The jar lands in `<starmade_root>/mods`. To compile against an engine checkout instead of the installed jar, pass `-Pstarmade_classes=<engine>/build/classes/java/main`.

```bash
./gradlew selfChecks
```

runs every `*_test` class's `main` with `-ea`.

## Docs

- [docs/DEVELOPER.md](docs/DEVELOPER.md): how the mod is put together, config, the mod API, guides and the status of the roadmap. Start here.
- [docs/design/](docs/design): the original design and audit, written while this was engine work. Kept for its decisions.
