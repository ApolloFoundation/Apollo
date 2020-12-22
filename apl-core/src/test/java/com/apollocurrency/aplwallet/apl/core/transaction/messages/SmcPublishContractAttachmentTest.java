/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.util.rlp.RlpWriteBuffer;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * @author andrew.zinchenko@gmail.com
 */
class SmcPublishContractAttachmentTest {

    final SmcPublishContractAttachment expected = SmcPublishContractAttachment.builder()
        .contractSource("class A {}")
        .constructorParams(List.of("Deal","123","0x0A0B0C0D0E0F"))
        .build();

    @Test
    void putMyJSON() {
        //GIVEN
        JSONObject json = expected.getJSONObject();

        //WHEN
        SmcPublishContractAttachment attachment = new SmcPublishContractAttachment(json);

        //THEN
        assertEquals(expected.getContractSource(), attachment.getContractSource());
        assertArrayEquals(
            expected.getConstructorParams().toArray(new String[0]),
            attachment.getConstructorParams().toArray(new String[0])
        );
    }

    @Test
    void putMyBytes() {
        //GIVEN
        RlpWriteBuffer buffer = new RlpWriteBuffer();
        expected.putBytes(buffer);

        //WHEN
        SmcPublishContractAttachment attachment = new SmcPublishContractAttachment(buffer.toByteArray());

        //THEN
        assertEquals(expected.getContractSource(), attachment.getContractSource());
        assertArrayEquals(
            expected.getConstructorParams().toArray(new String[0]),
            attachment.getConstructorParams().toArray(new String[0])
        );
    }

    @Test
    void putMyBytesWithException() {
        //GIVEN
        ByteBuffer buffer = ByteBuffer.allocate(1);

        //WHEN
        //THEN
        assertThrows(UnsupportedOperationException.class,() -> expected.putBytes(buffer));
    }
}