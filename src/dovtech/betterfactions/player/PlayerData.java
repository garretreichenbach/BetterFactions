package dovtech.betterfactions.player;

import java.io.Serializable;

public class PlayerData implements Serializable {

    public int playerID;
    public String[] contracts;

    public PlayerData(BetterPlayer player) {
        this.playerID = player.getInternalPlayer().getId();
        this.contracts = new String[player.getContracts().size()];
        for(int i = 0; i < contracts.length; i ++) {
           contracts[i] = player.getContracts().get(i).getUid();
        }
    }
}