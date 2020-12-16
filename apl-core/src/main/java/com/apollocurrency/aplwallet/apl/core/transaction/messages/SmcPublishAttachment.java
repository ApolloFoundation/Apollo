/*
 * Copyright (c) 2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

@Builder
@Data
@AllArgsConstructor
public class SmcPublishAttachment extends SmcAbstractAttachment {

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SMC_PUBLISH;
    }

    @Override
    public int getMySize() {
        return 0;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {

    }

    @Override
    public void putMyJSON(JSONObject json) {

    }
}
