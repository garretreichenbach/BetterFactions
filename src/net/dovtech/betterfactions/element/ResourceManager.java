package net.dovtech.betterfactions.element;

import api.inventory.ItemStack;
import net.dovtech.betterfactions.BetterFactions;
import org.schema.game.common.data.element.ElementInformation;

public class ResourceManager extends ItemStack {

    private ElementInformation blockInfo = getInfo();

    public ResourceManager(short id) {
        super(id);
        blockInfo.setCanActivate(true);
        BetterFactions.blocks.add(this);
    }

    public void onActivate() {

    }
}
