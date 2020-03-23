package net.dovtech.betterfactions.faction;

import api.entity.Player;
import api.entity.Station;
import api.faction.Faction;
import api.faction.FactionRank;
import api.faction.NewsPost;
import net.dovtech.betterfactions.entity.fleet.BetterFleet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BetterFaction {

    private String name;
    private List<Player> members;
    private List<Player> activePlayers;
    private List<Player> inactivePlayers;
    private ArrayList<FactionRank> ranks;
    private int memberCount;
    private List<NewsPost> newsPosts;
    private Station homebase;
    private List<Station> claimStations;
    private System ownedSystems;
    private Faction baseFaction;
    private Federation federation = null;
    private List<BetterFaction> alliedFactions;
    private List<BetterFaction> vassals;
    private BetterFaction overLord = null;
    private List<BetterFaction> nonAggressionPacts;
    private List<BetterFaction> enemyFactions;
    private List<Player> enemyPlayers;
    private List<BetterFleet> fleets = null;
    private List<TradeDeal> tradeDeals = null;
    private Map<BetterFaction, String> opinions;

    public BetterFaction(Faction baseFaction) {
        this.baseFaction = baseFaction;
        this.name = baseFaction.getName();
        this.members = baseFaction.getMembers();
        this.activePlayers = baseFaction.getActivePlayers();
        this.inactivePlayers = baseFaction.getInactivePlayers();
        this.ranks = baseFaction.getRanks();
        this.memberCount = baseFaction.getMemberCount();
        this.newsPosts = baseFaction.getNewsPosts();
        this.homebase = baseFaction.getHomebase();
        this.claimStations = baseFaction.getClaimStations();
        this.ownedSystems = baseFaction.getOwnedSystems();
        //Todo:Get stuff like allied factions and turn them into BetterFactions objects
    }

    public String getName() {
        return name;
    }

    public List<Player> getMembers() {
        return members;
    }

    public List<Player> getActivePlayers() {
        return activePlayers;
    }

    public List<Player> getInactivePlayers() {
        return inactivePlayers;
    }

    public ArrayList<FactionRank> getRanks() {
        return ranks;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public List<NewsPost> getNewsPosts() {
        return newsPosts;
    }

    public Station getHomebase() {
        return homebase;
    }

    public List<Station> getClaimStations() {
        return claimStations;
    }

    public System getOwnedSystems() {
        return ownedSystems;
    }

    public Faction getBaseFaction() {
        return baseFaction;
    }

    public Federation getFederation() {
        return federation;
    }

    public List<BetterFaction> getAlliedFactions() {
        return alliedFactions;
    }

    public List<BetterFaction> getVassals() {
        return vassals;
    }

    public BetterFaction getOverLord() {
        return overLord;
    }

    public List<BetterFaction> getNonAggressionPacts() {
        return nonAggressionPacts;
    }

    public List<BetterFaction> getEnemyFactions() {
        return enemyFactions;
    }

    public List<Player> getEnemyPlayers() {
        return enemyPlayers;
    }

    public List<BetterFleet> getFleets() {
        return fleets;
    }

    public void joinFederation(Federation federation) {
        federation.addFaction(this);
        this.federation = federation;
    }

    public void leaveFederation() {
        this.federation.removeFaction(this);
        this.federation = null;
    }

    public Map<BetterFaction, String> getOpinions() {
        return opinions;
    }

    public void addOpinion(BetterFaction faction, String opinion) {
        opinions.put(faction, opinion);
    }

    public List<TradeDeal> getTradeDeals() {
        return tradeDeals;
    }

    public void addTradeDeal(TradeDeal tradeDeal) {
        tradeDeals.add(tradeDeal);
    }

    public void removeTradeDeal(TradeDeal tradeDeal) {
        tradeDeals.remove(tradeDeal);
    }
}