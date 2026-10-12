package videogoose.betterfactions.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.blueprint.ChildStats;
import org.schema.game.server.data.blueprint.SegmentControllerOutline;
import org.schema.game.server.data.blueprint.ShipOutline;
import org.schema.game.server.data.blueprint.SpaceStationOutline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.DiplomacyManager;
import videogoose.betterfactions.diplomacy.CombatRatings;

/**
 * Gives a ship or station spawned straight into the database (NPC fleets and stations, mostly) a rating
 * from its blueprint, so an auto-resolved battle does not count it as nothing. It only fills a gap:
 * the first time the entity loads, its real rating replaces this one.
 */
@Mixin(value = {ShipOutline.class, SpaceStationOutline.class}, remap = false)
public abstract class BlueprintRatingMixin {

	@Inject(method = "spawnInDatabase", at = @At("RETURN"))
	private void betterFactions$rateFromBlueprint(Vector3i sector, GameServerState state, int creatorThreadId, ObjectArrayList<String> members,
	                                              ChildStats stats, boolean purgeDuplicate, CallbackInfoReturnable<Long> cir) {
		SegmentControllerOutline<?> outline = (SegmentControllerOutline<?>) (Object) this;
		long dbId = cir.getReturnValueJ();
		if (dbId > 0 && outline.getFactionId() != 0 && outline.en != null) {
			DiplomacyManager.server().getCombatRatings().putIfUnrated(dbId, outline.getFactionId(), CombatRatings.rateBlueprint(outline.en.getElementMap()));
		}
	}
}
