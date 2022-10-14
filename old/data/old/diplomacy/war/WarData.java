package thederpgamer.betterfactions.data.old.diplomacy.war;

import thederpgamer.betterfactions.data.faction.FactionData;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [12/15/2021]
 */
public class WarData {

    public final ConcurrentHashMap<Integer, WarParticipantData> defenders = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Integer, WarParticipantData> attackers = new ConcurrentHashMap<>();

    public WarData() {

    }

    public boolean isInvolved(FactionData factionData) {
        return defenders.containsKey(factionData.getId()) || attackers.containsKey(factionData.getId());
    }
}
