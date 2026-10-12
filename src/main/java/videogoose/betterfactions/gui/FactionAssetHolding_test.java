package videogoose.betterfactions.gui;

import org.schema.common.util.linAlg.Vector3i;
import videogoose.betterfactions.gui.FactionAssetScrollableListNew.Holding;
import videogoose.betterfactions.assets.FactionAssets;

import java.util.HashSet;
import java.util.Set;

/**
 * A holding built twice from the same faction data is the same holding.
 *
 * <p>Sounds like housekeeping and is not. {@code ScrollableTableList} refreshes by diffing the new
 * element list against the rows it already holds, keyed on {@code equals} - so an element that does
 * not equal its own twin means every row is destroyed and rebuilt on every refresh, and the player's
 * selection goes with it. The list is refreshed whenever any fleet in the game reports in, which is
 * often, so the failure reads as the panel flickering and forgetting what you clicked rather than as
 * a missing method.</p>
 *
 * <p>Run via {@code gradlew apiSelfChecks}, or {@code java -ea} on this class.</p>
 */
public final class FactionAssetHolding_test {

    private FactionAssetHolding_test() {
    }

    public static void main(String[] args) {
        rebuildingTheListFindsTheSameRows();
        aChangeTheRowWouldShowIsADifferentRow();
        System.out.println("FactionAssetHolding_test: OK");
    }

    private static FactionAssets.Asset asset(String uid, String name, Vector3i sector) {
        return new FactionAssets.Asset(uid, name, (byte) 2, sector);
    }

    private static void rebuildingTheListFindsTheSameRows() {
        Vector3i system = new Vector3i(3, 0, -2);
        Holding a = Holding.of(asset("ENTITY_SPACESTATION_Tarsis", "Tarsis Yard", new Vector3i(8, 1, 2)),
                system, true, true);
        Holding b = Holding.of(asset("ENTITY_SPACESTATION_Tarsis", "Tarsis Yard", new Vector3i(8, 1, 2)),
                new Vector3i(3, 0, -2), true, true);

        assert a.equals(b) : "an unchanged holding did not match itself across a refresh";
        assert a.hashCode() == b.hashCode() : "equal holdings hash differently, so the diff set misses them";

        //the shape the refresh actually relies on: the row set is keyed, not scanned
        Set<Holding> rows = new HashSet<Holding>();
        rows.add(a);
        assert rows.remove(b) : "the rebuilt element did not cancel out the row already on screen";

        Holding claimA = Holding.claim(new Vector3i(1, 1, 1));
        Holding claimB = Holding.claim(new Vector3i(1, 1, 1));
        assert claimA.equals(claimB) && claimA.hashCode() == claimB.hashCode();
    }

    /**
     * The other half, and the reason this is not just {@code return true}: a row that would draw
     * differently has to be rebuilt, or a renamed station keeps its old name until the panel is closed.
     */
    private static void aChangeTheRowWouldShowIsADifferentRow() {
        Vector3i system = new Vector3i(3, 0, -2);
        Holding base = Holding.of(asset("ENTITY_SPACESTATION_Tarsis", "Tarsis Yard", new Vector3i(8, 1, 2)),
                system, false, true);

        assert !base.equals(Holding.of(asset("ENTITY_SPACESTATION_Tarsis", "Tarsis Depot", new Vector3i(8, 1, 2)),
                system, false, true)) : "a rename would never reach the list";
        assert !base.equals(Holding.of(asset("ENTITY_SPACESTATION_Tarsis", "Tarsis Yard", new Vector3i(9, 1, 2)),
                system, false, true)) : "a move within the system would never reach the list";
        assert !base.equals(Holding.of(asset("ENTITY_SPACESTATION_Tarsis", "Tarsis Yard", new Vector3i(8, 1, 2)),
                system, false, false)) : "losing the claim would not recolour the row";
        assert !base.equals(Holding.claim(system)) : "a station and a bare claim are the same row";
    }
}
