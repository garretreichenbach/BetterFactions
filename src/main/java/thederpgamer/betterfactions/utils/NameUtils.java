package thederpgamer.betterfactions.utils;

import api.common.GameServer;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.data.Galaxy;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.federation.Federation;

import java.util.ArrayList;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/17/2022]
 */
public class NameUtils {

	public static String getSideName(FactionData[] side) {
		String name = null;
		if(side.length > 1) {
			ArrayList<Federation> federations = new ArrayList<>();
			for(FactionData factionData : side) {
				if(factionData.getFederation() != null) {
					if(!federations.contains(factionData.getFederation())) federations.add(factionData.getFederation());
				}
			}
			if(!federations.isEmpty()) { //If multiple members of a side are from the same federation, consider said federation as their side's primary entity / target
				int prevCount = 0;
				Federation primary = null;
				for(Federation federation : federations) {
					int count = 0;
					for(FactionData federationMember : federation.getMembers().values()) {
						if(ArrayUtils.contains(side, federationMember)) count ++;
					}
					if(count > prevCount) {
						prevCount = count;
						primary = federation;
					}
				}
				if(primary != null) name = primary.getNamePlural();
			} else name = side[0].getNamePlural();
		} else name = side[0].getNamePlural();
		return name;
	}

	/**
	 * Returns the name of the system at the given coordinates. Only works on server.
	 *
	 * @param systemCoords The coordinates of the system.
	 * @return The name of the system at the given coordinates.
	 */
	public static String getSystemName(Vector3i systemCoords) {
		if(GameServer.getServerState() != null) {
			Galaxy galaxy = GameServer.getUniverse().getGalaxyFromSystemPos(systemCoords);
			Vector3i relPos = Galaxy.getLocalCoordinatesFromSystem(systemCoords, new Vector3i());
			return galaxy.getName(relPos);
		} else return null;
	}

	/**
	 * Sets the name of the system at the given coordinates to a custom name. Only works on server.
	 *
	 * @param systemCoords The coordinates of the system.
	 * @param systemName The new name of the system.
	 */
	public static void setSystemName(Vector3i systemCoords, String systemName) {
		if(GameServer.getServerState() != null) {
			Galaxy galaxy = GameServer.getUniverse().getGalaxyFromSystemPos(systemCoords);
			Vector3i relPos = Galaxy.getLocalCoordinatesFromSystem(systemCoords, new Vector3i());
			long index = galaxy.getIndex(relPos.x, relPos.y, relPos.z);
			galaxy.nameCache.replace(index, systemName);
		}
	}
}
