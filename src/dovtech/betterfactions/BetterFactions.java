package dovtech.betterfactions;

import api.DebugFile;
import api.common.GameClient;
import api.common.GameServer;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.inventory.ItemStack;
import api.entity.StarPlayer;
import api.listener.Listener;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import api.utils.StarRunnable;
import api.utils.gui.GUIUtils;
import dovtech.betterfactions.contracts.Contract;
import dovtech.betterfactions.contracts.target.*;
import dovtech.betterfactions.gui.contracts.ContractsScrollableList;
import dovtech.betterfactions.gui.contracts.PlayerContractsScrollableList;
import dovtech.betterfactions.util.DataUtil;
import org.newdawn.slick.Image;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.input.InputState;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

public class BetterFactions extends StarMod {

    static BetterFactions inst;

    public BetterFactions() {
        inst = this;
    }

    //Resources
    private String resourcesPath;

    //Server
    private final File moddataFolder = new File("moddata");
    private final File betterFactionsDataFolder = new File("moddata/BetterFactions");
    private final File contractsFolder = new File("moddata/BetterFactions/contract");
    private final File playerDataFolder = new File("moddata/BetterFactions/player");
    public Image defaultLogo;

    //Config
    private FileConfiguration config;
    private String[] defaultConfig = {
            "debug-mode: false",
            "write-frequency: 12000"
    };

    //Config Settings
    public boolean debugMode;
    public int writeFrequency;

    public static void main(String[] args) {

    }

    @Override
    public void onGameStart() {
        inst = this;
        setModName("BetterFactions");
        setModAuthor("Dovtech");
        setModVersion("0.3.1");
        setModDescription("Improves faction interaction and diplomacy.");

        resourcesPath = this.getClass().getResource("").getPath();

        if (!moddataFolder.exists()) moddataFolder.mkdirs();
        if (!betterFactionsDataFolder.exists()) betterFactionsDataFolder.mkdirs();
        if (!contractsFolder.exists()) contractsFolder.mkdirs();
        if (!playerDataFolder.exists()) playerDataFolder.mkdirs();

        initConfig();
        registerListeners();

        try {
            DataUtil.readData();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new StarRunnable() {
            @Override
            public void run() {
                DataUtil.writeData();
            }
        }.runTimer(writeFrequency);
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
                            GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
                            GUIMainWindow guiWindow = new GUIMainWindow(GameClient.getClientState(), 1200, 650, "CONTRACTS");
                            guiWindow.onInit();

                            GUIContentPane contractsPane = guiWindow.addTab("CONTRACTS");
                            contractsPane.setTextBoxHeightLast(300);
                            PlayerContractsScrollableList playerContractsList = new PlayerContractsScrollableList(GameClient.getClientState(), 500, 300, contractsPane.getContent(0));
                            playerContractsList.onInit();
                            contractsPane.getContent(0).attach(playerContractsList);

                            GUIContentPane historyPane = guiWindow.addTab("HISTORY");
                            historyPane.setTextBoxHeightLast(300);

                            GUIUtils.activateCustomGUIWindow(guiWindow);
                            //Todo

                            //guiWindow.onInit();
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationHighlightCallback() {
                    @Override
                    public boolean isHighlighted(InputState inputState) {
                        return false;
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
                }
            }
        });

        DebugFile.log("Registered Listeners!", this);
    }

    private void initConfig() {
        this.config = getConfig("config");
        this.config.saveDefault(defaultConfig);

        this.debugMode = Boolean.parseBoolean(this.config.getString("debug-mode"));
        this.writeFrequency = this.config.getInt("write-frequency");
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


        if(DataUtil.contracts.size() <= 15) {
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
                contractTarget.setTarget(new StarPlayer(GameClient.getClientPlayerState()));
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
                        contractTarget.setTarget(new StarPlayer(GameClient.getClientPlayerState()));
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


                Contract randomContract = new Contract(GameServer.getServerState().getFactionManager().getFaction(-2), contractName, randomContractType, randomReward, UUID.randomUUID().toString(), contractTarget);
                DataUtil.contracts.add(randomContract);
                DataUtil.contractWriteBuffer.add(randomContract);
            }
            DataUtil.writeData();
        }
    }
}
