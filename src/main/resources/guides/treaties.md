# Treaties

Every relationship between two factions is a set of **treaties**. A pair can hold several at once (a non-aggression pact *and* a trade agreement), and whatever they hold decides whether their turrets fire on each other.

## Kinds of Treaty

| Treaty | Kind | Effect on relations |
|--------|------|---------------------|
| **War** | Declaration | Enemies. No acceptance needed |
| **Non-Aggression Pact** | Pact | Neutral |
| **Defensive Pact** | Pact | Allied |
| **Alliance** | Pact | Allied |
| **Trade Agreement** | Pact | None |
| **Intel Sharing** | Pact | None. Both factions' maps are merged every minute while it holds |
| **Vassalage** | Demand | Allied. One side answers to the other |
| **Tributary** | Demand | Neutral. One side pays the other |
| **Federation** | Declaration | Allied. Comes with federation membership and cannot be broken on its own |

- A **pact** is mutual and must be accepted.
- A **demand** is one-sided and must be accepted. NPC factions refuse every demand.
- A **declaration** takes effect the moment you make it.

If any treaty between a pair means war, they are at war. Otherwise any treaty that means allied makes them allies. Treaties that say nothing about relations leave them as they were.

## Proposing

Open **Diplomacy**, select a faction and press **PROPOSALS**. While you are at war with them, this offers peace.

1. Build the deal in the two columns, one for what each side gives. Pick something from a column's list to add it, and click it again to take it off. A pact both sides sign, like a Non-Aggression Pact, shows in both columns. One-sided treaties such as Tributary go in the column of the side that signs them, so you can offer to sign one yourself. Faction points and systems can go in either column, but nothing is required.
2. Pick a **term**: none, 1 hour, 1 day, 7 days or 30 days. Every treaty in the deal lapses after it.
3. Optionally **name** the agreement and add a **message**.

The line at the bottom previews whether an NPC faction would accept, and what is stopping it if not. Player factions decide for themselves.

Proposing a treaty the pair already holds replaces it, which is how you renew a term or rename an agreement (**RENAME** on the treaty row).

## Actions

Things you do without asking are under **ACTIONS** instead: declaring war, sanctions, and breaking a treaty. Declaring war opens a dialog to set your war goals, and shows whether you have a casus belli for them.

There are two sanctions. Only the side that imposes one can lift it, which costs nothing. The other side can ask you to lift it as part of a deal.

- **Close Trade** puts a trade embargo on the other faction. Your shops refuse their members and theirs refuse yours, no trade route runs between you, and any trade agreement you had ends (which counts as breaking it). Your shops also refuse anyone in an alliance, defensive pact or federation with them. Factions that keep trading with them lose standing with you, and with any NPC faction that thinks well of you. Lift it with **Reopen Trade**.
- **Impose Tariffs** makes your shops charge their members more and pay them less (20% by default). Trade carries on; it is a statement more than a blow, and they will resent it a little. Lift them with **Lift Tariffs**.

## Answering Offers

Offers waiting for your faction show as **OFFERS (n)** on the Overview tab and **INBOX (n)** on the Diplomacy tab. Each one can be:

- **Accepted**, which signs it.
- **Declined**.
- **Countered**, which declines it and sends your own version back.

## Breaking a Treaty

Press **ACTIONS** and pick the treaty, or expand its row on the Diplomacy tab and press **BREAK**. The other side is told, it costs you opinion with them, and they remember it.

Peace ends every treaty that was setting the relation (war, pacts, alliances) and returns the pair to neutral.

NPC factions haggle over peace terms. One losing a war gives more to end it; one winning wants paying to stop. If your offer falls short, they send back the terms they would sign instead, which you can accept or counter.

## Names

Every treaty has a name both sides see. If you do not give one, the server makes one up from the factions and their home systems.

A deal that signs several treaties is one agreement with one name, called after its most important treaty: an alliance with a trade agreement on the side is an alliance. From most to least important: Vassalage, Alliance, Tributary, Defensive Pact, Non-Aggression Pact, then Trade Agreement and Intel Sharing.

## Who May Negotiate

| Rank right | Can do |
|-----------|--------|
| **Declare war and peace** | Everything, including war and peace |
| **Sign treaties** | Every treaty except war and peace |
