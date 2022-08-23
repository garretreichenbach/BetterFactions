package thederpgamer.betterfactions.element;

import api.config.BlockConfig;
import org.apache.commons.lang3.StringUtils;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import thederpgamer.betterfactions.element.block.Block;

import java.util.ArrayList;

/**
 * Manages mod elements.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class ElementManager {

	public enum FactoryType {
		NONE,
		CAPSULE_REFINERY,
		MICRO_ASSEMBLER,
		BASIC_FACTORY,
		STANDARD_FACTORY,
		ADVANCED_FACTORY
	}

	private static final ArrayList<Block> blockList = new ArrayList<>();

	public static void initialize() {
		for(Block block : blockList) block.initialize();
	}

	public static ArrayList<Block> getAllBlocks() {
		return blockList;
	}

	public static Block getBlock(short id) {
		for(Block blockElement : getAllBlocks())
			if(blockElement.getBlockInfo().getId() == id) return blockElement;
		return null;
	}

	public static Block getBlock(String blockName) {
		for(Block block : getAllBlocks()) {
			if(block.getBlockInfo().getName().equalsIgnoreCase(blockName)) return block;
		}
		return null;
	}

	public static Block getBlock(SegmentPiece segmentPiece) {
		for(Block block : getAllBlocks()) if(block.getId() == segmentPiece.getType()) return block;
		return null;
	}

	public static void addBlock(Block block) {
		blockList.add(block);
	}

	public static ElementCategory getCategory(String path) {
		String[] split = path.split("\\.");
		ElementCategory category = ElementKeyMap.getCategoryHirarchy();
		for(String s : split) {
			boolean createNew = false;
			if(category.hasChildren()) {
				for(ElementCategory child : category.getChildren()) {
					if(child.getCategory().equalsIgnoreCase(s)) {
						category = child;
						break;
					}
					createNew = true;
				}
			} else createNew = true;
			if(createNew) category = BlockConfig.newElementCategory(category, StringUtils.capitalize(s));
		}
		return category;
	}

	public static ElementInformation getInfo(String name) {
		Block block = getBlock(name);
		if(block != null) return block.getBlockInfo();
		return null;
	}
}
