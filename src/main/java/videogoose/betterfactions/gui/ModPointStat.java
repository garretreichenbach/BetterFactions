package videogoose.betterfactions.gui;

import org.schema.common.util.StringTools;
import org.schema.game.client.view.gui.faction.newfaction.FactionPointStat;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.common.language.Lng;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.FactionPointMods;
import videogoose.betterfactions.diplomacy.FactionStrength;

import java.util.List;

/**
 * Rows the mod adds to the engine's Faction Point Statistics list: one per {@link FactionPointMods.Line},
 * and a net income that counts them (the engine's own net row shows the galaxy radius instead).
 */
public final class ModPointStat extends FactionPointStat {

	private final Faction faction;
	/** Null for the net income row. */
	private final FactionPointMods.Line line;

	private ModPointStat(String name, String description, Faction faction, FactionPointMods.Line line) {
		super(name, description);
		this.faction = faction;
		this.line = line;
	}

	@Override
	public String getValue() {
		float v = line == null ? FactionStrength.pointSurplus(faction)
				: DiplomacyManager.client().getPointMods().get(faction.getIdFaction(), line);
		return StringTools.formatPointZero(v);
	}

	/** Replaces the engine's net income row, the last it adds, and puts the mod's lines before it. */
	public static void addTo(List<FactionPointStat> stats, Faction f) {
		if (f == null) {
			return;
		}
		String net = "Net Faction Income (without player deaths)"; //the engine's, untranslated
		if (!stats.isEmpty() && net.equals(stats.get(stats.size() - 1).name)) {
			stats.remove(stats.size() - 1);
		}
		stats.add(new ModPointStat(Lng.str("Last Fleet Upkeep"), Lng.str("Points paid for the mass of members' fleets.\nDocked ships count with what they are docked to."), f, FactionPointMods.Line.FLEET));
		stats.add(new ModPointStat(Lng.str("Last Institution Upkeep"), Lng.str("Points paid for the faction's institutions.\nEach one costs more than the one before."), f, FactionPointMods.Line.INSTITUTIONS));
		stats.add(new ModPointStat(Lng.str("Last Spice Dealer Upkeep"), Lng.str("Extra territory upkeep on systems that border\na Neighbourhood Spice Dealer's systems."), f, FactionPointMods.Line.SPICE_DEALER));
		stats.add(new ModPointStat(Lng.str("Last Station Income"), Lng.str("Points earned from owned stations.\nLarger stations earn a little more; very small ones earn nothing."), f, FactionPointMods.Line.STATIONS));
		stats.add(new ModPointStat(Lng.str("Last Tribute"), Lng.str("Tribute received from tributaries (positive)\nor paid to an overlord (negative)."), f, FactionPointMods.Line.TRIBUTE));
		stats.add(new ModPointStat(Lng.str("Last Dividends"), Lng.str("Points paid by the faction's corporations\nout of their shops' sales."), f, FactionPointMods.Line.DIVIDENDS));
		stats.add(new ModPointStat(Lng.str(net), Lng.str("Income of a faction in faction points per turn, counting\nterritory, fleet and institution upkeep, station income and tribute,\nbut not the loss of faction points when players died."), f, null));
	}
}
