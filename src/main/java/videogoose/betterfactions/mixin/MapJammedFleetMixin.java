package videogoose.betterfactions.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gamemap.GameMapDrawer;
import org.schema.game.common.data.fleet.Fleet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import videogoose.betterfactions.fleet.FleetIntercepts;

/** Client: a radar-jamming fleet isn't drawn on the galaxy map (nor its order line) for anyone outside its faction. */
@Mixin(value = GameMapDrawer.class, remap = false)
public abstract class MapJammedFleetMixin {

	@Redirect(method = "drawFleet", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;get(J)Ljava/lang/Object;"))
	private Object betterFactions$hideJammed(Long2ObjectOpenHashMap<Fleet> cache, long id) {
		Fleet f = cache.get(id);
		return FleetIntercepts.hiddenOnMap(f, GameClientState.instance.getPlayer()) ? null : f;
	}
}
