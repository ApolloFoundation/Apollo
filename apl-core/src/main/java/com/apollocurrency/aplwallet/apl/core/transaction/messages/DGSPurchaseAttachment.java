/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.EqualsAndHashCode;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@EqualsAndHashCode(callSuper = true)
public final class DGSPurchaseAttachment extends AbstractAttachment {

    final long goodsId;
    final int quantity;
    final long priceATM;
    final int deliveryDeadlineTimestamp;

    public DGSPurchaseAttachment(ByteBuffer buffer) {
        super(buffer);
        this.goodsId = buffer.getLong();
        this.quantity = buffer.getInt();
        this.priceATM = buffer.getLong();
        this.deliveryDeadlineTimestamp = buffer.getInt();
    }

    public DGSPurchaseAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
        this.quantity = ((Number) attachmentData.get("quantity")).intValue();
        this.priceATM = Convert.parseLong(attachmentData.get("priceATM"));
        this.deliveryDeadlineTimestamp = ((Number) attachmentData.get("deliveryDeadlineTimestamp")).intValue();
    }

    public DGSPurchaseAttachment(long goodsId, int quantity, long priceATM, int deliveryDeadlineTimestamp) {
        this.goodsId = goodsId;
        this.quantity = quantity;
        this.priceATM = priceATM;
        this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
    }

    @Override
    public int getMySize() {
        return 8 + 4 + 8 + 4;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(goodsId);
        buffer.putInt(quantity);
        buffer.putLong(priceATM);
        buffer.putInt(deliveryDeadlineTimestamp);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("goods", Long.toUnsignedString(goodsId));
        attachment.put("quantity", quantity);
        attachment.put("priceATM", priceATM);
        attachment.put("deliveryDeadlineTimestamp", deliveryDeadlineTimestamp);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_PURCHASE;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getPriceATM() {
        return priceATM;
    }

    public int getDeliveryDeadlineTimestamp() {
        return deliveryDeadlineTimestamp;
    }

}
