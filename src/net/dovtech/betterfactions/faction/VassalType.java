package net.dovtech.betterfactions.faction;

public enum VassalType {

    NONE(false, false, true, true, true),
    OCCUPIED(false, false, true, true, false), //A faction can only be occupied for a limited amount of time depending on victor's war score.
    PUPPET(true, true, false, false, false),
    COLONY(true, true, false, false, true),
    DOMINION(true, false, true, true, true);

    private boolean taxable;
    private boolean autoCalltoWar;
    private boolean denyTrade;
    private boolean denyForces;
    private boolean canControlOwnPolicy;

    private VassalType(boolean taxable, boolean autoCalltoWar, boolean denyTrade, boolean denyForces, boolean canControlOwnPolicy) {
        this.taxable = taxable;
        this.autoCalltoWar = autoCalltoWar;
        this.denyTrade = denyTrade;
        this.denyForces = denyForces;
        this.canControlOwnPolicy = canControlOwnPolicy;
    }
}
