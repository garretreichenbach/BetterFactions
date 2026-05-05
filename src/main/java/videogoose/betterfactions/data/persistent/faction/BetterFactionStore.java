package videogoose.betterfactions.data.persistent.faction;

import videogoose.betterfactions.data.persistent.federation.FactionMessage;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Lightweight persistence wrapper for BetterFactions-specific faction data.
 * Stored via PersistentObjectUtil and loaded into Faction mixin fields on server start.
 * This avoids the need to inject into Faction's serialization while keeping
 * the runtime API clean via mixin accessors.
 */
public class BetterFactionStore implements Serializable {

    private static final long serialVersionUID = 1L;

    public int factionId;
    public int federationId = -1;
    public String factionLogo = "";
    public ArrayList<FactionMessage> inbox = new ArrayList<>();
    public ArrayList<FactionRank> ranks = new ArrayList<>();

    public BetterFactionStore() {}

    public BetterFactionStore(int factionId) {
        this.factionId = factionId;
        this.ranks.add(FactionRank.getDefaultRank());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BetterFactionStore other && other.factionId == factionId;
    }

    @Override
    public int hashCode() {
        return factionId;
    }
}
