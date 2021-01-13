/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.signature.Credential;
import com.apollocurrency.aplwallet.apl.core.signature.DocumentSigner;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
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
    private final DocumentSigner documentSignerV1;

    @Inject
    public TransactionSigner(AccountPublicKeyService accountPublicKeyService) {
        this.accountPublicKeyService = Objects.requireNonNull(accountPublicKeyService);
        this.documentSignerV1 = SignatureToolFactory.selectBuilder(1).orElseThrow(UnsupportedTransactionVersion::new);
    }

    /**
     * Sign the unsigned transaction and throw exception if transaction is already signed.
     *
     * @param transaction the unsigned transaction
     * @param keySeed     the key seed using to sign the transaction
     * @throws AplException.NotValidException if transaction is already signed
     */
    public void sign(Transaction transaction, byte[] keySeed) throws AplException.NotValidException {
        Objects.requireNonNull(keySeed);

        if (transaction.getSignature() != null
            && isNonZero(transaction.getSignature().bytes())
            && documentSignerV1.isCanonical(transaction.getSignature())) {
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

        sign(documentSignerV1, transaction, SignatureToolFactory.createCredential(1, keySeed));
    }

    /**
     * Sign the unsigned transaction using multi-sig credential.
     *
     * @param transaction the unsigned transaction
     * @param credential  the credential to sign the transaction
     */
    public void sign(Transaction transaction, Credential credential) {
        sign(
            SignatureToolFactory.selectBuilder(transaction.getVersion()).orElseThrow(UnsupportedTransactionVersion::new),
            Objects.requireNonNull(transaction),
            Objects.requireNonNull(credential));
    }

    private static void sign(DocumentSigner documentSigner, Transaction transaction, Credential credential) {
        if (log.isTraceEnabled()) {
            log.trace("#MULTI_SIG# sign credential={} document={}",
                credential,
                Convert.toHexString(transaction.getUnsignedBytes()));
        }
        Signature signature = documentSigner.sign(transaction.getUnsignedBytes(), credential);

        transaction.sign(
            signature
        );

        if (log.isTraceEnabled()) {
            log.trace("#MULTI_SIG# sign signature={} transaction={}", transaction.getSignature().getHexString(), transaction.getId());
        }
    }

    private static boolean isNonZero(byte[] array) {
        for (byte value : array) {
            if (value != 0)
                return true;
        }
        return false;
    }
}
