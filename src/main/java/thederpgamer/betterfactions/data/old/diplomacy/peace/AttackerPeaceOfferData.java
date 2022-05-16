package thederpgamer.betterfactions.data.old.diplomacy.peace;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class AttackerPeaceOfferData extends PeaceOfferData {

	@Override
	public FactionDataOld[] getCurrentSide() {
		return new FactionDataOld[0];
	}

	@Override
	public FactionDataOld[] getOpposingSide() {
		return new FactionDataOld[0];
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
