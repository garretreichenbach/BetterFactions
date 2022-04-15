package thederpgamer.betterfactions.data.diplomacy.relationship;

import thederpgamer.betterfactions.data.diplomacy.change.DiplomaticChangeData;
import thederpgamer.betterfactions.data.faction.FactionData;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/12/2022]
 */
public class RelationshipNeutral extends FactionRelationship {

	public RelationshipNeutral(FactionData self) {
		super(self);
	}

	@Override
	public String getDisplayName() {
		return "Neutral";
	}

	@Override
	public String getDisplayFrom(FactionData from) {
		if(isClientOwnFaction()) return "We are neutral to " + from.getName() + ".";
		else return self.getName() + " is neutral to " + from.getName();
	}

	@Override
	public String getDisplayTo(FactionData to) {
		if(isClientOwnFaction()) return to.getName() + " is neutral to us.";
		else return to.getName() + " is neutral to " + self.getName() + ".";
	}

	@Override
	public boolean canChangeRelationTo(DiplomaticChangeData changeData) {
		return false;
	}
}
