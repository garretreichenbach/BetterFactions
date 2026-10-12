package videogoose.betterfactions.diplomacy;

import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.server.controller.BluePrintController;
import org.schema.game.server.data.blueprintnw.BlueprintEntry;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import org.schema.game.server.data.simulation.npc.geo.NPCEntityContainer;
import videogoose.betterfactions.mixin.NPCEntityAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Rates the NPC ships and stations that were spawned into the database before the spawn-time blueprint
 * rating existed and have never loaded since, so auto-resolve does not count them as nothing.
 *
 * <p>Each NPC system records what it spawned as blueprint name to entity id. That is enough to rate
 * every one of them from its faction's blueprint. It runs once per world, at the first military sweep;
 * anything spawned afterwards is rated at spawn by {@code BlueprintRatingMixin}.</p>
 */
public final class NpcRatingBackfill {

	private NpcRatingBackfill() {
	}

	/**
	 * Server: backfills every NPC faction. A faction whose records cannot be read (its simulation thread
	 * was mid-change, or its blueprints are missing) is skipped and logged. @return entities rated
	 */
	public static int run(FactionManager fm, CombatRatings ratings) {
		int rated = 0;
		for (Faction f : fm.getFactionCollection()) {
			if (!(f instanceof NPCFaction npc) || npc.structure == null) {
				continue;
			}
			try {
				BluePrintController blueprints = npc.getConfig().getPreset().blueprintController;
				Map<String, BlueprintEntry> byName = new HashMap<>();
				for (BlueprintEntry e : blueprints.readBluePrints()) {
					byName.put(e.getName(), e);
				}
				Map<String, List<Long>> spawned = new HashMap<>();
				npc.structure.iterateTaken(sys -> {
					for (Map.Entry<String, List<NPCEntityContainer.NPCEntity>> e : sys.getContingent().spawnedEntities.getCounts().entrySet()) {
						List<Long> ids = spawned.computeIfAbsent(e.getKey(), k -> new ArrayList<>());
						for (NPCEntityContainer.NPCEntity ent : e.getValue()) {
							ids.add(((NPCEntityAccessor) (Object) ent).betterFactions$getEntityId());
						}
					}
				});
				//with children: the record names the root, and its turrets were spawned as unrecorded entities
				rated += backfill(f.getIdFaction(), spawned, name -> {
					BlueprintEntry e = byName.get(name);
					return e != null ? CombatRatings.rateBlueprint(e.getElementCountMapWithChildren()) : Double.NaN;
				}, ratings);
			} catch (Exception e) {
				System.err.println("[BetterFactions] could not backfill ratings for NPC faction " + f.getName() + ": " + e);
			}
		}
		return rated;
	}

	/**
	 * Gives each spawned entity its blueprint's rating, unless it already has one (it loaded, or was
	 * rated at spawn). A blueprint that cannot be found ({@code NaN}) rates nothing rather than zero, so
	 * a later fix to the blueprint list still fills it in. @return entities rated
	 */
	static int backfill(int faction, Map<String, List<Long>> spawned, ToDoubleFunction<String> rateOf, CombatRatings ratings) {
		int rated = 0;
		for (Map.Entry<String, List<Long>> e : spawned.entrySet()) {
			double rating = rateOf.applyAsDouble(e.getKey());
			if (Double.isNaN(rating)) {
				continue;
			}
			for (long id : e.getValue()) {
				if (id > 0 && !ratings.isRated(id)) {
					ratings.putIfUnrated(id, faction, rating);
					rated++;
				}
			}
		}
		return rated;
	}
}
