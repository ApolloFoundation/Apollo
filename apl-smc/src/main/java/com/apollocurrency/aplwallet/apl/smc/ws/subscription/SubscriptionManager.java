/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventMessage;
import com.apollocurrency.smc.contract.vm.event.SmcContractEvent;
import com.apollocurrency.smc.data.type.Address;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.eclipse.jetty.websocket.api.WebSocketException;

import java.util.Collection;

import static com.apollocurrency.smc.util.HexUtils.toHex;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SubscriptionManager {
    private static final int MAX_SIZE = 200;
    private final Multimap<Address, SmcEventSocket> multimap;

    public SubscriptionManager() {
        this.multimap = Multimaps.synchronizedMultimap(HashMultimap.create());
    }

    public boolean register(Address address, SmcEventSocket socket) {
        if (multimap.size() < MAX_SIZE) {
            return multimap.put(address, socket);
        } else {
            throw new WebSocketException("The queue size exceeds the MAX value.");
        }
    }

    Collection<SmcEventSocket> get(Address address) {
        return multimap.get(address);
    }

    public boolean remove(Address address, SmcEventSocket session) {
        return multimap.remove(address, session);
    }

    /**
     * Broadcast given event to all subscribers.
     *
     * @param contractEvent the fired contract event
     */
    public void fire(SmcContractEvent contractEvent) {
        var response = convert(contractEvent);
        var sockets = get(contractEvent.getContract());
        sockets.forEach(socket -> socket.sendWebSocket(response));
    }

    private static SmcEventMessage convert(SmcContractEvent contractEvent) {
        return SmcEventMessage.builder()
            .name(contractEvent.getName())
            .address(contractEvent.getContract().getHex())
            .signature(toHex(contractEvent.getSignature()))
            .transactionIndex(contractEvent.getTxIdx())
            .transaction(contractEvent.getTransaction().getHex())
            .data(contractEvent.getState())
            .build();
    }
}
