package thederpgamer.betterfactions;

import api.common.GameCommon;
import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.listener.events.player.PlayerJoinFactionEvent;
import api.listener.events.player.PlayerLeaveFactionEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.gui.NewFactionPanel;
import thederpgamer.betterfactions.network.client.CreateNewFederationPacket;
import thederpgamer.betterfactions.network.server.UpdateClientDataPacket;
import thederpgamer.betterfactions.utils.FactionUtils;
import thederpgamer.betterfactions.utils.FederationUtils;
import java.lang.reflect.Field;

public class BetterFactions extends StarMod {

    //Instance
    public BetterFactions() { }
    public static void main(String[] args) { }
    private static BetterFactions inst;
    public static BetterFactions getInstance() {
        return inst;
    }

    //Data
    public int lastClientUpdate = 0;
    private NewFactionPanel newFactionPanel;

    //Config
    public FileConfiguration config;
    private final String[] defaultConfig = {
            "debug-mode: false",
            "save-interval: 12000",
            "client-update-interval: 3500"
    };
    public boolean debugMode = false;
    public int saveInterval = 12000;
    public int clientUpdateInterval = 3500;

    @Override
    public void onEnable() {
        inst = this;
        initConfig();
        loadData();
        registerListeners();
        registerPackets();
        startTimers();
    }

    private void initConfig() {
        config = getConfig("config");
        config.saveDefault(defaultConfig);

        debugMode = config.getConfigurableBoolean("debug-mode", false);
        saveInterval = config.getConfigurableInt("save-interval", 12000);
        clientUpdateInterval = config.getConfigurableInt("client-update-interval", 3500);
    }

    private void loadData() {
        FactionUtils.loadData();
        FederationUtils.loadData();
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
            }
        }, this);

        StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
            @Override
            public void onEvent(PlayerJoinFactionEvent event) {
                FactionUtils.getFactionData(event.getFaction());
                FactionUtils.saveData();
                FederationUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    redrawFactionPane();
                }
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
            @Override
            public void onEvent(PlayerLeaveFactionEvent event) {
                FactionUtils.getFactionData(event.getFaction());
                FactionUtils.saveData();
                FederationUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    redrawFactionPane();
                }
            }
        }, this);
    }

    private void registerPackets() {
        PacketUtil.registerPacket(UpdateClientDataPacket.class);
        PacketUtil.registerPacket(CreateNewFederationPacket.class);
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
                }
            }.runTimer(this, saveInterval);

            new StarRunnable() {
                @Override
                public void run() {
                    if(lastClientUpdate >= clientUpdateInterval) updateClientData();
                }
            }.runTimer(this, clientUpdateInterval);
        }
    }

    public void updateClientData() {
        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
            UpdateClientDataPacket packet = new UpdateClientDataPacket();
            for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                PacketUtil.sendPacket(playerState, packet);
            }
            lastClientUpdate = 0;
        }
    }

    public void redrawFactionPane() {
        if(GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer()) {
            newFactionPanel.recreateTabs();
        }
    }
}
