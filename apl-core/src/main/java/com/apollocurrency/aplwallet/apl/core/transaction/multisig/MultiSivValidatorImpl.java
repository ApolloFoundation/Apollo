/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.multisig;

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
public class MultiSivValidatorImpl implements MultiSigValidator {

    private final MultiSigData multiSigData;
    private final byte[] document;
    private final Set<byte[]> verifiedPks;

    public MultiSivValidatorImpl(byte[] document, MultiSigData multiSigData) {
        this.multiSigData = Objects.requireNonNull(multiSigData);
        this.document = Objects.requireNonNull(document);
        this.verifiedPks = new HashSet<>();
    }

    @Override
    public boolean verify(int threshold, byte[]... pubicKeys) {
        Objects.requireNonNull(pubicKeys);
        if (threshold < 2 || threshold > pubicKeys.length) {
            throw new IllegalArgumentException("Wrong threshold value.");
        }
        if (threshold > multiSigData.participantCount()) {
            return false;
        }
        for (byte[] pk : pubicKeys) {
            verifyOne(pk);
        }
        return threshold == verifiedPks.size();
    }

    private boolean verifyOne(byte[] publicKey) {
        boolean rc;
        int idx = multiSigData.findParticipant(publicKey);
        if (idx < 0) {
            return false;
        }
        if (verifiedPks.contains(publicKey)) {
            if (log.isTraceEnabled()) {
                log.trace("Pk already verified, pk={}", Convert.toHexString(publicKey));
            }
            return true;
        }
        byte[] signature = multiSigData.getSignature(idx);
        rc = Crypto.verify(signature, document, publicKey);
        if (rc) {
            verifiedPks.add(publicKey);
        }
        return rc;
    }

}
