package net.dovtech.betterfactions.event;

import api.entity.Entity;
import api.entity.Player;
import api.listener.events.EntityDamageEvent;
import net.dovtech.betterfactions.BetterFactions;
import net.dovtech.betterfactions.faction.BetterFaction;
import org.schema.game.client.data.GameClientState;

import java.io.IOException;

public class NonAggressionPactDamageWarning {

    public void onEntityDamageEvent(EntityDamageEvent e) throws IOException {
        if(e.wasShotByEntity()) {
            Entity damager = e.getDamagerEntity();
            Entity entity = e.getEntity();
            int aggressorFactionid = damager.getFaction().getID();
            int entityFactionid = entity.getFaction().getID();
            GameClientState gameClientState = GameClientState.instance;

            BetterFaction aggressor = BetterFactions.factions.get(aggressorFactionid);
            BetterFaction target = BetterFactions.factions.get(entityFactionid);

            if(aggressor.getNonAggressionPacts().contains(target) || target.getNonAggressionPacts().contains(aggressor)) {
                Player aggressorPlayer = damager.getPilot();
                e.cancelWarDec();
                e.cancelDamage();
                aggressorPlayer.sendServerMessage("[INFO]: You cannot attack " + entity.getFaction().getName() + " while you have a non-aggression pact with them!");
            }
        }
    }
}
