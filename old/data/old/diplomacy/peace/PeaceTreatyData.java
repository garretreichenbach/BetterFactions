package thederpgamer.betterfactions.data.old.diplomacy.peace;

import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.old.diplomacy.DiplomaticData;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public abstract class PeaceTreatyData extends DiplomaticData {

	public PeaceTreatyData(FactionData from, FactionData to) {
		super(from, to);
	}

	public abstract boolean canAdd(PeaceOfferData peaceOfferData);
	public abstract float getBaseCost();
}
