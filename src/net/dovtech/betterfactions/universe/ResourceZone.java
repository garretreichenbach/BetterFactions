package net.dovtech.betterfactions.universe;

import api.faction.Faction;
import java.util.List;
import java.util.Map;

public class ResourceZone {

    private List<System> systems;
    private Map<Resource, Integer> resources;
    private Map<Faction, Double> influences;

    public ResourceZone(List<System> systems, Map<Resource, Integer> resources) {
        this.systems = systems;
        this.resources = resources;
    }

    public List<System> getSystems() {
        return systems;
    }

    public Map<Resource, Integer> getResources() {
        return resources;
    }

    public Map<Faction, Double> getInfluences() {
        return influences;
    }

    public void addInfluence(Faction faction, Double influence) {
        this.influences.put(faction, influence);
    }
}
