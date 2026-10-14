package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.ShopInterface;
import org.schema.game.common.controller.ShoppingAddOn;
import org.schema.game.common.data.player.AbstractOwnerState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import videogoose.betterfactions.DiplomacyManager;

/**
 * A trade embargo closes a faction's shops to the other side's members, and theirs to its own; the
 * embargoer's also to the allies of the side it embargoed ({@code DiplomacyManager#refusesTrade}).
 * {@code hasPermission} is the one gate every shop use goes through - the client's shop screen, and
 * the server's buy and sell handling alike - so refusing there refuses it everywhere, with the engine's
 * own "no permission" message. Checked before the owner list, which would otherwise wave anyone through
 * a shop with no named owners.
 */
@Mixin(value = ShoppingAddOn.class, remap = false)
public abstract class ShopEmbargoMixin {

	@Shadow
	@Final
	ShopInterface shop;

	@Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true)
	private void betterFactions$embargo(AbstractOwnerState ps, CallbackInfoReturnable<Boolean> cir) {
		if (ps != null && DiplomacyManager.of(shop.getState()).refusesTrade(shop.getFactionId(), ps.getFactionId())) {
			cir.setReturnValue(false);
		}
	}
}
