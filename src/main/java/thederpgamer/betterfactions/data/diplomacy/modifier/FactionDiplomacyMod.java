package thederpgamer.betterfactions.data.diplomacy.modifier;

public abstract class FactionDiplomacyMod {
	public abstract String getName();
	public abstract boolean isStatic();
	public abstract int getValue();
	public int getFrequency() {
		return 0;
	}
}