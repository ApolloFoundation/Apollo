/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket;
import com.apollocurrency.smc.data.type.Address;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Slf4j
public class RegisteredSocketContainer {
    private final Map<Address, Map<SmcEventSocket, Map<String, SubscriptionSocket>>> registeredSockets;

    static class SubscriptionSocket {
        @Getter
        private final Subscription subscription;
        @Getter
        private final SmcEventSocket socket;

        public SubscriptionSocket(SmcEventSocket socket, Subscription subscription) {
            this.socket = socket;
            this.subscription = subscription;
        }
    }

    public RegisteredSocketContainer() {
        this.registeredSockets = new HashMap<>();
    }

    RegisteredSocketContainer(Map<Address, Map<SmcEventSocket, Map<String, SubscriptionSocket>>> registeredSockets) {
        this.registeredSockets = registeredSockets;
    }

    public int size() {
        return registeredSockets.values().stream().mapToInt(Map::size).sum();
    }

    public boolean register(Address address, SmcEventSocket socket) {
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        if (m.containsKey(socket)) {
            log.debug("Socket already registered, socket={}.", socket);
            return false;
        } else {
            m.put(socket, new HashMap<>());
            log.debug("Register new socket, socket={}.", socket);
            return true;
        }
    }

    public boolean remove(Address address, SmcEventSocket socket) {
        if (registeredSockets.containsKey(address)) {
            var m = registeredSockets.get(address);
            if (m.containsKey(socket)) {
                m.remove(socket);
                log.debug("Remove socket, socket={}.", socket);
                return true;
            }
        }
        log.debug("Socket is not registered, socket={}.", socket);
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
            log.debug("Subscription already registered, subscription={}.", subscription);
            return false;
        } else {
            e.put(subscription.getSignature(), new SubscriptionSocket(socket, subscription));
            log.debug("Register new subscription, subscription={}.", subscription);
            return true;
        }
    }

    public boolean removeSubscription(Address address, SmcEventSocket socket, String eventSignature) {
        var e = getEntry(address, socket);
        if (e.containsKey(eventSignature)) {
            e.remove(eventSignature);
            log.debug("Remove subscription on signature, signature={}.", eventSignature);
            return true;
        } else {
            log.debug("Subscription on signature is not registered, signature={}.", eventSignature);
            return false;
        }
    }
}
