package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNewsEventTrading extends FactionNewsEventOtherEnt {
	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.TRADING;
	}

	@Override
	public String getMessage(FactionState state) {
		return "Faction " + getOwnName(state) + " is now trading with " + getOtherName(state);
	}
}
