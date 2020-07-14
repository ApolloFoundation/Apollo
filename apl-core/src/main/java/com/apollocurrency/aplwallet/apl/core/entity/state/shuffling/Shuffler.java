/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;

public final class Shuffler {
    private final long accountId;
    private final byte[] secretBytes;
    private final byte[] recipientPublicKey;
    private final byte[] shufflingFullHash;
    private volatile Transaction failedTransaction;
    private volatile AplException.NotCurrentlyValidException failureCause;

    public Shuffler(byte[] secretBytes, byte[] recipientPublicKey, byte[] shufflingFullHash) {
        this.secretBytes = secretBytes;
        this.accountId = AccountService.getId(Crypto.getPublicKey(Crypto.getKeySeed(secretBytes)));
        this.recipientPublicKey = recipientPublicKey;
        this.shufflingFullHash = shufflingFullHash;
    }


    public long getAccountId() {
        return accountId;
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

    public byte[] getSecretBytes() {
        return secretBytes;
    }

    public void setFailedTransaction(Transaction failedTransaction) {
        this.failedTransaction = failedTransaction;
    }

    public void setFailureCause(AplException.NotCurrentlyValidException failureCause) {
        this.failureCause = failureCause;
    }

}
