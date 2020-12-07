/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
@Singleton
public class TransactionVersionValidator {
    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;

    @Inject
    public TransactionVersionValidator(BlockchainConfig blockchainConfig, Blockchain blockchain) {
        this.blockchainConfig = Objects.requireNonNull(blockchainConfig);
        this.blockchain = Objects.requireNonNull(blockchain);
    }

    public int getActualVersion() {
        if (blockchainConfig.isTransactionV3ActiveAtHeight(blockchain.getHeight())) {
            return 3;
        }
        if (blockchainConfig.isTransactionV2ActiveAtHeight(blockchain.getHeight())) {
            return 2;
        }
        return 1;
    }

    public boolean isValidVersion(Transaction transaction) {
        return isValidVersion(transaction.getVersion());
    }

    public boolean isValidVersion(int transactionVersion) {
        return 0 < transactionVersion && transactionVersion <= getActualVersion();
    }

    public void checkVersion(int transactionVersion) {
        if (isValidVersion(transactionVersion)) {
            throw new UnsupportedTransactionVersion("Unsupported transaction version: " + transactionVersion +
                " at height " + blockchain.getHeight());
        }
    }

}
