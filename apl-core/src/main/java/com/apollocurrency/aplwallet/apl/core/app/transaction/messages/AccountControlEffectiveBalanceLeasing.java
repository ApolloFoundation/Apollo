/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.transaction.AccountControl;
import com.apollocurrency.aplwallet.apl.core.app.transaction.TransactionType;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
        this.period = ((Long) attachmentData.get("period")).intValue();
    }

    public AccountControlEffectiveBalanceLeasing(int period) {
        this.period = period;
    }

    @Override
    int getMySize() {
        return 2;
    }

    @Override
    void putMyBytes(ByteBuffer buffer) {
        buffer.putShort((short) period);
    }

    @Override
    void putMyJSON(JSONObject attachment) {
        attachment.put("period", period);
    }

    @Override
    public TransactionType getTransactionType() {
        return AccountControl.EFFECTIVE_BALANCE_LEASING;
    }

    public int getPeriod() {
        return period;
    }
    
}
