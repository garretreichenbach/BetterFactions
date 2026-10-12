package videogoose.betterfactions.mixin;

import org.schema.game.common.controller.database.tables.Table;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.sql.Connection;

/** The world database connection behind a table, for the one query the engine has no method for. */
@Mixin(value = Table.class, remap = false)
public interface TableAccessor {

	@Accessor("c")
	Connection betterFactions$connection();
}
