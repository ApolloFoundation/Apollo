/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.types.dgs.DigitalGoodsTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class DigitalGoodsDelisting extends AbstractAttachment {

    final long goodsId;

    public DigitalGoodsDelisting(ByteBuffer buffer) {
        super(buffer);
        this.goodsId = buffer.getLong();
    }

    public DigitalGoodsDelisting(JSONObject attachmentData) {
        super(attachmentData);
        this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
    }

    public DigitalGoodsDelisting(long goodsId) {
        this.goodsId = goodsId;
    }

    @Override
    public int getMySize() {
        return 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(goodsId);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("goods", Long.toUnsignedString(goodsId));
    }

    @Override
    public TransactionType getTransactionType() {
        return DigitalGoodsTransactionType.DELISTING;
    }

    public long getGoodsId() {
        return goodsId;
    }

}
