package videogoose.betterfactions.mixin;

import org.schema.game.client.view.gui.trade.GUITradeNodeScrollableList;
import org.schema.game.common.controller.ShopInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import videogoose.betterfactions.trade.RouteSafety;

/**
 * The trade network list says how safe the route from here to each station is, after its system and sector
 * ({@link RouteSafety}). The row's text knows only its own words, so the station's sector is read back out of
 * them: the system column prints the system, then the sector.
 */
@Mixin(value = GUITradeNodeScrollableList.class, remap = false)
public abstract class TradeNodeSafetyMixin {

	@Shadow
	private ShopInterface currentClosestShop;

	@ModifyArg(method = "updateListEntries", at = @At(value = "INVOKE",
			target = "Lorg/schema/schine/graphicsengine/forms/gui/newgui/GUITextOverlayTable;setTextSimple(Ljava/lang/Object;)V", ordinal = 0))
	private Object betterFactions$withSafety(Object system) {
		return RouteSafety.tradeListText(system, currentClosestShop);
	}
}
