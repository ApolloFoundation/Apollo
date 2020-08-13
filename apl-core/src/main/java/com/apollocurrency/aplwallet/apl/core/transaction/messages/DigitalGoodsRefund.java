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
public final class DigitalGoodsRefund extends AbstractAttachment {

    final long purchaseId;
    final long refundATM;

    public DigitalGoodsRefund(ByteBuffer buffer) {
        super(buffer);
        this.purchaseId = buffer.getLong();
        this.refundATM = buffer.getLong();
    }

    public DigitalGoodsRefund(JSONObject attachmentData) {
        super(attachmentData);
        this.purchaseId = Convert.parseUnsignedLong((String) attachmentData.get("purchase"));
        this.refundATM = attachmentData.containsKey("refundATM") ? Convert.parseLong(attachmentData.get("refundATM")) : Convert.parseLong(attachmentData.get("refundNQT"));
    }

    public DigitalGoodsRefund(long purchaseId, long refundATM) {
        this.purchaseId = purchaseId;
        this.refundATM = refundATM;
    }

    @Override
    public int getMySize() {
        return 8 + 8;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putLong(purchaseId);
        buffer.putLong(refundATM);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("purchase", Long.toUnsignedString(purchaseId));
        attachment.put("refundATM", refundATM);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.DGS_REFUND;
    }

    public long getPurchaseId() {
        return purchaseId;
    }

    public long getRefundATM() {
        return refundATM;
    }

}
