/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author andrew.zinchenko@gmail.com
 */
class SmcCallMethodAttachmentTest {

    final SmcCallMethodAttachment expected = SmcCallMethodAttachment.builder()
        .methodName("purchase")
        .methodParams("\"123\", \"0x0A0B0C0D0E0F\"")
        .fuelLimit(BigInteger.valueOf(5000L))
        .fuelPrice(BigInteger.valueOf(100L))
        .build();

    @SneakyThrows
    @Test
    void putMyJSON() {
        //GIVEN
        String jsonObj = expected.getJSONObject().toJSONString();
        JSONObject json = (JSONObject) new JSONParser().parse(jsonObj);

        //WHEN
        SmcCallMethodAttachment attachment = new SmcCallMethodAttachment(json);

        //THEN
        assertEquals(expected.getMethodName(), attachment.getMethodName());
        assertEquals(expected.getMethodParams(), attachment.getMethodParams());
    }

    @SneakyThrows
    @Test
    void putMyBytes_ByteBuffer() {
        //GIVEN
        ByteBuffer buffer = ByteBuffer.allocate(expected.getSize());
        //WHEN
        expected.putBytes(buffer);
        buffer.rewind();
        SmcCallMethodAttachment attachment = new SmcCallMethodAttachment(buffer);
        //THEN
        assertEquals(expected.getMethodName(), attachment.getMethodName());
        assertEquals(expected.getMethodParams(), attachment.getMethodParams());
        assertEquals(expected.getFuelLimit(), attachment.getFuelLimit());
        assertEquals(expected.getFuelPrice(), attachment.getFuelPrice());
    }
}