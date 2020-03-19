package net.dovtech.betterfactions.universe;

import api.element.Element;
import api.faction.Faction;
import java.util.List;
import java.util.Map;

public class ResourceZone {

    private List<System> systems;
    private Map<Element, Integer> resources;
    private Map<Faction, Double> influences;

    public ResourceZone(List<System> systems, Map<Element, Integer> resources) {
        this.systems = systems;
        this.resources = resources;
    }

    public List<System> getSystems() {
        return systems;
    }

    public Map<Element, Integer> getResources() {
        return resources;
    }

    public Map<Faction, Double> getInfluences() {
        return influences;
    }

    public void addInfluence(Faction faction, Double influence) {
        this.influences.put(faction, influence);
    }
}
