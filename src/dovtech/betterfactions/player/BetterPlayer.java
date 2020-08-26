package dovtech.betterfactions.player;

import api.entity.StarPlayer;
import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.faction.FactionRank;
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
}
