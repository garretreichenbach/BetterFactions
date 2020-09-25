package dovtech.betterfactions.player;

public class PlayerHistory {

    private String date;
    private String event;

    public PlayerHistory(String date, String event) {
        this.date = date;
        this.event = event;
    }

    public String getDate() {
        return date;
    }

    public String getEvent() {
        return event;
    }
}
