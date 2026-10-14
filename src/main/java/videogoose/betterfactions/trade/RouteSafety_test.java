package videogoose.betterfactions.trade;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.schine.resource.tag.FinishTag;
import org.schema.schine.resource.tag.Tag;

import java.util.List;

/**
 * Pins how safe a system and a route read: traffic and protection help, raids hurt, a route is its worst system,
 * and ambushes follow danger.
 *
 * <p>Run via {@code gradlew selfChecks}, or {@code java -ea} on this class.</p>
 */
public final class RouteSafety_test {

    private RouteSafety_test() {
    }

    public static void main(String[] args) {
        double empty = RouteSafety.safety(0, 0, -1);
        assert Math.abs(empty - 0.15) < 1e-9 : "nobody passes, nobody protects: barely safe";
        assert RouteSafety.safety(200, 0, -1) > empty && RouteSafety.safety(2000, 0, -1) > RouteSafety.safety(200, 0, -1) : "traffic helps, more of it more";
        assert RouteSafety.safety(0, 0, 0) > RouteSafety.safety(0, 0, 2) && RouteSafety.safety(0, 0, 2) > RouteSafety.safety(0, 0, RouteSafety.PROTECT_REACH) : "the nearer reputable space, the safer";
        assert RouteSafety.safety(0, 0, RouteSafety.PROTECT_REACH) == empty : "out of reach protects nothing";
        assert RouteSafety.safety(2000, 3, 0) < RouteSafety.safety(2000, 0, 0) && RouteSafety.safety(2000, 30, 0) < RouteSafety.safety(2000, 3, 0) : "raids hurt, more of them more";
        assert RouteSafety.safety(1e9, 0, 0) <= 1 && RouteSafety.safety(0, 1e9, -1) >= 0;

        List<Vector3i> line = RouteSafety.systemsAlong(new Vector3i(0, 0, 0), new Vector3i(4, -2, 0));
        assert line.size() == 5 && line.get(0).equals(new Vector3i(0, 0, 0)) && line.get(4).equals(new Vector3i(4, -2, 0)) : line;
        assert RouteSafety.systemsAlong(new Vector3i(1, 1, 1), new Vector3i(1, 1, 1)).size() == 1;

        assert RouteSafety.ambushChance(1, 15) == 0 && Math.abs(RouteSafety.ambushChance(0, 15) - 0.15) < 1e-9;
        assert RouteSafety.ambushChance(0.5, 15) < RouteSafety.ambushChance(0.2, 15) : "the less safe, the likelier";

        //a route is as safe as its worst system; it all survives the trip to a client
        RouteSafety rs = new RouteSafety();
        rs.fromTag(new Tag(Tag.Type.STRUCT, null, new Tag[]{
                new Tag(Tag.Type.STRUCT, null, new Tag[]{
                        new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.VECTOR3i, null, new Vector3i(2, 0, 0)),
                                new Tag(Tag.Type.DOUBLE, null, 0.0), new Tag(Tag.Type.DOUBLE, null, 10.0), FinishTag.INST}),
                        FinishTag.INST}),
                new Tag(Tag.Type.STRUCT, null, new Tag[]{new Tag(Tag.Type.VECTOR3i, null, new Vector3i(0, 0, 0)), FinishTag.INST}),
                FinishTag.INST}));
        RouteSafety back = new RouteSafety();
        back.fromTag(rs.toTag());
        assert back.systemSafety(new Vector3i(0, 0, 0)) > back.systemSafety(new Vector3i(9, 9, 9)) : "home ground protected";
        double route = back.routeSafety(new Vector3i(0, 0, 0), new Vector3i(4 * 16, 0, 0));
        assert Math.abs(route - back.systemSafety(new Vector3i(2, 0, 0))) < 1e-9 : "the raided system sets the route";
        assert RouteSafety.percent(0.734).equals("73%");
        System.out.println("RouteSafety_test: OK");
    }
}
