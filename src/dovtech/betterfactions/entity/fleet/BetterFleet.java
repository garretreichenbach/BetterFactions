package dovtech.betterfactions.entity.fleet;

import api.entity.Fleet;
import api.entity.StarPlayer;
import dovtech.betterfactions.faction.BetterFaction;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import org.schema.game.server.data.PlayerNotFountException;

public class BetterFleet {

    private Fleet internalFleet;
    private BetterFaction ownerFaction;
    private Alliance ownerAlliance;
    private StarPlayer ownerPlayer;
    private FleetOwnerType ownerType;

    public BetterFleet(Fleet internalFleet) {
        this.internalFleet = internalFleet;
        this.ownerType = FleetOwnerType.PLAYER_OWNED;
        update();
    }

    public Fleet getInternalFleet() {
        return internalFleet;
    }

    public Alliance getOwnerAlliance() {
        update();
        return ownerAlliance;
    }

    public BetterFaction getOwnerFaction() {
        update();
        return ownerFaction;
    }

    public StarPlayer getOwnerPlayer() throws PlayerNotFountException {
        update();
        return ownerPlayer;
    }

    private void update() {

    }

    public enum FleetOwnerType {
        PLAYER_OWNED,
        FACTION_OWNED,
        ALLIANCE_OWNED
    }
}
