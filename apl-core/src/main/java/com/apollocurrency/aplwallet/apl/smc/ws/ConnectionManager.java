/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.smc.data.type.Address;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Collection;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class ConnectionManager {
    private static final int MAX_SIZE = 200;
    private final Multimap<Address, Session> multimap;

    public ConnectionManager() {
        this.multimap = Multimaps.synchronizedMultimap(HashMultimap.create());
    }

    public boolean register(Address address, Session session) {
        if (multimap.size() < MAX_SIZE) {
            return multimap.put(address, session);
        } else {
            throw new RuntimeException("The queue size exceeds the MAX value.");
        }
    }

    Collection<Session> get(Address address) {
        return multimap.get(address);
    }

    public boolean remove(Address address, Session session) {
        return multimap.remove(address, session);
    }

}
