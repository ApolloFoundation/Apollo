/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
public class MultiSigValidatorImpl implements SignatureValidator {

    private MultiSigData multiSigData;
    private MultiSigCredential multiSigCredential;
    private final byte[] document;
    private final Set<byte[]> verifiedPks;

    public MultiSigValidatorImpl(byte[] document) {
        this.document = Objects.requireNonNull(document);
        this.verifiedPks = new HashSet<>();
    }

    @Override
    public boolean verify(Signature signature, Credential credential) {
        if (credential instanceof MultiSigCredential) {
            this.multiSigCredential = (MultiSigCredential) credential;
        } else {
            throw new IllegalArgumentException("Can't cast credential object to MultiSigCredential type.");
        }
        if (signature instanceof MultiSigData) {
            this.multiSigData = (MultiSigData) signature;
        } else {
            throw new IllegalArgumentException("Can't cast signature object to MultiSig type.");
        }
        if (multiSigCredential.getThreshold() > multiSigData.getParticipantCount()) {
            return false;
        }
        for (byte[] pk : multiSigCredential.getPublicKeys()) {
            verifyOne(pk);
        }
        return multiSigCredential.getThreshold() == verifiedPks.size();
    }

    private boolean verifyOne(byte[] publicKey) {
        boolean rc;
        if (!multiSigData.isParticipant(publicKey)) {
            return false;
        }
        if (verifiedPks.contains(publicKey)) {
            if (log.isTraceEnabled()) {
                log.trace("Pk already verified, pk={}", Convert.toHexString(publicKey));
            }
            return true;
        }
        byte[] signature = multiSigData.getSignature(publicKey);
        rc = Crypto.verify(signature, document, publicKey);
        if (rc) {
            verifiedPks.add(publicKey);
        }
        return rc;
    }

}
