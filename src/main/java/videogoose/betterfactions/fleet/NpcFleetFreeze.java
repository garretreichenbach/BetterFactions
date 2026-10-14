package videogoose.betterfactions.fleet;

import org.schema.game.common.data.fleet.Fleet;

/**
 * Debugging: {@code /npc_fleets_frozen} holds every NPC fleet where it is, loaded or not, so a raid can be set up
 * on one. Orders still change; the fleets just don't move. Not saved: a restart thaws them.
 */
public final class NpcFleetFreeze {

	public static boolean on;

	private NpcFleetFreeze() {
	}

	public static boolean frozen(Fleet f) {
		return on && f != null && f.isAnyNPCFleet();
	}
}
