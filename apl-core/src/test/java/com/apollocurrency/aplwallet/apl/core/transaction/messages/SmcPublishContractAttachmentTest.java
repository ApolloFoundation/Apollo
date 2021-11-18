/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.util.rlp.RlpList;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpReader;
import com.apollocurrency.aplwallet.apl.util.rlp.RlpWriteBuffer;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author andrew.zinchenko@gmail.com
 */
class SmcPublishContractAttachmentTest {

    final SmcPublishContractAttachment expected = SmcPublishContractAttachment.builder()
        .contractName("Deal")
        .baseContract("Contract")
        .contractSource("class Deal {}")
        .constructorParams("\"123\", \"0x0A0B0C0D0E0F\"")
        .languageName("js")
        .languageVersion("0.0.1")
        .fuelLimit(BigInteger.valueOf(5000L))
        .fuelPrice(BigInteger.valueOf(100L))
        .build();

    @Test
    void putMyJSON() {
        //GIVEN
        JSONObject json = expected.getJSONObject();

        //WHEN
        SmcPublishContractAttachment attachment = new SmcPublishContractAttachment(json);

        //THEN
        assertEquals(expected.getContractName(), attachment.getContractName());
        assertEquals(expected.getContractSource(), attachment.getContractSource());
        assertEquals(expected.getConstructorParams(), attachment.getConstructorParams());
        assertEquals(expected.getLanguageName(), attachment.getLanguageName());
    }

    @Test
    void putMyBytesToRlpWriter() {
        //GIVEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        RlpList.RlpListBuilder listBuilder = RlpList.builder();
        expected.putBytes(listBuilder);
        buffer.write(listBuilder.build());
        byte[] input = buffer.toByteArray();

        //WHEN
        RlpReader reader = new RlpReader(input).readListReader().readListReader();
        reader.readByte();//read appendix flag = 0x00
        SmcPublishContractAttachment attachment = new SmcPublishContractAttachment(reader);

        //THEN
        assertEquals(expected.getContractName(), attachment.getContractName());
        assertEquals(expected.getContractSource(), attachment.getContractSource());
        assertEquals(expected.getConstructorParams(), attachment.getConstructorParams());
        assertEquals(expected.getLanguageName(), attachment.getLanguageName());
    }

    @SneakyThrows
    @Test
    void putMyBytes_ByteBuffer() {
        //GIVEN
        ByteBuffer buffer = ByteBuffer.allocate(expected.getSize());

        //WHEN
        expected.putBytes(buffer);
        buffer.rewind();
        SmcPublishContractAttachment attachment = new SmcPublishContractAttachment(buffer);
        //THEN
        assertEquals(expected.getContractName(), attachment.getContractName());
        assertEquals(expected.getContractSource(), attachment.getContractSource());
        assertEquals(expected.getConstructorParams(), attachment.getConstructorParams());
        assertEquals(expected.getLanguageName(), attachment.getLanguageName());
        assertEquals(expected.getFuelLimit(), attachment.getFuelLimit());
        assertEquals(expected.getFuelPrice(), attachment.getFuelPrice());
    }
}