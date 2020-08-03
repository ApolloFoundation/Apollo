/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static com.apollocurrency.aplwallet.apl.core.signature.SignatureParser.SIGNATURE_FIELD_NAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        assertEquals(Signature.ECDSA_SIGNATURE_SIZE, sigData.getSize());
    }

    @Test
    void getJsonObject() {
        //GIVEN
        //WHEN
        JSONObject jsonObject = sigData.getJsonObject();

        //THEN
        assertNotNull(jsonObject.get(SIGNATURE_FIELD_NAME));
        byte[] key1 = Convert.parseHexString((String) (jsonObject.get(SIGNATURE_FIELD_NAME)));
        assertArrayEquals(SIGNATURE1, key1);

    }

    @Test
    void getJsonString() {
        //GIVEN
        //WHEN
        String jsonString = sigData.getJsonString();

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
    void testJsonParser() {
        //GIVEN
        JSONObject jsonObject = sigData.getJsonObject();

        SigData.Parser parser = new SigData.Parser();
        //WHEN
        Signature sigData = parser.parse(jsonObject);

        //THEN
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