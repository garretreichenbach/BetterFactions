package dovtech.betterfactions;

import api.listener.Listener;
import api.listener.events.gui.PlayerGUICreateEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import dovtech.betterfactions.gui.faction.NewFactionPanel;
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
    private final String version = "0.4.1";

    //Config
    private final String[] defaultConfig = {
            "debug-mode: false",
            "save-interval: 12000"
    };
    public boolean debugMode = false;
    public int saveInterval = 12000;

    @Override
    public void onGameStart() {
        inst = this;
        initialize();
    }

    @Override
    public void onEnable() {
        initConfig();
        registerListeners();
    }

    private void initialize() {
        setModName("BetterFactions");
        setModAuthor("TheDerpGamer");
        setModDescription("Improves faction interaction and diplomacy.");
        setModSMVersion("0.202.108");
        setModVersion(version);
    }

    private void registerListeners() {
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

    private void initConfig() {
        FileConfiguration config = getConfig("config");
        config.saveDefault(defaultConfig);

        this.debugMode = config.getConfigurableBoolean("debug-mode", false);
        this.saveInterval = config.getConfigurableInt("save-interval", 12000);
    }
}
