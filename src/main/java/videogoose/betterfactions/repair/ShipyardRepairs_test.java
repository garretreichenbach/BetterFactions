package videogoose.betterfactions.repair;

import org.schema.common.util.linAlg.Vector3i;

/** The repair queue survives a save, and a load puts each ship back at a step it can resume from. Run with -ea. */
public final class ShipyardRepairs_test {

	public static void main(String[] args) {
		ShipyardRepairs r = new ShipyardRepairs();
		ShipyardRepairs.Yard y = new ShipyardRepairs.Yard("ENTITY_SPACESTATION_Yard", "Yard", 5, new Vector3i(1, 2, 3));
		y.end = -1;
		String[] names = {"holding", "repairing", "leaving", "design"};
		ShipyardRepairs.Step[] steps = {ShipyardRepairs.Step.HOLDING, ShipyardRepairs.Step.REPAIRING, ShipyardRepairs.Step.LEAVING, ShipyardRepairs.Step.DESIGN};
		for (int i = 0; i < names.length; i++) {
			ShipyardRepairs.Entry e = new ShipyardRepairs.Entry(7, i, "ENTITY_SHIP_" + names[i], names[i], "Falcon");
			e.step(steps[i]);
			e.oldDbId = 100 + i;
			y.queue.add(e);
		}
		y.fleets.add(7L);
		r.yards.put(y.stationUid, y);
		r.yardIndexByStation.put(y.stationUid, 123456789L);
		ShipyardRepairs.Entry unloaded = new ShipyardRepairs.Entry(7, 4, "ENTITY_SHIP_unloaded", "unloaded", "Falcon");
		unloaded.step(ShipyardRepairs.Step.OFFLINE);
		y.queue.add(unloaded);

		ShipyardRepairs back = new ShipyardRepairs();
		back.fromTag(r.toTag());
		ShipyardRepairs.Yard b = back.yards.get("ENTITY_SPACESTATION_Yard");
		assert b != null && b.faction == 5 && b.end == -1 && b.sector.equals(new Vector3i(1, 2, 3)) && b.fleets.contains(7L);
		assert b.queue.size() == 4 : "a ship that was leaving is done";
		assert back.yardIndexByStation.get("ENTITY_SPACESTATION_Yard") == 123456789L : "where unloaded repairs pay from";
		assert b.queue.get(3).step == ShipyardRepairs.Step.EN_ROUTE : "unloaded repair, unpaid: starts over";
		assert b.queue.get(0).step == ShipyardRepairs.Step.EN_ROUTE : "in line: lines up again when seen";
		assert b.queue.get(1).step == ShipyardRepairs.Step.REPAIRING && b.queue.get(1).sawWork && b.queue.get(1).oldDbId == 101 : "repair in progress kept";
		assert b.queue.get(2).step == ShipyardRepairs.Step.EN_ROUTE && b.queue.get(2).fleetIndex == 3 && "Falcon".equals(b.queue.get(2).blueprint);

		javax.vecmath.Vector3f v = ShipyardRepairs.Approach.towards(new javax.vecmath.Vector3f(0, 0, 0), new javax.vecmath.Vector3f(10, 0, 0), 0.5f);
		assert Math.abs(v.x - 0.5f) < 1e-5f : "lead eases on, never jumps to the wanted velocity";
		v = ShipyardRepairs.Approach.towards(new javax.vecmath.Vector3f(9.8f, 0, 0), new javax.vecmath.Vector3f(10, 0, 0), 0.5f);
		assert Math.abs(v.x - 10) < 1e-5f : "and lands on it once within a step";
		javax.vecmath.Vector3f o = new javax.vecmath.Vector3f(), x = new javax.vecmath.Vector3f(100, 0, 0);
		assert Math.abs(ShipyardRepairs.distanceToSegment(new javax.vecmath.Vector3f(50, 7, 0), o, x) - 7) < 1e-4f : "beside the corridor";
		assert Math.abs(ShipyardRepairs.distanceToSegment(new javax.vecmath.Vector3f(110, 0, 0), o, x) - 10) < 1e-4f : "past its end";
		assert Math.abs(ShipyardRepairs.distanceToSegment(new javax.vecmath.Vector3f(-3, 4, 0), o, x) - 5) < 1e-4f : "behind the ship";
		System.out.println("ShipyardRepairs_test: OK");
	}
}
