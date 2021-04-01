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
public final class ColoredCoinsDividendPayment extends AbstractAttachment {

    final long assetId;
    final int height;
    final long amountATMPerATU;

    public ColoredCoinsDividendPayment(ByteBuffer buffer) {
        super(buffer);
        this.assetId = buffer.getLong();
        this.height = buffer.getInt();
        this.amountATMPerATU = buffer.getLong();
    }

    public ColoredCoinsDividendPayment(JSONObject attachmentData) {
        super(attachmentData);
        this.assetId = Convert.parseUnsignedLong((String) attachmentData.get("asset"));
        this.height = ((Number) attachmentData.get("height")).intValue();
        this.amountATMPerATU = Convert.parseLong(attachmentData.get("amountATMPerATU"));
    }

    public ColoredCoinsDividendPayment(long assetId, int height, long amountATMPerATU) {
        this.assetId = assetId;
        this.height = height;
        this.amountATMPerATU = amountATMPerATU;
    }

    @Override
    public int getMySize() {
        return 8 + 4 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(assetId);
        buffer.putInt(height);
        buffer.putLong(amountATMPerATU);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("asset", Long.toUnsignedString(assetId));
        attachment.put("height", height);
        attachment.put("amountATMPerATU", amountATMPerATU);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.CC_DIVIDEND_PAYMENT;
    }

    public long getAssetId() {
        return assetId;
    }

    public int getHeight() {
        return height;
    }

    public long getAmountATMPerATU() {
        return amountATMPerATU;
    }

}
