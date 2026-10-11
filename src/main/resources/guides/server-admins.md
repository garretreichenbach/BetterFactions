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

Treaty types are written like `starmade:alliance`. Running `/faction_treaty_list` shows them all.

## Custom Emblems

Players can upload a PNG emblem for their faction from **Settings** when the mod config sets `custom_emblems_enabled` to `true`. It is off by default. Uploads are re-encoded by the server to 128×128 and limited to 64 KB.

## NPC Diplomacy

Each NPC archetype reads its diplomacy rules from `moddata/BetterFactions/npc/<preset>.xml`, falling back to the copy shipped in the mod and then to `default.xml`. Copy a shipped preset there to tune it. Changes apply on the next world load.

## Save Data

Treaties, offers, federations, emblems and opinion are saved to `moddata/BetterFactions/diplomacy-<world>.tag` with the world.
