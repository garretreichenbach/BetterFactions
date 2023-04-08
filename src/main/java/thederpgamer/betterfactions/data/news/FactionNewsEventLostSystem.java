package thederpgamer.betterfactions.data.news;

import org.schema.game.server.data.FactionState;
import thederpgamer.betterfactions.utils.SectorUtils;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class FactionNewsEventLostSystem extends FactionNewsEventSystem {
	@Override
	public FactionNews.FactionNewsEventType getType() {
		return FactionNews.FactionNewsEventType.LOST_TERRITORY;
	}

	@Override
	public String getMessage(FactionState factionState) {
		return "Faction " + getOwnName(factionState) + " has lost the system " + SectorUtils.getSystemName(system);
	}
}
