/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignatureToolFactory {

    private static final SignatureValidator[] validators = new SignatureValidator[]
        {new SignatureValidatorV1(), new MultiSigValidatorImpl()};

    private static final SignatureParser[] parsers = new SignatureParser[]
        {new SigData.Parser(), new MultiSigData.Parser()};

    private static final SignatureBuilder[] builders = new SignatureBuilder[]
        {};

    public static Optional<SignatureValidator> selectValidator(int transactionVersion) {
        return selectTool(transactionVersion, validators);
    }

    public static Optional<SignatureParser> selectParser(int transactionVersion) {
        return selectTool(transactionVersion, parsers);
    }

    public static Optional<SignatureBuilder> selectBuilder(int transactionVersion) {
        return selectTool(transactionVersion, builders);
    }

    private static <T> Optional<T> selectTool(int transactionVersion, T[] tools) {
        int version = transactionVersion == 0 ? 0 : transactionVersion - 1;
        if (version < 0 || version >= tools.length) {
            return Optional.empty();
        }
        return Optional.of(tools[version]);
    }

    private static class MultiSigValidatorImpl implements SignatureValidator {

        @Override
        public boolean verify(byte[] document, Signature signature, Credential credential) {
            Objects.requireNonNull(document);
            MultiSigData multiSigData;
            MultiSigCredential multiSigCredential;
            if (credential instanceof MultiSigCredential) {
                multiSigCredential = (MultiSigCredential) credential;
            } else {
                throw new IllegalArgumentException("Can't cast credential object to MultiSigCredential type.");
            }
            if (signature instanceof MultiSigData) {
                multiSigData = (MultiSigData) signature;
            } else {
                throw new IllegalArgumentException("Can't cast signature object to MultiSig type.");
            }
            if (multiSigCredential.getThreshold() > multiSigData.getParticipantCount()) {
                return false;
            }

            Set<byte[]> verifiedPks = new HashSet<>();
            for (byte[] pk : multiSigCredential.getPublicKeys()) {
                if (multiSigData.isParticipant(pk)) {
                    if (verifiedPks.contains(pk)) {
                        if (log.isTraceEnabled()) {
                            log.trace("Pk already verified, pk={}", Convert.toHexString(pk));
                        }
                    } else {
                        if (Crypto.verify(multiSigData.getSignature(pk), document, pk)) {
                            verifiedPks.add(pk);
                        }
                    }
                }
            }
            multiSigData.setVerified(multiSigCredential.getThreshold() == verifiedPks.size());
            return multiSigData.isVerified();
        }
    }

    private static class SignatureValidatorV1 implements SignatureValidator {
        @Override
        public boolean verify(byte[] document, Signature signature, Credential credential) {
            Objects.requireNonNull(document);
            SigData sigData;
            SignatureCredential signatureCredential;
            if (credential instanceof SignatureCredential) {
                signatureCredential = (SignatureCredential) credential;
            } else {
                throw new IllegalArgumentException("Can't cast credential object to SignatureCredential type.");
            }
            if (signature instanceof SigData) {
                sigData = (SigData) signature;
            } else {
                throw new IllegalArgumentException("Can't cast signature object to SigData type.");
            }
            sigData.setVerified(Crypto.verify(sigData.bytes(), document, signatureCredential.getPublicKey()));
            return sigData.isVerified();
        }
    }
}
