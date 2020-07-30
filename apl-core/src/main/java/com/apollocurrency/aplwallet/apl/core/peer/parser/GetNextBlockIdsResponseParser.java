/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.apl.core.peer.respons.GetNextBlockIdsResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.json.simple.JSONObject;

public class GetNextBlockIdsResponseParser implements PeerResponseParser<GetNextBlockIdsResponse> {
    @Override
    public GetNextBlockIdsResponse parse(JSONObject json) throws JsonProcessingException {
        return JSON.getMapper().readValue(json.toJSONString(), GetNextBlockIdsResponse.class);
    }
}
