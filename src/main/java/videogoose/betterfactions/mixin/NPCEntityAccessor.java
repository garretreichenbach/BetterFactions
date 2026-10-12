package videogoose.betterfactions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The database id an NPC system's record of a spawned entity points at. The engine keeps it
 * package-private; the NPC rating backfill needs it to know which entity a blueprint name belongs to.
 */
@Mixin(targets = "org.schema.game.server.data.simulation.npc.geo.NPCEntityContainer$NPCEntity", remap = false)
public interface NPCEntityAccessor {

	@Accessor("entityId")
	long betterFactions$getEntityId();
}
