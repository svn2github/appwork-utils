package org.appwork.remoteapi.events.local;

import java.util.EventListener;

import org.appwork.remoteapi.events.Subscriber;

public interface LocalEventsAPIListener extends EventListener {

    void onEventsChannelUpdate(Subscriber subscriber);

    void onEventChannelOpened(Subscriber subscriber);

    void onEventChannelClosed(Subscriber subscriber);

}