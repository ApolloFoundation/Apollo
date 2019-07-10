/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.model;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;

public class Shuffler {
    private final long accountId;
    private final byte[] secretBytes;
    private final byte[] recipientPublicKey;
    private final byte[] shufflingFullHash;
    private volatile Transaction failedTransaction;
    private volatile AplException.NotCurrentlyValidException failureCause;

    public Shuffler(byte[] secretBytes, byte[] recipientPublicKey, byte[] shufflingFullHash) {
        this.secretBytes = secretBytes;
        this.accountId = Account.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
        this.recipientPublicKey = recipientPublicKey;
        this.shufflingFullHash = shufflingFullHash;
    }

    private Shuffler(long accountId, byte[] secretBytes, byte[] recipientPublicKey, byte[] shufflingFullHash) {
        this.accountId = accountId;
        this.secretBytes = secretBytes;
        this.recipientPublicKey = recipientPublicKey;
        this.shufflingFullHash = shufflingFullHash;
    }

    public void setFailedTransaction(Transaction failedTransaction) {
        this.failedTransaction = failedTransaction;
    }

    public void setFailureCause(AplException.NotCurrentlyValidException failureCause) {
        this.failureCause = failureCause;
    }

    public long getAccountId() {
        return accountId;
    }

    public byte[] getSecretBytes() {
        return secretBytes;
    }

    public byte[] getRecipientPublicKey() {
        return recipientPublicKey;
    }

    public byte[] getShufflingFullHash() {
        return shufflingFullHash;
    }

    public Transaction getFailedTransaction() {
        return failedTransaction;
    }

    public AplException.NotCurrentlyValidException getFailureCause() {
        return failureCause;
    }
}
