package org.appwork.remoteapi.events.local;

import org.appwork.remoteapi.events.EventsAPI;
import org.appwork.utils.event.SimpleEvent;

public class LocalEventsAPIEvent extends SimpleEvent<Object, Object, LocalEventsAPIEvent.Type> {

    public static enum Type {
        CHANNEL_OPENED,
        CHANNEL_CLOSED,
        CHANNEL_UPDATE
    }

    public LocalEventsAPIEvent(EventsAPI api, Type type, Object... parameters) {
        super(api, type, parameters);
    }
}