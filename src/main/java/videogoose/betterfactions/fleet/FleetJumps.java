package videogoose.betterfactions.fleet;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.elements.ShipManagerContainer;
import org.schema.game.common.controller.elements.jumpdrive.JumpDriveCollectionManager;
import org.schema.game.common.data.fleet.Fleet;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.server.ai.program.fleetcontrollable.FleetControllableProgram;
import org.schema.game.server.data.GameServerState;
import videogoose.betterfactions.BFConfig;

import java.util.HashSet;
import java.util.Set;

/**
 * Server: loaded fleet ships with a jump drive use it on the way to their fleet's move target. While the target is
 * at least {@link #MIN_SECTORS} away the drive auto-charges, and once charged it jumps at the target (the engine
 * caps the jump at the drive's range, so a far target takes several). Unloaded members move as before.
 *
 * <p>Ships with a player aboard, docked or overheating are left alone, as the engine's fleet move does.</p>
 */
public final class FleetJumps {

	private static final FleetJumps instance = new FleetJumps();
	private static final long CHECK_MS = 2000;
	/** Closer than this the ship just flies: a jump would land it about where flying gets it anyway. */
	static final int MIN_SECTORS = 2;

	/** Ships whose drive this turned auto-charge on for, so it can turn it back off when they arrive. */
	private final Set<Ship> charging = new HashSet<>();
	private long lastCheck;

	public static FleetJumps get() {
		return instance;
	}

	public void update(GameServerState state, long now) {
		if (now - lastCheck < CHECK_MS) {
			return;
		}
		lastCheck = now;
		Set<Ship> stillCharging = new HashSet<>();
		if (BFConfig.fleetJumpsEnabled()) {
			for (Fleet f : state.getFleetManager().fleetCache.values()) {
				Vector3i target = f.getCurrentMoveTarget();
				if (target == null) {
					continue;
				}
				for (FleetMember m : f.getMembers()) {
					if (m.getLoaded() instanceof Ship s && wantsJump(m.getSector(), target) && canJump(s)) {
						jump(s, target);
						stillCharging.add(s);
					}
				}
			}
		}
		for (Ship s : charging) {
			if (!stillCharging.contains(s)) {
				for (JumpDriveCollectionManager jd : drives(s)) {
					jd.setAutoCharging(false);
					jd.sendChargeUpdate();
				}
			}
		}
		charging.clear();
		charging.addAll(stillCharging);
	}

	static boolean wantsJump(Vector3i at, Vector3i target) {
		Vector3i d = new Vector3i(target);
		d.sub(at);
		return d.length() >= MIN_SECTORS;
	}

	private static boolean canJump(Ship s) {
		return !s.isCoreOverheating() && s.getAttachedPlayers().isEmpty() && !s.railController.isDockedOrDirty()
				&& s.getAiConfiguration().getAiEntityState().getCurrentProgram() instanceof FleetControllableProgram;
	}

	private static void jump(Ship s, Vector3i target) {
		for (JumpDriveCollectionManager jd : drives(s)) {
			if (jd.hasChargeAvailable()) {
				if (!jd.getActivationManager().isActive() && !jd.getCooldownManager().isCoolingDown(System.currentTimeMillis())) {
					s.jumpDestinationSector = new Vector3i(target);
					jd.attemptJump();
				}
				return; //one jump at a time; the other drives keep their charge for the next leg
			}
			if (!jd.isAutoCharging()) {
				jd.setAutoCharging(true);
				jd.sendChargeUpdate();
			}
		}
	}

	private static Iterable<JumpDriveCollectionManager> drives(Ship s) {
		return ((ShipManagerContainer) s.getManagerContainer()).getJumpDrive().getCollectionManagers();
	}
}
