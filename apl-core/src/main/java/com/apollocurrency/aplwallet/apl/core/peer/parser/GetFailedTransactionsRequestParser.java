/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.respons.GetFailedTransactionsRequest;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;

public class GetFailedTransactionsRequestParser implements JsonReqRespParser<GetFailedTransactionsRequest> {
    @SneakyThrows
    @Override
    public GetFailedTransactionsRequest parse(JSONObject json) {
        return JSON.getMapper().readValue(json.toJSONString(), GetFailedTransactionsRequest.class);
    }
}
