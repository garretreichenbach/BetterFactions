package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;
import org.schema.schine.common.language.Lng;
import thederpgamer.betterfactions.utils.SectorUtils;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNewsEventGrown extends FactionNewsEventSystem {

	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.GROWN;
	}

	@Override
	public String getMessage(FactionState state) {
		return Lng.str("Faction %s has grown its territory to system %s", getOwnName(state), SectorUtils.getSystemName(system));
	}
}
