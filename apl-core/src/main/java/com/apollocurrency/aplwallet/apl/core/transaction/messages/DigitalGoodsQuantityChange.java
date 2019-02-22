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
public final class DigitalGoodsQuantityChange extends AbstractAttachment {
    
    final long goodsId;
    final int deltaQuantity;

    public DigitalGoodsQuantityChange(ByteBuffer buffer) {
        super(buffer);
        this.goodsId = buffer.getLong();
        this.deltaQuantity = buffer.getInt();
    }

    public DigitalGoodsQuantityChange(JSONObject attachmentData) {
        super(attachmentData);
        this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
        this.deltaQuantity = ((Long) attachmentData.get("deltaQuantity")).intValue();
    }

    public DigitalGoodsQuantityChange(long goodsId, int deltaQuantity) {
        this.goodsId = goodsId;
        this.deltaQuantity = deltaQuantity;
    }

    @Override
    int getMySize() {
        return 8 + 4;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(goodsId);
        buffer.putInt(deltaQuantity);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("goods", Long.toUnsignedString(goodsId));
        attachment.put("deltaQuantity", deltaQuantity);
    }

    @Override
    public TransactionType getTransactionType() {
        return DigitalGoods.QUANTITY_CHANGE;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public int getDeltaQuantity() {
        return deltaQuantity;
    }
    
}
