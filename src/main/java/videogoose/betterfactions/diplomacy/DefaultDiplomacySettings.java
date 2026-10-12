package videogoose.betterfactions.diplomacy;

import org.schema.game.server.data.simulation.npc.NPCFactionConfig;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

/**
 * Settings used by factions that carry no NPC preset - which is every player faction.
 * <p>
 * The point tables are the same vanilla defaults an NPC preset starts from
 * ({@link DiplomacyConfig#addDefaultActions()}), so a player faction's ledger is scored on the same scale
 * an NPC would use. What differs is that nothing consumes it: reactions are only evaluated for NPC
 * factions, so for player factions this is a display ledger.
 * <p>
 * Phase 5 replaces these constants with a server-editable {@code DiplomacyConfig.xml}. They deliberately do
 * not live in {@code FactionConfig.xml}: that parser throws on any tag it cannot find a field for, so
 * adding entries there would break every existing custom faction config.
 */
public final class DefaultDiplomacySettings implements DiplomacySettings {

    private static final DefaultDiplomacySettings INSTANCE = new DefaultDiplomacySettings(null, null);

    /**
     * Mirrors NPCFactionConfig's defaults so both ledgers tick on the same cadence.
     */
    private static final long TIME_BETWEEN_TURNS_MS = 60000 * 15;
    private final long STATUS_CHECK_DELAY;
    private final long TURN_EFFECT_DELAY;
    private final long TURN_EFFECT_CHANGE_DELAY;
    private final int START_POINTS;
    private final int MAX_POINTS;
    private final int MIN_POINTS;

    private final DiplomacyConfig diplomacy;

    /**
     * @param diplomacy the point tables and acceptance rules, or null for the built-in defaults
     * @param npc       the NPC preset whose timings and point range apply, or null for the defaults
     */
    public DefaultDiplomacySettings(DiplomacyConfig diplomacy, NPCFactionConfig npc) {
        if (diplomacy == null) {
            diplomacy = new DiplomacyConfig();
            diplomacy.addDefaultActions();
            //player factions read nothing else from disk, but whether they may go to war over
            //something is a server's call to make
            loadCasusBelli(diplomacy);
        }
        this.diplomacy = diplomacy;
        STATUS_CHECK_DELAY = npc != null ? npc.diplomacyStatusCheckDelay : 60000;
        TURN_EFFECT_DELAY = npc != null ? npc.diplomacyTurnEffectDelay : 60000;
        TURN_EFFECT_CHANGE_DELAY = npc != null ? npc.diplomacyTurnEffectChangeDelay : 60000;
        START_POINTS = npc != null ? npc.diplomacyStartPoints : 0;
        MAX_POINTS = npc != null ? npc.diplomacyMaxPoints : 5000;
        MIN_POINTS = npc != null ? npc.diplomacyMinPoints : -5000;
    }

    public static DefaultDiplomacySettings get() {
        return INSTANCE;
    }

    private static final Map<String, DiplomacySettings> PRESETS = new ConcurrentHashMap<>();

    /**
     * An NPC preset's settings: its own timings from the vanilla preset config, and its point
     * tables and acceptance rules from {@code moddata/BetterFactions/npc/<preset>.xml} (lowercase, e.g. {@code trading guild.xml}), falling back
     * to the copy shipped in the mod jar, then to the mod's {@code default.xml}.
     *
     * <p>Kept out of the vanilla {@code npcConfig.xml}: that parser resets the diplomacy section on a
     * version it does not know, so the new vocabulary would never survive a load there.</p>
     */
    public static DiplomacySettings forPreset(NPCFactionConfig npc) {
        //the engine already lowercases preset names from their folder; lowercased again so the file
        //lookup cannot drift if it ever stops. A jar lookup is case-sensitive, and a miss silently
        //falls back to default.xml - every archetype then speaks with one generic voice
        String name = npc.getPreset() != null && npc.getPreset().factionPresetName != null
                ? npc.getPreset().factionPresetName.toLowerCase(java.util.Locale.ENGLISH) : "default";
        return PRESETS.computeIfAbsent(name, n -> new DefaultDiplomacySettings(loadPreset(n), npc));
    }

    public static void clearPresets() {
        PRESETS.clear();
    }

