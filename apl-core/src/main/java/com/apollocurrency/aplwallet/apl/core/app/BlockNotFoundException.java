/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

public class BlockNotFoundException extends RuntimeException{
    public BlockNotFoundException() {
    }

    public BlockNotFoundException(String message) {
        super(message);
    }

    public BlockNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public BlockNotFoundException(Throwable cause) {
        super(cause);
    }
}
