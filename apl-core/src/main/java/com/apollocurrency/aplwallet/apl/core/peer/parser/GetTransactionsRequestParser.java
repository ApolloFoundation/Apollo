/*
 *  Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.request.GetTransactionsRequest;
import com.apollocurrency.aplwallet.apl.util.JSON;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;

import javax.validation.Valid;

public class GetTransactionsRequestParser implements JsonReqRespParser<GetTransactionsRequest> {
    @SneakyThrows
    @Override
    public @Valid GetTransactionsRequest parse(JSONObject json) {
        return JSON.getMapper().readValue(json.toJSONString(), GetTransactionsRequest.class);
    }
}
