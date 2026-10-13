package videogoose.betterfactions.mixin;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.faction.Faction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** A faction's claimed systems as of its last upkeep turn. */
@Mixin(value = Faction.class, remap = false)
public interface FactionAccessor {

	@Accessor("lastSystems")
	List<Vector3i> betterFactions$lastSystems();
}
