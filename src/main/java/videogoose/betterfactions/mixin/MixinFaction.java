package videogoose.betterfactions.mixin;

import org.schema.game.common.data.player.faction.Faction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import videogoose.betterfactions.data.persistent.faction.FactionRank;
import videogoose.betterfactions.data.persistent.federation.FactionMessage;

import java.util.ArrayList;

/**
 * Injects BetterFactions-specific fields into the game's Faction class:
 * federation membership, custom logo, messaging inbox, and custom ranks.
 */
@Mixin(value = Faction.class, remap = false)
public abstract class MixinFaction {

	@Unique
	private int bf_federationId = -1;

	@Unique
	private String bf_factionLogo = "";

	@Unique
	private ArrayList<FactionMessage> bf_inbox = new ArrayList<>();

	@Unique
	private ArrayList<FactionRank> bf_ranks = new ArrayList<>();
}
