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

    JSONObject toJsonOld(Transaction transaction);

    JSONObject getPrunableAttachmentJSON(Transaction transaction);
}
