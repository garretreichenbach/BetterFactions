package net.dovtech.betterfactions.faction;

import api.faction.Faction;
import java.util.ArrayList;

public class Organization {

    private ArrayList<Faction> memberFactions = new ArrayList<Faction>();
    private String name;
    private String description;
    private String logoFile;

    public Organization(String name, String description, Faction founder) {
        this.name = name;
        this.description = description;
        memberFactions.add(founder);
        logoFile = "defaultOrgLogo.png";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoFile() {
        return logoFile;
    }

    public void setLogoFile(String logoFile) {
        this.logoFile = logoFile;
    }

    public void addMember(Faction faction) {
        memberFactions.add(faction);
    }

    public void removeMember(Faction faction) {
        memberFactions.remove(faction);
    }

    public ArrayList<Faction> getMemberFactions() {
        return memberFactions;
    }
}