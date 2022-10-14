package thederpgamer.betterfactions.data.diplomacy;

import thederpgamer.betterfactions.data.faction.FactionData;

/**
 * <Description>
 *
 * @author Garret Reichenbach
 * @version 1.0 - [05/17/2022]
 */
public interface DiplomaticRequirement {

	boolean meetsRequirements(FactionData[] from, FactionData[] to, Object... args);
	String getRequirementsDisplay(FactionData[] from, FactionData[] to, Object... args);
	String getWarName(FactionData[] from, FactionData[] to, Object... args);
}
