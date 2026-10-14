package videogoose.betterfactions.mixin;

import org.schema.game.common.data.world.RemoteSector;
import org.schema.game.common.data.world.Sector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.battle.BattleTracker;

import javax.vecmath.Vector3f;

/**
 * Nothing drops into a soft-loaded battle sector. Every item spawned into space goes through
 * {@code RemoteSector.addItem}: shot-up cargo, removed inventories, pirate loot. Those callers take the
 * items out of the inventory before calling, so in a battle nobody is watching, lost cargo is simply
 * lost rather than left floating for a sector that will unload anyway. Cargo bleed and shipyard
 * deconstruction in such a sector lose their items the same way, unless a fleet raiding trade there has
 * room for them ({@code TradeFleetOrders.catchDrop}).
 */
@Mixin(value = RemoteSector.class, remap = false)
public abstract class SoftLootMixin {

	@Inject(method = "addItem(Ljavax/vecmath/Vector3f;SII)V", at = @At("HEAD"), cancellable = true)
	private void betterFactions$noLootWhenUnwatched(Vector3f pos, short type, int meta, int count, CallbackInfo ci) {
		Sector sector = ((RemoteSector) (Object) this).getServerSector();
		if (sector != null && BattleTracker.isSoftLoaded(sector.getId())) {
			//a raid there takes what its holds fit instead
			videogoose.betterfactions.fleet.TradeFleetOrders.get().catchDrop(sector, type, count);
			ci.cancel();
		}
	}
}
