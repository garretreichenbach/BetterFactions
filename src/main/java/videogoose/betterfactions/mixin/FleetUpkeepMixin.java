package videogoose.betterfactions.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.FleetUpkeep;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.FactionCivics;

/**
 * Charges a player faction's fleet and institution upkeep after the engine's system upkeep and before its deficit check,
 * booked under the base rate so the engine's points dialog and {@code FactionStrength} add it up.
 */
@Mixin(value = Faction.class, remap = false)
public abstract class FleetUpkeepMixin {

	@Shadow
	public float factionPoints;
	@Shadow
	public float lastPointsSpendOnBaseRate;

	@Inject(method = "handleFactionPointExpensesServer", at = @At("TAIL"))
	private void betterFactions$fleetUpkeep(FactionManager man, Object2IntOpenHashMap<Vector3i> galaxyMapCounts, CallbackInfo ci) {
		Faction self = (Faction) (Object) this;
		int massPerPoint = BFConfig.fleetUpkeepMassPerPoint();
		if (self.isNPC()) {
			return;
		}
		float cost = massPerPoint <= 0 ? 0 : FleetUpkeep.get().upkeep(FleetUpkeep.shipsOf(self), massPerPoint);
		cost += FactionCivics.institutionUpkeep(Civics.server().of(self.getIdFaction()).institutions.size(), BFConfig.institutionUpkeep());
		factionPoints -= cost;
		lastPointsSpendOnBaseRate += cost;
	}
}
