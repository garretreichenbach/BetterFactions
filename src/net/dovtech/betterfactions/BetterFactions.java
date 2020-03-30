package net.dovtech.betterfactions;

import api.DebugFile;
import api.config.BlockConfig;
import api.inventory.ItemStack;
import api.mod.StarMod;
import java.util.List;

public class BetterFactions extends StarMod {
    static BetterFactions inst;
    public BetterFactions() {
        inst = this;
    }
    public static List<ItemStack> blocks;

    @Override
    public void onEnable() {
        super.onEnable();
        this.modName = "BetterFactions";
        DebugFile.log("Enabled", this);
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {
        for(ItemStack block : blocks) {
            config.add(block.getInfo());
        }
    }
}