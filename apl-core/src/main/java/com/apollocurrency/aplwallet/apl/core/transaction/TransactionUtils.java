/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public class TransactionUtils {
    public static boolean convertAppendixToString(StringBuilder builder, Appendix appendix) {
        if (appendix != null) {
            JSONObject json = appendix.getJSONObject();
            if (json != null) {
                builder.append(json.toJSONString());
                return true;
            }
        }
        return false;
    }

    public static byte getVersionSubtypeByte(Transaction transaction) {
        return (byte) ((transaction.getVersion() << 4) & 0xf0 | transaction.getType().getSpec().getSubtype() & 0x0f);
    }

    public static int getTransactionFlags(Transaction transaction) {
        int flags = 0;
        int position = 1;
        if (transaction.getMessage() != null) {
            flags |= position;
        }
        position <<= 1;
        if (transaction.getEncryptedMessage() != null) {
            flags |= position;
        }
        position <<= 1;
        if (transaction.getPublicKeyAnnouncement() != null) {
            flags |= position;
        }
        position <<= 1;
        if (transaction.getEncryptToSelfMessage() != null) {
            flags |= position;
        }
        position <<= 1;
        if (transaction.getPhasing() != null) {
            flags |= position;
        }
        position <<= 1;
        if (transaction.getPrunablePlainMessage() != null) {
            flags |= position;
        }
        position <<= 1;
        if (transaction.getPrunableEncryptedMessage() != null) {
            flags |= position;
        }
        return flags;
    }

    /**
     * Calculates the full size of transaction using {@link Transaction} itself
     * and it's default serialization size with appendices typically obtained using {@link com.apollocurrency.aplwallet.apl.core.transaction.common.TxSerializer}
     * @param tx transaction with appendices to calculate full size
     * @param txStandardByteSize default serialized size of given transaction obtained using {@link com.apollocurrency.aplwallet.apl.core.transaction.common.TxSerializer}
     * @return full size of the given transaction
     */
    public static int calculateFullSize(Transaction tx, int txStandardByteSize) {
        //byteLength acts here as tx size with appendices default size, to get tx size with appendices full size we need to substract default size and add full size
        return txStandardByteSize + tx.getAppendages()
            .stream()
            .mapToInt(app-> app.getFullSize() - app.getSize())
            .sum();
    }


    public static byte[] calculateFullHash(byte[] unsignedTxBytes, byte[] signatureBytes) {
        //calculate transaction Id and full hash
        byte[] signatureHash = Crypto.sha256().digest(signatureBytes);
        return calculateUnsignedFullHash(unsignedTxBytes, signatureHash);
    }

    public static byte[] calculateUnsignedFullHash(byte[] unsignedTxBytes, byte[] signatureHash) {
        //calculate transaction Id and full hash
        MessageDigest digest = Crypto.sha256();
        digest.update(unsignedTxBytes);
        byte[] fullHash = digest.digest(signatureHash);
        return fullHash;
    }

    public static byte[] getUnsignedBytes(byte[] data, int version, int signatureSize) {
        Objects.requireNonNull(data);
        if (version <= 1) {
            return zeroV1Signature(Arrays.copyOf(data, data.length));
        } else if (version == 2) {
            return Arrays.copyOf(data, data.length - signatureSize);
        } else
            throw new UnsupportedTransactionVersion();
    }

    /**
     * The transaction V2 header size, it doesn't contain the signature size
     *
     * @return the transaction V2 header size
     */
    public static int txV2HeaderSize() {
        return 1 + 1 + 4 + 2 + 32 + 8 + 8 + 8 + 32 + 4 + 4 + 8;
    }

    public static int signatureV1Offset() {
        return 1 + 1 + 4 + 2 + 32 + 8 + 8 + 8 + 32;
    }

    public static byte[] zeroV1Signature(byte[] data) {
        int start = signatureV1Offset();
        for (int i = start; i < start + Signature.ECDSA_SIGNATURE_SIZE; i++) {
            data[i] = 0;
        }
        return data;
    }
}
