/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.multisig;

import java.util.Set;

/**
 * Multi-signature is a digital signature scheme.
 *
 * @author andrii.zinchenko@firstbridge.io
 */
public interface MultiSig {

    byte[] getPayload();

    /**
     * @return the participant count of the multisig
     */
    int participantCount();

    /**
     * Return a signature a given number of a participant
     *
     * @param participantNumber the participant number
     * @return a signature or null if specified participant number is out of bound
     */
    byte[] getSignature(int participantNumber);

    /**
     * Returns the index in the signature list
     *
     * @param publicKey the public key
     * @return the index in the signature list or -1 if the specified pk was not used in the multi-signature
     */
    int findParticipant(byte[] publicKey);

    /**
     * Return set of the public key identifiers. Id is the first 4 bytes of the public key
     *
     * @return set of the public key identifiers
     */
    Set<byte[]> getPublicKeyIdSet();

}
