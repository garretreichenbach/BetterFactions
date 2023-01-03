package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNewsEventBattle extends FactionNewsEventOtherEnt {
	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.BATTLE;
	}

	@Override
	public String getMessage(FactionState state) {
		return "WIP"; //Todo
	}
}
