package net.dovtech.betterfactions.faction;

import java.util.List;

public class Federation {

    private String displayName;
    private List<BetterFaction> factions;
    private List<Federation> alliedFederations;
    private List<BetterFaction> alliedFactions;
    private List<Federation> nonAggressionPactFederations;
    private List<BetterFaction> nonAggressionPactFactions;
    private List<Federation> enemyFederations;
    private List<BetterFaction> enemyFactions;


    public Federation(String displayName, List<BetterFaction> factions) {
        this.displayName = displayName;
        this.factions = factions;
    }

    public void addFaction(BetterFaction faction) {
        factions.add(faction);
    }

    public void removeFaction(BetterFaction faction) {
        factions.remove(faction);
    }
}
