/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import org.json.simple.JSONObject;

public interface TransactionSerializer {

    JSONObject toJson(Transaction transaction);

    JSONObject getPrunableAttachmentJSON(Transaction transaction);
}
