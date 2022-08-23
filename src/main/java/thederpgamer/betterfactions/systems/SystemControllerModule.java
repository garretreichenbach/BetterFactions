package thederpgamer.betterfactions.systems;

import api.utils.game.module.util.SimpleDataStorageMCModule;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.schine.graphicsengine.core.Timer;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.system.SystemSpecialization;
import thederpgamer.betterfactions.element.block.module.SystemController;

/**
 * SystemController Module for SystemController block.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class SystemControllerModule extends SimpleDataStorageMCModule {

	public SystemControllerModule(SegmentController entity, ManagerContainer<?> managerContainer) {
		super(entity, managerContainer, BetterFactions.getInstance(), SystemController.getBlockId());
	}

	@Override
	public String getName() {
		return "SystemControllerModule";
	}

	@Override
	public void handle(Timer timer) {

	}

	public SystemControllerModuleData getData() {
		if(!(data instanceof SystemControllerModuleData)) data = new SystemControllerModuleData();
		return (SystemControllerModuleData) data;
	}

	public static class SystemControllerModuleData {

		private String systemName;
		private int factionId;
		private SystemSpecialization specialization;
	}
}
