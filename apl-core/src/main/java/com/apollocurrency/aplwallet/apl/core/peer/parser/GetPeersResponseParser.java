/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.response.GetPeersResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

public class GetPeersResponseParser implements JsonReqRespParser<GetPeersResponse> {
    @Override
    public GetPeersResponse parse(JSONObject json) {
        return JSON.getMapper().convertValue(json, GetPeersResponse.class);
    }
}
