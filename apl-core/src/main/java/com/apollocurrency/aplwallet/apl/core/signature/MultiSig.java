/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import org.web3j.rlp.RlpType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Multi-signature is a digital signature scheme.
 *
 * @author andrii.zinchenko@firstbridge.io
 */
public interface MultiSig extends Signature {

    byte[] getPayload();

    default List<RlpType> getPayloads(){
        return List.of();
    }

    /**
     * @return the participant count of the multisig
     * It's a min count of participants that needs signing the document
     */
    int getThresholdParticipantCount();

    /**
     * @return the count of participants
     */
    int getActualParticipantCount();

    /**
     * Return a signature a given public key of a participant
     *
     * @param publicKey the public key
     * @return a signature or null if specified participant number is out of bound
     */
    byte[] getSignature(byte[] publicKey);

    /**
     * Return a signature a given public key id of a participant
     *
     * @param publicKeyId the public key id
     * @return a signature or null if specified participant number is out of bound
     */
    byte[] getSignature(KeyId publicKeyId);

    /**
     * Returns {@code true } if the public key id presents in the signature list
     *
     * @param publicKey the public key
     * @return {@code true} if the public key id presents in the signature list or {@code false} if the specified pk was not used in the multi-signature
     */
    boolean isParticipant(byte[] publicKey);

    /**
     * Returns {@code true } if the public key id presents in the signature list
     *
     * @param publicKeyId the public key id
     * @return {@code true} if the public key id presents in the signature list or {@code false} if the specified pk was not used in the multi-signature
     */
    boolean isParticipant(KeyId publicKeyId);

    /**
     * Return set of the public key identifiers. Id is the first 4 bytes of the public key
     *
     * @return set of the public key identifiers
     */
    Set<KeyId> getPublicKeyIdSet();


    /**
     * Return the map of signature
     *
     * @return
     */
    Map<KeyId, byte[]> signaturesMap();

    void addSignature(byte[] keyId, byte[] signature);

    void addSignature(KeyId keyId, byte[] signature);

    interface KeyId {
        int KEY_LENGTH = 8;

        byte[] getKey();
    }

}
