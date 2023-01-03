package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;
import org.schema.schine.common.language.Lng;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNewsEventAlly extends FactionNewsEventOtherEnt {

	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.ALLIES;
	}

	@Override
	public String getMessage(FactionState state) {
		return Lng.str("Faction %s is now allied with %s", getOwnName(state), getOtherName(state));
	}
}
