package dovtech.betterfactions.faction;

import dovtech.betterfactions.faction.diplo.relations.FactionRelations;
import java.io.Serializable;
import java.util.ArrayList;

public class FactionData implements Serializable {

    public int factionID;
    public String allianceID;
    public FactionStats factionStats;
    public String[][] opinions;

    public FactionData(BetterFaction faction) {
        this.factionID = faction.getID();
        if(faction.getAlliance() != null) {
            this.allianceID = faction.getAlliance().getAllianceID();
        }
        this.factionStats = faction.getFactionStats();

        this.opinions = new String[faction.getRelations().keySet().size()][0];

        for(int f = 0; f < faction.getRelations().keySet().size(); f ++) {
            BetterFaction target = (BetterFaction) faction.getRelations().keySet().toArray()[f];
            FactionRelations relations = faction.getRelations().get(target);
            ArrayList<String> modifiers = new ArrayList<>();
            for(FactionRelations.RelationModifier modifier : relations.getModifiers()) {
                modifiers.add(modifier.toString());
            }
            this.opinions[f] = (String[]) modifiers.toArray();
        }
    }
}