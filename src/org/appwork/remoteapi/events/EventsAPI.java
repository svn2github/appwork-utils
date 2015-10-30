/**
 * 
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany   
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 * 	
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header. 	
 * 	
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the 
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 * 	
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.remoteapi.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.events.json.EventObjectStorable;
import org.appwork.remoteapi.events.json.PublisherResponse;
import org.appwork.remoteapi.events.json.SubscriptionResponse;
import org.appwork.remoteapi.events.json.SubscriptionStatusResponse;
import org.appwork.remoteapi.events.local.LocalEventsAPIEvent;
import org.appwork.remoteapi.events.local.LocalEventsAPIEventSender;
import org.appwork.remoteapi.exceptions.APIFileNotFoundException;
import org.appwork.remoteapi.exceptions.InternalApiException;

/**
 * @author daniel
 * 
 */
public class EventsAPI implements EventsAPIInterface, RemoteAPIEventsSender {
    private LocalEventsAPIEventSender localEventSender;

    /**
     * 
     */
    public EventsAPI() {
        localEventSender = new LocalEventsAPIEventSender();
    }

    public LocalEventsAPIEventSender getLocalEventSender() {
        return localEventSender;
    }

    protected final ConcurrentHashMap<Long, Subscriber> subscribers = new ConcurrentHashMap<Long, Subscriber>(8, 0.9f, 1);

    public ArrayList<Subscriber> getSubscribers() {
        return new ArrayList<Subscriber>(this.subscribers.values());
    }

    protected CopyOnWriteArrayList<EventPublisher> publishers             = new CopyOnWriteArrayList<EventPublisher>();
    protected final Object                         subscribersCleanupLock = new Object();
    protected Thread                               cleanupThread          = null;

    @Override
    public SubscriptionResponse addsubscription(final long subscriptionid, final String[] subscriptions, final String[] exclusions) {
        final Subscriber subscriber = this.subscribers.get(subscriptionid);
        if (subscriber == null) {
            return new SubscriptionResponse();
        } else {
            synchronized (subscriber.getModifyLock()) {
                if (exclusions != null) {
                    final ArrayList<String> newExclusions = new ArrayList<String>(Arrays.asList(subscriber.getExclusions()));
                    newExclusions.addAll(Arrays.asList(exclusions));
                    subscriber.setExclusions(newExclusions.toArray(new String[] {}));
                }
                if (subscriptions != null) {
                    final ArrayList<String> newSubscriptions = new ArrayList<String>(Arrays.asList(subscriber.getSubscriptions()));
                    newSubscriptions.addAll(Arrays.asList(subscriptions));
                    subscriber.setSubscriptions(newSubscriptions.toArray(new String[] {}));
                }
            }
            final SubscriptionResponse ret = new SubscriptionResponse(subscriber);
            ret.setSubscribed(true);
            localEventSender.fireEvent(new LocalEventsAPIEvent(this, LocalEventsAPIEvent.Type.CHANNEL_UPDATE, subscriber));
            return ret;
        }
    }

    @Override
    public SubscriptionResponse changesubscriptiontimeouts(final long subscriptionid, final long polltimeout, final long maxkeepalive) {
        final Subscriber subscriber = this.subscribers.get(subscriptionid);
        if (subscriber == null) {
            return new SubscriptionResponse();
        } else {
            subscriber.setMaxKeepalive(maxkeepalive);
            subscriber.setPollTimeout(polltimeout);
            subscriber.notifyListener();
            final SubscriptionResponse ret = new SubscriptionResponse(subscriber);
            ret.setSubscribed(true);
            localEventSender.fireEvent(new LocalEventsAPIEvent(this, LocalEventsAPIEvent.Type.CHANNEL_UPDATE, subscriber));
            return ret;
        }
    }

