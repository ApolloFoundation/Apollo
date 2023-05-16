/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.util.exception.AplException;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public class GenesisImportException extends AplException {

    public GenesisImportException(String message) {
        super(message);
    }

    public GenesisImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
