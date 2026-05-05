package videogoose.betterfactions.mixin;

import org.schema.game.common.data.player.faction.FactionPermission;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import videogoose.betterfactions.data.persistent.faction.FactionRank;

/**
 * Injects a custom rank field into the game's FactionPermission (member) class.
 * This allows BetterFactions' custom rank system to work alongside the game's
 * native 5-role bitmask system.
 */
@Mixin(value = FactionPermission.class, remap = false)
public abstract class MixinFactionPermission {

	@Unique
	private FactionRank bf_customRank;
}
