# Server Administration

## Commands

Admin only. Factions are given by numeric id.

| Command | Effect |
|---------|--------|
| `/faction_treaty_sign <a> <b> <type> [hours]` | Signs a treaty between two factions as if it had been negotiated |
| `/faction_treaty_break <a> <b> <type>` | Breaks a treaty, even one that cannot normally be broken |
| `/faction_treaty_list <faction>` | Lists the treaty types and a faction's treaties |
| `/faction_emblem_clear <faction>` | Removes a faction's uploaded emblem and tells them |
| `/faction_federation_list` | Lists federations, their members and open votes |
| `/faction_federation_dissolve <id>` | Dissolves a federation |
| `/faction_civic_grant <faction> <civic> [on\|off]` | Lets a faction pick a civic that only an admin can grant, or takes that back |

Treaty types are written like `starmade:alliance`. Running `/faction_treaty_list` shows them all.

## Custom Emblems

Players can upload a PNG emblem for their faction from **Settings** when the mod config sets `custom_emblems_enabled` to `true`. It is off by default. Uploads are re-encoded by the server to 128×128 and limited to 64 KB.

## NPC Diplomacy

Each NPC archetype reads its diplomacy rules from `moddata/BetterFactions/npc/<preset>.xml`, falling back to the copy shipped in the mod and then to `default.xml`. Copy a shipped preset there to tune it. Changes apply on the next world load.

## Civics

The mod config sets `civic_slot_milestones` (the strengths that each earn a civic slot) and `civics_disabled` (civic ids nobody may pick).

`moddata/BetterFactions/civics.xml` retunes what civics pay and adds new ones. It is read on world load and sent to players when they join. A `<Civic>` with an id that already exists replaces that civic's payouts. A new id adds a civic; without a namespace it becomes `custom:<id>`.

```xml
<Civics>
  <Civic id="betterfactions:warrior_code">
    <Payout event="SHIP_DESTROYED" where="VICTIM_AT_WAR" fp="25" dailyCap="500"/>
  </Civic>
  <Civic id="salvagers" name="Salvagers">
    <Description>Ships destroyed in our territory earn faction points.</Description>
    <Requires><Condition><Type>CLAIMED_SYSTEMS_AT_LEAST</Type><Value>2</Value></Condition></Requires>
    <Excludes>betterfactions:isolationists</Excludes>
    <Payout event="SHIP_DESTROYED" where="IN_OUR_TERRITORY" fp="5" dailyCap="150"/>
  </Civic>
</Civics>
```

- **Payout:** `fp` is paid when `event` happens and every filter in `where` holds. A negative `fp` charges. `dailyCap` limits what one payout pays a faction in a day; costs are never capped.
- **Events:** `PLAYER_KILLED`, `PLAYER_LOST`, `SHIP_DESTROYED`, `SYSTEM_CLAIMED`, `SYSTEM_LOST`, `TREATY_SIGNED`, `TREATY_KEPT` (ran its full term), `TREATY_BROKEN`, `WAR_DECLARED`, `CALL_TO_ARMS_ANSWERED`, `CALL_TO_ARMS_REFUSED`, `DAILY`.
- **Filters:** `VICTIM_AT_WAR`, `VICTIM_NOT_AT_WAR`, `IN_OUR_TERRITORY`, `BY_LEADER`, `WITH_CASUS_BELLI`, `FAIR_KILL` (the victim joined their faction over a day ago and has not paid out to this faction in the last hour), `ONLY_NON_AGGRESSION`.
- **Requires:** conditions that must all hold to pick the civic: `HOLDS_CIVIC` (with `<Argument>`), `STRENGTH_AT_LEAST`, `HELD_TREATIES_AT_LEAST`, `WARS_WON_AT_LEAST`, `CLAIMED_SYSTEMS_AT_LEAST`, `ADMIN_GRANTED`.

A civic that does not parse is skipped and logged; the rest still apply.

## Save Data

Treaties, offers, federations, emblems and opinion are saved to `moddata/BetterFactions/diplomacy-<world>.tag` with the world.
