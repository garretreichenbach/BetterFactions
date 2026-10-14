package videogoose.betterfactions.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.database.DatabaseEntry;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import videogoose.betterfactions.BFConfig;
import videogoose.betterfactions.BetterFactions;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.FactionPointMods;
import videogoose.betterfactions.FleetUpkeep;
import videogoose.betterfactions.assets.FactionAssets;
import videogoose.betterfactions.civics.Civics;
import videogoose.betterfactions.civics.Council;
import videogoose.betterfactions.civics.FactionCivics;

/**
 * Charges a player faction's fleet and institution upkeep and pays its station income after the engine's system
 * upkeep and before its deficit check. Each is recorded in {@link FactionPointMods} for the points screen and
 * {@code FactionStrength}.
 */
@Mixin(value = Faction.class, remap = false)
public abstract class FleetUpkeepMixin {

	@Shadow
	public float factionPoints;

	@Inject(method = "handleFactionPointExpensesServer", at = @At("TAIL"))
	private void betterFactions$fleetUpkeep(FactionManager man, Object2IntOpenHashMap<Vector3i> galaxyMapCounts, CallbackInfo ci) {
		Faction self = (Faction) (Object) this;
		int massPerPoint = BFConfig.fleetUpkeepMassPerPoint();
		if (self.isNPC()) {
			return;
		}
		int id = self.getIdFaction();
		Council council = Council.server();
		float fleet = council.scale(id, Council.GLORY, massPerPoint <= 0 ? 0 : FleetUpkeep.get().upkeep(FleetUpkeep.shipsOf(self), massPerPoint));
		float institutions = council.institutionUpkeep(id, Civics.server().of(id), BFConfig.institutionUpkeep());
		float stations = council.scale(id, Council.CALM, betterFactions$stationIncome(id));
		factionPoints += stations - fleet - institutions;
		FactionPointMods mods = DiplomacyManager.of(true).getPointMods();
		mods.set(id, FactionPointMods.Line.FLEET, -fleet);
		mods.set(id, FactionPointMods.Line.INSTITUTIONS, -institutions);
		mods.set(id, FactionPointMods.Line.STATIONS, stations);
	}

	@Unique
	private static float betterFactions$stationIncome(int factionId) {
		int base = BFConfig.stationIncome();
		BetterFactions bf = BetterFactions.getInstance();
		FactionAssets assets = base <= 0 || bf == null || bf.getAssetIndex() == null ? null : bf.getAssetIndex().get(factionId);
		if (assets == null) {
			return 0;
		}
		float income = 0;
		for (FactionAssets.Asset a : assets.assets) {
			if (a.type == SimpleTransformableSendableObject.EntityType.SPACE_STATION.dbTypeId) {
				double mass = a.mass;
				if (mass < 0) {
					try {
						mass = FleetUpkeep.get().massOf(DatabaseEntry.getWithFilePrefix(a.uid, a.type)); //keyed by full entity UID
					} catch (DatabaseEntry.EntityTypeNotFoundException e) {
						continue;
					}
				}
				income += FleetUpkeep.stationIncome(mass, base, BFConfig.stationIncomeMinMass());
			}
		}
		return income;
	}
}
