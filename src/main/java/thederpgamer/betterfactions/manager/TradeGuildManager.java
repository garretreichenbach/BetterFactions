package thederpgamer.betterfactions.manager;

import api.listener.events.Event;
import api.listener.events.player.PlayerDeathEvent;
import org.schema.game.common.data.player.PlayerState;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [09/16/2021]
 */
public class TradeGuildManager {

    public static void handleAggressionEvent(Event e) {
        if(e instanceof PlayerDeathEvent) {
            PlayerDeathEvent event = (PlayerDeathEvent) e;
            if(event.getDamager().isSegmentController()) {

            } else if(event.getDamager() instanceof PlayerState) {

            }
        }
    }
}
