package thederpgamer.betterfactions;

import api.common.GameCommon;
import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.listener.events.network.ClientLoginEvent;
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
    public final String defaultLogo = "Todo";
    public int lastClientUpdate = 0;

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

        this.debugMode = config.getConfigurableBoolean("debug-mode", false);
        this.saveInterval = config.getConfigurableInt("save-interval", 12000);
        this.clientUpdateInterval = config.getConfigurableInt("client-update-interval", 3500);
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
                    NewFactionPanel newFactionPanel = new NewFactionPanel(event.getPlayerPanel().getState());
                    newFactionPanel.onInit();
                    factionPanelNewField.set(event.getPlayerPanel(), newFactionPanel);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }, this);
    }

    private void registerPackets() {
        PacketUtil.registerPacket(UpdateClientDataPacket.class);
    }

    private void startTimers() {
        if(isServer()) {
            new StarRunnable(){
                @Override
                public void run() {
                    FactionUtils.saveData();
                    FederationUtils.saveData();
                }
            }.runLater(this, saveInterval);

            new StarRunnable() {
                @Override
                public void run() {
                    if(lastClientUpdate >= clientUpdateInterval) {
                        UpdateClientDataPacket packet = new UpdateClientDataPacket();
                        for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                            PacketUtil.sendPacket(playerState, packet);
                        }
                    }
                }
            }.runLater(this, clientUpdateInterval);
        }
    }

    public boolean isServer() {
        return GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer();
    }

    public boolean isClient() {
        return GameCommon.isClientConnectedToServer() || GameCommon.isOnSinglePlayer();
    }
}
