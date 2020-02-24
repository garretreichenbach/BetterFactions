package net.dovtech.betterfactions.gui;

import api.faction.Faction;
import api.gui.GUIMenu;
import api.gui.GUIPanel;

public class FactionInfoGUI {

    private Faction faction;
    private String guiName = "Faction Info";
    private int[] relativePos = { 0, 0 };
    private int[] preferredSize = { 100, 100 };
    private int[] minSize = { 50, 50 };
    private int[] maxSize = { 150, 150 };

    public GUIMenu factionInfoGUI = new GUIMenu(guiName, relativePos, preferredSize, minSize, maxSize);

    public GUIPanel infoPanel = new GUIPanel(factionInfoGUI, new int[] { -50, 50}, new int[] { 50, 30 }, new int[] { 30, 15}, new int[] { 70, 50});
}
