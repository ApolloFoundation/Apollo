/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
class MultiSigDataTest extends AbstractSigData {

    MultiSigData multiSigData;

    @BeforeEach
    void setUp() {
        multiSigData = new MultiSigData(2);
        multiSigData.addSignature(PUBLIC_KEY1, SIGNATURE1);
        multiSigData.addSignature(PUBLIC_KEY2, SIGNATURE2);
    }

    @Test
    void testOneKeyConstructor() {
        //GIVEN
        //WHEN
        MultiSigData multiSigData = new MultiSigData(PUBLIC_KEY1, SIGNATURE1);

        //THEN
        assertEquals(1, multiSigData.getThresholdParticipantCount());
        assertArrayEquals(ZERO4BYTES, multiSigData.getPayload());
        assertEquals(8 + 2 + 8 + 64, multiSigData.getSize());
    }

    @Test
    void getSize() {
        //GIVEN

        //WHEN
        int size = multiSigData.getSize();

        //THEN
        assertEquals(4 + 4 + 2 + 2 * (8 + 64), size);

    }

    @Test
    void getPayload() {
        //GIVEN
        byte[] payload = new byte[]{1, 2, 3, 4};
        multiSigData = new MultiSigData(2, payload);
        //WHEN
        byte[] rc = multiSigData.getPayload();

        //THEN
        assertArrayEquals(payload, rc);
    }

    @Test
    void getParticipantCount() {
        //GIVEN

        //WHEN
        int count = multiSigData.getThresholdParticipantCount();

        //THEN
        assertEquals(2, count);
    }

    @Test
    void getSignature() {
        //GIVEN

        //WHEN

        //THEN
        assertArrayEquals(SIGNATURE1, multiSigData.getSignature(PUBLIC_KEY1));
        assertArrayEquals(SIGNATURE2, multiSigData.getSignature(PUBLIC_KEY2));
    }

    @Test
    void testGetSignature() {
        //GIVEN
        MultiSig.KeyId keyId1 = MultiSigData.createKey(PUBLIC_KEY1);
        MultiSig.KeyId keyId2 = MultiSigData.createKey(PUBLIC_KEY2);
        //WHEN

        //THEN
        assertArrayEquals(SIGNATURE1, multiSigData.getSignature(keyId1));
        assertArrayEquals(SIGNATURE2, multiSigData.getSignature(keyId2));
    }

    @Test
    void isParticipant() {
        //GIVEN
        //WHEN

        //THEN
        assertTrue(multiSigData.isParticipant(PUBLIC_KEY1));
        assertTrue(multiSigData.isParticipant(PUBLIC_KEY2));
        assertFalse(multiSigData.isParticipant(PUBLIC_KEY3));

    }

    @Test
    void testIsParticipant() {
        //GIVEN
        MultiSig.KeyId keyId1 = MultiSigData.createKey(PUBLIC_KEY1);
        MultiSig.KeyId keyId2 = MultiSigData.createKey(PUBLIC_KEY2);
        MultiSig.KeyId keyId3 = MultiSigData.createKey(PUBLIC_KEY3);
        //WHEN

        //THEN
        assertTrue(multiSigData.isParticipant(keyId1));
        assertTrue(multiSigData.isParticipant(keyId2));
        assertFalse(multiSigData.isParticipant(keyId3));
    }

    @Test
    void getPublicKeyIdSet() {
        //GIVEN
        Set<MultiSig.KeyId> keyIdSet = Set.of(
            MultiSigData.createKey(PUBLIC_KEY1),
            MultiSigData.createKey(PUBLIC_KEY2)
        );
        //WHEN
        Set<MultiSig.KeyId> keyIdRez = multiSigData.getPublicKeyIdSet();

        //THEN
        assertEquals(keyIdSet, keyIdRez);
    }

    @Test
    void signaturesMap() {
        //GIVEN
        Map<MultiSig.KeyId, byte[]> signaturesMap = Map.of(
            MultiSigData.createKey(PUBLIC_KEY1), SIGNATURE1,
            MultiSigData.createKey(PUBLIC_KEY2), SIGNATURE2
        );
        //WHEN
        Map<MultiSig.KeyId, byte[]> signaturesRez = multiSigData.signaturesMap();

        //THEN
        assertEquals(signaturesMap, signaturesRez);
    }

