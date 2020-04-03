package net.dovtech.betterfactions;

import api.DebugFile;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.block.FactoryType;
import api.mod.StarMod;
import net.dovtech.betterfactions.block.FleetManager;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.FactoryResource;

public class BetterFactions extends StarMod {
    static BetterFactions inst;
    public BetterFactions() {
        inst = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.modName = "BetterFactions";
        this.modAuthor = "DovTech";
        this.modVersion = "0.1.5";
        this.modDescription = "An all-in-one, versatile faction management and diplomacy mod aimed at improving the game's player to player interaction.";
        DebugFile.log("Enabled", this);
    }

    public void onBlockConfigLoad(BlockConfig config) {

        //Fleet Manager
        FleetManager fleetManager = new FleetManager();
        ElementInformation fleetManagerInfo = fleetManager.getBlockInfo();
        FactoryResource[] fleetManagerRecipe = {
                new FactoryResource(1, Blocks.STORAGE.getId()),
                new FactoryResource(5, Blocks.FACTION_MODULE.getId()),
                new FactoryResource(5, Blocks.BOBBY_AI_MODULE.getId())
        };
        BlockConfig.addRecipe(fleetManagerInfo, FactoryType.ADVANCED, 5, fleetManagerRecipe);
        config.add(fleetManagerInfo);
    }
}