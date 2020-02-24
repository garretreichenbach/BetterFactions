package net.dovtech.betterfactions;

import api.DebugFile;
import api.mod.StarMod;

public class BetterFactions extends StarMod {
    static BetterFactions inst;
    public BetterFactions() {
        inst = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.modName = "BetterFactions";
        DebugFile.log("Enabled", this);
    }
}