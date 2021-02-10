package thederpgamer.betterfactions.gui.faction.news;

import org.schema.common.util.StringTools;
import org.schema.schine.common.language.Lng;
import thederpgamer.betterfactions.data.faction.FactionData;
import thederpgamer.betterfactions.game.faction.Federation;
import java.io.Serializable;
import java.util.Date;

/**
 * FactionNewsEntry.java
 * <Description>
 * ==================================================
 * Created 02/10/2021
 * @author TheDerpGamer
 */
public class FactionNewsEntry implements Serializable {

    public enum FactionNewsType {
        ALL, EVENT, DIPLOMATIC, FACTION, FEDERATION, WAR, ECONOMIC, RELEVANT;

        @Override
        public String toString() {
            return Lng.str(StringTools.format(super.toString()));
        }
    }

    private Object subject;
    public final String date;
    public final FactionNewsType type;
    public final String title;
    public final String text;
    private boolean read;

    public FactionNewsEntry(FactionNewsType type, String title, String text) {
        this.date = new Date().toString();
        this.type = type;
        this.title = Lng.str(title);
        this.text = Lng.str(text);
        this.read = false;
        this.subject = null;
    }

    public FactionNewsEntry(FactionNewsType type, FactionData subject, String title, String text) {
        this.date = new Date().toString();
        this.type = type;
        this.title = Lng.str(title);
        this.text = Lng.str(text);
        this.read = false;
        this.subject = subject;
    }

    public FactionNewsEntry(FactionNewsType type, Federation subject, String title, String text) {
        this.date = new Date().toString();
        this.type = type;
        this.title = Lng.str(title);
        this.text = Lng.str(text);
        this.read = false;
        this.subject = subject;
    }

    public boolean isRead() {
        return read;
    }

    public void markRead(boolean read) {
        this.read = read;
    }

    public boolean hasSubject() {
        return subject != null;
    }

    public Object getSubject() {
        if(subject != null) {
            if (subject instanceof FactionData) {
                FactionData factionData = (FactionData) subject;
                return factionData;
            } else if (subject instanceof Federation) {
                Federation federation = (Federation) subject;
                return federation;
            }
        }
        return null;
    }
}
