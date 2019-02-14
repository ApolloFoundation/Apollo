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
public final class ShufflingVerification extends AbstractShufflingAttachment {
    
    public ShufflingVerification(ByteBuffer buffer) {
        super(buffer);
    }

    public ShufflingVerification(JSONObject attachmentData) {
        super(attachmentData);
    }

    public ShufflingVerification(long shufflingId, byte[] shufflingStateHash) {
        super(shufflingId, shufflingStateHash);
    }

    @Override
    public TransactionType getTransactionType() {
        return ShufflingTransaction.SHUFFLING_VERIFICATION;
    }
    
}
