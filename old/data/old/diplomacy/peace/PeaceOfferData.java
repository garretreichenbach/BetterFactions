package thederpgamer.betterfactions.data.old.diplomacy.peace;

import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.old.diplomacy.DiplomaticOfferData;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public abstract class PeaceOfferData extends DiplomaticOfferData {

	public abstract FactionData[] getCurrentSide();
	public abstract FactionData[] getOpposingSide();
	public abstract float calculateCost();
	public abstract float getCurrentProgress();
	public abstract float getOpposingProgress();

	public static String generateTitle(PeaceOfferData peaceOfferData) {
		return ""; //Todo
	}
}
