/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.DigitalGoods;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public final class DigitalGoodsPurchase extends AbstractAttachment {
    
    final long goodsId;
    final int quantity;
    final long priceATM;
    final int deliveryDeadlineTimestamp;

    public DigitalGoodsPurchase(ByteBuffer buffer) {
        super(buffer);
        this.goodsId = buffer.getLong();
        this.quantity = buffer.getInt();
        this.priceATM = buffer.getLong();
        this.deliveryDeadlineTimestamp = buffer.getInt();
    }

    public DigitalGoodsPurchase(JSONObject attachmentData) {
        super(attachmentData);
        this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
        this.quantity = ((Long) attachmentData.get("quantity")).intValue();
        this.priceATM = attachmentData.containsKey("priceATM") ? Convert.parseLong(attachmentData.get("priceATM")) : Convert.parseLong(attachmentData.get("priceNQT"));
        this.deliveryDeadlineTimestamp = ((Long) attachmentData.get("deliveryDeadlineTimestamp")).intValue();
    }

    public DigitalGoodsPurchase(long goodsId, int quantity, long priceATM, int deliveryDeadlineTimestamp) {
        this.goodsId = goodsId;
        this.quantity = quantity;
        this.priceATM = priceATM;
        this.deliveryDeadlineTimestamp = deliveryDeadlineTimestamp;
    }

    @Override
    int getMySize() {
        return 8 + 4 + 8 + 4;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(goodsId);
        buffer.putInt(quantity);
        buffer.putLong(priceATM);
        buffer.putInt(deliveryDeadlineTimestamp);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("goods", Long.toUnsignedString(goodsId));
        attachment.put("quantity", quantity);
        attachment.put("priceATM", priceATM);
        attachment.put("deliveryDeadlineTimestamp", deliveryDeadlineTimestamp);
    }

    @Override
    public TransactionType getTransactionType() {
        return DigitalGoods.PURCHASE;
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
