/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpWriteBuffer;
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

    @Test
    void putMyBytes() {
        //GIVEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        RlpList.RlpListBuilder listBuilder = RlpList.builder();
        expected.putBytes(listBuilder);
        buffer.write(listBuilder.build());
        byte[] input = buffer.toByteArray();

        //WHEN
        RlpReader reader = new RlpReader(input).readListReader().readListReader();
        reader.readByte();//read appendix flag = 0x00
        SmcCallMethodAttachment attachment = new SmcCallMethodAttachment(reader);

        //THEN
        assertEquals(expected.getMethodName(), attachment.getMethodName());
        assertEquals(expected.getMethodParams(), attachment.getMethodParams());
    }

    @Test
    void putMyBytesV2() {
        //GIVEN
        ByteBuffer buffer = ByteBuffer.allocate(expected.getMySize());
        //WHEN
        expected.putBytes(buffer);
        //THEN

    }
}