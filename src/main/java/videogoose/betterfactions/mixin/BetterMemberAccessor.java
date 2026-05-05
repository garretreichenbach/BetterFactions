package videogoose.betterfactions.mixin;

import org.schema.game.common.data.player.faction.FactionPermission;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import videogoose.betterfactions.data.persistent.faction.FactionRank;

/**
 * Accessor interface for the BetterFactions custom rank field injected into FactionPermission.
 * Usage: ((BetterMemberAccessor) factionPermission).getCustomRank()
 */
@Mixin(value = FactionPermission.class, remap = false)
public interface BetterMemberAccessor {

	@Accessor("bf_customRank")
	FactionRank getCustomRank();

	@Accessor("bf_customRank")
	void setCustomRank(FactionRank rank);
}
