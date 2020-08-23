package dovtech.betterfactions.faction;

import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.schine.network.StateInterface;

public class BetterFaction extends Faction {

    private Alliance alliance;

    public BetterFaction(StateInterface stateInterface) {
        super(stateInterface);
    }

    public Alliance getAlliance() {
        return alliance;
    }

    public void setAlliance(Alliance alliance) {
        this.alliance = alliance;
    }
}
