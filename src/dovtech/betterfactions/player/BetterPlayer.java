package dovtech.betterfactions.player;

import api.entity.StarPlayer;
import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.faction.BetterFaction;
import dovtech.betterfactions.faction.FactionRank;
import dovtech.betterfactions.util.DataUtil;
import java.util.ArrayList;

public class BetterPlayer {

    private StarPlayer internalPlayer;
    private FactionRank rank;
    private ArrayList<Contract> contracts;

    public BetterPlayer(StarPlayer internalPlayer) {
        this.internalPlayer = internalPlayer;
        this.contracts = new ArrayList<>();
    }

    public StarPlayer getInternalPlayer() {
        return internalPlayer;
    }

    public FactionRank getRank() {
        return rank;
    }

    public void setRank(FactionRank rank) {
        this.rank = rank;
    }

    public String getName() {
        return internalPlayer.getName();
    }

    public ArrayList<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(ArrayList<Contract> contracts) {
        this.contracts = contracts;
    }

    public BetterFaction getFaction() {
        return DataUtil.getBetterFaction(internalPlayer.getFaction().getInternalFaction());
    }
}
