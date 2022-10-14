package thederpgamer.betterfactions.data.diplomacy;

import thederpgamer.betterfactions.data.faction.FactionData;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public interface DiplomaticModifier {
	String getModifierDisplay(FactionData[] from, FactionData[] to, Object... args);
	void applyModifier(FactionData[] from, FactionData[] to, Object... args);
}
