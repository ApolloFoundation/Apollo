/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import org.json.simple.JSONObject;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class JsonBuffer implements NamedBuffer {
    private final JSONObject jsonObject;

    public JsonBuffer() {
        this(new JSONObject());
    }

    public JsonBuffer(JSONObject jsonObject) {
        Objects.requireNonNull(jsonObject);
        this.jsonObject = jsonObject;
    }

    @Override
    public WriteBuffer put(String tag, byte value) {
        jsonObject.put(tag, value);
        return this;
    }

    @Override
    public WriteBuffer put(String tag, byte[] value) {
        jsonObject.put(tag, value);
        return this;
    }

    @Override
    public WriteBuffer put(String tag, boolean value) {
        jsonObject.put(tag, value);
        return this;
    }

    @Override
    public WriteBuffer put(String tag, long value) {
        return null;
    }

    @Override
    public WriteBuffer put(String tag, String hex) {
        jsonObject.put(tag, hex);
        return this;
    }

    @Override
    public WriteBuffer put(String tag, BigInteger value) {
        jsonObject.put(tag, value);
        return this;
    }

    public WriteBuffer put(String tag, JSONObject value) {
        jsonObject.put(tag, value);
        return this;
    }

    @Override
    public byte[] toByteArray() {
        return jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public WriteBuffer concat(byte[] bytes) {
        throw new UnsupportedOperationException();
    }
}
