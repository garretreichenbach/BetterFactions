package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;
import org.schema.schine.common.language.Lng;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNewsEventNonAggressionPact extends FactionNewsEventOtherEnt {

	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.NON_AGGRESSION_PACT;
	}

	@Override
	public String getMessage(FactionState state) {
		return Lng.str("Faction %s has entered a non-aggression pact with %s", getOwnName(state), getOtherName(state));
	}
}
