package dovtech.betterfactions;

import api.DebugFile;
import api.common.GameClient;
import api.common.GameServer;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.inventory.ItemStack;
import api.entity.StarPlayer;
import api.listener.Listener;
import api.listener.events.SegmentControllerOverheatEvent;
import api.listener.events.faction.FactionRelationChangeEvent;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.contracts.target.*;
import dovtech.betterfactions.gui.controlmanagers.contractpanel.ContractMenuControlManager;
import dovtech.betterfactions.faction.BetterFaction;
import dovtech.betterfactions.gui.controlmanagers.viewclaimantspanel.ClaimantsMenuControlManager;
import dovtech.betterfactions.player.BetterPlayer;
import dovtech.betterfactions.gui.contracts.ContractsScrollableList;
import dovtech.betterfactions.gui.faction.alliance.*;
import dovtech.betterfactions.util.DataUtil;
import org.newdawn.slick.Image;
import org.schema.game.client.controller.manager.AbstractControlManager;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.common.data.player.faction.FactionRelation;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.DialogInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.input.InputState;
import java.io.*;
import java.util.*;

public class BetterFactions extends StarMod {

    static BetterFactions inst;

    public BetterFactions() {
        inst = this;
    }

    //Resources
    private String resourcesPath;

    //Server
    private File moddataFolder;
    private File betterFactionsDataFolder;
    public Image defaultLogo;

    //Config
    private FileConfiguration config;
    private String[] defaultConfig = {
            "debug-mode: false",
            "guardian-faction-enable: true",
            "guardian-faction-id: -1"
    };

    //Config Settings
    public boolean debugMode;
    private boolean guardianFactionEnabled;
    private int guardianFactionID;

    public static void main(String[] args) {

    }

