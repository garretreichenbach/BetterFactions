package thederpgamer.betterfactions.utils;

import api.common.GameServer;
import org.schema.common.util.linAlg.Vector3i;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class SectorUtils {

	public static String getSystemName(Vector3i system) {
		if(GameServer.getServerState() != null) {
			try {
				return GameServer.getServerState().getUniverse().getStellarSystemFromStellarPos(system).getName() + " [" + system.x + ":" + system.y + ":" + system.z + "]";
			} catch(IOException exception) {
				throw new RuntimeException(exception);
			}
		} else return "Unknown [" + system.x + ", " + system.y + ", " + system.z + "]";
	}
}
