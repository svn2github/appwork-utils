package org.appwork.remoteapi.events.local;

import org.appwork.remoteapi.events.Subscriber;
import org.appwork.utils.event.Eventsender;

public class LocalEventsAPIEventSender extends Eventsender<LocalEventsAPIListener, LocalEventsAPIEvent> {

    @Override
    protected void fireEvent(LocalEventsAPIListener listener, LocalEventsAPIEvent event) {
        switch (event.getType()) {
        case CHANNEL_CLOSED:
            listener.onEventChannelClosed(((Subscriber) event.getParameter()));
            return;
        case CHANNEL_OPENED:
            listener.onEventChannelOpened(((Subscriber) event.getParameter()));
            return;
        case CHANNEL_UPDATE:
            listener.onEventsChannelUpdate(((Subscriber) event.getParameter()));
            return;
        default:
            System.out.println("Unhandled Event: " + event);
        }
    }
}