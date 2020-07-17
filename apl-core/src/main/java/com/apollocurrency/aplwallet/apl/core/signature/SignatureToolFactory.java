/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    private static final SignatureSigner[] sigSigners = new SignatureSigner[]
        {};

    public static Signature createSignature(byte[] signature) {
        return new SigData(Objects.requireNonNull(signature));
    }

    public static Credential createCredential(int version, byte[]... keys) {
        switch (version) {
            case 0:
            case 1:
                return new SignatureCredential(keys[0]);
            case 2:
                return new MultiSigCredential(keys.length, keys);
            default:
                throw new UnsupportedTransactionVersion("Can't crate credential a given transaction version: " + version);
        }
    }

    public static Optional<SignatureValidator> selectValidator(int transactionVersion) {
        return selectTool(transactionVersion, validators);
    }

    public static Optional<SignatureParser> selectParser(int transactionVersion) {
        return selectTool(transactionVersion, parsers);
    }

    public static Optional<SignatureSigner> selectBuilder(int transactionVersion) {
        return selectTool(transactionVersion, sigSigners);
    }

    private static <T> Optional<T> selectTool(int transactionVersion, T[] tools) {
        int version = transactionVersion == 0 ? 0 : transactionVersion - 1;
        if (version < 0 || version >= tools.length) {
            return Optional.empty();
        }
        return Optional.of(tools[version]);
    }

    private static SigData getSigData(Signature signature) {
        SigData sigData;
        if (signature instanceof SigData) {
            sigData = (SigData) signature;
        } else {
            throw new IllegalArgumentException("Can't cast signature object to SigData type.");
        }
        return sigData;
    }

    private static MultiSigData getMultiSigData(Signature signature) {
        MultiSigData multiSigData;
        if (signature instanceof MultiSigData) {
            multiSigData = (MultiSigData) signature;
        } else {
            throw new IllegalArgumentException("Can't cast signature object to MultiSig type.");
        }
        return multiSigData;
    }

    private static MultiSigCredential getMultiSigCredential(Credential credential) {
        MultiSigCredential multiSigCredential;
        if (credential instanceof MultiSigCredential) {
            multiSigCredential = (MultiSigCredential) credential;
        } else {
            throw new IllegalArgumentException("Can't cast credential object to MultiSigCredential type.");
        }
        return multiSigCredential;
    }

    private static SignatureCredential getSignatureCredential(Credential credential) {
        SignatureCredential signatureCredential;
        if (credential instanceof SignatureCredential) {
            signatureCredential = (SignatureCredential) credential;
        } else {
            throw new IllegalArgumentException("Can't cast credential object to SignatureCredential type.");
        }
        return signatureCredential;
    }

    private static class MultiSigValidatorImpl implements SignatureValidator {

        @Override
        public boolean verify(byte[] document, Signature signature, Credential credential) {
            Objects.requireNonNull(document);
            MultiSigData multiSigData;
            MultiSigCredential multiSigCredential;
            multiSigCredential = getMultiSigCredential(credential);
            multiSigData = getMultiSigData(signature);
            if (multiSigCredential.getThreshold() > multiSigData.getParticipantCount()) {
                return false;
            }

            Set<byte[]> verifiedPks = new HashSet<>();
            for (byte[] pk : multiSigCredential.getKeys()) {
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
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# verified pk count={} multi-signature: {} isVerified={}",
                    verifiedPks.size(),
                    multiSigData.getJsonString(),
                    multiSigData.isVerified());
            }
            return multiSigData.isVerified();
        }
    }

    private static class SignatureValidatorV1 implements SignatureValidator {
        @Override
        public boolean verify(byte[] document, Signature signature, Credential credential) {
            Objects.requireNonNull(document);
            SigData sigData;
            SignatureCredential signatureCredential;
            signatureCredential = getSignatureCredential(credential);
            sigData = getSigData(signature);
            sigData.setVerified(Crypto.verify(sigData.bytes(), document, signatureCredential.getKey()));
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# verify signature: {}  isVerified={}", sigData.getJsonString(), sigData.isVerified());
            }
            return sigData.isVerified();
        }
    }

    private static class MultiSigSigner implements SignatureSigner {
        @Override
        public Signature sign(byte[] document, Credential credential) {
            Objects.requireNonNull(document);

            MultiSigCredential multiSigCredential;
            multiSigCredential = getMultiSigCredential(credential);
            Map<byte[], byte[]> signatures = new HashMap<>();
            for (byte[] seed : multiSigCredential.getKeys()) {
                signatures.put(
                    Crypto.getPublicKey(seed),
                    Crypto.sign(document, seed)
                );
            }
            MultiSigData multiSigData = new MultiSigData(signatures.size());
            signatures.forEach(multiSigData::addSignature);
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# sign multi-signature: {}", multiSigData.getJsonString());
            }
            return multiSigData;
        }
    }

    private static class SignatureSignerV1 implements SignatureSigner {
        @Override
        public Signature sign(byte[] document, Credential credential) {
            Objects.requireNonNull(document);
            SignatureCredential signatureCredential = getSignatureCredential(credential);
            SigData sigData = new SigData(Crypto.sign(document, signatureCredential.getKey()));
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# sign single-signature: {}", sigData.getJsonString());
            }
            return sigData;
        }
    }
}
