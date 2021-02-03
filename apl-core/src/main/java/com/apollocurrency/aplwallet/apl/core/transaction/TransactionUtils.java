/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.json.simple.JSONObject;

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

}
