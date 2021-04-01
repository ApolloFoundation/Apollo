/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
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
        this.deltaQuantity = ((Number) attachmentData.get("deltaQuantity")).intValue();
    }

    public DigitalGoodsQuantityChange(long goodsId, int deltaQuantity) {
        this.goodsId = goodsId;
        this.deltaQuantity = deltaQuantity;
    }

    @Override
    public int getMySize() {
        return 8 + 4;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(goodsId);
        buffer.putInt(deltaQuantity);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("goods", Long.toUnsignedString(goodsId));
        attachment.put("deltaQuantity", deltaQuantity);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_CHANGE_QUANTITY;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public int getDeltaQuantity() {
        return deltaQuantity;
    }

}
