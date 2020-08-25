package dovtech.betterfactions.faction;

import api.entity.StarPlayer;

public class BetterPlayer {

    private StarPlayer internalPlayer;
    private FactionRank rank;

    public BetterPlayer(StarPlayer internalPlayer) {
        this.internalPlayer = internalPlayer;
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
}
