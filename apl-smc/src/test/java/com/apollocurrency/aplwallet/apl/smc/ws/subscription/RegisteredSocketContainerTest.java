/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.data.type.Address;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
class RegisteredSocketContainerTest {
    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    Address address = new AplAddress(1);
    SmcEventSocket socket = mock(SmcEventSocket.class);
    String signature = "0x010203040506070809";
    RegisteredSocketContainer container;

    Map<Address, Map<SmcEventSocket, Map<String, RegisteredSocketContainer.SubscriptionSocket>>> registeredSockets = spy(new HashMap<>());

    @BeforeEach
    void setUp() {
        container = new RegisteredSocketContainer(registeredSockets);
    }

    @Test
    void size() {
        //GIVEN empty container
        //WHEN
        assertEquals(0, container.size());

        //GIVEN
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        m.put(socket, new HashMap<>());
        //WHEN //THEN
        assertEquals(1, container.size());

        //GIVEN
        m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        m.remove(socket);
        //WHEN //THEN
        assertEquals(1, registeredSockets.size());
        assertEquals(0, container.size());
    }

    @Test
    void register() {
        //GIVEN empty container
        //WHEN
        var rc = container.register(address, socket);
        //THEN
        assertTrue(rc);
        assertTrue(registeredSockets.get(address).containsKey(socket));
        //check already registered
        //WHEN
        rc = container.register(address, socket);
        //THEN
        assertFalse(rc);
    }

    @Test
    void remove() {
        //GIVEN empty container
        //WHEN
        var rc = container.remove(address, socket);
        //THEN
        assertFalse(rc);

        //GIVEN
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        m.put(socket, new HashMap<>());
        //WHEN
        assertEquals(1, container.size());
        rc = container.remove(address, socket);
        //THEN
        assertTrue(rc);
        m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        assertFalse(m.containsKey(socket));
        assertEquals(0, container.size());
    }

    @Test
    void isRegistered() {
        //GIVEN empty container
        //WHEN
        var rc = container.isRegistered(address, socket);
        //THEN
        assertFalse(rc);

        //GIVEN
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        m.put(socket, new HashMap<>());
        //WHEN
        rc = container.isRegistered(address, socket);
        //THEN
        assertTrue(rc);
    }

    @Test
    void getEntry() {
        //GIVEN empty container
        //WHEN
        var rc = container.getEntry(address, socket);
        //THEN
        assertTrue(rc.isEmpty());
        //GIVEN
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        m.put(socket, new HashMap<>());
        //WHEN
        rc = container.getEntry(address, socket);
        //THEN
        assertEquals(0, rc.size());
        assertTrue(rc.isEmpty());
    }

    @Test
    void getSubscriptionSockets() {
        //GIVEN empty container
        //WHEN
        var rc = container.getSubscriptionSockets(address, signature);
        //THEN
        assertTrue(rc.isEmpty());

        //GIVEN
        var subscription = mock(Subscription.class);
        when(subscription.getSignature()).thenReturn(signature);
        var ssocket = new RegisteredSocketContainer.SubscriptionSocket(socket, subscription);
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        Map<String, RegisteredSocketContainer.SubscriptionSocket> sm = new HashMap<>();
        sm.put(signature, ssocket);
        m.put(socket, sm);
        //WHEN
        rc = container.getSubscriptionSockets(address, signature);
        //THEN
        assertTrue(rc.contains(ssocket));
        var ss = rc.iterator().next();
        assertEquals(subscription, ss.getSubscription());
        assertEquals(socket, ss.getSocket());
    }

    @Test
    void addSubscription() {
        //GIVEN
        var subscription = mock(Subscription.class);
        when(subscription.getSignature()).thenReturn(signature);
        //WHEN
        var rc = container.addSubscription(address, socket, subscription);
        //THEN
        assertTrue(rc);
        var m = registeredSockets.get(address);
        var ss = m.get(socket).get(subscription.getSignature());
        assertEquals(subscription, ss.getSubscription());
    }

    @Test
    void removeSubscription() {
        //GIVEN empty container
        //WHEN
        var rc = container.removeSubscription(address, socket, signature);
        //THEN
        assertFalse(rc);

        //GIVEN
        var subscription = mock(Subscription.class);
        when(subscription.getSignature()).thenReturn(signature);
        var ssocket = new RegisteredSocketContainer.SubscriptionSocket(socket, subscription);
        var m = registeredSockets.computeIfAbsent(address, key -> new HashMap<>());
        Map<String, RegisteredSocketContainer.SubscriptionSocket> sm = new HashMap<>();
        sm.put(signature, ssocket);
        m.put(socket, sm);
        //WHEN
        rc = container.removeSubscription(address, socket, signature);
        //THEN
        assertTrue(rc);
        m = registeredSockets.get(address);
        rc = m.get(socket).containsKey(subscription.getSignature());
        assertFalse(rc);
    }
}