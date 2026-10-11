package videogoose.betterfactions;

import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionPermission;
import org.schema.game.common.data.player.faction.FactionPermission.PermType;
import org.schema.schine.common.language.Lng;

/**
 * The faction rank rights the mod adds. They live in the engine's spare permission bits, which the
 * founder rank already holds, so existing factions need no migration.
 */
public final class Perms {

	/** Sign or break treaties other than war - so signing a trade pact can be delegated without war. */
	public static final PermType TREATIES = PermType.UNDEF5;
	/** Speak for the faction in its federation: found one, vote, invite, leave. */
	public static final PermType FEDERATION_VOTE = PermType.UNDEF6;
	/** See the faction's holdings list. */
	public static final PermType ASSETS_VIEW = PermType.UNDEF7;

	private Perms() {
	}

	/**
	 * What a right is called. The engine's spare bits carry no name of their own, so the mod's are
	 * named here.
	 */
	public static String label(PermType type) {
		if (type == TREATIES) {
			return Lng.str("Sign Treaties");
		}
		if (type == FEDERATION_VOTE) {
			return Lng.str("Federation Vote");
		}
		if (type == ASSETS_VIEW) {
			return Lng.str("View Holdings");
		}
		return type.getName();
	}

	public static boolean has(Faction f, FactionPermission member, PermType type) {
		return f != null && member != null && f.getRoles().hasPermission(member.role, type);
	}

	/**
	 * Client: whether this client's own rank holds the right. Courtesy only - the server checks again.
	 */
	public static boolean mine(PermType type) {
		org.schema.game.client.data.GameClientState s = org.schema.game.client.data.GameClientState.instance;
		if (s == null || s.getPlayer() == null) {
			return false;
		}
		return has(s.getFactionManager().getFaction(s.getPlayer().getFactionId()), s.getPlayerName(), type);
	}

	public static boolean has(Faction f, String playerName, PermType type) {
		return f != null && playerName != null && has(f, f.getMembersUID().get(playerName), type);
	}
}
