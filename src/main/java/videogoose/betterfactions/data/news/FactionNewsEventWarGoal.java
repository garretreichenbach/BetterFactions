package videogoose.betterfactions.data.news;

import org.schema.game.server.data.FactionState;
import org.schema.schine.common.language.Lng;


public class FactionNewsEventWarGoal extends FactionNewsEventOtherEnt {
	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.WAR_GOAL;
	}

	@Override
	public String getMessage(FactionState state) {
		return Lng.str("Faction %s is justifying a war goal against %s", getOwnName(state), getOtherName(state));
	}
}
