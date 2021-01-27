/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
class SignatureToolFactoryTest extends AbstractSigData {

    @BeforeEach
    void setUp() {
    }

    @Test
    void createSignature() {
        Signature signature = SignatureToolFactory.createSignature(SIGNATURE1);
        assertTrue(signature instanceof SigData);
    }

    @Test
    void createCredentialAndValidate() {
        //GIVEN
        KeyValidator keyValidator1 = new KeyValidatorImpl(PUBLIC_KEY1);
        KeyValidator keyValidator2 = new KeyValidatorImpl(PUBLIC_KEY2);
        MultiSigCredential multiSigCredential1 = new MultiSigCredential(1, PUBLIC_KEY1);
        MultiSigCredential multiSigCredential2 = new MultiSigCredential(PUBLIC_KEY1);

        //WHEN
        Credential credential0 = SignatureToolFactory.createCredential(0, PUBLIC_KEY1);
        Credential credential01 = SignatureToolFactory.createCredential(0, PUBLIC_KEY1, PUBLIC_KEY2);
        Credential credential1 = SignatureToolFactory.createCredential(1, PUBLIC_KEY1);
        Credential credential2 = SignatureToolFactory.createCredential(2, PUBLIC_KEY1, PUBLIC_KEY2);

        //THEN
        assertTrue(credential0 instanceof SignatureCredential);
        assertTrue(credential1 instanceof SignatureCredential);
        assertTrue(credential2 instanceof MultiSigCredential);

        assertEquals(credential0, credential01);
        assertEquals(credential0.toString(), credential01.toString());

        assertTrue(credential0.validateCredential(keyValidator1));
        assertTrue(credential01.validateCredential(keyValidator1));
        assertFalse(credential01.validateCredential(keyValidator2));

        assertFalse(credential2.validateCredential(keyValidator2));

        assertEquals(multiSigCredential1, multiSigCredential2);
        assertEquals(multiSigCredential1.toString(), multiSigCredential2.toString());

    }

    static class KeyValidatorImpl implements KeyValidator {
        private final byte[] key;

        public KeyValidatorImpl(byte[] key) {
            this.key = key;
        }

        @Override
        public boolean validate(byte[] publicKey) {
            return Arrays.equals(key, publicKey);
        }
    }

    @Test
    void selectValidator() {
        assertTrue(SignatureToolFactory.selectValidator(1).isPresent());
        assertTrue(SignatureToolFactory.selectValidator(2).isPresent());
        assertTrue(SignatureToolFactory.selectValidator(3).isPresent());
        assertTrue(SignatureToolFactory.selectValidator(4).isEmpty());

    }

    @Test
    void selectParser() {
        assertTrue(SignatureToolFactory.createParser(1).isPresent());
        assertTrue(SignatureToolFactory.createParser(2).isPresent());
        assertTrue(SignatureToolFactory.createParser(3).isPresent());
        assertTrue(SignatureToolFactory.createParser(4).isEmpty());
    }

    @Test
    void selectBuilder() {
        assertTrue(SignatureToolFactory.selectBuilder(1).isPresent());
        assertTrue(SignatureToolFactory.selectBuilder(2).isPresent());
        assertTrue(SignatureToolFactory.selectBuilder(3).isPresent());
        assertTrue(SignatureToolFactory.selectBuilder(4).isEmpty());
    }

    @Test
    void testSigningRoutine() {
        //GIVEN
        String secretPhrase = "topSecret";
        byte[] document = "The document".getBytes();
        Credential signCredential = SignatureToolFactory.createCredential(1, Crypto.getKeySeed(secretPhrase));
        Credential verifyCredential = SignatureToolFactory.createCredential(1, Crypto.getPublicKey(secretPhrase));
        DocumentSigner documentSigner = SignatureToolFactory.selectBuilder(1).get();
        SignatureVerifier signatureVerifier = SignatureToolFactory.selectValidator(1).get();

        //WHEN
        Signature signature = documentSigner.sign(document, signCredential);
        boolean rc = signatureVerifier.verify(document, signature, verifyCredential);

        //THEN
        assertNotNull(signature);
        assertTrue(signature instanceof SigData);
        assertEquals(Signature.ECDSA_SIGNATURE_SIZE, signature.getSize());
        assertTrue(rc);
        assertTrue(signature.isVerified());
    }

    @Test
    void testMultiSigningRoutine() {
        //GIVEN
        String secretPhrase1 = "topSecret1";
        String secretPhrase2 = "topSecret2";
        byte[] document = "The document".getBytes();
        Credential signCredential = SignatureToolFactory.createCredential(2, Crypto.getKeySeed(secretPhrase1), Crypto.getKeySeed(secretPhrase2));
        Credential verifyCredential = SignatureToolFactory.createCredential(2, Crypto.getPublicKey(secretPhrase2), Crypto.getPublicKey(secretPhrase1));
        DocumentSigner documentSigner = SignatureToolFactory.selectBuilder(2).get();
        SignatureVerifier signatureVerifier = SignatureToolFactory.selectValidator(2).get();

        //WHEN
        Signature signature = documentSigner.sign(document, signCredential);
        boolean rc = signatureVerifier.verify(document, signature, verifyCredential);

        //THEN
        assertNotNull(signature);
        assertTrue(signature instanceof MultiSigData);
        assertEquals(2, ((MultiSigData) signature).getActualParticipantCount());
        assertEquals(2, ((MultiSigData) signature).getPublicKeyIdSet().size());
        assertTrue(rc);
        assertTrue(signature.isVerified());
    }
}