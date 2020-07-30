/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.apl.core.peer.respons.GetPeersResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.simple.JSONObject;

public class GetPeersResponseParser implements PeerResponseParser<GetPeersResponse> {
    @Override
    public GetPeersResponse parse(JSONObject json) throws JsonProcessingException {
        return JSON.getMapper().readValue(json.toJSONString(), GetPeersResponse.class);
    }
}
