/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.sharding;

import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MerkleHash {
    /**
     * Hash value as byte array.
     */
    private byte[] value;

    public MerkleHash() {
    }

    public MerkleHash(byte[] hash) {
        this.value = hash;
    }

    /**
     * Create a MerkleHash from an array of bytes.
     *
     * @param buffer of bytes
     * @return a MerkleHash
     */
    public static MerkleHash create(byte[] buffer) {
        MerkleHash hash = new MerkleHash();
        hash.computeHash(buffer);
        return hash;
    }

    /**
     * Create a MerkleHash from a string. The string needs
     * first to be transformed in a UTF8 sequence of bytes.
     * Used for leaf hashes.
     *
     * @param buffer string
     * @return a MerkleHash
     */
    public static MerkleHash create(String buffer) {
        return create(buffer.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Create a MerkleHash from two MerkleHashes by concatenation
     * of the byte arrays. Used for internal nodes.
     *
     * @param left  subtree hash
     * @param right subtree hash
     * @return a MerkleHash
     */
    public static MerkleHash create(MerkleHash left, MerkleHash right) {
        return create(concatenate(left.getValue(), right.getValue()));
    }

    /**
     * Get the byte value of a MerkleHash.
     *
     * @return an array of bytes
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * Compare the MerkleHash with a given byte array.
     *
     * @param hash as byte array
     * @return boolean
     */
    public boolean equals(byte[] hash) {
        return Arrays.equals(this.value, hash);
    }

    /**
     * Compare the MerkleHash with a given MerkleHash.
     *
     * @param hash as MerkleHash
     * @return boolean
     */
    public boolean equals(MerkleHash hash) {
        boolean result = false;
        if (hash != null) {
            result = Arrays.equals(this.value, hash.getValue());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    /**
     * Encode in hexadecimal format the MerkleHash.
     *
     * @return the string encoding of MerkleHash.
     */
    @Override
    public String toString() {
        return Convert.toHexString(this.value);
    }

    /**
     * Compute SHA256 hash of a byte array.
     *
     * @param buffer of bytes
     */
    private void computeHash(byte[] buffer) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            this.value = digest.digest(buffer);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * Concatenate two array of bytes.
     *
     * @param a is the first array
     * @param b is the second array
     * @return a byte array
     */
    public static byte[] concatenate(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}