package dovtech.starmadeplus.faction;

public class FactionStats {

    private int[] stats;

    public FactionStats(int[] stats) {
        this.stats = stats;
    }

    public int getFactionID() {
        return stats[0];
    }

    public int getReputationScore() {
        return stats[1];
    }

    public void setReputationScore(int reputationScore) {
        stats[1] = reputationScore;
    }

    public int getAttackingWars() {
        return stats[2];
    }

    public void setAttackingWars(int attackingWars) {
        stats[2] = attackingWars;
    }

    public int getDefendingWars() {
        return stats[3];
    }

    public void setDefendingWars(int defendingWars) {
        stats[3] = defendingWars;
    }

    public int getWarsWon() {
        return stats[4];
    }

    public void setWarsWon(int warsWon) {
        stats[4] = warsWon;
    }

    public int getWarsLost() {
        return stats[5];
    }

    public void setWarsLost(int warsLost) {
        stats[5] = warsLost;
    }

    public int getCoalitionsParticipated() {
        return stats[6];
    }

    public void setCoalitionsParticipated(int coalitionParticipated) {
        stats[6] = coalitionParticipated;
    }

    public int getCoalitionsAgainst() {
        return stats[7];
    }

    public void setCoalitionsAgainst(int coalitionsAgainst) {
        stats[7] = coalitionsAgainst;
    }

    public int getShipsDestroyed() {
        return stats[8];
    }

    public void setShipsDestroyed(int shipsDestroyed) {
        stats[8] = shipsDestroyed;
    }

    public int getShipsLost() {
        return stats[9];
    }

    public void setShipsLost(int shipsLost) {
        stats[9] = shipsLost;
    }

    public int getStationsDestroyed() {
        return stats[10];
    }

    public void setStationsDestroyed(int stationsDestroyed) {
        stats[10] = stationsDestroyed;
    }

    public int getStationsLost() {
        return stats[11];
    }

    public void setStationsLost(int stationsLost) {
        stats[11] = stationsLost;
    }

    public int getPlayersKilled() {
        return stats[12];
    }

    public void setPlayersKilled(int playersKilled) {
        stats[12] = playersKilled;
    }

    public int getPlayersLost() {
        return stats[13];
    }

    public void setPlayersLost(int playersLost) {
        stats[13] = playersLost;
    }

    public int getTerritoriesWon() {
        return stats[14];
    }

    public void setTerritoriesWon(int territoriesWon) {
        stats[14] = territoriesWon;
    }

    public int getTerritoriesLost() {
        return stats[15];
    }

    public void setTerritoriesLost(int territoriesLost) {
        stats[15] = territoriesLost;
    }

    public int getCurrentTerritoryCount() {
        return stats[16];
    }

    public void setCurrentTerritoryCount(int currentTerritoryCount) {
        stats[16] = currentTerritoryCount;
    }

    public int getAllyCount() {
        return stats[17];
    }

    public void setAllyCount(int allyCount) {
        stats[17] = allyCount;
    }

    public int getEnemyCount() {
        return stats[18];
    }

    public void setEnemyCount(int enemyCount) {
        stats[18] = enemyCount;
    }

    public int getTradeDeals() {
        return stats[19];
    }

    public void setTradeDeals(int tradeDeals) {
        stats[19] = tradeDeals;
    }

    public int getTotalPower() {
        return stats[20];
    }

    public void setTotalPower(int totalPower) {
        stats[20] = totalPower;
    }

    public int getAggressionScore() {
        return stats[21];
    }

    public void setAggressionScore(int aggressionScore) {
        stats[21] = aggressionScore;
    }

    public int[] getStats() {
        return stats;
    }
}
