package dovtech.betterfactions.player;

import api.entity.StarPlayer;
import dovtech.betterfactions.contracts.Contract;
import java.util.ArrayList;

public class PlayerData {

    private String playerName;
    private ArrayList<Contract> contracts;
    private ArrayList<PlayerHistory> history;

    public PlayerData(StarPlayer player) {
        this.playerName = player.getName();
        this.contracts = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    public ArrayList<Contract> getContracts() {
        return contracts;
    }

    public ArrayList<PlayerHistory> getHistory() {
        return history;
    }

    public String getPlayerName() {
        return playerName;
    }
}