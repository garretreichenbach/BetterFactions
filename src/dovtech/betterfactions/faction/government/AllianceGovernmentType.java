package dovtech.betterfactions.faction.government;

public enum AllianceGovernmentType {
    FEDERATION("Federation"),
    CONFEDERATION("Confederation"),
    DEFENSIVE_ALLIANCE("Defensive Alliance"),
    MEGA_CORPORATION("Mega Corporation"),
    EMPIRE("Empire");

    public String displayName;

    AllianceGovernmentType(String displayName) {
        this.displayName = displayName;
    }
}
