/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket;
import com.apollocurrency.smc.data.type.Address;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class RegisteredSocketContainer {
    private final Map<Address, Map<SmcEventSocket, Map<String, SubscriptionSocket>>> registeredSockets;

    static class SubscriptionSocket {
        final Subscription subscription;
        final SmcEventSocket socket;

        public SubscriptionSocket(SmcEventSocket socket, Subscription subscription) {
            this.socket = socket;
            this.subscription = subscription;
        }
    }

    public RegisteredSocketContainer() {
        this.registeredSockets = new HashMap<>();
    }

    public int size() {
        return registeredSockets.size();
    }

    public boolean register(Address address, SmcEventSocket socket) {
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        if (m.containsKey(socket)) {
            return false;
        } else {
            m.put(socket, new HashMap<>());
            return true;
        }
    }

    public boolean remove(Address address, SmcEventSocket socket) {
        if (registeredSockets.containsKey(address)) {
            var m = registeredSockets.get(address);
            if (m.containsKey(socket)) {
                m.remove(socket);
                return true;
            }
        }
        return false;
    }

    public boolean isRegistered(Address address, SmcEventSocket socket) {
        if (registeredSockets.containsKey(address)) {
            var m = registeredSockets.get(address);
            return m.containsKey(socket);
        } else {
            return false;
        }
    }

    public Map<String, SubscriptionSocket> getEntry(Address address, SmcEventSocket socket) {
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        m.computeIfAbsent(socket, key -> new HashMap<>());
        return m.get(socket);
    }

    public Collection<SubscriptionSocket> getSubscriptionSockets(Address address, String eventSignature) {
        var m = registeredSockets.get(address);
        if (m == null) {
            return Collections.emptyList();
        } else {
            return m.values().stream().map(socketMap -> socketMap.get(eventSignature)).collect(Collectors.toList());
        }
    }

    public boolean addSubscription(Address address, SmcEventSocket socket, Subscription subscription) {
        var e = getEntry(address, socket);
        if (e.containsKey(subscription.getSignature())) {
            //Subscription already registered
            return false;
        } else {
            e.put(subscription.getSignature(), new SubscriptionSocket(socket, subscription));
            return true;
        }
    }

    public boolean removeSubscription(Address address, SmcEventSocket socket, String eventSignature) {
        var e = getEntry(address, socket);
        if (e.containsKey(eventSignature)) {
            e.remove(eventSignature);
            return true;
        } else {
            return false;
        }
    }
}
