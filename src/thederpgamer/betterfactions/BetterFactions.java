package thederpgamer.betterfactions;

import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.listener.events.player.PlayerJoinFactionEvent;
import api.listener.events.player.PlayerLeaveFactionEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
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
        if(isServer()) startServerTimers();
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
                FactionUtils.saveData();
                FederationUtils.saveData();
            }
        }, this);

        StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
            @Override
            public void onEvent(PlayerJoinFactionEvent event) {
                FactionUtils.saveData();
                FederationUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    newFactionPanel.recreateTabs();
                }
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
            @Override
            public void onEvent(PlayerLeaveFactionEvent event) {
                FactionUtils.saveData();
                FederationUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    newFactionPanel.recreateTabs();
                }
            }
        }, this);
    }

    private void registerPackets() {
        PacketUtil.registerPacket(UpdateClientDataPacket.class);
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
        return GameServerState.instance != null;
    }

    public boolean isClient() {
        return GameClientState.instance != null;
    }
}
