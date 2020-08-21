package dovtech.betterfactions.faction.government;

public enum FactionGovernmentType {
    OLIGARCHY(0, "Oligarchy"),
    MONARCHY(1, "Monarchy"),
    TECHNOCRACY(2, "Technocracy"),
    CORPORATION(3, "Corporation"),
    MERCENARY(4, "Mercenary Group"),
    REPUBLIC(5, "Republic"),
    DICTATORSHIP(6, "Dictatorship"),
    SOCIALIST_UNION(7, "Socialist Union");

    public int governmentID;
    public String displayName;

    FactionGovernmentType(int governmentID, String displayName) {
        this.governmentID = governmentID;
        this.displayName = displayName;
    }

    public static FactionGovernmentType getFromID(int id) {

        for(FactionGovernmentType gType : FactionGovernmentType.values()) {
            if(gType.governmentID == id) {
                return gType;
            }
        }

        return null;
    }
}
