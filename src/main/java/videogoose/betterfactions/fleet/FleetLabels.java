package videogoose.betterfactions.fleet;

import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.missions.machines.states.FleetState;
import org.schema.schine.ai.stateMachines.State;
import videogoose.betterfactions.repair.ShipyardRepairs;

/**
 * Server: what a fleet's order reads in the fleet list and on the map while it is under one of the mod's orders.
 * The engine sends its state's name out on every state change; FleetMissionLabelMixin swaps ours in there.
 */
public final class FleetLabels {

	private FleetLabels() {
	}

	/** The mod's label for the fleet, or null when no mod order has it. */
	public static String of(Fleet f) {
		String l = FleetIntercepts.get().labelFor(f.dbid);
		if (l == null) {
			l = TradeFleetOrders.get().labelFor(f.dbid);
		}
		return l != null ? l : ShipyardRepairs.get().labelFor(f.dbid);
	}

	/**
	 * Sends the fleet's label out if it is not what clients have: ours while an order has it, else back to its
	 * state's own name, since an order ending does not change the fleet's state.
	 */
	public static void refresh(Fleet f) {
		String want = of(f);
		if (want == null) {
			State s = f.getMachine().getFsm().getCurrentState();
			if (!(s instanceof FleetState fs)) {
				return;
			}
			want = fs.getType().getName();
		}
		if (!want.equals(f.missionString)) {
			f.missionString = want;
			f.getFleetManager().submitMissionChangeToClients(f);
		}
	}
}
