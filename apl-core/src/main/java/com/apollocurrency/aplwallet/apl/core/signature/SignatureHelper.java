/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignatureHelper {

    /**
     * Sign the document
     *
     * @param document the document
     * @param keySeed  the key seed using to sign the document
     * @return the signature
     */
    public static Signature sign(byte[] document, byte[] keySeed) {
        int version = 1;
        Credential credential = SignatureToolFactory.createCredential(version, Crypto.getKeySeed(keySeed));
        SignatureSigner signatureSigner = SignatureToolFactory.selectBuilder(version).get();
        Signature signature = signatureSigner.sign(document, credential);
        if (log.isTraceEnabled()) {
            log.trace("#MULTI_SIG# sign signature: {}", signature.getJsonString());
        }
        return signature;
    }

}
