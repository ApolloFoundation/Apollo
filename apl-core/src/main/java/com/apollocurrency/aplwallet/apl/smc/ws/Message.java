/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import java.util.HashMap;
import java.util.Map;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class Message {
    public enum OP {
        ERROR_INFO,
        EVENT,
        SUBSCRIBE
    }

    public static final Message EMPTY = new Message(null);

    public OP op;
    public Map<String, Object> data = new HashMap<>();

    public Message(OP op) {
        this.op = op;
    }

    public Message put(String k, Object v) {
        data.put(k, v);
        return this;
    }

    public Object get(String k) {
        return data.get(k);
    }

    public <T> T getType(String key) {
        return (T) data.get(key);
    }

    @Override
    public String toString() {
        return "Message{" +
            "op=" + op +
            ", data=" + data +
            '}';
    }
}
