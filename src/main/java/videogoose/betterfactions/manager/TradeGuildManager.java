package videogoose.betterfactions.manager;

import api.listener.events.Event;
import api.listener.events.player.PlayerDeathEvent;
import org.schema.game.common.data.player.PlayerState;

public class TradeGuildManager {

    public static void handleAggressionEvent(Event e) {
        if(e instanceof PlayerDeathEvent event) {
            if(event.getDamager().isSegmentController()) {

            } else if(event.getDamager() instanceof PlayerState damager) {

            }
        }
    }
}
