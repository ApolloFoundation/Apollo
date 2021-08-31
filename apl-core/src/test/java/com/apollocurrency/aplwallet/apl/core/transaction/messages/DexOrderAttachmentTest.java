/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class DexOrderAttachmentTest {

    private final DexOrder order = new DexOrder(1000L, 1111L, OrderType.BUY, 1L,
        DexCurrency.APL, 50L, DexCurrency.ETH, new BigDecimal("1.2"), 2500, OrderStatus.OPEN,
        0, "APL-BBZM-F4DB-82NH-BC95J", "0x3459CAf655F56EeFC2d00855e4D421691350b3fA");
    private final DexOrderAttachment attachment = new DexOrderAttachment(order);

    @Test
    void getTransactionTypeSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.DEX_ORDER, attachment.getTransactionTypeSpec());
    }

    @Test
    void verifyVersion() {
        assertTrue(attachment.verifyVersion(), "DexOrderAttachment version should pass validation");
    }

    @Test
    void createFromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(24);
//        buff.put((byte) 1); // version is already read by DexOrderAttachmentFactory
        buff.put((byte) 0); // order type - BUY
        buff.put((byte) 0); // APL - order currency
        buff.putLong(50); // order amount
        buff.put((byte) 1); // ETH - paired currency
        buff.putLong(1_200_000_000L); // pair rate in GWEI
        buff.put((byte) 0); // OPEN - order status
        buff.putInt(2500);
        buff.flip();

        DexOrderAttachment parsed = new DexOrderAttachment(buff);

        assertEquals(attachment, parsed);
        assertEquals(1, parsed.getVersion(), "Version of the DexOrderAttachment should be 1");
    }

    @Test
    void createFromJSON() {
        JSONObject json = new JSONObject();
        json.put("version.DexOrder", (byte) 1);
        json.put("type", "0");
        json.put("offerCurrency", "0");
        json.put("offerAmount", "50");
        json.put("pairCurrency", "1");
        json.put("pairRate", "1200000000");
        json.put("status", "0");
        json.put("finishTime", "2500");

        DexOrderAttachment parsed = new DexOrderAttachment(json);

        assertEquals(attachment, parsed);
        assertEquals(1, parsed.getVersion());
    }

    @Test
    void getSize() {
        assertEquals(24, attachment.getMySize());
        assertEquals(24, attachment.getMyFullSize()); // the same as above
        assertEquals(25, attachment.getFullSize()); // with a version byte
    }

    @Test
    void serializeToBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(25);

        attachment.putBytes(buffer);

        assertFalse(buffer.hasRemaining(), "Serialization buffer should filled entirely during " +
            "DexOrderAttachment serialization to bytes");

        buffer.flip();
        byte version = buffer.get(); // used in the DexOrderAttachmentFactory to decide on the type of the DexOrder
        // attachment object to instantiate
        assertEquals(1, version);
        assertEquals(attachment, new DexOrderAttachment(buffer));
    }

    @Test
    void serializeToJson() {
        JSONObject json = attachment.getJSONObject();

        DexOrderAttachment deserialized = new DexOrderAttachment(json);

        assertEquals(1, deserialized.getVersion(), "Version for the DexOrderAttachment should be always 1");
        assertEquals(attachment, deserialized);
    }
}