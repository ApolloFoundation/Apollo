/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import org.json.simple.JSONObject;

public interface TransactionJsonSerializer {

    byte[] serialize(Transaction transaction);

    byte[] serializeUnsigned(Transaction transaction);

    JSONObject toJson(Transaction transaction);

    /**
     * Only use for tests.
     *
     * @param transaction the given transaction
     * @return serialized transaction as a JSONObject
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    JSONObject toLegacyJsonFormat(Transaction transaction);

    JSONObject getPrunableAttachmentJSON(Transaction transaction);
}
