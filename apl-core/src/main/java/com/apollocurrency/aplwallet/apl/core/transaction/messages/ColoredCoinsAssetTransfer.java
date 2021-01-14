/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class ColoredCoinsAssetTransfer extends AbstractAttachment {

    final long assetId;
    final long quantityATU;

    public ColoredCoinsAssetTransfer(ByteBuffer buffer) throws AplException.NotValidException {
        super(buffer);
        this.assetId = buffer.getLong();
        this.quantityATU = buffer.getLong();
    }

    public ColoredCoinsAssetTransfer(JSONObject attachmentData) {
        super(attachmentData);
        this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.quantityATU = Convert.parseLong(attachmentData.get("quantityATU"));
    }

    public ColoredCoinsAssetTransfer(long assetId, long quantityATU) {
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
        return TransactionTypes.TransactionTypeSpec.CC_ASSET_TRANSFER;
    }

    public long getAssetId() {
        return assetId;
    }

    public long getQuantityATU() {
        return quantityATU;
    }

}
