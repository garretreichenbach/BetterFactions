package net.dovtech.betterfactions.universe;

import api.element.block.Block;

public class Resource {

    private Block material;

    public Resource(Block material) {
        this.material = material;
    }

    public Block getMaterial() {
        return material;
    }
}
