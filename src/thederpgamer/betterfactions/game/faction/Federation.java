package thederpgamer.betterfactions.game.faction;

import thederpgamer.betterfactions.data.FactionData;
import thederpgamer.betterfactions.utils.FederationUtils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Federation.java
 * <Description>
 * ==================================================
 * Created 01/30/2021
 * @author TheDerpGamer
 */
public class Federation implements Serializable {

    private int id;
    private String name;
    private ArrayList<FactionData> members;

    public Federation(String name, FactionData founder) {
        this.name = name;
        this.members = new ArrayList<>();
        this.members.add(founder);
        this.id = FederationUtils.getNewId(this);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<FactionData> getMembers() {
        return members;
    }
}
