/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.model.TransactionImpl;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.core.signature.Credential;
import com.apollocurrency.aplwallet.apl.core.signature.DocumentSigner;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Objects;

/**
 * The transaction signer
 *
 * @author andrew.zinchenko@gmail.com
 * @deprecated Use the offline signing routine
 */
@Deprecated(forRemoval = true)
@Slf4j
@Singleton
public class TransactionSignerImpl implements TransactionSigner {
    private final BlockchainConfig blockchainConfig;
    private final TxBContext txBContext;

    @Inject
    public TransactionSignerImpl(BlockchainConfig blockchainConfig) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }

    /**
     * Sign the unsigned transaction and throw exception if transaction is already signed.
     *
     * @param transaction the unsigned transaction
     * @param keySeed     the key seed using to sign the transaction
     * @throws AplException.NotValidException if transaction is already signed
     */
    @Override
    public void sign(Transaction transaction, byte[] keySeed) throws AplException.NotValidException {
        Objects.requireNonNull(keySeed);

        if (transaction.getSignature() != null && isNonZero(transaction.getSignature().bytes())) {
            throw new AplException.NotValidException("Transaction is already signed");
        }

        byte[] publicKey = transaction.getSenderPublicKey();
        if (publicKey == null) {
            throw new AplException.NotValidException("Sender public key doesn't set.");
        } else if (!Arrays.equals(publicKey, Crypto.getPublicKey(keySeed))) {
            throw new AplException.NotValidException("Secret phrase doesn't match the sender public key.");
        }

        sign(
            SignatureToolFactory.selectSigner(transaction.getVersion()).orElseThrow(UnsupportedTransactionVersion::new)
            , transaction
            , SignatureToolFactory.createCredential(transaction.getVersion(), keySeed)
        );
    }

    /**
     * Sign the unsigned transaction using multi-sig credential.
     *
     * @param transaction the unsigned transaction
     * @param credential  the credential to sign the transaction
     */
    @Override
    public void sign(Transaction transaction, Credential credential) {
        sign(
            SignatureToolFactory.selectSigner(transaction.getVersion()).orElseThrow(UnsupportedTransactionVersion::new)
            , Objects.requireNonNull(transaction)
            , Objects.requireNonNull(credential)
        );
    }

    @Override
    public void sign(DocumentSigner documentSigner, Transaction transaction, Credential credential) {
        Result unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), unsignedTxBytes);

        if (TransactionSignerImpl.log.isTraceEnabled()) {
            TransactionSignerImpl.log.trace("#MULTI_SIG# sign credential={} document={}",
                credential,
                Convert.toHexString(unsignedTxBytes.array()));
        }
        Signature signature = documentSigner.sign(unsignedTxBytes.array(), credential);

        ((TransactionImpl) transaction.getTransactionImpl()).sign(signature, unsignedTxBytes);

        if (TransactionSignerImpl.log.isTraceEnabled()) {
            TransactionSignerImpl.log.trace("#MULTI_SIG# sign signature={} transaction={}", transaction.getSignature().getHexString(), transaction.getId());
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