    @Override
    public void onGameStart() {
        inst = this;
        setModName("BetterFactions");
        setModAuthor("Dovtech");
        setModVersion("0.2.4");
        setModDescription("Improves faction interaction and diplomacy.");

        resourcesPath = this.getClass().getResource("").getPath();

        moddataFolder = new File("moddata");
        if (!moddataFolder.exists()) moddataFolder.mkdir();
        betterFactionsDataFolder = new File("moddata/BetterFactions");
        if (!betterFactionsDataFolder.exists()) betterFactionsDataFolder.mkdir();

        initConfig();
        registerListeners();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        DebugFile.log("Enabled", this);
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {

    }

    private void registerListeners() {

        StarLoader.registerListener(GUITopBarCreateEvent.class, new Listener<GUITopBarCreateEvent>() {
            @Override
            public void onEvent(final GUITopBarCreateEvent guiTopBarCreateEvent) {
                GUITopBar.ExpandedButton dropDownButton = guiTopBarCreateEvent.getDropdownButtons().get(guiTopBarCreateEvent.getDropdownButtons().size() - 1);
                dropDownButton.addExpandedButton("CONTRACTS", new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if(mouseEvent.pressedLeftMouse()) {
                            for(AbstractControlManager controlManager : GameClient.getClientState().getGlobalGameControlManager().getControlManagers()) {
                                if(!(controlManager instanceof ContractMenuControlManager)) {
                                    controlManager.setActive(false);
                                }
                            }
                            for(DialogInterface p : GameClient.getClientState().getController().getPlayerInputs()) {
                                if(!(p instanceof ContractMenuControlManager)) {
                                    p.deactivate();
                                }
                            }

                            ContractMenuControlManager contractMenuControlManager = new ContractMenuControlManager(GameClient.getClientState());
                            contractMenuControlManager.setActive(true);

                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationHighlightCallback() {
                    @Override
                    public boolean isHighlighted(InputState inputState) {
                        return DataUtil.getContracts(new StarPlayer(GameClient.getClientPlayerState())).size() > 0;
                    }

                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return true;
                    }
                });

                dropDownButton.addExpandedButton("STATS", new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {

                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationHighlightCallback() {
                    @Override
                    public boolean isHighlighted(InputState inputState) {
                        return false; //Todo
                    }

                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return true;
                    }
                });
            }
        });


        StarLoader.registerListener(MainWindowTabAddEvent.class, new Listener<MainWindowTabAddEvent>() {
            @Override
            public void onEvent(MainWindowTabAddEvent event) {
                if (event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_SHOP_SHOPNEW_SHOPPANELNEW_2)) {

                    testFunction();

                    GUIContentPane contractsTab = event.createTab("CONTRACTS");
                    contractsTab.setName("CONTRACTS");
                    contractsTab.setTextBoxHeightLast(300);

                    contractsTab.addDivider(300);
                    /* Faction/Contractor Logo
                    contractsTab.addNewTextBox(0, 150);
                    Sprite contractorLogo = new Sprite(new Texture(0, 0, resourcesPath + "/gui/logo/trading-guild-logo.png")); //Default Contractor
                    GUIOverlay logoOverlay = new GUIOverlay();
                    contractsTab.getContent(0, 0).attach(logoOverlay);
                     */

                    GUITextOverlay contractorDescOverlay = new GUITextOverlay(250, 300, contractsTab.getState());
                    contractorDescOverlay.onInit();
                    contractorDescOverlay.setTextSimple("Placeholder Text");
                    contractsTab.getContent(0, 0).attach(contractorDescOverlay);

                    contractsTab.addNewTextBox(0, 85);

                    ContractsScrollableList contractsScrollableList = new ContractsScrollableList(contractsTab.getState(), 500, 300, contractsTab.getContent(1, 0));
                    contractsScrollableList.onInit();
                    contractsTab.getContent(1, 0).attach(contractsScrollableList);


                } else if (event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_NEWFACTION_FACTIONPANELNEW_9)) {
                    final GUIContentPane allianceTab = event.createTab("ALLIANCE");
                    allianceTab.setName("ALLIANCE");
                    allianceTab.setTextBoxHeightLast(300);

                    allianceTab.addDivider(700);

                    allianceTab.addNewTextBox(0, 100);

                    AllianceInfoBox allianceInfoBox = new AllianceInfoBox(allianceTab.getState(), 130, 100);
                    allianceInfoBox.onInit();
                    allianceTab.getContent(0, 0).attach(allianceInfoBox);

                    allianceTab.addNewTextBox(1, 60);

                    if (GameClient.getClientState().getFaction() != null) {
                        BetterFaction faction = DataUtil.getBetterFaction(GameClient.getClientState().getFaction());
                        if (faction.getAlliance() != null) {
                            AllianceFleetsList allianceFleetsList = new AllianceFleetsList(allianceTab.getState(), allianceTab.getContent(0, 1));
                            allianceFleetsList.onInit();
                            allianceTab.getContent(0, 1).attach(allianceFleetsList);

                            AllianceNewsBox allianceNewsBox = new AllianceNewsBox(allianceTab.getState(), allianceTab.getContent(1, 1));
                            allianceTab.getContent(1, 1).attach(allianceNewsBox);
                            GUITextButton createAllianceButton = new GUITextButton(allianceTab.getState(), 80, 30, "Create Alliance", new GUICallback() {
                                @Override
                                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                    if (mouseEvent.pressedLeftMouse()) {
                                        CreateAlliancePanel createAlliancePanel = new CreateAlliancePanel(allianceTab.getState());
                                        createAlliancePanel.draw();
                                    }
                                }

                                @Override
                                public boolean isOccluded() {
                                    return false;
                                }
                            });
                            createAllianceButton.onInit();
                            allianceTab.getContent(0, 1).attach(createAllianceButton);
                        } else {
                            GUITextOverlay noAllianceOverlay = new GUITextOverlay(70, 70, allianceTab.getState());
                            noAllianceOverlay.setTextSimple("No Alliance");
                            allianceTab.getContent(0, 1).attach(noAllianceOverlay);
                        }

                        GUITextOverlay noNewsOverlay = new GUITextOverlay(70, 70, allianceTab.getState());
                        noNewsOverlay.setTextSimple("No News");
                        allianceTab.getContent(1, 1).attach(noNewsOverlay);
                    }


                    AlliancesScrollableList alliancesScrollableList = new AlliancesScrollableList(allianceTab.getState(), 150, 100, allianceTab.getContent(1, 0));
                    alliancesScrollableList.onInit();
                    allianceTab.getContent(1, 0).attach(alliancesScrollableList);

                    GUIContentPane warsTab = event.createTab("WARS");
                    warsTab.setName("WARS");
                    warsTab.setTextBoxHeightLast(300);
                    allianceInfoBox.draw();

                } else if (event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_NEWFACTION_FACTIONPANELNEW_2)) {
                    event.getPane().setName("FACTION DIPLOMACY");
                } else if (event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_NEWFACTION_FACTIONPANELNEW_9)) {
                    event.getPane().setName("FACTION LIST");
                }
            }
        });

        StarLoader.registerListener(FactionRelationChangeEvent.class, new Listener<FactionRelationChangeEvent>() {
            @Override
            public void onEvent(FactionRelationChangeEvent event) {
                BetterFaction from = DataUtil.getBetterFaction(event.getFrom());
                BetterFaction to = DataUtil.getBetterFaction(event.getTo());
                if (event.getNewRelation().equals(FactionRelation.RType.ENEMY)) {
                    from.getFactionStats().offensiveWars += 1;
                    to.getFactionStats().defensiveWars += 1;
                }
                DataUtil.saveFaction(from);
                DataUtil.saveFaction(to);

            }
        });

        StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
            @Override
            public void onEvent(PlayerDeathEvent event) {
                Faction fromFaction = GameServer.getServerState().getFactionManager().getFaction(event.getDamager().getFactionId());
                Faction toFaction = GameServer.getServerState().getFactionManager().getFaction(event.getPlayer().getFactionId());
                if (fromFaction != null) {
                    BetterFaction from = DataUtil.getBetterFaction(fromFaction);
                    from.getFactionStats().playersKilled += 1;
                    DataUtil.saveFaction(from);
                }

                if (toFaction != null) {
                    BetterFaction to = DataUtil.getBetterFaction(toFaction);
                    to.getFactionStats().playersLost += 1;
                    DataUtil.saveFaction(to);
                }
            }
        });

        StarLoader.registerListener(SegmentControllerOverheatEvent.class, new Listener<SegmentControllerOverheatEvent>() {
            @Override
            public void onEvent(SegmentControllerOverheatEvent event) {
                Faction fromFaction = GameServer.getServerState().getFactionManager().getFaction(event.getLastDamager().getFactionId());
                Faction toFaction = GameServer.getServerState().getFactionManager().getFaction(event.getEntity().getFactionId());
                if (fromFaction != null) {
                    BetterFaction from = DataUtil.getBetterFaction(fromFaction);
                    from.getFactionStats().entitiesKilled += 1;
                    DataUtil.saveFaction(from);
                }

                if (toFaction != null) {
                    BetterFaction to = DataUtil.getBetterFaction(toFaction);
                    to.getFactionStats().entitiesLost += 1;
                    DataUtil.saveFaction(to);
                }
            }
        });

        DebugFile.log("Registered Listeners!", this);
    }

    private void initConfig() {
        this.config = getConfig("config");
        this.config.saveDefault(defaultConfig);

        this.debugMode = Boolean.parseBoolean(this.config.getString("debug-mode"));
        this.guardianFactionEnabled = Boolean.parseBoolean(this.config.getString("guardian-faction-enable"));
        this.guardianFactionID = this.config.getInt("guardian-faction-id");
    }

    public static BetterFactions getInstance() {
        return inst;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public String getResourcesPath() {
        return resourcesPath;
    }

    public void testFunction() {
        //Testing Purposes only

        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            Contract.ContractType randomContractType = Contract.ContractType.BOUNTY;
            int randomTypeInt = random.nextInt(5 - 1) + 1;
            switch (randomTypeInt) {
                case 1:
                    randomContractType = Contract.ContractType.BOUNTY;
                    break;
                case 2:
                    randomContractType = Contract.ContractType.MINING;
                    break;
                case 3:
                    randomContractType = Contract.ContractType.PRODUCTION;
                    break;
                case 4:
                    randomContractType = Contract.ContractType.CARGO_ESCORT;
                    break;
            }
            int randomReward = (random.nextInt(1000 - 100) + 100) * 1000;

            ContractTarget contractTarget = new PlayerTarget();
            contractTarget.setTarget(new BetterPlayer(new StarPlayer(GameClient.getClientPlayerState())));
            String contractName = "";
            switch (randomContractType) {
                case MINING:
                    contractTarget = new MiningTarget();
                    ItemStack miningTargetStack = new ItemStack(Blocks.THRENS_ORE_RAW.getId());
                    int randomAmount = (random.nextInt(300 - 1) + 1) * 10;
                    miningTargetStack.setAmount(randomAmount);
                    contractTarget.setTarget(miningTargetStack);
                    contractName = "Mine x" + miningTargetStack.getAmount() + " " + miningTargetStack.getName();
                    break;
                case BOUNTY:
                    contractTarget = new PlayerTarget();
                    contractTarget.setTarget(new BetterPlayer(new StarPlayer(GameClient.getClientPlayerState())));
                    contractName = "Kill " + GameClient.getClientPlayerState().getName();
                    break;
                case PRODUCTION:
                    contractTarget = new ProductionTarget();
                    ItemStack productionTargetStatck = new ItemStack(Blocks.REACTOR_POWER.getId());
                    int randomProductionAmount = (random.nextInt(300 - 1) + 1) * 10;
                    productionTargetStatck.setAmount(randomProductionAmount);
                    contractTarget.setTarget(productionTargetStatck);
                    contractName = "Produce x" + randomProductionAmount + " " + productionTargetStatck.getName();
                    break;
                case CARGO_ESCORT:
                    contractTarget = new CargoTarget();
                    ItemStack cargoTargetStack = new ItemStack(Blocks.REACTOR_POWER.getId());
                    int randomCargoAmount = (random.nextInt(300 - 1) + 1) * 10;
                    cargoTargetStack.setAmount(randomCargoAmount);
                    contractTarget.setTarget(cargoTargetStack);
                    contractTarget.setLocation(new int[]{2, 2, 2});
                    contractName = "Deliver cargo to (" + contractTarget.getLocation()[0] + "." + contractTarget.getLocation()[1] + "." + contractTarget.getLocation()[2] + ")";
                    break;
            }


            Contract randomContract = new Contract(new BetterFaction(GameServer.getServerState().getFactionManager().getFaction(-2)), contractName, randomContractType, randomReward, contractTarget);
            DataUtil.saveContract(randomContract);
        }

        /*
        Alliance alliance = new Alliance("Dual Monarchy", AllianceGovernmentType.CONFEDERATION);
        alliance.getMembers().add(new BetterFaction(GameClient.getClientState().getFaction()));
        alliance.setAllianceID("0");
        this.getFactionAlliances().put(alliance.getAllianceID(), alliance);

        WarParticipant attacker = new WarParticipant(GameClientState.instance.getFaction(), new WarGoal(GameClientState.instance.getFaction(), GameClientState.instance.getFactionManager().getFaction(-2), WarGoal.WarGoalType.DEFEAT));
        WarParticipant defender = new WarParticipant(GameClientState.instance.getFactionManager().getFaction(-2), new WarGoal(GameClientState.instance.getFactionManager().getFaction(-2), GameClientState.instance.getFaction(), WarGoal.WarGoalType.NONE));

        FactionWar holyWar = new FactionWar(attacker, defender);
        wars.add(holyWar);
         */
    }
}
