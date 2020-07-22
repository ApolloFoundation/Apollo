/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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
    void createCredential() {
        //GIVEN
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
    }

    @Test
    void selectValidator() {
        //GIVEN
        //WHEN
        Optional<SignatureVerifier> tool1 = SignatureToolFactory.selectValidator(1);
        Optional<SignatureVerifier> tool2 = SignatureToolFactory.selectValidator(2);
        Optional<SignatureVerifier> tool3 = SignatureToolFactory.selectValidator(3);
        //THEN
        assertTrue(tool1.isPresent());
        assertTrue(tool2.isPresent());
        assertTrue(tool3.isEmpty());
    }

    @Test
    void selectParser() {
        //GIVEN
        //WHEN
        Optional<SignatureParser> parser1 = SignatureToolFactory.selectParser(1);
        Optional<SignatureParser> parser2 = SignatureToolFactory.selectParser(2);
        Optional<SignatureParser> parser3 = SignatureToolFactory.selectParser(3);
        //THEN
        assertTrue(parser1.isPresent());
        assertTrue(parser2.isPresent());
        assertTrue(parser3.isEmpty());
    }

    @Test
    void selectBuilder() {
        //GIVEN
        //WHEN
        Optional<DocumentSigner> tool1 = SignatureToolFactory.selectBuilder(1);
        Optional<DocumentSigner> tool2 = SignatureToolFactory.selectBuilder(2);
        Optional<DocumentSigner> tool3 = SignatureToolFactory.selectBuilder(3);
        //THEN
        assertTrue(tool1.isPresent());
        assertTrue(tool2.isPresent());
        assertTrue(tool3.isEmpty());
    }
}