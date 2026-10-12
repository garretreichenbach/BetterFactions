package videogoose.betterfactions.diplomacy;

import java.util.Collections;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.schema.game.common.data.player.faction.FactionRelation.RType;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;
import org.schema.schine.resource.tag.Tag.Type;

/**
 * Every treaty in force between one unordered pair of factions.
 *
 * <p>Unlike the relation byte it supersedes, a pair can hold several of these at once - a
 * non-aggression pact and a trade agreement and shared vision are three separate agreements, not
 * three points on one scale.</p>
 *
 * <h2>Why the derived relation is cached</h2>
 * {@code FactionManager.getRelation} is not a cold path. Turret target selection calls
 * {@code isEnemy} once per turret per tick, and ship AI re-checks whether its target is still
 * hostile every tick so that signing peace makes it disengage. Recomputing the strongest implied
 * relation by walking the treaty list on every one of those calls would put a loop in the combat
 * hot path for no benefit, since the answer only changes when a treaty is signed, broken or
 * expires. {@link #invalidate()} is called on each of those.
 */
public class FactionTreatySet {

    private static final byte VERSION = 0;

    private final ObjectArrayList<FactionTreaty> treaties = new ObjectArrayList<FactionTreaty>();

    private RType cachedRelation;
    private boolean cacheValid;

    public List<FactionTreaty> getTreaties() {
        return Collections.unmodifiableList(treaties);
    }

    public boolean isEmpty() {
        return treaties.isEmpty();
    }

    public int size() {
        return treaties.size();
    }

    public void add(FactionTreaty treaty) {
        treaties.add(treaty);
        invalidate();
    }

    public boolean remove(FactionTreaty treaty) {
        boolean removed = treaties.remove(treaty);
        if (removed) {
            invalidate();
        }
        return removed;
    }

    /**
     * @return the treaty removed, or null if the pair did not hold one of that type.
     */
    public FactionTreaty removeByType(String typeId) {
        for (int i = 0; i < treaties.size(); i++) {
            if (treaties.get(i).typeId.equals(typeId)) {
                FactionTreaty removed = treaties.remove(i);
                invalidate();
                return removed;
            }
        }
        return null;
    }

    public FactionTreaty get(String typeId) {
        for (int i = 0; i < treaties.size(); i++) {
            if (treaties.get(i).typeId.equals(typeId)) {
                return treaties.get(i);
            }
        }
        return null;
    }

    public boolean has(String typeId) {
        return get(typeId) != null;
    }

    /**
     * Drops anything past its expiry. @return true if something was dropped.
     */
    public boolean removeExpired(long now) {
        boolean changed = false;
        for (int i = treaties.size() - 1; i >= 0; i--) {
            if (treaties.get(i).isExpired(now)) {
                treaties.remove(i);
                changed = true;
            }
        }
        if (changed) {
            invalidate();
        }
        return changed;
    }

    public void invalidate() {
        cacheValid = false;
        cachedRelation = null;
    }

    /**
     * The legacy three-value relation these treaties add up to, or null if none of them says
     * anything about who shoots whom (a pair holding only a trade agreement is still whatever the
     * stored relation says it is).
     *
     * <p>Hostility wins over friendship: a pair at war that somehow still holds a trade agreement is
     * at war. That ordering matters because the fallback is permissive - resolving a contested pair
     * to FRIEND would make turrets hold fire during a war.</p>
     */
    public RType getDerivedRelation() {
        if (!cacheValid) {
            cachedRelation = computeRelation();
            cacheValid = true;
        }
        return cachedRelation;
    }

    private RType computeRelation() {
        RType strongest = null;
        for (int i = 0; i < treaties.size(); i++) {
            TreatyType t = treaties.get(i).getType();
            RType implied = t == null ? null : t.getImplies();
            if (implied == null) {
                continue;
            }
            if (implied == RType.ENEMY) {
                return RType.ENEMY;
            }
            if (strongest == null || implied == RType.FRIEND) {
                strongest = implied;
            }
        }
        return strongest;
    }

    public Tag toTag() {
        Tag[] t = new Tag[treaties.size() + 2];
        t[0] = new Tag(Type.BYTE, null, VERSION);
        for (int i = 0; i < treaties.size(); i++) {
            t[i + 1] = treaties.get(i).toTag();
        }
        t[t.length - 1] = FinishTag.INST;
        return new Tag(Type.STRUCT, null, t);
    }

    public void fromTag(Tag tag) {
        treaties.clear();
        invalidate();
        Tag[] t = tag.getStruct();
        for (int i = 1; i < t.length - 1; i++) {
            FactionTreaty treaty = new FactionTreaty();
            if (treaty.fromTag(t[i])) {
                treaties.add(treaty);
            } else {
                System.err.println("[FACTION][TREATY] dropping treaty of unregistered type: " + treaty.typeId);
            }
        }
    }
}
