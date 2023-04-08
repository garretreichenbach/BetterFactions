package thederpgamer.betterfactions.data.old.diplomacy.peace;

import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.manager.ConfigManager;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/08/2022]
 */
public class WhitePeaceData extends PeaceTreatyData {

	public WhitePeaceData(FactionData from, FactionData to) {
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
