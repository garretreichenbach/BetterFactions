package thederpgamer.betterfactions.utils;

import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.common.language.Lng;
import thederpgamer.betterfactions.BetterFactions;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.game.faction.Federation;
import thederpgamer.betterfactions.gui.faction.news.FactionNewsEntry;
import java.util.ArrayList;
import java.util.Date;

/**
 * FactionNewsUtils.java
 * <Description>
 * ==================================================
 * Created 02/10/2021
 * @author TheDerpGamer
 */
public class FactionNewsUtils {

    public static final ArrayList<FactionNewsEntry> factionNews = new ArrayList<>();

    public static void addNewsEntry(FactionNewsEntry newsEntry) {
        factionNews.add(newsEntry);
        saveData();
        BetterFactions.getInstance().newFactionPanel.factionNewsTab.updateTab();
    }

    public static void saveData() {
        ArrayList<FactionNewsEntry> newsEntriesToDelete = new ArrayList<>();
        for(FactionNewsEntry newsEntry : factionNews) {
            int daysBetween = GeneralUtils.getDaysBetween(GeneralUtils.getCurrentDate(), new Date(newsEntry.date));
            if(daysBetween > BetterFactions.getInstance().maxNewsBackup) {
                newsEntriesToDelete.add(newsEntry);
            } else {
                PersistentObjectUtil.addObject(BetterFactions.getInstance().getSkeleton(), newsEntry);
            }
        }
        for(FactionNewsEntry newsEntry : newsEntriesToDelete) factionNews.remove(newsEntry);
    }

    public static void loadData() {
        ArrayList<Object> fNewsObjects = PersistentObjectUtil.getObjects(BetterFactions.getInstance().getSkeleton(), FactionNewsEntry.class);
        for(Object newsObject : fNewsObjects) {
            factionNews.add((FactionNewsEntry) newsObject);
        }
        saveData();
    }

    public static FactionNewsEntry getFactionCreateNews(FactionData faction, PlayerState player) {
        String factionName = faction.getFactionName();
        String playerName = player.getName();
        String title = Lng.str("Creation of the " + factionName + ".");
        String text = Lng.str("A group led by an individual known as " + playerName + " has officially declared the creation of a new state known as " + factionName + ".\n" +
                "With this, they are now recognized as a faction and will be able to participate in galactic politics.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FACTION, faction, title, text);
    }

    public static FactionNewsEntry getFactionDisbandNews(FactionData faction) {
        String factionName = faction.getFactionName();
        String title = Lng.str(factionName + " has been disbanded.");
        String text = Lng.str("With the last member of " + factionName + " leaving, the faction been officially disbanded.\n" +
                "There is now one less force in the galaxy.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FACTION, faction, title, text);
    }

    public static FactionNewsEntry getFactionJoinNews(FactionData faction, PlayerState player) {
        String factionName = faction.getFactionName();
        String playerName = player.getName();
        String title = Lng.str(playerName + " has joined the " + factionName + ".");
        String text = Lng.str(playerName + " has officially been accepted as a new member of the " + factionName + ".\n" +
                "They shall now stand together.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FACTION, faction, title, text);
    }

    public static FactionNewsEntry getFactionLeaveNews(FactionData faction, PlayerState player) {
        String factionName = faction.getFactionName();
        String playerName = player.getName();
        String title = Lng.str(playerName + " has left the " + factionName + ".");
        String text = Lng.str(playerName + " has officially cut ties with the " + factionName + " and is no longer considered a member.\n" +
                "They shall now stand alone.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FACTION, faction, title, text);
    }

    public static FactionNewsEntry getFederationCreateNews(Federation federation) {
        String faction1Name = federation.getMembers().get(0).getFactionName();
        String faction2Name = federation.getMembers().get(0).getFactionName();
        String title = Lng.str("Formation of the " + federation.getName() + ".");
        String text = Lng.str(
                faction1Name + " has formed a new federation with " + faction2Name + " known as " + federation.getName() + ".\n" +
                        "They now stand together as a new force in the galaxy, ready to defend and achieve their goals, whatever those may be.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FEDERATION, federation, title, text);
    }

    public static FactionNewsEntry getFederationDisbandNews(Federation federation) {
        String title = Lng.str(federation.getName() + " has been disbanded.");
        String text = Lng.str("With the last member of " + federation.getName() + " leaving, the federation has been officially disbanded.\n" +
                "There is now one less great power in the galaxy.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FEDERATION, federation, title, text);
    }

    public static FactionNewsEntry getFederationJoinNews(Federation federation, FactionData faction) {
        String title = Lng.str(faction.getFactionName() + " has joined the " + federation.getName());
        String text = Lng.str(faction.getFactionName() + " has officially been accepted as a new member of the " + federation.getName() + ".\n" +
                "They will now join forces with their new allies and work together for their mutual interests.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FEDERATION, federation, title, text);
    }

    public static FactionNewsEntry getFederationLeaveNews(Federation federation, FactionData faction) {
        String title = Lng.str(faction.getFactionName() + " has left the " + federation.getName());
        String text = Lng.str(faction.getFactionName() + " has officially severed ties with the " + federation.getName() + ".\n" +
                "They shall now stand alone.");
        return new FactionNewsEntry(FactionNewsEntry.FactionNewsType.FEDERATION, federation, title, text);
    }
}
