package dovtech.betterfactions.faction;

import api.entity.StarPlayer;

public class BetterPlayer {

    private StarPlayer internalPlayer;
    private FactionRank rank;

    public BetterPlayer(StarPlayer internalPlayer) {
        this.internalPlayer = internalPlayer;
    }

    public FactionRank getRank() {
        return rank;
    }

    public void setRank(FactionRank rank) {
        this.rank = rank;
    }
}
