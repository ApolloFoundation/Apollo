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
public final class DGSPriceChangeAttachment extends AbstractAttachment {

    final long goodsId;
    final long priceATM;

    public DGSPriceChangeAttachment(ByteBuffer buffer) {
        super(buffer);
        this.goodsId = buffer.getLong();
        this.priceATM = buffer.getLong();
    }

    public DGSPriceChangeAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
        this.priceATM = Convert.parseLong(attachmentData.get("priceATM"));
    }

    public DGSPriceChangeAttachment(long goodsId, long priceATM) {
        this.goodsId = goodsId;
        this.priceATM = priceATM;
    }

    @Override
    public int getMySize() {
        return 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(goodsId);
        buffer.putLong(priceATM);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("goods", Long.toUnsignedString(goodsId));
        attachment.put("priceATM", priceATM);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_CHANGE_PRICE;
    }

    public long getGoodsId() {
        return goodsId;
    }

    public long getPriceATM() {
        return priceATM;
    }

}
