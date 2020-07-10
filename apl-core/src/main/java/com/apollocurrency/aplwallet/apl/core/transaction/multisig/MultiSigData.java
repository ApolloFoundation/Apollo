/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.multisig;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

/**
 * Multi-signature is a digital signature scheme.
 * It's a collection of distinct signatures from all users (participants)
 *
 * @author andrii.zinchenko@firstbridge.io
 */
public class MultiSigData implements MultiSig {

    /**
     * Parse the byte array and build the multisig object
     *
     * @param buffer input data array
     * @return the multisig object
     */
    public static MultiSigData parse(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
/*
        this.addressScope = AddressScope.from(buffer.get());
        this.childCount = buffer.getShort();
*/

        return null;
    }

    @Override
    public byte[] getPayload() {
        return new byte[0];
    }

    /**
     * @return the participant count of the multisig
     */
    public int participantCount() {
        return 0;
    }

    /**
     * Return a signature a given number of a participant
     *
     * @param participantNumber the participant number
     * @return a signature or null if specified participant number is out of bound
     */
    public byte[] getSignature(int participantNumber) {
        return null;
    }

    /**
     * Returns the index in the signature list
     *
     * @param publicKey the public key
     * @return the index in the signature list or -1 if the specified pk was not used in the multi-signature
     */
    public int findParticipant(byte[] publicKey) {
        return 0;
    }

    /**
     * Return set of the public key identifiers. Id is the first 4 bytes of the public key
     *
     * @return set of the public key identifiers
     */
    public Set<byte[]> getPublicKeyIdSet() {
        return null;
    }

}
