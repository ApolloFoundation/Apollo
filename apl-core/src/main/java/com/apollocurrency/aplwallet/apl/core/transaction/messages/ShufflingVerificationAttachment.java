/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling.ShufflingTransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
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
    public TransactionType getTransactionType() {
        return ShufflingTransactionType.SHUFFLING_VERIFICATION;
    }

}
