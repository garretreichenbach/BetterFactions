package thederpgamer.betterfactions;

import api.common.GameCommon;
import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.listener.events.player.PlayerJoinFactionEvent;
import api.listener.events.player.PlayerLeaveFactionEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import api.utils.StarRunnable;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.resource.ResourceLoader;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRank;
import thederpgamer.betterfactions.gui.NewFactionPanel;
import thederpgamer.betterfactions.manager.ConfigManager;
import thederpgamer.betterfactions.manager.FactionManager;
import thederpgamer.betterfactions.manager.ResourceManager;
import thederpgamer.betterfactions.network.server.UpdateClientDataPacket;
import thederpgamer.betterfactions.network.server.UpdateGUIsPacket;
import thederpgamer.betterfactions.utils.FactionNewsUtils;

import java.lang.reflect.Field;

/**
 * BetterFactions mod main class.
 *
 * @author TheDerpGamer
 * @version 1.0 - [01/30/2021]
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
    public NewFactionPanel newFactionPanel;

    @Override
    public void onEnable() {
        inst = this;
        ConfigManager.initialize(this);
        registerListeners();
        registerPackets();
        startRunners();
    }

    @Override
    public void onResourceLoad(ResourceLoader resourceLoader) {
        ResourceManager.loadResources(resourceLoader);
    }

    private void registerListeners() {
        StarLoader.registerListener(PlayerGUICreateEvent.class, new Listener<PlayerGUICreateEvent>() {
            @Override
            public void onEvent(PlayerGUICreateEvent event) {
                FactionManager.initializeFactions();
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
                FactionManager.getFactionData(event.getFaction());
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionCreateNews(FactionManager.getFactionData(event.getFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    FactionData factionData = FactionManager.getFactionData(event.getFaction());
                    FactionRank founderRank = new FactionRank("Founder", 5, "*");
                    factionData.addRank(founderRank);
                    factionData.addMember(event.getPlayer().getName());
                    factionData.getMember(event.getPlayer().getName()).setRank(founderRank);
                }
                updateClientData();
            }
        }, this);

        StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
            @Override
            public void onEvent(PlayerJoinFactionEvent event) {
                FactionManager.getFactionData(event.getFaction());
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionJoinNews(FactionManager.getFactionData(event.getFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) FactionManager.getFactionData(event.getFaction()).addMember(event.getPlayer().getName());
                updateClientData();
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
            @Override
            public void onEvent(PlayerLeaveFactionEvent event) {
                if(event.getFaction() != null) {
                    if(event.getFaction().getMembersUID().keySet().size() <= 1) {
                        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionDisbandNews(FactionManager.getFactionData(event.getFaction())));
                    } else {
                        FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionLeaveNews(FactionManager.getFactionData(event.getFaction()), event.getPlayer()));
                    }
                    FactionManager.getFactionData(event.getFaction());
                }
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) FactionManager.getFactionData(event.getFaction()).removeMember(event.getPlayer().getName());
                updateClientData();
            }
        }, this);
    }

    private void registerPackets() {
        PacketUtil.registerPacket(UpdateClientDataPacket.class);
        PacketUtil.registerPacket(UpdateGUIsPacket.class);
    }

    private void startRunners() {
        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
            updateClientData();
            new StarRunnable() {
                @Override
                public void run() {
                    updateClientData();
                }
            }.runTimer(this, 1000);
        }
    }

    public void updateClientData() {
        if(GameCommon.isDedicatedServer() || GameCommon.isOnSinglePlayer()) {
            for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) PacketUtil.sendPacket(playerState, new UpdateClientDataPacket());
        } else newFactionPanel.updateTabs();
    }
}
