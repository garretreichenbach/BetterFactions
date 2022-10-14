package thederpgamer.betterfactions.data.old.diplomacy.peace;

import thederpgamer.betterfactions.data.faction.FactionData;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class AttackerPeaceOfferData extends PeaceOfferData {

	@Override
	public FactionData[] getCurrentSide() {
		return new FactionData[0];
	}

	@Override
	public FactionData[] getOpposingSide() {
		return new FactionData[0];
	}

	@Override
	public float calculateCost() {
		return 0;
	}

	@Override
	public float getCurrentProgress() {
		return 0;
	}

	@Override
	public float getOpposingProgress() {
		return 0;
	}
}
