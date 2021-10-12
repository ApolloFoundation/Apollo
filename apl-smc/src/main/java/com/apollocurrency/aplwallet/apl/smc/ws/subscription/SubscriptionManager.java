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
import org.eclipse.jetty.websocket.api.WebSocketException;

import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SubscriptionManager {
    static final int MAX_SIZE = 200;
    private final RegisteredSocketContainer registeredSockets;

    private final Converter<SmcEventSubscriptionRequest, Subscription> converter;

    public SubscriptionManager() {
        this(new RegisteredSocketContainer());
    }

    public SubscriptionManager(RegisteredSocketContainer registeredSockets) {
        this.registeredSockets = registeredSockets;
        this.converter = new RequestToSubscriptionConverter();
    }

    public int getRegisteredSocketsCount() {
        return registeredSockets.size();
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
            if (!registeredSockets.register(address, socket)) {
                throw new WebSocketException("The socket is already registered.");
            } else {
                return true;
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
        if (registeredSockets.isRegistered(address, socket)) {
            if (!request.getEvents().isEmpty()) {
                return registeredSockets.addSubscription(address, socket, converter.convert(request));
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
        if (registeredSockets.isRegistered(address, socket)) {
            if (!request.getEvents().isEmpty()) {
                var event = request.getEvents().get(0);
                return registeredSockets.removeSubscription(address, socket, event.getSignature());
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
        var signature = toHex(contractEvent.getSignature());
        var sockets = registeredSockets.getSubscriptionSockets(contractEvent.getContract(), signature);
        sockets.forEach(socket -> {
            var subscription = socket.getSubscription();
            if (subscription.getFromBlock() <= contractEvent.getHeight()
                && subscription.getFilter().test(params.getMap())) {
                response.setSubscriptionId(subscription.getSubscriptionId());
                response.setParsedParams(params.getMap());
                socket.getSocket().sendWebSocket(response);
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
