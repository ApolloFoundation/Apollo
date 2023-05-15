/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.data.DexTestData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DexOrderAttachmentV2Test {
    DexTestData td = new DexTestData();
    private final DexOrder order = td.ORDER_BEA_4;
    private final DexOrderAttachmentV2 attachment = new DexOrderAttachmentV2(order);

    @Test
    void createFromDexOrder() {
        DexOrderAttachmentV2 attachment = this.attachment;

        assertEquals("DexOrder_v2", attachment.getAppendixName());
        assertEquals(TransactionTypes.TransactionTypeSpec.DEX_ORDER, attachment.getTransactionTypeSpec());
        assertEquals(order.getFromAddress(), attachment.getFromAddress());
        assertEquals(order.getToAddress(), attachment.getToAddress());
        assertEquals(order.getOrderAmount(), attachment.getOrderAmount());
        assertEquals(order.getFinishTime(), attachment.getFinishTime());
        assertEquals(order.getType().ordinal(), attachment.getType());
        assertEquals(order.getOrderCurrency().ordinal(), attachment.getOrderCurrency());
        assertEquals(order.getStatus().ordinal(), attachment.getStatus());
        assertEquals(order.getPairCurrency().ordinal(), attachment.getPairCurrency());
        assertEquals(pairRate(), attachment.getPairRate());
        assertEquals(attachment.getMySize(), 94);
        assertEquals(attachment.getMyFullSize(), 94);
        assertEquals(attachment.getFullSize(), 95);
        assertEquals(2, attachment.getVersion());
        assertTrue(attachment.verifyVersion(), "DexOrderAttachmentV2 should pass version validation");
    }

    @Test
    void parseFromBytes_invalidFromAddress() {
        ByteBuffer buffer = createDexOrderV1Buffer();
        buffer.putShort((short) 1000);
        buffer.flip();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> new DexOrderAttachmentV2(buffer));

        assertEquals("Max parameter length exceeded", ex.getMessage());
    }


    @SneakyThrows
    @Test
    void parseFromBytes() {
        ByteBuffer buffer = createDexOrderV1Buffer();
        byte[] fromAddressBytes = order.getFromAddress().getBytes();
        buffer.putShort((short) fromAddressBytes.length);
        buffer.put(fromAddressBytes);
        byte[] toAddressBytes = order.getToAddress().getBytes();
        buffer.putShort((short) toAddressBytes.length);
        buffer.put(toAddressBytes);
        assertFalse(buffer.hasRemaining(), "Buffer for DexOrderAttachme v2 should be fully filled");
        buffer.flip();

        DexOrderAttachmentV2 parsed = new DexOrderAttachmentV2(buffer);

        assertEquals(attachment, parsed);
        assertEquals(2, parsed.getVersion());
    }

    @Test
    void parseFromJson() {
        JSONObject json = new JSONObject();
        json.put("version.DexOrder_v2", 2);
        json.put("type", order.getType().ordinal());
        json.put("offerCurrency", order.getOrderCurrency().ordinal());
        json.put("offerAmount", order.getOrderAmount());
        json.put("pairCurrency", order.getPairCurrency().ordinal());
        json.put("pairRate", pairRate());
        json.put("status", order.getStatus().ordinal());
        json.put("finishTime", order.getFinishTime());
        json.put("fromAddress", order.getFromAddress());
        json.put("toAddress", order.getToAddress());

        DexOrderAttachmentV2 parsed = new DexOrderAttachmentV2(json);

        assertEquals(attachment, parsed);
        assertEquals(2, parsed.getVersion());
    }

    @Test
    @SneakyThrows
    void serializeToBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(95); // with version byte included

        attachment.putBytes(buffer);

        assertFalse(buffer.hasRemaining(), "Attachment should have size of 95 bytes");
        buffer.flip();
        assertEquals(2, buffer.get(), "Version should first byte and equal to 2");
        DexOrderAttachmentV2 deserialized = new DexOrderAttachmentV2(buffer);
        assertEquals(attachment, deserialized);
        assertEquals(2, deserialized.getVersion());
    }

    @Test
    void serializeToJson() {
        JSONObject json = attachment.getJSONObject();

        DexOrderAttachmentV2 deserialized = new DexOrderAttachmentV2(json);

        assertEquals(attachment, deserialized);
        assertEquals(2, deserialized.getVersion(), "Version should 2 for the DexOrderV2 attachment");
    }

    @Test
    void failedVersionVerification_whenGetVersionOverridden() {
        TestDexOrderAttachmentV3 v3Attachment = new TestDexOrderAttachmentV3(this.order);

        assertFalse(v3Attachment.verifyVersion(), "V3 attachment with version field 3 should not pass version " +
            "validation derived from the DexOrderAttachmentV2");
    }


    private long pairRate() {
        return order.getPairRate().multiply(BigDecimal.TEN.pow(9)).longValueExact();
    }

    private ByteBuffer createDexOrderV1Buffer() {
        ByteBuffer buffer = ByteBuffer.allocate(94);
        buffer.put((byte) order.getType().ordinal());
        buffer.put((byte) order.getOrderCurrency().ordinal());
        buffer.putLong(order.getOrderAmount());
        buffer.put((byte) order.getPairCurrency().ordinal());
        buffer.putLong(pairRate());
        buffer.put((byte) order.getStatus().ordinal());
        buffer.putInt(order.getFinishTime());
        return buffer;
    }

    private static class TestDexOrderAttachmentV3 extends DexOrderAttachmentV2 {
        public TestDexOrderAttachmentV3(DexOrder order) {
            super(order);
        }

        @Override
        public byte getVersion() {
            return 3;
        }
    }
}