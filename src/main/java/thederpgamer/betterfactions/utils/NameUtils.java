package thederpgamer.betterfactions.utils;

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
}
