/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public interface SignatureVerifier {

    boolean verify(byte[] document, Signature signature, Credential credential);

}
