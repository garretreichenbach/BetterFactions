package net.dovtech.betterfactions.faction;

import api.faction.Faction;
import net.dovtech.betterfactions.inventory.TabInventory;
import net.dovtech.betterfactions.Global;
import java.util.ArrayList;
import java.util.Map;

public class BetterFaction {

    private Faction baseFaction;
    private Federation federation;
    private ArrayList<BetterFaction> nonAggressionPacts;
    private ArrayList<BetterFaction> guarantees;
    private Map<BetterFaction, Integer> opinions;

    public BetterFaction(Faction baseFaction) {
        this.baseFaction = baseFaction;
    }

    public TabInventory getFactionInventory() {
        return Global.factionInventories.get(this);
    }

    public void joinFederation(Federation federation) {
        federation.addFaction(this);
        this.federation = federation;
    }

    public void leaveFederation() {
        federation.removeFaction(this);
        this.federation = null;
    }

    public Federation getFederation() {
        return federation;
    }

    public void addNonAggressionPact(BetterFaction targetFaction) {
        targetFaction.nonAggressionPacts.add(this);
        nonAggressionPacts.add(targetFaction);
        addOpinion(targetFaction, 30);
        targetFaction.addOpinion(this, 30);
    }

    public void breakNonAggressionPact(BetterFaction targetFaction) {
        targetFaction.nonAggressionPacts.remove(this);
        nonAggressionPacts.remove(targetFaction);
        addOpinion(targetFaction, -50);
        targetFaction.addOpinion(this, -50);
    }

    public ArrayList<BetterFaction> getNonAggressionPacts() {
        return nonAggressionPacts;
    }

    public void addGuarantee(BetterFaction targetFaction) {
        guarantees.add(targetFaction);
        addOpinion(targetFaction, 50);
        targetFaction.addOpinion(this, 50);
    }

    public void removeGuarantee(BetterFaction targetFaction) {
        guarantees.remove(targetFaction);
        addOpinion(targetFaction, -15);
        targetFaction.addOpinion(this, -30);
    }

    public ArrayList<BetterFaction> getGuarantees() {
        return guarantees;
    }

    public int getOpinion(BetterFaction targetFaction) {
        return opinions.get(targetFaction);
    }

    public void addOpinion(BetterFaction targetFaction, int amount) {
        opinions.replace(targetFaction, opinions.get(targetFaction), opinions.get(targetFaction) + amount);
    }

    public OpinionType getTotalOpinion(BetterFaction targetFaction) {
        if(opinions.get(targetFaction) <= -200) {
            return OpinionType.HOSTILE;
        } else if(opinions.get(targetFaction) > -200 && opinions.get(targetFaction) <= -100) {
            return OpinionType.ANGRY;
        }  else if(opinions.get(targetFaction) > -100 && opinions.get(targetFaction) <= -30) {
            return OpinionType.WARY;
        }  else if(opinions.get(targetFaction) > -30 && opinions.get(targetFaction) <= 30) {
            return OpinionType.NEUTRAL;
        } else if(opinions.get(targetFaction) > 30 && opinions.get(targetFaction) <= 100) {
            return OpinionType.WARM;
        }  else if(opinions.get(targetFaction) > 100 && opinions.get(targetFaction) < 200) {
            return OpinionType.FRIENDLY;
        }  else if(opinions.get(targetFaction) >= 200) {
            return OpinionType.VERY_FRIENDLY;
        }
        return null;
    }
}
