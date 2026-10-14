package videogoose.betterfactions;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.config.FactionSystemOwnerBonusConfig;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.HashMap;
import java.util.Map;

/**
 * When each system was last claimed, so the owner's mining bonus grows in over
 * {@code mining_bonus_ramp_hours} instead of rewarding claim-and-unclaim churn. A system claimed before
 * this was kept has no entry and counts as long held.
 */
public final class ClaimAges {

    private static ClaimAges instance = new ClaimAges();

    /** System position to wall-clock ms it was claimed by its current owner. */
    private final Map<Vector3i, Long> claimedAt = new HashMap<>();

    public static ClaimAges get() {
        return instance;
    }

    public static void reset() {
        instance = new ClaimAges();
    }

    public synchronized void claimed(Vector3i system, long now) {
        claimedAt.put(new Vector3i(system), now);
    }

    public synchronized void unclaimed(Vector3i system) {
        claimedAt.remove(system);
    }

    /** 0 just claimed to 1 fully held. */
    public synchronized float maturity(Vector3i system, long now) {
        Long at = claimedAt.get(system);
        long ramp = BFConfig.miningBonusRampMs();
        return at == null || ramp <= 0 ? 1 : ramp(now - at, ramp);
    }

    static float ramp(long held, long rampMs) {
        return Math.clamp((float) held / rampMs, 0f, 1f);
    }

    /** What to multiply the owner's mining bonus by: unowned bonus at maturity 0, the full owner bonus at 1. */
    public static float ownerBonusScale(float maturity) {
        float owner = FactionSystemOwnerBonusConfig.MINING_BONUS_OWNER;
        float unowned = FactionSystemOwnerBonusConfig.MINING_BONUS_UNOWNED;
        return owner <= 0 ? 1 : (unowned + (owner - unowned) * maturity) / owner;
    }

    public synchronized Tag toTag() {
        Tag[] t = new Tag[claimedAt.size() + 1];
        int i = 0;
        for (Map.Entry<Vector3i, Long> e : claimedAt.entrySet()) {
            Vector3i p = e.getKey();
            t[i++] = new Tag(Tag.Type.STRUCT, null, new Tag[]{
                    new Tag(Tag.Type.INT, null, p.x), new Tag(Tag.Type.INT, null, p.y), new Tag(Tag.Type.INT, null, p.z),
                    new Tag(Tag.Type.LONG, null, e.getValue()), FinishTag.INST});
        }
        t[i] = FinishTag.INST;
        return new Tag(Tag.Type.STRUCT, null, t);
    }

    public synchronized void fromTag(Tag tag) {
        for (Tag e : tag.getStruct()) {
            if (e.getType() == Tag.Type.STRUCT) {
                Tag[] s = e.getStruct();
                claimedAt.put(new Vector3i(s[0].getInt(), s[1].getInt(), s[2].getInt()), s[3].getLong());
            }
        }
    }
}
