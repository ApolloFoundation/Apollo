/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import org.json.simple.JSONObject;

public interface TransactionJsonSerializer {

    byte[] serialize(Transaction transaction);

    byte[] serializeUnsigned(Transaction transaction);

    JSONObject toJson(Transaction transaction);

    /**
     * The legacy JSON format of transactions, it used to be used in P2P
     *
     * @param transaction the transaction
     * @return the given transaction in JSON format
     * @deprecated Use {@link #toJson(Transaction)} method
     */
    @Deprecated
    JSONObject toLegacyJsonFormat(Transaction transaction);

    JSONObject getPrunableAttachmentJSON(Transaction transaction);
}
