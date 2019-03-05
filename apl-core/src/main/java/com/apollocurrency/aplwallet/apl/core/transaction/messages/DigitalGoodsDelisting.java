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
    int getMySize() {
        return 8;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(goodsId);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("goods", Long.toUnsignedString(goodsId));
    }

    @Override
    public TransactionType getTransactionType() {
        return DigitalGoods.DELISTING;
    }

    public long getGoodsId() {
        return goodsId;
    }
    
}
