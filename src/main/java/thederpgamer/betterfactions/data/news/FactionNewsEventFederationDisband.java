package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;
import org.schema.schine.common.language.Lng;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNewsEventFederationDisband extends FactionNewsEventOtherEnt {
	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.FEDERATION_DISBAND;
	}

	@Override
	public String getMessage(FactionState state) {
		return Lng.str("Federation %s has disbanded", getOtherName(state));
	}
}
