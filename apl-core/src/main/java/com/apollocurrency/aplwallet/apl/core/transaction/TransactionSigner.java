/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.DocumentSigner;
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

    private static final byte[] ZERO_ECDS = new byte[Signature.ECDSA_SIGNATURE_SIZE];

    private final AccountPublicKeyService accountPublicKeyService;
    private final DocumentSigner documentSigner;

    @Inject
    public TransactionSigner(AccountPublicKeyService accountPublicKeyService) {
        this.accountPublicKeyService = Objects.requireNonNull(accountPublicKeyService);
        this.documentSigner = SignatureToolFactory.selectBuilder(1).orElseThrow(UnsupportedTransactionVersion::new);
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

        if (transaction.getSignature() != null
            && !Arrays.equals(ZERO_ECDS, transaction.getSignature().bytes())
            && documentSigner.isCanonical(transaction.getSignature())) {
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
        if (log.isTraceEnabled()) {
            log.trace("#MULTI_SIG# sign keySeed={} publicKey={} document={}",
                Convert.toHexString(keySeed),
                Convert.toHexString(publicKey),
                Convert.toHexString(transaction.getUnsignedBytes()));
        }
        Signature signature = documentSigner.sign(transaction.getUnsignedBytes(), SignatureToolFactory.createCredential(1, keySeed));
        transaction.sign(signature);
        if (log.isTraceEnabled()) {
            log.trace("#MULTI_SIG# sign signature={} transaction={}", transaction.getSignature().getJsonString(), transaction.getJSONObject().toJSONString());
        }
    }
}
