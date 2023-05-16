/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
class SigDataTest extends AbstractSigData {

    SigData sigData;

    @BeforeEach
    void setUp() {
        sigData = new SigData(SIGNATURE1);
    }

    @Test
    void getSize() {
        //GIVEN

        //WHEN
        int size = sigData.getSize();

        //THEN
        assertEquals(Signature.ECDSA_SIGNATURE_SIZE, size);
    }

    @Test
    void getJsonString() {
        //GIVEN
        //WHEN
        String jsonString = sigData.getHexString();

        //THEN
        assertEquals(Convert.toHexString(sigData.bytes()), jsonString);
    }

    @Test
    void getSignature() {
        //GIVEN

        //WHEN

        //THEN
        assertArrayEquals(SIGNATURE1, sigData.bytes());
    }

    @Test
    void testParser() {
        //GIVEN
        byte[] signature = sigData.bytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(signature);
        SigData.Parser parser = new SigData.Parser();
        //WHEN
        Signature sigData = parser.parse(byteBuffer);

        //THEN
        assertEquals(Signature.ECDSA_SIGNATURE_SIZE, parser.calcDataSize(1));
        assertEquals(Signature.ECDSA_SIGNATURE_SIZE, parser.calcDataSize(Integer.MAX_VALUE));

        checkData(sigData);
    }


    @Test
    void testBytesParser() {
        //GIVEN
        SigData.Parser parser = new SigData.Parser();
        //WHEN
        byte[] actualBytes = parser.bytes(sigData);

        //THEN
        assertArrayEquals(sigData.bytes(), actualBytes);
    }

    private void checkData(Signature sigData) {
        assertTrue(sigData instanceof SigData);
        SigData newSigData = (SigData) sigData;
        assertArrayEquals(this.sigData.bytes(), newSigData.bytes());
        assertEquals(this.sigData.getSize(), newSigData.getSize());
    }

}