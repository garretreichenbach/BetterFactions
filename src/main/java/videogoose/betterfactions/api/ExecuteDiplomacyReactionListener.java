package videogoose.betterfactions.api;

import api.event.EventResult;
import api.event.EventType;
import videogoose.betterfactions.diplomacy.DiplomacyReaction;
import videogoose.betterfactions.diplomacy.DiplomacyEntity;

/**
 * [Description]
 *
 * @author VideoGoose
 */
public interface ExecuteDiplomacyReactionListener {

    /**
     * Dispatch token. Register with {@code ExecuteDiplomacyReactionListener.TYPE.register(listener, mod)}.
     */
    EventType<ExecuteDiplomacyReactionListener> TYPE = new EventType<>(ExecuteDiplomacyReactionListener.class);

    /**
     * @return {@link EventResult#CANCEL} to stop the action. Any listener cancelling wins.
     */
    EventResult onExecuteDiplomacyReaction(DiplomacyEntity entity, DiplomacyReaction reaction, boolean isServer);
}
