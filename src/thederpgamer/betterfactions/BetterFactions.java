package thederpgamer.betterfactions;

import api.common.GameCommon;
import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.controller.ClientInitializeEvent;
import api.listener.events.controller.ServerInitializeEvent;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.listener.events.input.KeyPressEvent;
import api.listener.events.network.ClientLoginEvent;
import api.listener.events.player.PlayerJoinFactionEvent;
import api.listener.events.player.PlayerLeaveFactionEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import api.mod.config.SyncedConfigUtil;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.betterfactions.gui.NewFactionPanel;
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
    public final String defaultLogo = "https://i.imgur.com/8wKjlBR.png";
    public int lastClientUpdate = 0;
    public boolean showDebugText = false;
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
    }

    @Override
    public void onClientCreated(ClientInitializeEvent event) {
        startClientTimers();
    }

    @Override
    public void onServerCreated(ServerInitializeEvent event) {
        startServerTimers();
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
        StarLoader.registerListener(ClientLoginEvent.class, new Listener<ClientLoginEvent>() {
            @Override
            public void onEvent(ClientLoginEvent event) {
                SyncedConfigUtil.sendConfigToClient(event.getServerProcessor(), config);
            }
        }, this);

        StarLoader.registerListener(PlayerGUICreateEvent.class, new Listener<PlayerGUICreateEvent>() {
            @Override
            public void onEvent(PlayerGUICreateEvent event) {
                try {
                    Field factionPanelNewField = event.getPlayerPanel().getClass().getDeclaredField("factionPanelNew");
                    factionPanelNewField.setAccessible(true);
                    newFactionPanel = new NewFactionPanel(event.getPlayerPanel().getState());
                    newFactionPanel.onInit();
                    factionPanelNewField.set(event.getPlayerPanel(), newFactionPanel);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }, this);

        StarLoader.registerListener(KeyPressEvent.class, new Listener<KeyPressEvent>() {
            @Override
            public void onEvent(final KeyPressEvent event) {
                if (event.getKey() == 210 || event.getKey() == 260) {
                    if (debugMode) showDebugText = !showDebugText;
                }
            }
        }, this);

        StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
            @Override
            public void onEvent(PlayerJoinFactionEvent event) {
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    newFactionPanel.recreateTabs();
                }
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
            @Override
            public void onEvent(PlayerLeaveFactionEvent event) {
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    newFactionPanel.recreateTabs();
                }
            }
        }, this);
    }

    private void registerPackets() {
        PacketUtil.registerPacket(UpdateClientDataPacket.class);
    }

    private void startClientTimers() {

    }

    private void startServerTimers() {
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
                if (lastClientUpdate >= clientUpdateInterval) {
                    UpdateClientDataPacket packet = new UpdateClientDataPacket();
                    for (PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                        PacketUtil.sendPacket(playerState, packet);
                    }
                }
            }
        }.runTimer(this, clientUpdateInterval);
    }

    public boolean isServer() {
        return GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer();
    }

    public boolean isClient() {
        return GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer();
    }
}
