package videogoose.betterfactions.diplomacy;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.schema.game.common.data.element.ElementKeyMap;

/**
 * Run with {@code -ea}; see {@code ./gradlew selfChecks}.
 */
public class CombatRatings_test {

    public static void main(String[] args) {
        //unarmed rates nothing however big: the reason it is not mass
        assert CombatRatings.rate(0, 5000, 100000) == 0;
        //guns with nothing to protect them rate little; protection makes them count
        assert CombatRatings.rate(100, 0, 100) < CombatRatings.rate(100, 400, 1000);
        //twice as big in every respect rates twice as much
        double one = CombatRatings.rate(200, 300, 4000);
        assert Math.abs(CombatRatings.rate(400, 600, 8000) - 2 * one) < 1e-6;
        //filler buys a tenth of what shields do
        assert CombatRatings.rate(100, 0, 10000) == CombatRatings.rate(100, 1000, 0);
        //guns the reactor cannot feed buy nothing: 400 guns at quarter power rate as 100 guns
        assert Math.abs(CombatRatings.rate(400, 500, 1000, 0.25) - CombatRatings.rate(100, 500, 1000)) < 1e-6;
        assert CombatRatings.rate(100, 500, 1000, 3) == CombatRatings.rate(100, 500, 1000) : "spare power is not extra guns";

        CombatRatings r = new CombatRatings();
        r.put(1, 7, 100);
        r.put(2, 7, 50);
        r.put(3, 8, 10);
        assert r.ratingOf(2) == 50 && r.ratingOf(99) == 0 : "an entity never rated counts nothing";

        //the blueprint stand-in fills a gap and nothing more
        r.putIfUnrated(2, 7, 9999);
        assert r.ratingOf(2) == 50 : "a loaded rating is never replaced by a blueprint one";
        r.putIfUnrated(4, 8, 30);
        assert r.ratingOf(4) == 30 : "never loaded: the blueprint is all there is";
        r.put(4, 8, 25);
        assert r.ratingOf(4) == 25 : "the first real rating replaces the stand-in";
        r.putIfUnrated(4, 8, 30);
        assert r.ratingOf(4) == 25 : "and the blueprint does not come back";
        r.retain(8, new LongOpenHashSet(new long[]{3})); //drop the extra row so the totals below hold

        //a blueprint rates like the ship it spawns: weapon outputs, shield blocks, everything for bulk
        java.util.Map<Integer, Integer> bp = new java.util.HashMap<>();
        bp.put((int) ElementKeyMap.WEAPON_ID, 60);
        bp.put((int) ElementKeyMap.MISSILE_TUBE, 30);
        bp.put((int) ElementKeyMap.DAMAGE_BEAM_MODULE, 10);
        bp.put((int) ElementKeyMap.SHIELD_CAP_ID, 200);
        bp.put((int) ElementKeyMap.SHIELD_REGEN_ID, 100);
        bp.put((int) ElementKeyMap.HULL_ID, 5000);
        assert CombatRatings.rateBlocks(id -> bp.getOrDefault(id, 0), 5400) == CombatRatings.rate(100, 300, 5400)
                : "cannons, missiles and beams count; capacitors and rechargers are its shields";
        assert CombatRatings.rateBlocks(id -> id == ElementKeyMap.HULL_ID ? 5000 : 0, 5000) == 0 : "an unarmed hull rates nothing";
        assert r.recomputeTotals();
        assert r.totalOf(7) == 150 && r.totalOf(8) == 10;
        assert !r.recomputeTotals() : "nothing changed";

        //entity 2 was destroyed while unloaded
        r.retain(7, new LongOpenHashSet(new long[]{1}));
        r.recomputeTotals();
        assert r.totalOf(7) == 100 : r.totalOf(7);
        assert r.totalOf(8) == 10 : "another faction's rows are not touched";

        //entity 1 changed hands
        r.put(1, 8, 100);
        r.recomputeTotals();
        assert r.totalOf(7) == 0 && r.totalOf(8) == 110;

        CombatRatings back = new CombatRatings();
        back.fromTag(r.toTag());
        assert back.totalOf(8) == 110 : back.totalOf(8);
        //an NPC's systems ride with the totals, even for a faction with no rated ships
        assert r.setNpcSystems(-5, 12) && !r.setNpcSystems(-5, 12);
        CombatRatings client = new CombatRatings();
        client.fromTotalsTag(r.totalsTag());
        assert client.npcSystemsOf(-5) == 12 && client.totalOf(-5) == 0;
        assert client.totalOf(8) == 110 && client.ratedFactions().length == 0;

        r.removeFaction(8);
        assert r.totalOf(8) == 0;

        //one faction loses its last ship as another fields an unarmed one: still a change to send
        CombatRatings swap = new CombatRatings();
        swap.put(1, 7, 100);
        swap.recomputeTotals();
        swap.retain(7, new LongOpenHashSet());
        swap.put(2, 9, 0);
        assert swap.recomputeTotals() : "faction 7's total vanished";

        //destroyed while loaded: gone at once, not at the next sweep
        swap.put(3, 9, 40);
        swap.recomputeTotals();
        assert swap.remove(3) && !swap.remove(3);
        assert swap.recomputeTotals() && swap.totalOf(9) == 0;
        System.out.println("CombatRatings_test: OK");
    }
}
