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

Open **Diplomacy**, select a faction and press **ACTIONS**.

1. Pick an action: *Propose* a pact, *Demand* something, or *Declare* war. While at war, **Propose: Peace** ends it.
2. Pick a **term**: none, 1 hour, 1 day, 7 days or 30 days. A treaty with a term lapses on its own.
3. Optionally **name** the agreement and add a **message**.

The line at the bottom previews whether an NPC faction would accept, and what is stopping it if not. Player factions decide for themselves.

Proposing a treaty the pair already holds replaces it, which is how you renew a term or rename an agreement (**RENAME** on the treaty row).

## Answering Offers

Offers waiting for your faction show as **OFFERS (n)** on the Overview tab and **INBOX (n)** on the Diplomacy tab. Each one can be:

- **Accepted**, which signs it.
- **Declined**.
- **Countered**, which declines it and sends your own version back.

## Breaking a Treaty

Expand the treaty row on the Diplomacy tab and press **BREAK**. The other side is told, it costs you opinion with them, and they remember it.

Peace ends every treaty that was setting the relation (war, pacts, alliances) and returns the pair to neutral.

NPC factions haggle over peace terms. One losing a war gives more to end it; one winning wants paying to stop. If your offer falls short, they send back the terms they would sign instead, which you can accept or counter.

## Names

Every treaty has a name both sides see. If you do not give one, the server makes one up from the factions and their home systems.

## Who May Negotiate

| Rank right | Can do |
|-----------|--------|
| **Declare war and peace** | Everything, including war and peace |
| **Sign treaties** | Every treaty except war and peace |
