package videogoose.betterfactions;

import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.language.Lng;
import org.schema.schine.common.language.Translatable;
import org.schema.schine.resource.ResourceLoader;
import videogoose.betterfactions.data.commands.ForceDiploCommand;
import videogoose.betterfactions.manager.*;
import videogoose.betterfactions.network.ClientUpdatePacket;
import videogoose.betterfactions.network.FactionDiplomacyPacket;
import videogoose.betterfactions.utils.ReflectionUtils;

import javax.vecmath.Vector3f;

public class BetterFactions extends StarMod {

	private static BetterFactions instance;

	public BetterFactions() {}

	public static BetterFactions getInstance() {
		return instance;
	}

	public static void main(String[] args) {}

	@Override
	public void onEnable() {
		instance = this;
		ConfigManager.initialize(this);
		injectCustomRelationTypes();
		EventManager.registerEvents(this);
		FactionDiplomacyManager.initialize();
		CasusBelliManager.initialize();
		ClaimsManager.initialize();
		WarManager.initialize();
		registerCommands();
		registerPackets();
	}

	@Override
	public void onResourceLoad(ResourceLoader loader) {
		ResourceManager.loadResources(loader);
	}

	public void logDebug(String message) {
		if(ConfigManager.debugMode.getValue()) {
			logMessage("[DEBUG]:" + message);
		}
	}

	/**
	 * Injects custom RType enum values (NON_AGGRESSION, FEDERATION) into
	 * FactionRelation.RType at runtime using reflection.
	 */
	private void injectCustomRelationTypes() {
		try {
			ReflectionUtils.injectEnumValue(
				FactionRelation.RType.class,
				"NON_AGGRESSION",
				3,
				(Translatable) en -> Lng.str("NEUTRAL (NO AGGRESSION)"),
				3,
				new Vector3f(0.81f, 0.81f, 0.36f),
				(byte) 3
			);
			ReflectionUtils.injectEnumValue(
				FactionRelation.RType.class,
				"FEDERATION",
				4,
				(Translatable) en -> Lng.str("FEDERATION"),
				2,
				new Vector3f(0.19f, 0.65f, 0.55f),
				(byte) 4
			);
			logInfo("Successfully injected custom relation types");
		} catch (Exception e) {
			logException("Failed to inject custom relation types", e);
		}
	}

	private void registerCommands() {
		StarLoader.registerCommand(new ForceDiploCommand());
	}

	private void registerPackets() {
		PacketUtil.registerPacket(ClientUpdatePacket.class);
		PacketUtil.registerPacket(FactionDiplomacyPacket.class);
	}
}
