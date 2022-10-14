package thederpgamer.betterfactions;

import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import thederpgamer.betterfactions.data.commands.ForceDiploCommand;
import thederpgamer.betterfactions.manager.ConfigManager;
import thederpgamer.betterfactions.manager.EventManager;
import thederpgamer.betterfactions.manager.FactionDiplomacyManager;
import thederpgamer.betterfactions.utils.DataUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main class for BetterFactions mod.
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class BetterFactions extends StarMod {

	//Instance
	private static BetterFactions instance;
	public BetterFactions() {}
	public static BetterFactions getInstance() {
		return instance;
	}
	public static void main(String[] args) {}

	//Data
	public static Logger log;

	@Override
	public void onEnable() {
		instance = this;
		ConfigManager.initialize(this);
		initLogger();
		EventManager.registerEvents(this);
		FactionDiplomacyManager.initialize();
		registerCommands();
	}

	@Override
	public void onClientCreated(ClientInitializeEvent event) {
		super.onClientCreated(event);
	}

	private void initLogger() {
		String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
		File logsFolder = new File(logFolderPath);
		if(!logsFolder.exists()) logsFolder.mkdirs();
		else {
			if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
				File[] logFiles = new File[logsFolder.listFiles().length];
				int j = logFiles.length - 1;
				for(int i = 0; i < logFiles.length && j >= 0; i++) {
					logFiles[i] = logsFolder.listFiles()[j];
					j--;
				}

				for(File logFile : logFiles) {
					if(!logFile.getName().endsWith(".txt")) continue;
					String fileName = logFile.getName().replace(".txt", "");
					int logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("log") + 3)) + 1;
					String newName = logFolderPath + "/log" + logNumber + ".txt";
					if(logNumber < ConfigManager.getMainConfig().getInt("max-world-logs") - 1) logFile.renameTo(new File(newName));
					else logFile.delete();
				}
			}
		}
		try {
			File newLogFile = new File(logFolderPath + "/log0.txt");
			if(newLogFile.exists()) newLogFile.delete();
			newLogFile.createNewFile();
			log = Logger.getLogger(newLogFile.getPath());
			FileHandler handler = new FileHandler(newLogFile.getPath());
			log.addHandler(handler);
			SimpleFormatter formatter = new SimpleFormatter();
			handler.setFormatter(formatter);
		} catch(IOException exception) {
			exception.printStackTrace();
		}
	}

	private void registerCommands() {
		StarLoader.registerCommand(new ForceDiploCommand());
	}
}
