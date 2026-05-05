package videogoose.betterfactions.data.news;

import org.schema.game.server.data.FactionState;


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
