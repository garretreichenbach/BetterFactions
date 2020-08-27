package dovtech.betterfactions.faction;

import api.common.GameServer;
import api.entity.StarPlayer;
import dovtech.betterfactions.entity.fleet.BetterFleet;
import dovtech.betterfactions.faction.diplo.alliance.Alliance;
import dovtech.betterfactions.faction.diplo.relations.FactionRelations;
import dovtech.betterfactions.faction.government.FactionGovernmentType;
import dovtech.betterfactions.player.BetterPlayer;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.server.data.PlayerNotFountException;
import java.util.ArrayList;
import java.util.HashMap;

public class BetterFaction {

    private Faction internalFaction;
    private Alliance alliance;
    private HashMap<BetterPlayer, ArrayList<BetterFleet>> fleets;
    private HashMap<BetterFaction, FactionRelations> relations;
    private FactionGovernmentType governmentType;
    private FactionStats factionStats;

    public BetterFaction(Faction internalFaction) {
        this.internalFaction = internalFaction;
        this.factionStats = new FactionStats();
        this.relations = new HashMap<>();
    }

    public Faction getInternalFaction() {
        return internalFaction;
    }

    public Alliance getAlliance() {
        return alliance;
    }

    public void setAlliance(Alliance alliance) {
        this.alliance = alliance;
    }

    public HashMap<BetterPlayer, ArrayList<BetterFleet>> getFleets() {
        return fleets;
    }

    public void addFleet(BetterPlayer player, BetterFleet fleet) {
        if(this.fleets.containsKey(player)) {
            this.fleets.get(player).add(fleet);
        } else {
            ArrayList<BetterFleet> playerFleets = new ArrayList<>();
            playerFleets.add(fleet);
            this.fleets.put(player, playerFleets);
        }
    }

    public void removeFleet(BetterPlayer player, BetterFleet fleet) {
        if(this.fleets.containsKey(player)) {
            this.fleets.get(player).remove(fleet);
        }
    }

    public ArrayList<BetterFaction> getAllies() {
        ArrayList<BetterFaction> allies = new ArrayList<>();
        for(Faction f : internalFaction.getFriends()) {
            allies.add(new BetterFaction(f));
        }

        return allies;
    }

    public ArrayList<BetterFaction> getEnemies() {
        ArrayList<BetterFaction> enemies = new ArrayList<>();
        for(Faction f : internalFaction.getEnemies()) {
            enemies.add(new BetterFaction(f));
        }

        return enemies;
    }

    public String getName() {
        return internalFaction.getName();
    }

    public ArrayList<BetterPlayer> getMembers() throws PlayerNotFountException {
        ArrayList<BetterPlayer> membersList = new ArrayList<>();

        for(String uid : internalFaction.getMembersUID().keySet()) {
            PlayerState playerState = GameServer.getServerState().getPlayerFromName(uid);
            BetterPlayer player = new BetterPlayer(new StarPlayer(playerState));
            player.setRank(new FactionRank(internalFaction.getMembersUID().get(uid)));
            membersList.add(player);
        }

        return membersList;
    }

    public int getID() {
        if(internalFaction != null) {
            return internalFaction.getIdFaction();
        }
        return 0;
    }

    public HashMap<BetterFaction, FactionRelations> getRelations() {
        return relations;
    }

    public FactionStats getFactionStats() {
        return factionStats;
    }

    public void setFactionStats(FactionStats factionStats) {
        this.factionStats = factionStats;
    }

    public FactionGovernmentType getGovernmentType() {
        return governmentType;
    }
}
