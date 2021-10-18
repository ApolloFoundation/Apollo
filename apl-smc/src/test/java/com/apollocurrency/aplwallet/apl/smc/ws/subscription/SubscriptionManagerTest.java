/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.aplwallet.apl.smc.model.AplAddress;
import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import com.apollocurrency.aplwallet.apl.util.Convert2;
import com.apollocurrency.smc.contract.vm.event.EventArguments;
import com.apollocurrency.smc.contract.vm.event.SmcContractEvent;
import com.apollocurrency.smc.data.expr.FalseTerm;
import com.apollocurrency.smc.data.expr.TrueTerm;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.ContractEventType;
import com.apollocurrency.smc.util.HexUtils;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author andrew.zinchenko@gmail.com
 */
class SubscriptionManagerTest {

    static {
        Convert2.init("APL", 1739068987193023818L);
    }

    SubscriptionManager manager;
    RegisteredSocketContainer container = spy(new RegisteredSocketContainer());
    Address address = new AplAddress(-1234567890L);
    SmcEventSocket socket = mock(SmcEventSocket.class);
    String signature = "0x010203040506070809";
    Subscription subscription = Subscription.builder()
        .subscriptionId("0x01")
        .signature(signature)
        .filter(new TrueTerm())//this subscription should be fired
        .fromBlock(0)
        .build();
    SmcEventSubscriptionRequest.Event event = SmcEventSubscriptionRequest.Event.builder()
        .subscriptionId(subscription.getSubscriptionId())
        .name("Transfer")
        .signature(signature)
        .fromBlock("0")
        .build();

    @BeforeEach
    void setUp() {
        manager = new SubscriptionManager(container);
    }

    @Test
    void register() {
        //GIVEN empty container
        //WHEN
        manager.register(address, socket);
        //THEN
        verify(container).register(address, socket);
        assertEquals(1, container.size());
    }

    @Test
    void registerQueueIsFull() {
        //GIVEN empty container
        //WHEN
        for (int i = 0; i < manager.MAX_SIZE; i++)
            manager.register(new AplAddress(i), socket);
        //THEN
        assertThrows(WebSocketException.class, () -> manager.register(address, socket));
    }

    @Test
    void addSubscription() {
        //GIVEN empty container
        //WHEN
        var rc = manager.addSubscription(address, socket, mock(SmcEventSubscriptionRequest.class));
        //THEN
        assertFalse(rc);

        //GIVEN
        var request = SmcEventSubscriptionRequest.builder()
            .requestId("1")
            .operation(SmcEventSubscriptionRequest.Operation.SUBSCRIBE)
            .events(List.of(event))
            .build();
        container.register(address, socket);
        //WHEN
        rc = manager.addSubscription(address, socket, request);
        //THEN
        assertTrue(rc);
        assertEquals(1, container.size());
        verify(container).addSubscription(eq(address), eq(socket), any());
    }

    @Test
    void remove() {
        //GIVEN empty container
        //WHEN
        manager.remove(address, socket);
        //THEN
        verify(container).remove(address, socket);
    }

    @Test
    void removeSubscription() {
        //GIVEN empty container
        //WHEN
        var rc = manager.removeSubscription(address, socket, mock(SmcEventSubscriptionRequest.class));
        //THEN
        assertFalse(rc);

        //GIVEN
        var request = SmcEventSubscriptionRequest.builder()
            .requestId("1")
            .operation(SmcEventSubscriptionRequest.Operation.SUBSCRIBE)
            .events(List.of(event))
            .build();

        container.register(address, socket);
        container.addSubscription(address, socket, subscription);
        //WHEN
        rc = manager.removeSubscription(address, socket, request);
        //THEN
        assertTrue(rc);
        assertEquals(1, container.size());
        verify(container).removeSubscription(eq(address), eq(socket), any());
    }

    @Test
    void fire() {
        //GIVEN
        //second subscription
        var socket2 = mock(SmcEventSocket.class);
        var subscription2 = Subscription.builder()
            .subscriptionId("0x02")
            .signature(signature)
            .filter(new FalseTerm())//this subscription should be skipped
            .fromBlock(0)
            .build();
        //registered subscriptions
        when(container.getSubscriptionSockets(address, signature))
            .thenReturn(List.of(
                new RegisteredSocketContainer.SubscriptionSocket(socket, subscription)
                , new RegisteredSocketContainer.SubscriptionSocket(socket2, subscription2)));
        //
        var params = mock(EventArguments.class);
        when(params.getMap()).thenReturn(new HashMap<>());
        //catch event
        var event = SmcContractEvent.builder()
            .eventType(ContractEventType.builder()
                .spec("Transfer:from,to,amount")
                .indexedFieldsCount(2)
                .anonymous(false)
                .build())
            .signature(HexUtils.parseHex(signature))
            .transaction(address)
            .contract(address)
            .txIdx(0)
            .build();
        //WHEN
        manager.fire(event, params);
        //THEN
        verify(container).getSubscriptionSockets(address, signature);
        verify(socket).sendWebSocket(any());
        verifyNoInteractions(socket2);
    }
}