package videogoose.betterfactions.api;

import api.event.EventResult;
import api.event.EventType;

import videogoose.betterfactions.diplomacy.DiplomacyAction;
import videogoose.betterfactions.diplomacy.DiplomacyEntity;

/**
 * [Description]
 *
 * @author VideoGoose
 */
public interface DiplomacyActionListener {

    /**
     * Dispatch token. Register with {@code DiplomacyActionListener.TYPE.register(listener, mod)}.
     */
    EventType<DiplomacyActionListener> TYPE = new EventType<>(DiplomacyActionListener.class);

    /**
     * @return {@link EventResult#CANCEL} to stop the action. Any listener cancelling wins.
     */
    EventResult onDiplomacyAction(DiplomacyEntity entity, java.util.Map<String, DiplomacyAction> actions, DiplomacyAction action, boolean isServer);
}
