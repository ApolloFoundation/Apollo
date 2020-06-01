/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.prunable;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.PrunableMessage;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;

import java.util.List;

public interface PrunableMessageService {
    int getCount();

    List<PrunableMessage> getAll(int from, int to);

    PrunableMessage get(long transactionId);

    List<PrunableMessage> getAll(long accountId, int from, int to);

    List<PrunableMessage> getAll(long accountId, long otherAccountId, int from, int to);

    byte[] decrypt(PrunableMessage message, String secretPhrase);

    byte[] decryptUsingSharedKey(PrunableMessage message, byte[] sharedKey);

    byte[] decryptUsingKeySeed(PrunableMessage message, byte[] keySeed);

    void add(Transaction transaction, PrunablePlainMessageAppendix appendix);

    void add(Transaction transaction, PrunablePlainMessageAppendix appendix, int blockTimestamp, int height);

    void add(Transaction transaction, PrunableEncryptedMessageAppendix appendix);

    void add(Transaction transaction, PrunableEncryptedMessageAppendix appendix, int blockTimestamp, int height);

    boolean isPruned(long transactionId, boolean hasPrunablePlainMessage, boolean hasPrunableEncryptedMessage);
}
