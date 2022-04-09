package thederpgamer.betterfactions.utils;

import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.common.language.Lng;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.old.faction.FactionDataOld;
import thederpgamer.betterfactions.data.old.federation.FederationData;
import thederpgamer.betterfactions.gui.faction.news.FactionNewsEntry;
import thederpgamer.betterfactions.manager.ConfigManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * FactionNewsUtils.java
 * <Description>
 *
 * @since 02/10/2021
 * @author TheDerpGamer
 */
public class FactionNewsUtils {

    private static final ModSkeleton instance = BetterFactions.getInstance().getSkeleton();

    public static void addNewsEntry(FactionNewsEntry newsEntry) {
        PersistentObjectUtil.addObject(instance, newsEntry);
        saveData();
        BetterFactions.getInstance().newFactionPanel.factionNewsTab.updateTab();
    }

    public static HashMap<String, FactionNewsEntry> getNewsMap() {
        saveData();
        HashMap<String, FactionNewsEntry> newsMap = new HashMap<>();
        for(Object newsObject : PersistentObjectUtil.getObjects(instance, FactionNewsEntry.class)) {
            FactionNewsEntry newsEntry = (FactionNewsEntry) newsObject;
            newsMap.put(newsEntry.date, newsEntry);
        }
        return newsMap;
    }

    public static void saveData() {
        ArrayList<FactionNewsEntry> toRemove = new ArrayList<>();
        for(Object newsObject : PersistentObjectUtil.getObjects(instance, FactionNewsEntry.class)) {
            FactionNewsEntry newsEntry = (FactionNewsEntry) newsObject;
            int daysBetween = GeneralUtils.getDaysBetween(GeneralUtils.getCurrentDate(), new Date(newsEntry.date));
            if(daysBetween > ConfigManager.getMainConfig().getInt("max-news-backup")) toRemove.add(newsEntry);
        }
        for(FactionNewsEntry newsEntry : toRemove) PersistentObjectUtil.removeObject(instance, newsEntry);
        PersistentObjectUtil.save(instance);
    }

    public static FactionNewsEntry getFactionCreateNews(FactionDataOld faction, PlayerState player) {
        String factionName = faction.getFactionName();
        String playerName = player.getName();
        String title = Lng.str("Creation of the " + factionName + ".");
        String text = Lng.str("A group led by an individual known as " + playerName + " has officially declared the creation of a new state known as " + factionName + ".\n" +
                "With this, they are now recognized as a faction and will be able to participate in galactic politics.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FACTION, faction, title, text);
    }

    public static FactionNewsEntry getFactionDisbandNews(FactionDataOld faction) {
        String factionName = faction.getFactionName();
        String title = Lng.str(factionName + " has been disbanded.");
        String text = Lng.str("With the last member of " + factionName + " leaving, the faction been officially disbanded.\n" +
                "There is now one less force in the galaxy.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FACTION, faction, title, text);
    }

    public static FactionNewsEntry getFactionJoinNews(FactionDataOld faction, PlayerState player) {
        String factionName = faction.getFactionName();
        String playerName = player.getName();
        String title = Lng.str(playerName + " has joined the " + factionName + ".");
        String text = Lng.str(playerName + " has officially been accepted as a new member of the " + factionName + ".\n" +
                "They shall now stand together.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FACTION, faction, title, text);
    }

    public static FactionNewsEntry getFactionLeaveNews(FactionDataOld faction, PlayerState player) {
        String factionName = faction.getFactionName();
        String playerName = player.getName();
        String title = Lng.str(playerName + " has left the " + factionName + ".");
        String text = Lng.str(playerName + " has officially cut ties with the " + factionName + " and is no longer considered a member.\n" +
                "They shall now stand alone.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FACTION, faction, title, text);
    }

    public static FactionNewsEntry getFederationCreateNews(FederationData federationData) {
        String faction1Name = federationData.getMembers().get(0).getFactionName();
        String faction2Name = federationData.getMembers().get(0).getFactionName();
        String title = Lng.str("Formation of the " + federationData.getName() + ".");
        String text = Lng.str(
                faction1Name + " has formed a new federation with " + faction2Name + " known as " + federationData.getName() + ".\n" +
                "They now stand together as a new force in the galaxy, ready to defend and achieve their goals, whatever those may be.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FEDERATION, federationData, title, text);
    }

    public static FactionNewsEntry getFederationDisbandNews(FederationData federationData) {
        String title = Lng.str(federationData.getName() + " has been disbanded.");
        String text = Lng.str("With the last member of " + federationData.getName() + " leaving, the federation has been officially disbanded.\n" +
                              "There is now one less great power in the galaxy.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FEDERATION, federationData, title, text);
    }

    public static FactionNewsEntry getFederationJoinNews(FederationData federationData, FactionDataOld faction) {
        String title = Lng.str(faction.getFactionName() + " has joined the " + federationData.getName());
        String text = Lng.str(faction.getFactionName() + " has officially been accepted as a new member of the " + federationData.getName() + ".\n" +
                              "They will now join forces with their new allies and work together for their mutual interests.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FEDERATION, federationData, title, text);
    }

    public static FactionNewsEntry getFederationLeaveNews(FederationData federationData, FactionDataOld faction) {
        String title = Lng.str(faction.getFactionName() + " has left the " + federationData.getName());
        String text = Lng.str(faction.getFactionName() + " has officially severed ties with the " + federationData.getName() + ".\n" +
                              "They shall now stand alone.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FEDERATION, federationData, title, text);
    }
}
