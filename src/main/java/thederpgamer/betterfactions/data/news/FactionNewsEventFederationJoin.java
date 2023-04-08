package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;
import org.schema.schine.common.language.Lng;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNewsEventFederationJoin extends FactionNewsEventOtherEnt {

	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.FEDERATION_JOIN;
	}

	@Override
	public String getMessage(FactionState state) {
		return Lng.str("Faction %s has joined federation %s", getOwnName(state), getOtherName(state));
	}
}

