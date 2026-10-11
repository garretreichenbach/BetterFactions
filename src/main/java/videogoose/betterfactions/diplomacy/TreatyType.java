package videogoose.betterfactions.diplomacy;

import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.language.Lng;

/**
 * A kind of agreement two factions can hold.
 *
 * <p>Deliberately a registry object rather than an enum, mirroring
 * {@code PlanetTypeConfig} and {@code UniversalRegistry.RegistryType}: mods contribute types, and
 * types are persisted by id, so neither a fixed set nor a stable ordering can be assumed. Ids are
 * namespaced ({@code starmade:alliance}, {@code mymod:tribute}) and
 * {@link TreatyTypeRegistry} refuses a bare one.</p>
 *
 * <h2>Relation to {@code RType}</h2>
 * The engine's ~19 consumers of faction relation - turret target selection, ship AI engage and
 * disengage, beams, mines, shop permissions, trade nodes, fleet missions, HUD colour, map drawing -
 * all speak {@link FactionRelation.RType}, a three-value enum. Treaties do not replace that; they feed it.
 * {@link #implies} is how a treaty collapses into the legacy answer, and a pair's strongest implied
 * relation wins. A type with a null {@code implies} (a trade agreement, shared vision) changes what
 * is permitted without changing whether anyone shoots.
 */
public final class TreatyType {

    private final String id;
    private final String registeredBy;
    private final String displayName;
    private final boolean symmetric;
    private final FactionRelation.RType implies;
    private final boolean needsAcceptance;
    private final boolean breakable;
    private final int opinionModifier;
    private final String[] nameTemplates;

    TreatyType(String id, String registeredBy, String displayName, boolean symmetric, FactionRelation.RType implies, boolean needsAcceptance, boolean breakable, int opinionModifier, String[] nameTemplates) {
        this.nameTemplates = nameTemplates;
        this.id = id;
        this.registeredBy = registeredBy;
        this.displayName = displayName;
        this.symmetric = symmetric;
        this.implies = implies;
        this.needsAcceptance = needsAcceptance;
        this.breakable = breakable;
        this.opinionModifier = opinionModifier;
    }


    /**
     * @see Kind
     */
    public Kind getKind() {
        if (!needsAcceptance) {
            return Kind.DECLARATION;
        }
        return symmetric ? Kind.PACT : Kind.DEMAND;
    }

    /**
     * The verb a player uses for this: "Propose", "Demand" or "Declare".
     */
    public String getVerb() {
        switch (getKind()) {
            case DECLARATION:
                return Lng.str("Declare");
            case DEMAND:
                return Lng.str("Demand");
            default:
                return Lng.str("Propose");
        }
    }

    /**
     * What kind of thing this is to the player, as opposed to what it is to the engine.
     *
     * <p>Everything here is stored the same way - a record between two factions, which is what lets
     * {@code getRelation} have one code path - but calling all of it a "treaty" is wrong in the
     * player's mouth. You do not propose a war, and being made somebody's vassal is not an agreement
     * you strike between equals. The distinction is not a new field: it already follows from whether
     * the other side gets a say ({@link #needsAcceptance}) and whether the two sides end up in the
     * same position ({@link #symmetric}).</p>
     */
    public enum Kind {
        /**
         * Agreed between equals, and both sides take on the same obligation. A pact, an alliance, a
         * trade agreement: the things that are genuinely treaties.
         */
        PACT,
        /**
         * Agreed, but one side ends up under the other. Vassalage, tribute - offered as a demand,
         * accepted as a submission, and never a meeting of equals however politely it is worded.
         */
        DEMAND,
        /**
         * Not agreed at all. War is declared; the other side is informed, not asked.
         */
        DECLARATION,
    }

    /**
     * Namespaced, stable, and what gets written to disk and the wire.
     */
    public String getId() {
        return id;
    }

    /**
     * Namespace the id was registered under - {@code starmade} for vanilla, else a mod id.
     */
    public String getRegisteredBy() {
        return registeredBy;
    }

    /**
     * Patterns the engine names an unnamed treaty of this type from; see {@link TreatyNames} for the
     * tokens. Empty means "just use the display name", which is what an agreement nobody signs
     * directly wants.
     */
    public String[] getNameTemplates() {
        return nameTemplates;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * False for a treaty where the two sides hold different roles - an overlord is not a vassal.
     * An asymmetric treaty keeps its direction in the record's from/to, not in the storage key.
     */
    public boolean isSymmetric() {
        return symmetric;
    }

    /**
     * The legacy relation this treaty forces, or null if it does not change who shoots whom.
     */
    public FactionRelation.RType getImplies() {
        return implies;
    }

    /**
     * False for a treaty that takes effect the moment it is declared rather than when it is agreed.
     * War is the one that matters: you do not ask permission to declare it.
     */
    public boolean needsAcceptance() {
        return needsAcceptance;
    }

    /**
     * Whether a signatory may walk away from it unilaterally.
     */
    public boolean isBreakable() {
        return breakable;
    }

    /**
     * Opinion the ledger carries while this treaty is held.
     */
    public int getOpinionModifier() {
        return opinionModifier;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TreatyType && id.equals(((TreatyType) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Fluent construction; {@link TreatyTypeRegistry#register} takes the finished builder.
     */
    public static final class Builder {
        private final String name;
        private String displayName;
        private boolean symmetric = true;
        private FactionRelation.RType implies;
        private boolean needsAcceptance = true;
        private boolean breakable = true;
        private int opinionModifier;
        private String[] nameTemplates = new String[0];

        public Builder(String name) {
            this.name = name;
            displayName = name;
        }

        public Builder displayName(String v) {
            displayName = v;
            return this;
        }

        public Builder asymmetric() {
            symmetric = false;
            return this;
        }

        public Builder implies(FactionRelation.RType v) {
            implies = v;
            return this;
        }

        public Builder unilateral() {
            needsAcceptance = false;
            return this;
        }

        public Builder unbreakable() {
            breakable = false;
            return this;
        }

        public Builder opinion(int v) {
            opinionModifier = v;
            return this;
        }

        /**
         * Templates for naming an unnamed treaty of this type. More than one is worth having: the
         * same two factions signing the same kind of agreement twice should not produce the same
         * name twice. See {@link TreatyNames} for the tokens.
         */
        public Builder names(String... v) {
            nameTemplates = v;
            return this;
        }

        TreatyType build(String namespace) {
            return new TreatyType(namespace + ":" + name, namespace, displayName, symmetric, implies, needsAcceptance, breakable, opinionModifier, nameTemplates);
        }

        String getName() {
            return name;
        }
    }
}
