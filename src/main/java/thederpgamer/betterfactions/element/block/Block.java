package thederpgamer.betterfactions.element.block;

import api.config.BlockConfig;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;
import thederpgamer.betterfactions.BetterFactions;

/**
 * Abstract class for all blocks.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public abstract class Block {

	private static short id = 0;
	protected ElementInformation blockInfo;

	public Block(String name, ElementCategory category) {
		blockInfo = BlockConfig.newElement(BetterFactions.getInstance(), name, new short[6]);
		id = blockInfo.getId();
		BlockConfig.setElementCategory(blockInfo, category);
	}

	public final ElementInformation getBlockInfo() {
		return blockInfo;
	}

	public final short getId() {
		return blockInfo.getId();
	}

	public abstract void initialize();

	public static short getBlockId() {
		return id;
	}
}