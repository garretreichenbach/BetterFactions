package videogoose.betterfactions.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.schema.game.common.data.player.faction.config.FactionPointGalaxyConfig;
import org.schema.game.common.data.player.faction.config.FactionPointSpendingConfig;
import org.schema.game.common.data.world.StellarSystem;
import org.schema.game.server.data.Galaxy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.civics.CivicRegistry;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.SpiceDealer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Charges Neighbourhood Spice Dealer upkeep after the engine's own, before its deficit check, so a
 * faction the extra pushes negative loses systems the same way. The extra is booked under the base
 * rate so the engine's points dialog and {@code FactionStrength} add it up.
 *
 * <p>Dealers' systems come from their last upkeep turn: a dealer later in this turn's order counts with
 * last turn's claims.</p>
 */
@Mixin(value = Faction.class, remap = false)
public abstract class SpiceDealerUpkeepMixin {

	@Shadow
	public float factionPoints;
	@Shadow
	public float lastPointsSpendOnBaseRate;
	@Shadow
	private List<Vector3i> lastSystems;

	@Shadow
	public abstract int getIdFaction();

	@Shadow
	public abstract String getHomebaseUID();

	@Shadow
	public abstract Vector3i getHomeSector();

	@Shadow
	private float getSpendingDisToCenter(Vector3i sysPos, int totalTakenInGalaxy) {
		return 0;
	}

	@Shadow
	private float getSpendingDisToHome(Vector3i homebase, Vector3i sysPos) {
		return 0;
	}

	@Inject(method = "handleFactionPointExpensesServer", at = @At("TAIL"))
	private void betterFactions$spiceDealer(FactionManager man, Object2IntOpenHashMap<Vector3i> galaxyMapCounts, CallbackInfo ci) {
		int percent = BFConfig.spiceDealerPercent();
		List<Integer> dealerIds = Civics.server().factionsWith(CivicRegistry.SPICE_DEALER);
		if (percent <= 0 || dealerIds.isEmpty() || lastSystems.isEmpty()) {
			return;
		}
		Map<Integer, List<Vector3i>> bySystems = new HashMap<>();
		for (int id : dealerIds) {
			Faction d = man.getFaction(id);
			if (d != null) {
				bySystems.put(id, ((FactionAccessor) d).betterFactions$lastSystems());
			}
		}
		// Same per-system upkeep as the engine's loop, which keeps no per-system figure.
		Vector3i home = getHomebaseUID().length() > 0
				? StellarSystem.getPosFromSector(getHomeSector(), new Vector3i()) : lastSystems.get(0);
		float[] upkeep = new float[lastSystems.size()];
		for (int i = 0; i < upkeep.length; i++) {
			Vector3i sys = lastSystems.get(i);
			if (FactionPointGalaxyConfig.FREE_HOMEBASE && home.equals(sys)) {
				continue;
			}
			int owned = galaxyMapCounts.getInt(Galaxy.getContainingGalaxyFromSystemPos(sys, new Vector3i()));
			upkeep[i] = FactionPointSpendingConfig.FACTION_POINTS_PER_CONTROLLED_SYSTEM
					+ getSpendingDisToCenter(sys, owned) + getSpendingDisToHome(home, sys);
		}
		float extra = SpiceDealer.extra(lastSystems, upkeep, SpiceDealer.dealerMap(bySystems), getIdFaction(), percent);
		factionPoints -= extra;
		lastPointsSpendOnBaseRate += extra;
	}
}
