/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.ShufflingTransaction;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
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
        return ShufflingTransaction.SHUFFLING_VERIFICATION;
    }
    
}