    @Override
    public SubscriptionResponse getsubscription(final long subscriptionid) {
        final Subscriber subscriber = this.subscribers.get(subscriptionid);
        if (subscriber == null) {
            return new SubscriptionResponse();
        } else {
            final SubscriptionResponse ret = new SubscriptionResponse(subscriber);
            ret.setSubscribed(true);
            return ret;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.appwork.remoteapi.events.EventsAPIInterface#getsubscriptionstatus
     * (long)
     */
    @Override
    public SubscriptionStatusResponse getsubscriptionstatus(final long subscriptionid) {
        final Subscriber subscriber = this.subscribers.get(subscriptionid);
        if (subscriber == null) {
            return new SubscriptionStatusResponse();
        } else {
            subscriber.keepAlive();
            final SubscriptionStatusResponse ret = new SubscriptionStatusResponse(subscriber);
            ret.setSubscribed(true);
            return ret;
        }
    }

    public List<EventPublisher> list() {
        return Collections.unmodifiableList(this.publishers);
    }

    @Override
    public void listen(final RemoteAPIRequest request, final RemoteAPIResponse response, final long subscriptionid) throws APIFileNotFoundException, InternalApiException {
        final Subscriber subscriber = this.subscribers.get(subscriptionid);
        if (subscriber == null) { throw new APIFileNotFoundException();

        }
        final ArrayList<EventObject> events = new ArrayList<EventObject>();
        final ArrayList<EventObjectStorable> eventStorables = new ArrayList<EventObjectStorable>();
        try {
            EventObject event;
            while ((event = subscriber.poll(events.size() == 0 ? subscriber.getPollTimeout() : 0)) != null && this.subscribers.get(subscriptionid) == subscriber) {
                events.add(event);
                eventStorables.add(new EventObjectStorable(event));
            }
        } catch (final InterruptedException e) {
        }
        try {
            response.getRemoteAPI().writeStringResponse(eventStorables, null, request, response);
        } catch (final Throwable e) {
            subscriber.pushBack(events);
            throw new InternalApiException(e);

        }
    }

    @Override
    public List<PublisherResponse> listpublisher() {
        final ArrayList<PublisherResponse> ret = new ArrayList<PublisherResponse>();
        for (final EventPublisher publisher : this.publishers) {
            ret.add(new PublisherResponse(publisher));
        }
        return ret;
    }

    public List<Long> publishEvent(final EventObject event, final List<Long> subscriptionids) {
        ArrayList<Subscriber> publishTo = new ArrayList<Subscriber>();
        final ArrayList<Long> ret = new ArrayList<Long>();
        if (subscriptionids != null && subscriptionids.size() > 0) {
            /* publish to given subscriptionids */
            for (final long subscriptionid : subscriptionids) {
                final Subscriber subscriber = this.subscribers.get(subscriptionid);
                if (subscriber != null) {
                    publishTo.add(subscriber);
                }
            }
        } else {
            /* publish to all subscribers */
            publishTo = new ArrayList<Subscriber>(this.subscribers.values());
        }
        for (final Subscriber subscriber : publishTo) {

            if (push(subscriber, event)) {
                ret.add(subscriber.getSubscriptionID());
            }
        }
        return ret;
    }

    public synchronized boolean register(final EventPublisher publisher) {
        if (publisher == null) { throw new NullPointerException(); }
        if (publisher.getPublisherName() == null) { throw new IllegalArgumentException("no Publishername given"); }
        for (final EventPublisher existingPublisher : this.publishers) {
            if (existingPublisher == publisher) { return false; }
            if (publisher.getPublisherName().equalsIgnoreCase(existingPublisher.getPublisherName())) { throw new IllegalArgumentException("publisher with same name already registered"); }
        }
        this.publishers.add(publisher);
        publisher.register(this);
        return true;
    }

    @Override
    public SubscriptionResponse removesubscription(final long subscriptionid, final String[] subscriptions, final String[] exclusions) {
        final Subscriber subscriber = this.subscribers.get(subscriptionid);
        if (subscriber == null) {
            return new SubscriptionResponse();
        } else {
            synchronized (subscriber.getModifyLock()) {
                if (exclusions != null) {
                    final ArrayList<String> newExclusions = new ArrayList<String>(Arrays.asList(subscriber.getExclusions()));
                    newExclusions.removeAll(Arrays.asList(exclusions));
                    subscriber.setExclusions(newExclusions.toArray(new String[] {}));
                }
                if (subscriptions != null) {
                    final ArrayList<String> newSubscriptions = new ArrayList<String>(Arrays.asList(subscriber.getSubscriptions()));
                    newSubscriptions.removeAll(Arrays.asList(subscriptions));
                    subscriber.setSubscriptions(newSubscriptions.toArray(new String[] {}));
                }

            }
            final SubscriptionResponse ret = new SubscriptionResponse(subscriber);
            ret.setSubscribed(true);
            localEventSender.fireEvent(new LocalEventsAPIEvent(this, LocalEventsAPIEvent.Type.CHANNEL_UPDATE, subscriber));
            return ret;
        }
    }

    @Override
    public SubscriptionResponse setsubscription(final long subscriptionid, final String[] subscriptions, final String[] exclusions) {
        final Subscriber subscriber = this.subscribers.get(subscriptionid);
        if (subscriber == null) {
            return new SubscriptionResponse();
        } else {
            synchronized (subscriber.getModifyLock()) {
                final ArrayList<String> newExclusions = new ArrayList<String>();
                if (exclusions != null) {
                    newExclusions.addAll(Arrays.asList(exclusions));
                }
                subscriber.setExclusions(newExclusions.toArray(new String[] {}));

                final ArrayList<String> newSubscriptions = new ArrayList<String>();
                if (subscriptions != null) {
                    newSubscriptions.addAll(Arrays.asList(subscriptions));
                }
                subscriber.setSubscriptions(newSubscriptions.toArray(new String[] {}));
            }
            final SubscriptionResponse ret = new SubscriptionResponse(subscriber);
            ret.setSubscribed(true);
            localEventSender.fireEvent(new LocalEventsAPIEvent(this, LocalEventsAPIEvent.Type.CHANNEL_UPDATE, subscriber));
            return ret;
        }
    }

    @Override
    public SubscriptionResponse subscribe(final String[] subscriptions, final String[] exclusions) {
        final Subscriber subscriber = new Subscriber(subscriptions, exclusions);
        this.subscribers.put(subscriber.getSubscriptionID(), subscriber);
        this.subscribersCleanupThread();
        final SubscriptionResponse ret = new SubscriptionResponse(subscriber);
        ret.setSubscribed(true);
        localEventSender.fireEvent(new LocalEventsAPIEvent(this, LocalEventsAPIEvent.Type.CHANNEL_OPENED, subscriber));

        return ret;
    }

    /*
     * starts a cleanupThread (if needed) to remove subscribers that are no
     * longer alive
     * 
     * current implementation has a minimum delay of 1 minute
     */
    protected void subscribersCleanupThread() {
        synchronized (this.subscribersCleanupLock) {
            if (this.cleanupThread == null || this.cleanupThread.isAlive() == false) {
                this.cleanupThread = null;
            } else {
                return;
            }
            this.cleanupThread = new Thread("EventsAPI:subscribersCleanupThread") {
                @Override
                public void run() {
                    try {
                        while (Thread.currentThread() == EventsAPI.this.cleanupThread) {
                            try {
                                Thread.sleep(60 * 1000);
                                final Iterator<Entry<Long, Subscriber>> it = EventsAPI.this.subscribers.entrySet().iterator();
                                while (it.hasNext()) {
                                    final Entry<Long, Subscriber> next = it.next();
                                    final Subscriber subscriber = next.getValue();
                                    if (subscriber.getLastPolledTimestamp() + subscriber.getMaxKeepalive() < System.currentTimeMillis()) {
                                        it.remove();
                                        final long subscriptionid = subscriber.getSubscriptionID();
                                        try {
                                            localEventSender.fireEvent(new LocalEventsAPIEvent(EventsAPI.this, LocalEventsAPIEvent.Type.CHANNEL_CLOSED, subscriber));

                                        } catch (final Throwable e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                synchronized (EventsAPI.this.subscribersCleanupLock) {
                                    if (EventsAPI.this.subscribers.size() == 0) {
                                        EventsAPI.this.cleanupThread = null;
                                        break;
                                    }
                                }
                            } catch (final Throwable e) {
                            }
                        }
                    } finally {
                        synchronized (EventsAPI.this.subscribersCleanupLock) {
                            if (Thread.currentThread() == EventsAPI.this.cleanupThread) {
                                EventsAPI.this.cleanupThread = null;
                            }
                        }
                    }
                };
            };
            this.cleanupThread.setDaemon(true);
            this.cleanupThread.start();
        }
    }

    public synchronized boolean unregister(final EventPublisher publisher) {
        if (publisher == null) { throw new NullPointerException(); }
        final boolean removed = this.publishers.remove(publisher);
        publisher.unregister(this);
        return removed;
    }

    @Override
    public SubscriptionResponse unsubscribe(final long subscriptionid) {
        final Subscriber subscriber = this.subscribers.remove(subscriptionid);
        if (subscriber != null) {
            subscriber.notifyListener();
            try {
                localEventSender.fireEvent(new LocalEventsAPIEvent(this, LocalEventsAPIEvent.Type.CHANNEL_CLOSED, subscriber));

            } catch (final Throwable e) {
                e.printStackTrace();
            }
            return new SubscriptionResponse(subscriber);
        }
        return new SubscriptionResponse();
    }

    /**
     * @param subscriber
     * @param eventObject
     */
    public boolean push(Subscriber subscriber, EventObject eventObject) {
        if (subscriber.isSubscribed(eventObject)) {
            subscriber.push(eventObject);
            subscriber.notifyListener();
            return true;
        }
        return false;

    }

    /**
     * @param key
     * @param value
     */
    public void push(Subscriber subscriber, List<EventObject> value) {
        ArrayList<EventObject> filtered = new ArrayList<EventObject>();

        for (EventObject o : value) {
            if (subscriber.isSubscribed(o)) {
                filtered.add(o);
            }
        }
        if (filtered.size() > 0) {
            subscriber.push(filtered);
            subscriber.notifyListener();

        }

    }
}
