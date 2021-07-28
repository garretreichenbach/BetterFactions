package thederpgamer.betterfactions;

import api.common.GameCommon;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.listener.events.player.PlayerJoinFactionEvent;
import api.listener.events.player.PlayerLeaveFactionEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import api.utils.StarRunnable;
import org.apache.commons.io.IOUtils;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.schine.resource.ResourceLoader;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRank;
import thederpgamer.betterfactions.gui.NewFactionPanel;
import thederpgamer.betterfactions.manager.SpriteManager;
import thederpgamer.betterfactions.utils.FactionNewsUtils;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.FederationUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * BetterFactions mod main class.
 *
 * @author TheDerpGamer
 * @since 01/30/2021
 */
public class BetterFactions extends StarMod {

    //Instance
    public BetterFactions() {

    }
    public static void main(String[] args) {

    }
    private static BetterFactions inst;
    public static BetterFactions getInstance() {
        return inst;
    }

    //Data
    public int lastClientUpdate = 0;
    public NewFactionPanel newFactionPanel;

    //Config
    public FileConfiguration config;
    private final String[] defaultConfig = {
            "debug-mode: false",
            "save-interval: 12000",
            "max-news-backup: 30"
    };
    public boolean debugMode = false;
    public int saveInterval = 12000;
    public int maxNewsBackup = 30;

    //Overwrites
    private final String[] overwriteClasses = {
            "GUIHorizontalButtonTablePane"
    };

    @Override
    public void onEnable() {
        inst = this;
        initConfig();
        registerListeners();
        startTimers();
    }

    @Override
    public void onResourceLoad(ResourceLoader loader) {
        (new SpriteManager()).initialize();
    }

    @Override
    public byte[] onClassTransform(String className, byte[] byteCode) {
        for(String name : overwriteClasses) if(className.endsWith(name)) return overwriteClass(className, byteCode);
        return super.onClassTransform(className, byteCode);
    }

    private void initConfig() {
        config = getConfig("config");
        config.saveDefault(defaultConfig);

        debugMode = config.getConfigurableBoolean("debug-mode", false);
        saveInterval = config.getConfigurableInt("save-interval", 12000);
        maxNewsBackup = config.getConfigurableInt("max-news-backup", 30);
    }

    private void registerListeners() {
        StarLoader.registerListener(PlayerGUICreateEvent.class, new Listener<PlayerGUICreateEvent>() {
            @Override
            public void onEvent(PlayerGUICreateEvent event) {
                FactionUtils.initializeFactions();
                try {
                    PlayerPanel playerPanel = event.getPlayerPanel();
                    Field factionPanelNewField = playerPanel.getClass().getDeclaredField("factionPanelNew");
                    factionPanelNewField.setAccessible(true);
                    if(!(factionPanelNewField.get(playerPanel) instanceof NewFactionPanel)) {
                        (newFactionPanel = new NewFactionPanel(playerPanel.getState())).onInit();
                        factionPanelNewField.set(playerPanel, newFactionPanel);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }, this);

        StarLoader.registerListener(FactionCreateEvent.class, new Listener<FactionCreateEvent>() {
            @Override
            public void onEvent(FactionCreateEvent event) {
                FactionUtils.getFactionData(event.getFaction());
                FactionUtils.saveData();
                FederationUtils.saveData();
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionCreateNews(FactionUtils.getFactionData(event.getFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    FactionData factionData = FactionUtils.getFactionData(event.getFaction());
                    FactionRank founderRank = new FactionRank("Founder", 5, "*");
                    factionData.addRank(founderRank);
                    factionData.addMember(event.getPlayer().getName());
                    factionData.getMember(event.getPlayer().getName()).setRank(founderRank);
                    newFactionPanel.factionDiplomacyTab.updateTab();
                    newFactionPanel.factionManagementTab.updateTab();
                }
            }
        }, this);

        StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
            @Override
            public void onEvent(PlayerJoinFactionEvent event) {
                FactionUtils.getFactionData(event.getFaction());
                FactionUtils.saveData();
                FederationUtils.saveData();
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionJoinNews(FactionUtils.getFactionData(event.getFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    FactionUtils.getFactionData(event.getFaction()).addMember(event.getPlayer().getName());
                    newFactionPanel.factionManagementTab.updateTab();
                }
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
            @Override
            public void onEvent(PlayerLeaveFactionEvent event) {
                if(event.getFaction() != null) {
                    if(event.getFaction().getMembersUID().keySet().size() <= 1) {
                        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionDisbandNews(FactionUtils.getFactionData(event.getFaction())));
                    } else {
                        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionLeaveNews(FactionUtils.getFactionData(event.getFaction()), event.getPlayer()));
                    }
                    FactionUtils.getFactionData(event.getFaction());
                }
                FactionUtils.saveData();
                FederationUtils.saveData();
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    FactionUtils.getFactionData(event.getFaction()).removeMember(event.getPlayer().getName());
                    newFactionPanel.factionManagementTab.updateTab();
                }
            }
        }, this);
    }

    private void startTimers() {
        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
            new StarRunnable() {

                @Override
                public void run() {
                    lastClientUpdate += 5;
                }
            }.runTimer(this, 5);

            new StarRunnable() {
                @Override
                public void run() {
                    FactionUtils.saveData();
                    FederationUtils.saveData();
                    FactionNewsUtils.saveData();
                }
            }.runTimer(this, saveInterval);
        }
    }

    private byte[] overwriteClass(String className, byte[] byteCode) {
        byte[] bytes = null;
        try {
            ZipInputStream file = new ZipInputStream(new FileInputStream(this.getSkeleton().getJarFile()));
            while(true) {
                ZipEntry nextEntry = file.getNextEntry();
                if(nextEntry == null) break;
                if(nextEntry.getName().endsWith(className + ".class")) bytes = IOUtils.toByteArray(file);
            }
            file.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        if(bytes != null) return bytes;
        else return byteCode;
    }
}
