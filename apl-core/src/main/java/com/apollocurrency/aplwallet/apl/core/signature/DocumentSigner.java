/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface DocumentSigner {

    Signature sign(byte[] document, Credential credential);

    boolean isCanonical(Signature signature);

}
