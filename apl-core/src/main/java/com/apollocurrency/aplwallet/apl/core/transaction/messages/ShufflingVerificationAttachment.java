/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import lombok.ToString;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
@ToString(callSuper = true)
public final class ShufflingVerificationAttachment extends AbstractShufflingAttachment {

    public ShufflingVerificationAttachment(ByteBuffer buffer) {
        super(buffer);
    }

    public ShufflingVerificationAttachment(JSONObject attachmentData) {
        super(attachmentData);
    }

    public ShufflingVerificationAttachment(long shufflingId, byte[] shufflingStateHash) {
        super(shufflingId, shufflingStateHash);
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getTransactionTypeSpec() {
        return TransactionTypes.TransactionTypeSpec.SHUFFLING_VERIFICATION;
    }

}
