package videogoose.betterfactions.repair;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.elements.shipyard.ShipyardCollectionManager;
import org.schema.game.common.controller.elements.shipyard.orders.ShipyardEntityState;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.game.common.data.player.inventory.InventoryChangeMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Shipyard repairs are paid in components, loaded or not: the old ship is broken down into the components its
 * blocks are assembled from, and the new one is built from components. A block with no component recipe (not made
 * in the block assembler) counts as itself; a shape (wedge, slab) counts as its base block. Used by the loaded
 * repair through {@code ShipyardRepairComponentsMixin} and by {@link UnloadedRepairs}, so the two cost the same.
 */
public final class RepairComponents {

	/** {@code ElementInformation.producedInFactory} of the block assembler. */
	private static final int BLOCK_ASSEMBLER = 4;

	private RepairComponents() {
	}

	/** Whether a shipyard machine is working a repair, the only order paid in components. */
	public static boolean repairing(ShipyardEntityState s) {
		return s.isInRepair;
	}

	/** A shape's base block, or the block itself. */
	static short base(short type) {
		int s = ElementKeyMap.isValidType(type) ? ElementKeyMap.getInfoFast(type).getSourceReference() : 0;
		return s != 0 ? (short) s : type;
	}

	/** The components one of this (base) block is assembled from, or null when it is not assembled from any. */
	private static List<FactoryResource> recipe(short block) {
		if (!ElementKeyMap.isValidType(block)) {
			return null;
		}
		ElementInformation info = ElementKeyMap.getInfoFast(block);
		return info.producedInFactory != BLOCK_ASSEMBLER || info.getConsistence().isEmpty() ? null : info.getConsistence();
	}

	/** What these blocks break down into. */
	public static ElementCountMap components(ElementCountMap blocks) {
		ElementCountMap out = new ElementCountMap();
		for (short t : ElementKeyMap.typeList()) {
			int n = blocks.get(t);
			if (n <= 0) {
				continue;
			}
			short b = base(t);
			List<FactoryResource> r = recipe(b);
			if (r == null) {
				out.inc(b, n);
			} else {
				for (FactoryResource c : r) {
					out.inc(base(c.type), c.count * n);
				}
			}
		}
		return out;
	}

	/** How many of this (base) block the inventory holds the components for. */
	public static int makeable(Inventory inv, short block) {
		if (ShipyardRepairs.free) {
			return Integer.MAX_VALUE / 2;
		}
		List<FactoryResource> r = recipe(block);
		if (r == null) {
			return inv.getOverallQuantity(block);
		}
		int m = Integer.MAX_VALUE;
		for (FactoryResource c : r) {
			m = Math.min(m, inv.getOverallQuantity(base(c.type)) / Math.max(1, c.count));
		}
		return m;
	}

	/** Takes the components for {@code n} of this (base) block. */
	public static void take(Inventory inv, short block, int n, IntOpenHashSet mod) {
		if (ShipyardRepairs.free) {
			return;
		}
		List<FactoryResource> r = recipe(block);
		if (r == null) {
			inv.decreaseBatch(block, n, mod);
			return;
		}
		for (FactoryResource c : r) {
			inv.decreaseBatch(base(c.type), c.count * n, mod);
		}
	}

	/**
	 * The repair's version of the yard pulling what it still needs from the chests connected to it: components,
	 * where the vanilla pull asks for blocks and would wait forever.
	 */
	public static void pull(ShipyardEntityState s) {
		if (ShipyardRepairs.free) {
			return;
		}
		ShipyardCollectionManager yard = s.getShipyardCollectionManager();
		Short2ObjectOpenHashMap<? extends LongOpenHashSet> connected = yard.getSegmentController().getControlElementMap()
				.getControllingMap().get(yard.getControllerElement().getAbsoluteIndex());
		if (connected == null) {
			return;
		}
		ElementCountMap remaining = new ElementCountMap();
		for (short t : ElementKeyMap.typeList()) {
			int n = s.currentMapFrom.get(t) - s.currentMapTo.get(t);
			if (n > 0) {
				remaining.inc(t, n);
			}
		}
		ElementCountMap need = components(remaining);
		Inventory inv = s.getInventory();
		InventoryChangeMap changes = new InventoryChangeMap();
		for (short t : ElementKeyMap.typeList()) {
			int lack = need.get(t) - inv.getOverallQuantity(t);
			for (Inventory from : chests(yard, connected)) {
				if (lack <= 0) {
					break;
				}
				int take = Math.min(from.getOverallQuantity(t), lack);
				if (take > 0 && (take = inv.canPutInHowMuch(t, take, -1)) > 0) {
					from.decreaseBatch(t, take, changes.getInv(from));
					changes.getInv(inv).add(inv.putNextFreeSlotWithoutException(t, take, -1));
					lack -= take;
				}
			}
		}
		if (!changes.isEmpty()) {
			changes.sendAll();
		}
	}

	/** How many of this the yard holds, counting the chests connected to it, which a repair pulls from. */
	public static int onHand(ShipyardCollectionManager yard, short type) {
		int n = yard.getServerEntityState().getInventory().getOverallQuantity(type);
		Short2ObjectOpenHashMap<? extends LongOpenHashSet> connected = yard.getSegmentController().getControlElementMap()
				.getControllingMap().get(yard.getControllerElement().getAbsoluteIndex());
		if (connected != null) {
			for (Inventory c : chests(yard, connected)) {
				n += c.getOverallQuantity(type);
			}
		}
		return n;
	}

	private static List<Inventory> chests(ShipyardCollectionManager yard, Short2ObjectOpenHashMap<? extends LongOpenHashSet> connected) {
		List<Inventory> out = new ArrayList<>();
		for (short chestType : ElementKeyMap.inventoryTypes) {
			LongOpenHashSet chests = connected.get(chestType);
			if (chests != null) {
				for (long l : chests) {
					Inventory c = yard.getContainer().getInventory(ElementCollection.getPosIndexFrom4(l));
					if (c != null) {
						out.add(c);
					}
				}
			}
		}
		return out;
	}
}