    @Test
    void addSignature() {
        //GIVEN
        Set<MultiSig.KeyId> keyIdSet = Set.of(
            MultiSigData.createKey(PUBLIC_KEY1),
            MultiSigData.createKey(PUBLIC_KEY2),
            MultiSigData.createKey(PUBLIC_KEY3)
        );

        assertEquals(2, multiSigData.getThresholdParticipantCount());

        //WHEN
        multiSigData.addSignature(PUBLIC_KEY3, SIGNATURE2);

        //THEN
        assertEquals(3, multiSigData.getActualParticipantCount());
        assertEquals(2, multiSigData.getThresholdParticipantCount());
    }

    @Test
    void testAddSignature() {
        //GIVEN
        Set<MultiSig.KeyId> keyIdSet = Set.of(
            MultiSigData.createKey(PUBLIC_KEY1),
            MultiSigData.createKey(PUBLIC_KEY2),
            MultiSigData.createKey(PUBLIC_KEY3)
        );

        assertEquals(2, multiSigData.getThresholdParticipantCount());

        //WHEN
        multiSigData.addSignature(MultiSigData.createKey(PUBLIC_KEY3), SIGNATURE2);

        //THEN
        assertEquals(3, multiSigData.getActualParticipantCount());
        assertEquals(2, multiSigData.getThresholdParticipantCount());
    }

    @Test
    void testParser() {
        //GIVEN
        byte[] signature = multiSigData.bytes();
        ByteBuffer byteBuffer = ByteBuffer.wrap(signature);
        MultiSigData.Parser parser = new MultiSigData.Parser();
        //WHEN
        Signature sigData = parser.parse(byteBuffer);

        //THEN
        assertEquals(4 + 4 + 2 + 2 * (8 + 64), parser.calcDataSize(2));

        checkData(sigData);
    }

    @Test
    void testParser2() {
        //GIVEN
        String sigStr = "4d53494700000000020039dc2e813bb45ff0c9e2d8f29da5c5c9d9246cabd6ef9046e3bbec6980ac7b23ebdb86e3b2254f0cf3694dae2acebcc2e474a1f7053a56fb7441e8d1b543d6ef1355732dc3f05b0e98e3de5568034d3367242a496b61a370198b652667051ec4fc4fdb7d5ffcbf7f11f5da7f665e230278bd9416ffd4010e951a98e04b2301450dd112b3aca6b2f1636c21a28936d5e8";
        byte[] signature = Convert.parseHexString(sigStr);
        byte[] pkId1 = Convert.parseHexString("39dc2e813bb45ff0");
        byte[] pkId2 = Convert.parseHexString("98e3de5568034d33");

        ByteBuffer byteBuffer = ByteBuffer.wrap(signature);
        MultiSigData.Parser parser = new MultiSigData.Parser();
        //WHEN
        MultiSigData sigData = (MultiSigData) parser.parse(byteBuffer);

        //THEN
        assertEquals(4 + 4 + 2 + 2 * (8 + 64), parser.calcDataSize(2));
        assertEquals(2, sigData.getThresholdParticipantCount());
        assertTrue(sigData.getPublicKeyIdSet().contains(new MultiSigData.KeyIdImpl(pkId1)));
        assertTrue(sigData.getPublicKeyIdSet().contains(new MultiSigData.KeyIdImpl(pkId2)));


    }

    private void checkData(Signature sigData) {
        assertTrue(sigData instanceof MultiSigData);
        MultiSigData newSigData = (MultiSigData) sigData;
        assertEquals(multiSigData.getThresholdParticipantCount(), newSigData.getThresholdParticipantCount());
        assertEquals(multiSigData.getPublicKeyIdSet(), newSigData.getPublicKeyIdSet());
        assertEquals(multiSigData.getActualParticipantCount(), newSigData.getActualParticipantCount());
        assertEquals(multiSigData.getSize(), newSigData.getSize());
    }

}