/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket;
import com.apollocurrency.aplwallet.apl.smc.ws.converter.RequestToSubscriptionConverter;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventMessage;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;
import com.apollocurrency.aplwallet.apl.util.api.converter.Converter;
import com.apollocurrency.smc.contract.vm.event.NamedParameters;
import com.apollocurrency.smc.data.type.Address;
import com.apollocurrency.smc.data.type.ContractEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.experimental.Delegate;
import org.eclipse.jetty.websocket.api.WebSocketException;

import java.util.HashMap;
import java.util.Map;

import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SubscriptionManager {
    private static final int MAX_SIZE = 200;
    private final Multimap<Address, SmcEventSocket> registeredSockets;

    private final Converter<SmcEventSubscriptionRequest, Subscription> converter;

    private static class SubscriptionSocket extends SmcEventSocket {
        @Delegate
        //Map of <signature, subscription>
        Map<String, Subscription> subscriptionMap;

        public SubscriptionSocket(SmcEventSocket socket) {
            super(socket);
            this.subscriptionMap = new HashMap<>();
        }
    }

    public SubscriptionManager() {
        this.registeredSockets = Multimaps.synchronizedMultimap(HashMultimap.create());
        this.converter = new RequestToSubscriptionConverter();
    }

    /**
     * Registers the listener for any events from the contract address.
     *
     * @param address the contract address
     * @param socket  the incoming socket object
     * @return true if listener registered successfully.
     */
    public boolean register(Address address, SmcEventSocket socket) {
        if (registeredSockets.size() < MAX_SIZE) {
            if (registeredSockets.containsEntry(address, socket)) {
                throw new WebSocketException("The socket is already registered.");
            } else {
                return registeredSockets.put(address, new SubscriptionSocket(socket));
            }
        } else {
            throw new WebSocketException("The queue size exceeds the MAX value.");
        }
    }

    /**
     * Registers the listener for any events from the contract address.
     *
     * @param address the contract address
     * @param socket  the incoming socket object
     * @param request the incoming subscription request
     * @return true if subscription added successfully.
     */
    public boolean addSubscription(Address address, SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        if (registeredSockets.containsEntry(address, socket)) {
            var subscription = (SubscriptionSocket) registeredSockets.get(address);
            if (!request.getEvents().isEmpty()) {
                var event = request.getEvents().get(0);
                if (subscription.containsKey(event.getSignature())) {
                    //Subscription already registered
                    return false;
                } else {
                    subscription.put(event.getSignature(), converter.convert(request));
                }
            }
            return true;
        } else {
            throw new WebSocketException("The socket is not registered.");
        }
    }

    public void remove(Address address, SmcEventSocket session) {
        registeredSockets.remove(address, session);
    }

    public boolean removeSubscription(Address address, SmcEventSocket socket, SmcEventSubscriptionRequest request) {
        if (registeredSockets.containsEntry(address, socket)) {
            var subscription = (SubscriptionSocket) registeredSockets.get(address);
            if (!request.getEvents().isEmpty()) {
                var event = request.getEvents().get(0);
                if (subscription.containsKey(event.getSignature())) {
                    subscription.remove(event.getSignature());
                } else {
                    //Subscription doesn't exist
                    return false;
                }
            }
            return true;
        } else {
            throw new WebSocketException("The socket is not registered.");
        }
    }

    /**
     * Broadcast given event to all subscribers.
     *
     * @param contractEvent the fired contract event
     */
    public void fire(ContractEvent contractEvent, NamedParameters params) {
        var response = toMessage(contractEvent);
        var sockets = registeredSockets.get(contractEvent.getContract());
        sockets.forEach(s -> {
            var socket = (SubscriptionSocket) s;
            var subscription = socket.get(toHex(contractEvent.getSignature()));
            if (subscription != null && subscription.getFromBlock() <= contractEvent.getHeight() && subscription.getFilter().test(params.getMap())) {
                response.setSubscriptionId(subscription.getSubscriptionId());
                socket.sendWebSocket(response);
            }
        });
    }

    private static SmcEventMessage toMessage(ContractEvent contractEvent) {
        return SmcEventMessage.builder()
            .name(contractEvent.getSpec())
            .address(contractEvent.getContract().getHex())
            .signature(toHex(contractEvent.getSignature()))
            .transactionIndex(contractEvent.getTxIdx())
            .transaction(contractEvent.getTransaction().getHex())
            .data(contractEvent.getState())
            .build();
    }
}
