/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class CCAssetDeleteAttachment extends AbstractAttachment {

    final long assetId;
    final long quantityATU;

    public CCAssetDeleteAttachment(ByteBuffer buffer) {
        super(buffer);
        this.assetId = buffer.getLong();
        this.quantityATU = buffer.getLong();
    }

    public CCAssetDeleteAttachment(JSONObject attachmentData) {
        super(attachmentData);
        this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.quantityATU = Convert.parseLong(attachmentData.get("quantityATU"));
        ;
    }

    public CCAssetDeleteAttachment(long assetId, long quantityATU) {
        this.assetId = assetId;
        this.quantityATU = quantityATU;
    }

    @Override
    public int getMySize() {
        return 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(assetId);
        buffer.putLong(quantityATU);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("asset", Long.toUnsignedString(assetId));
        attachment.put("quantityATU", quantityATU);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_ASSET_DELETE;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getQuantityATU() {
        return quantityATU;
    }

}
