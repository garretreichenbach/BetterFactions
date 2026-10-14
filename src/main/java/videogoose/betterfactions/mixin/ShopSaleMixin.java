package videogoose.betterfactions.mixin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.ShopInterface;
import org.schema.game.common.controller.ShoppingAddOn;
import org.schema.game.common.data.player.PlayerState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import videogoose.betterfactions.assets.FactionAssets;
import videogoose.betterfactions.civics.Corporations;

/**
 * A completed sale at a shop on a station a corporation runs: whatever the buyer paid into the shop is
 * measured around {@code buy}, and the corporation's cut moves from the shop to its treasury. Measured
 * rather than priced, so a sale that falls through (full inventory, too poor) earns nothing; an owner
 * taking from their own shop pays nothing, and so earns nothing either.
 */
@Mixin(value = ShoppingAddOn.class, remap = false)
public abstract class ShopSaleMixin {

	@Unique
	private static final ThreadLocal<long[]> betterFactions$before = ThreadLocal.withInitial(() -> new long[1]);

	@Inject(method = "buy", at = @At("HEAD"))
	private void betterFactions$beforeSale(PlayerState player, short buyItem, int quantity, ShopInterface shop, IntOpenHashSet invMod,
										   IntOpenHashSet shopHash, CallbackInfo ci) {
		betterFactions$before.get()[0] = shop.getCredits();
	}

	@Inject(method = "buy", at = @At("TAIL"))
	private void betterFactions$afterSale(PlayerState player, short buyItem, int quantity, ShopInterface shop, IntOpenHashSet invMod,
										  IntOpenHashSet shopHash, CallbackInfo ci) {
		long paid = shop.getCredits() - betterFactions$before.get()[0];
		if (paid > 0 && shop.isOnServer() && shop.getSegmentController() != null) {
			SegmentController sc = shop.getSegmentController();
			//a shop on any plate of a planet is the planet's
			String uid = FactionAssets.isPlanet(sc.getType().dbTypeId)
					? FactionAssets.planetUid(sc.getType().dbTypeId, sc.getSector(new Vector3i())) : sc.getUniqueIdentifier();
			long cut = Corporations.server().onSale(shop.getFactionId(), uid, paid);
			if (cut > 0) {
				shop.modCredits(-cut);
			}
		}
	}
}
