/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface Credential {
    boolean validateCredential(KeyValidator validator);
}
