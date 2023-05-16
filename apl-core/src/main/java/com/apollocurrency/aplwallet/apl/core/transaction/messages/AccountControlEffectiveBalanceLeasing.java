/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public final class AccountControlEffectiveBalanceLeasing extends AbstractAttachment {

    final int period;

    public AccountControlEffectiveBalanceLeasing(ByteBuffer buffer) {
        super(buffer);
        this.period = Short.toUnsignedInt(buffer.getShort());
    }

    public AccountControlEffectiveBalanceLeasing(JSONObject attachmentData) {
        super(attachmentData);
        this.period = ((Number) attachmentData.get("period")).intValue();
    }

    public AccountControlEffectiveBalanceLeasing(int period) {
        this.period = period;
    }

    @Override
    public int getMySize() {
        return 2;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.putShort((short) period);
    }

    @Override
    public void putMyJSON(JSONObject attachment) {
        attachment.put("period", period);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.EFFECTIVE_BALANCE_LEASING;
    }

    public int getPeriod() {
        return period;
    }

}
