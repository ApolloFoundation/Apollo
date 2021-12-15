/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface ErrorInfo {
    int getErrorCode();

    String getErrorDescription();
}
