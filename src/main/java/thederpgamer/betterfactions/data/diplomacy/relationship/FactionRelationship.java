package thederpgamer.betterfactions.data.diplomacy.relationship;

import api.common.GameClient;
import thederpgamer.betterfactions.data.diplomacy.change.DiplomaticChangeData;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.manager.FactionDataManager;
import thederpgamer.betterfactions.utils.NetworkUtils;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [04/12/2022]
 */
public abstract class FactionRelationship {

	protected final FactionData self;

	public FactionRelationship(FactionData self) {
		this.self = self;
	}

	public abstract String getDisplayName();
	public abstract String getDisplayFrom(FactionData from);
	public abstract String getDisplayTo(FactionData to);
	public abstract boolean canChangeRelationTo(DiplomaticChangeData changeData);

	public final FactionData getSelf() {
		return self;
	}

	public final boolean isClientOwnFaction() {
		return NetworkUtils.onClient() && FactionDataManager.instance.getPlayerFaction(GameClient.getClientPlayerState()).equals(self);
	}
	/*
	public enum RelationType {
		SELF("Self", errMsg, errMsg), //This should never be visible in-game
		NEUTRAL("Neutral", "%s is neutral to us.", "%s is neutral to %s."),
		NON_AGGRESSION_PACT("Non-Aggression Pact", "%s has a non-aggression pact with us."),
		//TRADE_PACT("", ""),
		LEND_LEASE("Lend-Lease", ""),
		GUARANTEED_INDEPENDENCE("Guaranteed Independence", ""),
		ALLIANCE("Allied", ""),
		FEDERATION_MEMBERS("Federation Members", ""),
		FEDERATION_ALLY("Aligned with Federation", ""),
		ENEMY("Enemy", ""),
		AT_WAR("At war", ""),
		TRIBUTARY("Tributary", ""),
		PAYING_TRIBUTE("Paying Tribute"),
		SUBJECT("Subject", ""),
		MASTER("Master", "");

		public final String name;
		public final String relationSelf;
		public final String relationOther;


		RelationType(String name, String relationSelf, String relationOther) {
			this.name = name;
			this.relationSelf = relationSelf;
			this.relationOther = relationOther;
		}
	}

	public enum RelationModifier {

	}
	*/

}
