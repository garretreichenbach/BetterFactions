package thederpgamer.betterfactions;

import api.DebugFile;
import api.common.GameClient;
import api.common.GameCommon;
import api.listener.Listener;
import api.listener.events.controller.ServerInitializeEvent;
import api.listener.events.faction.FactionCreateEvent;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.listener.events.player.PlayerJoinFactionEvent;
import api.listener.events.player.PlayerLeaveFactionEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.schine.resource.ResourceLoader;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.data.faction.FactionRank;
import thederpgamer.betterfactions.gui.NewFactionPanel;
import thederpgamer.betterfactions.manager.ConfigManager;
import thederpgamer.betterfactions.manager.ResourceManager;
import thederpgamer.betterfactions.manager.TradeGuildManager;
import thederpgamer.betterfactions.manager.data.DataManager;
import thederpgamer.betterfactions.manager.data.FactionDataManager;
import thederpgamer.betterfactions.manager.data.FactionMemberManager;
import thederpgamer.betterfactions.network.client.CreateNewFederationPacket;
import thederpgamer.betterfactions.network.client.ModifyFactionMessagePacket;
import thederpgamer.betterfactions.network.client.RequestDataPacket;
import thederpgamer.betterfactions.network.client.SendFactionMessagePacket;
import thederpgamer.betterfactions.network.server.SendDataPacket;
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
        DataManager.initializeManagers();
        registerListeners();
        registerPackets();
    }

    @Override
    public void onServerCreated(ServerInitializeEvent event) {
        if(GameCommon.getGameState() != null) {
            FactionDataManager.instance.initialize();
            FactionMemberManager.instance.initialize();
        }
    }


    @Override
    public void onResourceLoad(ResourceLoader resourceLoader) {
        ResourceManager.loadResources(resourceLoader);
    }

    private void registerListeners() {
        StarLoader.registerListener(PlayerGUICreateEvent.class, new Listener<PlayerGUICreateEvent>() {
            @Override
            public void onEvent(PlayerGUICreateEvent event) {
                FactionDataManager.instance.initialize();
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
                FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction());
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionCreateNews(FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                FactionData factionData = FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction());
                FactionRank founderRank = new FactionRank("Founder", 4, "*");
                factionData.addMember(event.getPlayer().getName());
                factionData.getMember(event.getPlayer().getName()).setRank(founderRank);
                FactionNewsUtils.saveData();
                FactionDataManager.instance.saveData(factionData);
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) newFactionPanel.recreateTabs();
            }
        }, this);

        StarLoader.registerListener(PlayerJoinFactionEvent.class, new Listener<PlayerJoinFactionEvent>() {
            @Override
            public void onEvent(PlayerJoinFactionEvent event) {
                FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction());
                FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionJoinNews(FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction()), event.getPlayer()));
                FactionNewsUtils.saveData();
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) {
                    if(FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction()).getMembers().size() <= 1) {
                        FactionRank founderRank = new FactionRank("Founder", 4, "*");
                        FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction()).getMembers().get(0).setRank(founderRank);
                    }
                }
                FactionNewsUtils.saveData();
                FactionData factionData = FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction());
                factionData.addMember(event.getPlayer().getName());
                FactionDataManager.instance.saveData(factionData);
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) newFactionPanel.recreateTabs();
            }
        }, this);

        StarLoader.registerListener(PlayerLeaveFactionEvent.class, new Listener<PlayerLeaveFactionEvent>() {
            @Override
            public void onEvent(PlayerLeaveFactionEvent event) {
                if(event.getFaction() != null) {
                    if(event.getFaction().getMembersUID().keySet().size() <= 1) FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionDisbandNews(FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction())));
                    else FactionNewsUtils.addNewsEntry(FactionNewsUtils.getFactionLeaveNews(FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction()), event.getPlayer()));
                    FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction());
                }
                FactionNewsUtils.saveData();
                FactionData factionData = FactionDataManager.instance.getFactionData(event.getFaction().getIdFaction());
                if(factionData != null) {
                    factionData.removeMember(event.getPlayer().getName());
                    FactionDataManager.instance.saveData(factionData);
                }
                if(newFactionPanel != null && newFactionPanel.getOwnPlayer().equals(event.getPlayer())) newFactionPanel.recreateTabs();
            }
        }, this);

        StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
            @Override
            public void onEvent(PlayerDeathEvent event) {
                TradeGuildManager.handleAggressionEvent(event);
            }
        }, this);
    }

    private void registerPackets() {
        PacketUtil.registerPacket(RequestDataPacket.class);
        PacketUtil.registerPacket(SendDataPacket.class);
        PacketUtil.registerPacket(UpdateGUIsPacket.class);
        PacketUtil.registerPacket(CreateNewFederationPacket.class);
        PacketUtil.registerPacket(SendFactionMessagePacket.class);
        PacketUtil.registerPacket(ModifyFactionMessagePacket.class);
    }
}
