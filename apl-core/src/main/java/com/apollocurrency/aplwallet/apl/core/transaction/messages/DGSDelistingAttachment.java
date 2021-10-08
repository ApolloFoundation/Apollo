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
public final class DGSDelistingAttachment extends AbstractAttachment {

    final long goodsId;

    public DGSDelistingAttachment(ByteBuffer buffer) {
        super(buffer);
        this.goodsId = buffer.getLong();
    }

    public DGSDelistingAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.goodsId = Convert.parseUnsignedLong((String) attachmentData.get("goods"));
    }

    public DGSDelistingAttachment(long goodsId) {
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
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_DELISTING;
    }

    public long getGoodsId() {
        return goodsId;
    }

}
