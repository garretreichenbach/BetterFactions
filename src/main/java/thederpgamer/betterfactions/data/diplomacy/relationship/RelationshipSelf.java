package thederpgamer.betterfactions.data.diplomacy.relationship;

import thederpgamer.betterfactions.data.diplomacy.change.DiplomaticChangeData;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.manager.LogManager;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/12/2022]
 */
public class RelationshipSelf extends FactionRelationship {

	public RelationshipSelf(FactionData self) {
		super(self);
	}

	@Override
	public String getDisplayName() {
		return "Self";
	}

	@Override
	public String getDisplayFrom(FactionData from) {
		String message = self.getName() + " cannot have a relationship with itself! This should never be visible in-game!";
		LogManager.logFailure(message, true, "RelationshipSelf", "getDisplayFrom()", Thread.currentThread().getStackTrace()[1].getLineNumber());
		return message;
	}

	@Override
	public String getDisplayTo(FactionData to) {
		String message = self.getName() + " cannot have a relationship with itself! This should never be visible in-game!";
		LogManager.logFailure(message, true, "RelationshipSelf", "getDisplayTo()", Thread.currentThread().getStackTrace()[1].getLineNumber());
		return message;
	}

	@Override
	public boolean canChangeRelationTo(DiplomaticChangeData changeData) {
		return false;
	}
}
