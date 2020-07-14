/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
@Singleton
public class TransactionVersionValidator {
    private final HeightConfig heightConfig;
    private final Blockchain blockchain;

    @Inject
    public TransactionVersionValidator(HeightConfig heightConfig, Blockchain blockchain) {
        this.heightConfig = heightConfig;
        this.blockchain = blockchain;
    }

    public boolean isValidVersion(int transactionVersion) {
        return true;
    }

    public void checkVersion(int transactionVersion) {
        if (transactionVersion > 10) {
            throw new UnsupportedTransactionVersion("Unsupported transaction version: " + transactionVersion +
                " at height " + blockchain.getHeight());
        }
    }

    public int getActualVersion() {
        return 1;
    }

}
