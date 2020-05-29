package net.dovtech.betterfactions.block;

import api.config.BlockConfig;
import org.schema.game.common.data.element.ElementInformation;

public class ResourceLinker {

    private ElementInformation info;

    public ResourceLinker() {
        info = BlockConfig.newElement("Resource Linker", new short[]{ 3200 });
    }

    public ElementInformation getElementInformation() {
        return info;
    }
}
