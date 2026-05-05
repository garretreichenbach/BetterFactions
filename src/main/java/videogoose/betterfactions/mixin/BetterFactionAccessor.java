package videogoose.betterfactions.mixin;

import org.schema.game.common.data.player.faction.Faction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import videogoose.betterfactions.data.persistent.faction.FactionRank;
import videogoose.betterfactions.data.persistent.federation.FactionMessage;

import java.util.ArrayList;

/**
 * Accessor interface for the BetterFactions fields injected into Faction.
 * Usage: ((BetterFactionAccessor) faction).getFederationId()
 */
@Mixin(value = Faction.class, remap = false)
public interface BetterFactionAccessor {

	@Accessor("bf_federationId")
	int getFederationId();

	@Accessor("bf_federationId")
	void setFederationId(int id);

	@Accessor("bf_factionLogo")
	String getFactionLogo();

	@Accessor("bf_factionLogo")
	void setFactionLogo(String logo);

	@Accessor("bf_inbox")
	ArrayList<FactionMessage> getInbox();

	@Accessor("bf_inbox")
	void setInbox(ArrayList<FactionMessage> inbox);

	@Accessor("bf_ranks")
	ArrayList<FactionRank> getRanks();

	@Accessor("bf_ranks")
	void setRanks(ArrayList<FactionRank> ranks);
}
