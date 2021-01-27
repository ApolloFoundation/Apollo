/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
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
    private static final List<String> DEFAULT_MULTISIG_V3_PARAMS = List.of("elliptic-curve=Curve25519", "pk-size=32");

    MultiSigData multiSigData;

    @BeforeEach
    void setUp() {
        multiSigData = new MultiSigData(2, DEFAULT_MULTISIG_V3_PARAMS, new MultiSigData.Parser());
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
        assertEquals(Collections.emptyList(), multiSigData.getParams());
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
    void getParams() {
        //GIVEN
        multiSigData = new MultiSigData(2, DEFAULT_MULTISIG_V3_PARAMS, new MultiSigData.Parser());
        //WHEN
        //THEN
        assertEquals(DEFAULT_MULTISIG_V3_PARAMS, multiSigData.getParams());
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
    void testParserV3() {
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

    private void checkData(Signature sigData) {
        assertTrue(sigData instanceof MultiSigData);
        MultiSigData newSigData = (MultiSigData) sigData;
        assertEquals(multiSigData.getThresholdParticipantCount(), newSigData.getThresholdParticipantCount());
        assertEquals(multiSigData.getPublicKeyIdSet(), newSigData.getPublicKeyIdSet());
        assertEquals(multiSigData.getActualParticipantCount(), newSigData.getActualParticipantCount());
        assertEquals(multiSigData.getSize(), newSigData.getSize());
    }

}