    /**
     * An optional {@code moddata/BetterFactions/casus_belli.xml} whose root holds a {@code <CasusBelli>}
     * section; see {@link DiplomacyConfig#parseCasusBelli}.
     */
    private static void loadCasusBelli(DiplomacyConfig into) {
        File f = new File("moddata/BetterFactions/casus_belli.xml");
        if (!f.exists()) {
            return;
        }
        try (InputStream in = new FileInputStream(f)) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
            org.w3c.dom.Element root = doc.getDocumentElement();
            org.w3c.dom.NodeList sections = "casusbelli".equalsIgnoreCase(root.getNodeName())
                    ? null : root.getElementsByTagName("CasusBelli");
            into.parseCasusBelli(sections == null ? root : sections.getLength() > 0 ? sections.item(0) : root);
        } catch (Exception e) {
            System.err.println("[BetterFactions] could not read casus_belli.xml, keeping the defaults: " + e);
        }
    }

    private static DiplomacyConfig loadPreset(String name) {
        for (String candidate : new String[]{name, "default"}) {
            File override = new File("moddata/BetterFactions/npc/" + candidate + ".xml");
            try (InputStream in = override.exists() ? new FileInputStream(override)
                    : DefaultDiplomacySettings.class.getResourceAsStream("/data/npc/" + candidate + ".xml")) {
                if (in == null) {
                    continue;
                }
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
                DiplomacyConfig c = new DiplomacyConfig();
                c.parse(doc.getDocumentElement(), false);
                return c;
            } catch (Exception e) {
                System.err.println("[BetterFactions] could not read NPC diplomacy for " + candidate + ": " + e);
            }
        }
        return null;
    }

    @Override
    public long getDiplomacyStatusCheckDelay() {
        return STATUS_CHECK_DELAY;
    }

    @Override
    public long getDiplomacyTurnEffectDelay() {
        return TURN_EFFECT_DELAY;
    }

    @Override
    public long getDiplomacyTurnEffectChangeDelay() {
        return TURN_EFFECT_CHANGE_DELAY;
    }

    @Override
    public int getDiplomacyStartPoints() {
        return START_POINTS;
    }

    @Override
    public int getDiplomacyMaxPoints() {
        return MAX_POINTS;
    }

    @Override
    public int getDiplomacyMinPoints() {
        return MIN_POINTS;
    }

    @Override
    public boolean existsAction(DiplomacyAction.DiplActionType action) {
        return diplomacy.get(action) != null;
    }

    @Override
    public boolean existsStatus(DiplomacyEntity.DiplStatusType status) {
        return diplomacy.get(status) != null;
    }

    @Override
    public int getDiplomacyUpperLimit(DiplomacyAction.DiplActionType action) {
        return diplomacy.get(action).upperLimit;
    }

    @Override
    public int getDiplomacyLowerLimit(DiplomacyAction.DiplActionType action) {
        return diplomacy.get(action).lowerLimit;
    }

    @Override
    public int getDiplomacyValue(DiplomacyAction.DiplActionType action) {
        return diplomacy.get(action).value;
    }

    @Override
    public int getDiplomacyValue(DiplomacyEntity.DiplStatusType status) {
        return diplomacy.get(status).value;
    }

    @Override
    public DiplomacyReaction getDiplomacyActionRequired(DiplomacyAction.DiplActionType action) {
        return diplomacy.get(action).reaction;
    }

    @Override
    public int getDiplomacyExistingActionModifier(DiplomacyAction.DiplActionType action) {
        return diplomacy.get(action).existingModifier;
    }

    @Override
    public int getDiplomacyNonExistingActionModifier(DiplomacyAction.DiplActionType action) {
        return diplomacy.get(action).nonExistingModifier;
    }

    @Override
    public long getDiplomacyTurnTimeout(DiplomacyAction.DiplActionType action) {
        return (long) ((double) diplomacy.get(action).turnsActionDuration * TIME_BETWEEN_TURNS_MS);
    }

    @Override
    public long getDiplomacyStaticTimeout(DiplomacyEntity.DiplStatusType status) {
        return (long) ((double) diplomacy.get(status).staticTimeoutTurns * TIME_BETWEEN_TURNS_MS);
    }

    @Override
    public long getDiplomacyActionTimeout(DiplomacyAction.DiplActionType action) {
        if (diplomacy.actionTimeoutMap.containsKey(action)) {
            return diplomacy.actionTimeoutMap.getLong(action);
        }
        return TURN_EFFECT_CHANGE_DELAY * 3;
    }

    @Override
    public List<DiplomacyReaction> getDiplomacyReactions() {
        return diplomacy.reactions;
    }

    @Override
    public DiplomacyConditionGroup getDiplomacyAcceptance(String treatyId) {
        return diplomacy.getAcceptance(treatyId);
    }

    @Override
    public String getDiplomacyRefusal(String treatyId) {
        return diplomacy.getRefusal(treatyId);
    }

    @Override
    public DiplomacyConditionGroup getCasusBelli(SettlementTerm.Kind kind) {
        return diplomacy.casusBelli.get(kind);
    }

    @Override
    public String getCasusBelliRefusal(SettlementTerm.Kind kind) {
        return diplomacy.casusBelliRefusals.get(kind);
    }

    @Override
    public double getDiplomacyPowerSurplusWeight() {
        return diplomacy.powerSurplusWeight;
    }

    @Override
    public double getDiplomacyPowerSystemWeight() {
        return diplomacy.powerSystemWeight;
    }

    @Override
    public double getDiplomacyPowerMemberWeight() {
        return diplomacy.powerMemberWeight;
    }

    @Override
    public double getDiplomacyPowerMilitaryWeight() {
        return diplomacy.powerMilitaryWeight;
    }

    @Override
    public double getSettlementPeaceValue() {
        return diplomacy.settlementPeaceValue;
    }

    @Override
    public double getSettlementTreatyValue() {
        return diplomacy.settlementTreatyValue;
    }
}
