/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.request.ProcessTransactionsRequest;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;

public class ProcessTransactionsRequestParser implements JsonReqRespParser<ProcessTransactionsRequest> {
    @SneakyThrows
    @Override
    public ProcessTransactionsRequest parse(JSONObject json) {
        return JSON.getMapper().readValue(json.toJSONString(), ProcessTransactionsRequest.class);
    }
}
