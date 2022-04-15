package thederpgamer.betterfactions.data.old.diplomacy.peace;

import thederpgamer.betterfactions.data.old.faction.FactionDataOld;
import thederpgamer.betterfactions.manager.ConfigManager;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class WhitePeaceData extends PeaceTreatyData {

	public WhitePeaceData(FactionDataOld from, FactionDataOld to) {
		super(from, to);
	}

	@Override
	public boolean canAdd(PeaceOfferData peaceOfferData) {
		return peaceOfferData.getDiploMap().isEmpty() && peaceOfferData.calculateCost() <= getBaseCost() && peaceOfferData.getOpposingProgress() <= ConfigManager.getDiploConfig().getConfigurableFloat("white-peace-opposing-side-progress-limit", 60.0f);
	}

	@Override
	public float getBaseCost() {
		return ConfigManager.getDiploConfig().getConfigurableFloat("white-peace-base-cost", 15.0f);
	}
}
