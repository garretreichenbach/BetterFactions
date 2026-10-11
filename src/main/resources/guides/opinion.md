# Opinion and NPC Factions

Every faction keeps an **opinion** of every other faction and of every lone player, from -5000 to 5000. Select a faction on the **Diplomacy** tab to see what it thinks of your faction and of you, and the list of modifiers behind the number.

Between player factions opinion is for show: it never stops you doing anything. **NPC factions** act on it.

## What Moves Opinion

Things you **do** add a modifier that grows while you keep doing them and fades once you stop:

| Action | Effect |
|--------|--------|
| Attacking them | Down, to -500 |
| Attacking their enemies | Up, to +500 |
| Declaring war | -1000 |
| Mining in their territory | -30 |
| Trading with them | Up, to +300 |
| Trading with their enemies | Down, to -200 |
| Breaking a treaty with them | -300 |

Things that are **true** add a flat modifier while they hold:

| Status | Effect |
|--------|--------|
| At war with them | -10000 |
| Allied with them | +1000 |
| At war with their enemies | +500 each |
| Allied with their enemies | -500 each |
| Not attacking them | +1 |
| Relative power | + or - with how much stronger you are |
| Trust | Grows with hours of treaties held |

## Power

A faction's **power** is shown on the Overview and Diplomacy tabs. It counts:

- **Territory**: claimed systems.
- **People**: members, with inactive members counting a quarter.
- **Income**: faction point surplus per period.

Ships are not counted yet. NPC archetypes weigh the three parts differently.

## How NPC Factions Answer

An NPC faction answers an offer the moment it arrives. Each kind of NPC has its own rules: how much it must like you, how long you must have been at peace, whether you share an enemy. If it says no, it tells you why by faction mail. Use the preview line in the propose dialog to check before you send.

NPC factions also act on their own: they offer peace, propose pacts, break treaties and send warnings as their opinion of you changes.

| Archetype | Temperament |
|-----------|-------------|
| **Trading Guild** | Forgiving and eager to trade. Hard to win as an ally |
| **Scavengers** | Quick to deal, and they side with whoever is stronger |
| **Outcasts** | Slow to trust, never trade, and allow only one alliance |

NPC factions will never become anyone's vassal or tributary.
