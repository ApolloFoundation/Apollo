/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import javax.enterprise.inject.Vetoed;

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
@Vetoed
@Slf4j
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class SignatureToolFactory {

    private static final SignatureVerifier[] validators = new SignatureVerifier[]
        {new SignatureVerifierV1(), new MultiSigVerifierImpl()};

    private static final SignatureParser[] parsers = new SignatureParser[]
        {new SigData.Parser(), new MultiSigData.Parser()};

    private static final DocumentSigner[] docSigners = new DocumentSigner[]
        {new DocumentSignerV1(), new MultiSigSigner()};

    public static Signature createSignature(byte[] signature) {
        return new SigData(Objects.requireNonNull(signature));
    }

    public static Credential createCredential(int transactionVersion, byte[]... keys) {
        Objects.requireNonNull(keys);
        switch (transactionVersion) {
            case 0:
            case 1:
                return new SignatureCredential(keys[0]);
            case 2:
                return new MultiSigCredential(keys.length, keys);
            default:
                throw new UnsupportedTransactionVersion("Can't crate credential a given transaction version: " + transactionVersion);
        }
    }

    public static Optional<SignatureVerifier> selectValidator(int transactionVersion) {
        return selectTool(transactionVersion, validators);
    }

    public static Optional<SignatureParser> selectParser(int transactionVersion) {
        return selectTool(transactionVersion, parsers);
    }

    public static Optional<DocumentSigner> selectSigner(int transactionVersion) {
        return selectTool(transactionVersion, docSigners);
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

    @Vetoed
    static class MultiSigVerifierImpl implements SignatureVerifier {

        @Override
        public boolean verify(byte[] document, Signature signature, Credential credential) {
            Objects.requireNonNull(document);
            MultiSigData multiSigData;
            MultiSigCredential multiSigCredential;
            multiSigCredential = getMultiSigCredential(credential);
            multiSigData = getMultiSigData(signature);
            if (multiSigCredential.getThreshold() > multiSigData.getThresholdParticipantCount()) {
                return false;
            }

            Set<byte[]> verifiedPks = new HashSet<>();
            for (byte[] pk : multiSigCredential.getKeys()) {
                if (multiSigData.isParticipant(pk)) {
                    if (verifiedPks.contains(pk)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Pk already verified, pk={}", Convert.toHexString(pk));
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
                    multiSigData.getHexString(),
                    multiSigData.isVerified());
            }
            return multiSigData.isVerified();
        }
    }

    @Vetoed
    static class SignatureVerifierV1 implements SignatureVerifier {
        @Override
        public boolean verify(byte[] document, Signature signature, Credential credential) {
            Objects.requireNonNull(document);
            SigData sigData;
            SignatureCredential signatureCredential;
            signatureCredential = getSignatureCredential(credential);
            sigData = getSigData(signature);
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# verify signature={} publicKey={} document={}",
                    Convert.toHexString(sigData.bytes()),
                    Convert.toHexString(signatureCredential.getKey()),
                    Convert.toHexString(document));
            }
            sigData.setVerified(
                Crypto.verify(sigData.bytes(), document, signatureCredential.getKey())
            );
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# verify isVerified={} signature={}", sigData.isVerified(), sigData.getHexString());
            }
            return sigData.isVerified();
        }
    }

    @Vetoed
    static class MultiSigSigner implements DocumentSigner {
        @Override
        public Signature sign(byte[] document, Credential credential) {
            Objects.requireNonNull(document);
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# sign credential: {}", credential);
            }
            MultiSigCredential multiSigCredential = getMultiSigCredential(credential);
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
                log.trace("#MULTI_SIG# sign multi-signature: {}", multiSigData.getHexString());
            }
            return multiSigData;
        }

        @Override
        public boolean isCanonical(Signature signature) {
            MultiSigData multiSigData = getMultiSigData(Objects.requireNonNull(signature));
            final boolean[] rc = {multiSigData.getThresholdParticipantCount() > 0};
            if (rc[0]) {
                multiSigData.signaturesMap().
                    forEach((keyId, bytes) -> rc[0] = rc[0] && Crypto.isCanonicalSignature(bytes));
            }
            return rc[0];
        }
    }

    @Vetoed
    static class DocumentSignerV1 implements DocumentSigner {
        @Override
        public Signature sign(byte[] document, Credential credential) {
            Objects.requireNonNull(document);
            SignatureCredential signatureCredential = getSignatureCredential(credential);
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# crypto sign keySeed={} document={}",
                    Convert.toHexString(signatureCredential.getKey()),
                    Convert.toHexString(document));
            }
            SigData sigData = new SigData(
                Crypto.sign(document, signatureCredential.getKey())
            );
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# crypto sign single-signature: {}", sigData.getHexString());
            }
            return sigData;
        }

        @Override
        public boolean isCanonical(Signature signature) {
            Objects.requireNonNull(signature);
            return Crypto.isCanonicalSignature(signature.bytes());
        }
    }
}
