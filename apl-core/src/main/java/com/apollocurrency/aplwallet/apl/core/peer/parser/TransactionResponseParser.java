/*
 * Copyright (c) 2020-2021. Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.apl.core.peer.respons.Transaction;
import org.json.simple.JSONObject;

public class TransactionResponseParser implements ReqRespParser<Transaction> {


    @Override
    public Transaction parse(JSONObject json) {
        return null;
    }
}
