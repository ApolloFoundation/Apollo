/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app;

public class BlockchainScanException extends RuntimeException {
    public BlockchainScanException() {
    }

    public BlockchainScanException(String message) {
        super(message);
    }

    public BlockchainScanException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockchainScanException(Throwable cause) {
        super(cause);
    }
}
