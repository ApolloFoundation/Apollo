/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureHelper;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
@Singleton
public class TransactionSigner {

    private final AccountPublicKeyService accountPublicKeyService;

    @Inject
    public TransactionSigner(AccountPublicKeyService accountPublicKeyService) {
        this.accountPublicKeyService = accountPublicKeyService;
    }

    /**
     * Sign the unsigned transaction and throw exception if transaction is already signed.
     *
     * @param transaction the unsigned transaction
     * @param keySeed     the key seed using to sign the transaction
     * @throws AplException.NotValidException
     */
    public void sign(Transaction transaction, byte[] keySeed) throws AplException.NotValidException {
        Objects.requireNonNull(keySeed);
        if (transaction.getSignature() != null) {
            throw new AplException.NotValidException("Transaction is already signed");
        }
        byte[] publicKey = transaction.getSenderPublicKey();
        if (publicKey == null) {
            publicKey = accountPublicKeyService.getPublicKeyByteArray(transaction.getSenderId());
        }

        if (publicKey != null
            && !Arrays.equals(publicKey, Crypto.getPublicKey(keySeed))) {
            throw new AplException.NotValidException("Secret phrase doesn't match transaction sender public key");
        }
        transaction.sign(SignatureHelper.sign(transaction.getUnsignedBytes(), keySeed));
    }
}
